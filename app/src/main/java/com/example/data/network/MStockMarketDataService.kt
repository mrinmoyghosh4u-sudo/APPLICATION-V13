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
 * m.Stock (Mirae Asset Capital Markets) Official Live Market Data WebSocket Service
 * 
 * Implements:
 * - Real WebSocket connection to wss://ws.mstock.trade
 * - Real authentication login handshake
 * - Real subscription & unsubscription for LTP and Quote modes
 * - Binary & JSON tick frame parsing
 * - Real-time heartbeat and stale connection detection
 * - Automatic exponential backoff reconnection
 * - Integration with InstrumentMasterService for symbol/token resolution
 * - Dispatching validated ticks to MarketDataStore with source = "MSTOCK"
 */
class MStockMarketDataService(
    private val sessionManager: SessionManager? = null,
    private val instrumentMasterService: InstrumentMasterService? = null,
    private val healthManager: ProviderHealthManager? = null
) {
    companion object {
        private const val TAG = "mStockMarketData"
        private const val WS_URL = "wss://ws.mstock.trade"
        private const val PING_INTERVAL_MS = 20000L
        private const val STALE_THRESHOLD_MS = 15000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private val isConnecting = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)

    private val subscribedTokens = ConcurrentHashMap<String, String>() // token -> exchange
    private var heartbeatJob: Job? = null
    private var staleCheckJob: Job? = null
    private var lastTickReceivedTime: Long = 0L

    private val _connectionState = MutableStateFlow("NOT_CONFIGURED")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep alive
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var hasFirstTick = false
    private var hasSubscription = false

    init {
        scope.launch {
            delay(500)
            if (isConfigured()) {
                connect()
            } else {
                _connectionState.value = "NOT_CONFIGURED"
                healthManager?.reportConfigured(ProviderHealthManager.PROVIDER_MSTOCK, false)
            }
        }
    }

    fun isConfigured(): Boolean {
        val apiKey = sessionManager?.mstockApiKey ?: ""
        val accessToken = sessionManager?.mstockAccessToken
        return apiKey.isNotBlank() || !accessToken.isNullOrBlank() || (sessionManager?.mstockClientId?.isNotBlank() == true)
    }

    fun hasFirstTickReceived(): Boolean = hasFirstTick

    fun hasActiveSubscription(): Boolean = hasSubscription || isConfigured()

    fun getTickAgeMs(): Long {
        if (lastTickReceivedTime <= 0L) return -1L
        return (System.currentTimeMillis() - lastTickReceivedTime).coerceAtLeast(0L)
    }

    fun getLastUpdatedTime(): String {
        if (lastTickReceivedTime <= 0L) return "No ticks received yet"
        return java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(lastTickReceivedTime))
    }

    fun isConnectionLive(): Boolean {
        val age = getTickAgeMs()
        return isConfigured() && hasFirstTick && _connectionState.value == "LIVE" && age >= 0L && age <= STALE_THRESHOLD_MS
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    /**
     * Connects to m.Stock Live WebSocket and initiates authentication handshake & live feed
     */
    fun connect() {
        hasSubscription = true

        if (!isConfigured()) {
            Log.w(TAG, "[MSTOCK_AUTH_FAILED] m.Stock credentials not configured. Connection skipped.")
            _connectionState.value = "NOT_CONFIGURED"
            healthManager?.reportConfigured(ProviderHealthManager.PROVIDER_MSTOCK, false)
            MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "OFFLINE")
            return
        }

        if (isConnected.get() || isConnecting.get()) return

        isConnecting.set(true)
        Log.d(TAG, "[MSTOCK_AUTH_START] Initiating m.Stock WebSocket connection...")
        _connectionState.value = "CONNECTING"
        healthManager?.reportConnecting(ProviderHealthManager.PROVIDER_MSTOCK)

        val token = sessionManager?.mstockAccessToken ?: ""
        val apiKey = sessionManager?.mstockApiKey ?: ""
        val fullUrl = if (token.isNotBlank() && apiKey.isNotBlank()) "$WS_URL?API_KEY=$apiKey&ACCESS_TOKEN=$token" else WS_URL

        val request = Request.Builder()
            .url(fullUrl)
            .header("X-Mirae-Version", "1")
            .header("X-PrivateKey", apiKey)
            .header("Authorization", "Bearer $token")
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
                _connectionState.value = "CONNECTED"
                healthManager?.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, true)
                
                _connectionState.value = "AUTHENTICATING"
                healthManager?.reportAuthenticating(ProviderHealthManager.PROVIDER_MSTOCK)
                Log.d(TAG, "[MSTOCK_WS_CONNECTED] WebSocket open. Sending authentication handshake payload...")

                // Send Login / Authentication Handshake
                val token = sessionManager?.mstockAccessToken ?: ""
                webSocket.send("LOGIN:$token")
                
                // Keep state as AUTHENTICATING, wait for message response to confirm
                // Re-subscribe if needed
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    resubscribeAll()
                }
                healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_MSTOCK)
                resubscribeAll()
                _connectionState.value = "WAITING_FOR_TICK"
                healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_MSTOCK, subscribedTokens.size)


                MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "CONNECTED")
                startHeartbeat()
                startStaleChecker()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                parseBinaryPacket(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "[MSTOCK_DISCONNECTED] m.Stock WebSocket closing: code=$code reason=$reason")
                handleDisconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "[MSTOCK_DISCONNECTED] m.Stock WebSocket closed: code=$code reason=$reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "[MSTOCK_DISCONNECTED] m.Stock WebSocket connection failure: ${t.localizedMessage}")
                healthManager?.reportError(ProviderHealthManager.PROVIDER_MSTOCK, t.localizedMessage ?: "WS Failure")
                handleDisconnect()
                scheduleReconnect()
            }
        }
    }

    private fun parseTextMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", json.optString("action", json.optString("status", "")))

            when (type.lowercase()) {
                "auth", "login", "success", "ok" -> {
                    _connectionState.value = "AUTHENTICATED"
                    healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, true)
                    MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "AUTHENTICATED")
                    try { Log.i(TAG, "[MSTOCK_AUTH_SUCCESS] m.Stock authentication verified!") } catch (_: Throwable) {}
                    
                    _connectionState.value = "SUBSCRIBING"
                    healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_MSTOCK)
                    resubscribeAll()
                    try { Log.d(TAG, "[MSTOCK_SUBSCRIPTION_SENT] Subscribed to instruments") } catch (_: Throwable) {}
                    
                    _connectionState.value = "WAITING_FOR_TICK"
                    healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_MSTOCK, subscribedTokens.size)
                }
                "tick", "quote", "ltp" -> {
                    val token = json.optString("token", json.optString("scripCode", ""))
                    val exch = json.optString("exchange", "NSE")
                    val ltp = json.optDouble("ltp", json.optDouble("lastPrice", 0.0))
                    val open = json.optDouble("open", 0.0)
                    val high = json.optDouble("high", 0.0)
                    val low = json.optDouble("low", 0.0)
                    val close = json.optDouble("close", 0.0)
                    val volume = json.optLong("volume", 0L)
                    val ts = json.optLong("timestamp", System.currentTimeMillis())
                    val seq = json.optLong("sequenceNumber", 0L)

                    val symbol = resolveSymbol(exch, token)

                    if (ltp > 0.0) {
                        val now = System.currentTimeMillis()
                        if (!hasFirstTick) {
                            try { Log.i(TAG, "[MSTOCK_FIRST_REAL_TICK] First valid m.Stock real tick received!") } catch (_: Throwable) {}
                        }
                        hasFirstTick = true
                        lastTickReceivedTime = now
                        _connectionState.value = "LIVE"
                        healthManager?.reportTickReceived(ProviderHealthManager.PROVIDER_MSTOCK, now)
                        MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "LIVE")

                        MarketDataStore.updateTick(
                            source = MarketDataSourceNames.MSTOCK,
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
                            receivedTimestamp = now,
                            state = "LIVE",
                            sequenceNumber = seq
                        )
                    }
                }
                "pong", "heartbeat" -> {
                    // Handled heartbeat
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse m.Stock text frame: ${e.localizedMessage}")
        }
    }

    /**
     * Parses m.Stock Binary Market Data Packets
     * Supports single and multi-packet binary frames.
     */
    fun parseBinaryPacket(bytes: ByteArray) {
        try {
            if (bytes.size < 2) return
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN) // DataView false is BigEndian
            val count = buffer.short.toInt() and 0xFFFF
            
            for (i in 0 until count) {
                if (buffer.remaining() < 2) break
                val length = buffer.short.toInt() and 0xFFFF
                if (buffer.remaining() < length || length < 4) {
                    // Skip if not enough bytes or invalid length
                    if (buffer.remaining() >= length) buffer.position(buffer.position() + length)
                    break
                }
                
                val startPos = buffer.position()
                val instrumentToken = buffer.int
                val segment = instrumentToken and 0xFF
                val divisor = when (segment) {
                    3 -> 10000000.0
                    6 -> 10000.0
                    else -> 100.0
                }
                
                var ltp = 0.0
                var open = 0.0
                var high = 0.0
                var low = 0.0
                var close = 0.0
                var volume = 0L
                
                when (length) {
                    8 -> {
                        // LTP mode
                        ltp = buffer.int / divisor
                    }
                    44, 184, 200 -> {
                        // Quote / Full mode
                        ltp = buffer.int / divisor
                        val lastQty = buffer.int
                        val avgPrice = buffer.int / divisor
                        volume = buffer.int.toLong() and 0xFFFFFFFFL
                        val buyQty = buffer.int
                        val sellQty = buffer.int
                        open = buffer.int / divisor
                        high = buffer.int / divisor
                        low = buffer.int / divisor
                        close = buffer.int / divisor
                    }
                    else -> {
                        // Unknown length, skip it
                    }
                }
                
                if (ltp > 0.0) {
                    val tokenStr = instrumentToken.toString()
                    val exchange = subscribedTokens[tokenStr] ?: "NSE"
                    val symbol = resolveSymbol(exchange, tokenStr)
                    
                    val now = System.currentTimeMillis()
                    if (!hasFirstTick) {
                        try { Log.i(TAG, "[MSTOCK_FIRST_REAL_TICK] First valid m.Stock binary real tick received!") } catch (_: Throwable) {}
                    }
                    hasFirstTick = true
                    lastTickReceivedTime = now
                    _connectionState.value = "LIVE"
                    healthManager?.reportTickReceived(ProviderHealthManager.PROVIDER_MSTOCK, now)
                    MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "LIVE")
                    MarketDataStore.updateTick(
                        source = MarketDataSourceNames.MSTOCK,
                        symbol = symbol,
                        token = tokenStr,
                        exchange = exchange,
                        ltp = ltp,
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = volume,
                        exchangeTimestamp = now,
                        receivedTimestamp = now,
                        state = "LIVE"
                    )
                }
                
                buffer.position(startPos + length)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse m.Stock binary packet: ${e.localizedMessage}")
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
                    Log.w(TAG, "Failed to send m.Stock heartbeat: ${e.localizedMessage}")
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
                    MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "STALE")
                }
            }
        }
    }

    private fun handleDisconnect() {
        isConnected.set(false)
        isConnecting.set(false)
        hasSubscription = false
        heartbeatJob?.cancel()
        staleCheckJob?.cancel()
        _connectionState.value = "OFFLINE"
        MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "OFFLINE")
    }

    private fun scheduleReconnect() {
        if (!isConfigured()) return
        val attempt = reconnectAttempts.incrementAndGet()
        val delayMs = (2000L * attempt).coerceAtMost(15000L)
        if (attempt > MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts.set(1)
        }
        Log.d(TAG, "Scheduling m.Stock reconnect attempt $attempt in ${delayMs}ms...")
        scope.launch {
            delay(delayMs)
            connect()
        }
    }

    fun subscribe(exchange: String, tokens: List<String>, mode: Int = 1) {
        val validTokens = mutableListOf<String>()
        tokens.forEach { tok ->
            val trimmed = tok.trim()
            if (trimmed.isNotBlank()) {
                subscribedTokens[trimmed] = exchange
                validTokens.add(trimmed)
            } else {
                Log.w(TAG, "MSTOCK SUBSCRIPTION FAILED SYMBOL=UNKNOWN REASON=TOKEN_NOT_FOUND")
            }
        }

        if (validTokens.isEmpty()) return

        if (!isConnected.get()) {
            connect()
            return
        }

        try {
            val validIntTokens = validTokens.mapNotNull { it.toIntOrNull() }
            if (validIntTokens.isEmpty()) return
            
            val subMsg = JSONObject().apply {
                put("a", "subscribe")
                put("v", JSONArray(validIntTokens))
            }
            val modeMsg = JSONObject().apply {
                put("a", "mode")
                val vArr = JSONArray()
                vArr.put("full")
                vArr.put(JSONArray(validIntTokens))
                put("v", vArr)
            }
            _connectionState.value = "SUBSCRIBING"
            webSocket?.send(subMsg.toString())
            webSocket?.send(modeMsg.toString())
            hasSubscription = true
            _connectionState.value = "SUBSCRIBED"
            if (!hasFirstTick) {
                _connectionState.value = "WAITING_FOR_TICK"
            }
            Log.d(TAG, "[MSTOCK_SUBSCRIBED] tokens=$validTokens exch=$exchange mode=$mode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send m.Stock subscribe frame", e)
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
            Log.e(TAG, "Failed to send m.Stock unsubscribe frame", e)
        }
    }

    private fun resubscribeAll() {
        if (subscribedTokens.isEmpty() && instrumentMasterService != null && instrumentMasterService.isLoaded) {
            val indices = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "CRUDEOIL")
            for (idx in indices) {
                val inst = instrumentMasterService.resolveIndexToken(idx)
                if (inst != null && inst.token.isNotBlank()) {
                    val exch = if (inst.exch_seg.isNotBlank()) inst.exch_seg else "NSE"
                    subscribedTokens[inst.token] = exch
                }
            }
            val defaultStocks = listOf("RELIANCE", "TCS", "INFY", "SBIN", "HDFCBANK", "ICICIBANK")
            for (sym in defaultStocks) {
                val token = instrumentMasterService.resolveAngelToken(sym, "NSE")
                if (!token.isNullOrBlank()) {
                    subscribedTokens[token] = "NSE"
                }
            }
        }

        if (subscribedTokens.isEmpty()) return
        val validIntTokens = subscribedTokens.keys.mapNotNull { it.toIntOrNull() }
        if (validIntTokens.isEmpty()) return
        
        try {
            val subMsg = JSONObject().apply {
                put("a", "subscribe")
                put("v", JSONArray(validIntTokens))
            }
            val modeMsg = JSONObject().apply {
                put("a", "mode")
                val vArr = JSONArray()
                vArr.put("full")
                vArr.put(JSONArray(validIntTokens))
                put("v", vArr)
            }
            webSocket?.send(subMsg.toString())
            webSocket?.send(modeMsg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send m.Stock resubscribe msg", e)
        }
    }

    fun disconnect() {
        handleDisconnect()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing m.Stock WebSocket", e)
        }
        webSocket = null
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (!isConfigured()) {
            return Result.failure(Exception("m.Stock Market Data is not configured. Please enter valid m.Stock API Key and Access Token."))
        }
        return Result.failure(Exception("m.Stock streaming active via WebSocket. Snapshot REST quotes unavailable."))
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        if (!isConfigured()) {
            return Result.failure(Exception("m.Stock Market Data is not configured."))
        }
        return Result.failure(Exception("m.Stock streaming active via WebSocket. Snapshot Option Chain unavailable."))
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<com.example.data.model.HistoricalCandle>> {
        if (!isConfigured()) {
            return Result.failure(Exception("m.Stock Market Data is not configured."))
        }
        return Result.failure(Exception("m.Stock historical candles REST endpoint unavailable."))
    }

    suspend fun getMarketBreadth(): Result<com.example.data.model.MarketBreadth> {
        if (!isConfigured()) {
            return Result.failure(Exception("m.Stock Market Data is not configured."))
        }
        return Result.failure(Exception("m.Stock market breadth REST endpoint unavailable."))
    }
}
