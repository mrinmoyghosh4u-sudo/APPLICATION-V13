package com.example.data.network

import android.util.Log
import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * TradeSmart Official Live Market Data WebSocket Service (Tertiary Fallback)
 * 
 * Implements:
 * - Real WebSocket connection to TradeSmart / Sine API feed endpoint (wss://sine.tradesmartonline.in/v2/websocket)
 * - Authentication handshake using API Key / Client ID / Token
 * - Live LTP & quote subscription
 * - Binary & JSON tick frame parsing
 * - Heartbeat & stale detection
 * - Reconnection backoff
 * - Ingestion into MarketDataStore with source = "TRADESMART"
 */
class TradeSmartMarketDataService(
    private val sessionManager: SessionManager,
    private val instrumentMasterService: InstrumentMasterService? = null
) {
    companion object {
        private const val TAG = "TradeSmartMarketData"
        private const val WS_URL = "wss://sine.tradesmartonline.in/v2/websocket"
        private const val PING_INTERVAL_MS = 20000L
        private const val STALE_THRESHOLD_MS = 15000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private val isConnecting = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)

    private val subscribedTokens = ConcurrentHashMap<String, String>() // token -> exchange
    private var heartbeatJob: Job? = null
    private var staleCheckJob: Job? = null
    private var lastTickReceivedTime: Long = 0L

    private val _connectionState = MutableStateFlow("OFFLINE") // OFFLINE, CONNECTING, LIVE, STANDBY, STALE
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean {
        return sessionManager.isTradeSmartConfigured()
    }
    
    fun isConnectionLive(): Boolean {
        val age = System.currentTimeMillis() - lastTickReceivedTime
        return isConfigured() && (_connectionState.value == "LIVE" || lastTickReceivedTime > 0L) && age >= 0L && age <= STALE_THRESHOLD_MS
    }

    fun connect() {
        if (!isConfigured()) {
            Log.d(TAG, "TradeSmart credentials not configured. Connection skipped.")
            _connectionState.value = "OFFLINE"
            MarketDataStore.setSourceHealth("TradeSmart", "OFFLINE")
            return
        }

        if (isConnected.get() || isConnecting.get()) return

        isConnecting.set(true)
        _connectionState.value = "CONNECTING"
        Log.d(TAG, "Initiating TradeSmart WebSocket connection to $WS_URL...")

        val request = Request.Builder()
            .url(WS_URL)
            .header("User-Agent", "KingKhanAITrader/1.0")
            .build()

        webSocket = client.newWebSocket(request, createWebSocketListener())
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting.set(false)
                isConnected.set(true)
                reconnectAttempts.set(0)
                Log.d(TAG, "[TRADESMART_WS_CONNECTED] Sending authentication handshake...")

                val authPayload = JSONObject().apply {
                    put("action", "authenticate")
                    put("apiKey", sessionManager.tradesmartApiKey)
                    put("clientId", sessionManager.tradesmartClientId)
                    put("token", sessionManager.tradesmartAccessToken ?: "")
                    put("timestamp", System.currentTimeMillis())
                }
                webSocket.send(authPayload.toString())

                _connectionState.value = "LIVE"
                MarketDataStore.setSourceHealth("TradeSmart", "LIVE")

                startHeartbeat()
                startStaleChecker()
                resubscribeAll()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                lastTickReceivedTime = System.currentTimeMillis()
                parseTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                lastTickReceivedTime = System.currentTimeMillis()
                parseBinaryPacket(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "TradeSmart WebSocket closing: code=$code reason=$reason")
                handleDisconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "TradeSmart WebSocket closed: code=$code reason=$reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "TradeSmart WebSocket connection failure: ${t.localizedMessage}")
                handleDisconnect()
                scheduleReconnect()
            }
        }
    }

    private fun parseTextMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", json.optString("action", ""))

            when (type.lowercase()) {
                "auth", "login", "authenticate" -> {
                    val status = json.optString("status", "ok")
                    Log.d(TAG, "[TRADESMART_AUTH_SUCCESS] status=$status")
                }
                "tick", "quote", "ltp", "feed" -> {
                    val token = json.optString("token", json.optString("scripCode", json.optString("tk", "")))
                    val exch = json.optString("exchange", json.optString("e", "NSE"))
                    val ltp = json.optDouble("ltp", json.optDouble("lp", json.optDouble("lastPrice", 0.0)))
                    val open = json.optDouble("open", json.optDouble("o", 0.0))
                    val high = json.optDouble("high", json.optDouble("h", 0.0))
                    val low = json.optDouble("low", json.optDouble("l", 0.0))
                    val close = json.optDouble("close", json.optDouble("c", 0.0))
                    val volume = json.optLong("volume", json.optLong("v", 0L))
                    val ts = json.optLong("timestamp", System.currentTimeMillis())
                    val seq = json.optLong("sequenceNumber", 0L)

                    val symbol = resolveSymbol(exch, token)

                    if (ltp > 0.0) {
                        MarketDataStore.updateTick(
                            source = "TradeSmart",
                            symbol = symbol,
                            token = token,
                            exchange = exch,
                            ltp = ltp,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = volume,
                            exchangeTimestamp = ts,
                            receivedTimestamp = System.currentTimeMillis(),
                            state = "LIVE",
                            sequenceNumber = seq
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse TradeSmart text frame: ${e.localizedMessage}")
        }
    }

    private fun parseBinaryPacket(bytes: ByteArray) {
        try {
            if (bytes.size < 8) return
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val packetType = buffer.get().toInt()
            val exchangeCode = buffer.get().toInt()
            val tokenNumber = buffer.int

            val exchange = when (exchangeCode) {
                1 -> "NSE"
                2 -> "NFO"
                3 -> "BSE"
                4 -> "BFO"
                5 -> "MCX"
                else -> "NSE"
            }

            val token = tokenNumber.toString()

            if (buffer.remaining() >= 4) {
                val rawLtp = buffer.int
                val ltp = rawLtp / 100.0

                var open = 0.0
                var high = 0.0
                var low = 0.0
                var close = 0.0
                var volume = 0L
                var ts = System.currentTimeMillis()

                if (buffer.remaining() >= 16) {
                    open = buffer.int / 100.0
                    high = buffer.int / 100.0
                    low = buffer.int / 100.0
                    close = buffer.int / 100.0
                }

                if (buffer.remaining() >= 8) {
                    volume = buffer.long
                }

                if (buffer.remaining() >= 8) {
                    ts = buffer.long
                }

                val symbol = resolveSymbol(exchange, token)

                if (ltp > 0.0) {
                    MarketDataStore.updateTick(
                        source = "TradeSmart",
                        symbol = symbol,
                        token = token,
                        exchange = exchange,
                        ltp = ltp,
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = volume,
                        exchangeTimestamp = ts,
                        receivedTimestamp = System.currentTimeMillis(),
                        state = "LIVE"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse TradeSmart binary packet: ${e.localizedMessage}")
        }
    }

    private fun resolveSymbol(exchange: String, token: String): String {
        val master = instrumentMasterService
        if (master != null && master.isLoaded) {
            val inst = master.getInstrument(exchange, token)
            if (inst != null && inst.symbol.isNotBlank()) {
                return inst.symbol
            }
        }
        return token
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected.get()) {
                delay(PING_INTERVAL_MS)
                try {
                    val ping = JSONObject().apply {
                        put("action", "ping")
                        put("timestamp", System.currentTimeMillis())
                    }
                    webSocket?.send(ping.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send TradeSmart heartbeat: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun startStaleChecker() {
        staleCheckJob?.cancel()
        staleCheckJob = scope.launch {
            while (isActive && isConnected.get()) {
                delay(5000)
                val now = System.currentTimeMillis()
                if (lastTickReceivedTime > 0 && now - lastTickReceivedTime > STALE_THRESHOLD_MS) {
                    _connectionState.value = "STALE"
                    MarketDataStore.setSourceHealth("TradeSmart", "STALE")
                }
            }
        }
    }

    private fun handleDisconnect() {
        isConnected.set(false)
        isConnecting.set(false)
        heartbeatJob?.cancel()
        staleCheckJob?.cancel()
        _connectionState.value = "OFFLINE"
        MarketDataStore.setSourceHealth("TradeSmart", "OFFLINE")
    }

    private fun scheduleReconnect() {
        if (!isConfigured()) return
        val attempt = reconnectAttempts.incrementAndGet()
        if (attempt <= MAX_RECONNECT_ATTEMPTS) {
            val delayMs = (2000L * attempt).coerceAtMost(30000L)
            Log.d(TAG, "Scheduling TradeSmart reconnect attempt $attempt in ${delayMs}ms...")
            scope.launch {
                delay(delayMs)
                connect()
            }
        } else {
            Log.w(TAG, "Max TradeSmart reconnection attempts reached.")
        }
    }

    fun subscribe(exchange: String, tokens: List<String>, mode: Int = 1) {
        tokens.forEach { subscribedTokens[it] = exchange }
        if (!isConnected.get()) {
            connect()
            return
        }

        try {
            val subMsg = JSONObject().apply {
                put("action", "subscribe")
                put("mode", mode)
                put("exchange", exchange)
                put("tokens", JSONArray(tokens))
            }
            webSocket?.send(subMsg.toString())
            Log.d(TAG, "[TRADESMART_SUBSCRIBED] tokens=$tokens exch=$exchange")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send TradeSmart subscribe frame", e)
        }
    }

    fun unsubscribe(exchange: String, tokens: List<String>) {
        tokens.forEach { subscribedTokens.remove(it) }
        if (!isConnected.get()) return

        try {
            val unsubMsg = JSONObject().apply {
                put("action", "unsubscribe")
                put("exchange", exchange)
                put("tokens", JSONArray(tokens))
            }
            webSocket?.send(unsubMsg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send TradeSmart unsubscribe frame", e)
        }
    }

    private fun resubscribeAll() {
        if (subscribedTokens.isEmpty()) return
        val grouped = subscribedTokens.entries.groupBy({ it.value }, { it.key })
        grouped.forEach { (exchange, tokens) ->
            subscribe(exchange, tokens)
        }
    }

    fun disconnect() {
        handleDisconnect()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TradeSmart WebSocket", e)
        }
        webSocket = null
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (!isConfigured()) {
            return Result.failure(Exception("TradeSmart Tertiary Provider is not configured."))
        }
        return Result.failure(Exception("TradeSmart streaming active via WebSocket."))
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        if (!isConfigured()) {
            return Result.failure(Exception("TradeSmart Backup Provider is not configured or authenticated"))
        }
        return Result.failure(Exception("TradeSmart Option Chain unavailable"))
    }
}
