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
 * - Strict State Flow: DISCONNECTED -> CONNECTING -> AUTHENTICATING -> AUTHENTICATED -> SUBSCRIBING -> LIVE
 * - Confirmation of server authentication before subscription
 * - Subscription registry preventing duplicates
 * - Robust binary parser for Little-Endian packet format handling truncated/malformed frames
 * - Dynamic Exchange Code mapping (1=NSE, 2=NFO, 3=BSE, 4=BFO, 5=CDS, 6=MCX)
 * - LIVE state set ONLY on genuine first real market tick
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
    private val isAuthenticated = AtomicBoolean(false)
    private val isReconnecting = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)

    // Subscription registry: token -> exchange
    private val subscribedTokens = ConcurrentHashMap<String, String>()
    private var heartbeatJob: Job? = null
    private var staleCheckJob: Job? = null
    private var lastTickReceivedTime: Long = 0L

    private val _connectionState = MutableStateFlow("DISCONNECTED")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var hasFirstTick = false
    @Volatile
    private var hasSubscription = false

    init {
        if (isConfigured()) {
            scope.launch {
                connect()
            }
        } else {
            _connectionState.value = "NOT_CONFIGURED"
            healthManager?.reportConfigured(ProviderHealthManager.PROVIDER_MSTOCK, false)
        }
    }

    fun isConfigured(): Boolean {
        val apiKey = sessionManager?.mstockApiKey ?: ""
        val accessToken = sessionManager?.mstockAccessToken
        return apiKey.isNotBlank() || !accessToken.isNullOrBlank() || (sessionManager?.mstockClientId?.isNotBlank() == true)
    }

    fun hasFirstTickReceived(): Boolean = hasFirstTick

    fun hasActiveSubscription(): Boolean = subscribedTokens.isNotEmpty() || hasSubscription || isConfigured()

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

    private fun safeLogD(tag: String, msg: String) { try { Log.d(tag, msg) } catch (_: Throwable) {} }
    private fun safeLogI(tag: String, msg: String) { try { Log.i(tag, msg) } catch (_: Throwable) {} }
    private fun safeLogW(tag: String, msg: String) { try { Log.w(tag, msg) } catch (_: Throwable) {} }
    private fun safeLogE(tag: String, msg: String, t: Throwable? = null) {
        try {
            if (t != null) Log.e(tag, msg, t) else Log.e(tag, msg)
        } catch (_: Throwable) {}
    }

    /**
     * Map m.Stock Exchange Codes
     * 1 -> NSE
     * 2 -> NFO
     * 3 -> BSE
     * 4 -> BFO
     * 5 -> CDS
     * 6 -> MCX
     * Else -> UNKNOWN
     */
    fun mapExchangeCode(code: Int): String {
        return when (code) {
            1 -> "NSE"
            2 -> "NFO"
            3 -> "BSE"
            4 -> "BFO"
            5 -> "CDS"
            6 -> "MCX"
            else -> "UNKNOWN"
        }
    }

    fun mapExchangeToCode(exchange: String): Int {
        return when (exchange.trim().uppercase()) {
            "NSE" -> 1
            "NFO" -> 2
            "BSE" -> 3
            "BFO" -> 4
            "CDS" -> 5
            "MCX" -> 6
            else -> 1
        }
    }

    fun reconnect() {
        isReconnecting.set(true)
        _connectionState.value = "RECONNECTING"
        disconnect()
        connect()
    }

    /**
     * Connects to m.Stock Live WebSocket and initiates authentication handshake
     */
    fun connect() {
        if (!isConfigured()) {
            safeLogW(TAG, "[MSTOCK_ERROR] m.Stock credentials not configured. Connection skipped.")
            _connectionState.value = "NOT_CONFIGURED"
            healthManager?.reportConfigured(ProviderHealthManager.PROVIDER_MSTOCK, false)
            MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "OFFLINE")
            return
        }

        if (isConnected.get() || isConnecting.get()) return

        isConnecting.set(true)
        isAuthenticated.set(false)
        hasFirstTick = false

        if (isReconnecting.get()) {
            _connectionState.value = "RECONNECTING"
        } else {
            _connectionState.value = "CONNECTING"
        }
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
                isReconnecting.set(false)
                reconnectAttempts.set(0)

                safeLogD(TAG, "[MSTOCK_WS_OPEN]")
                healthManager?.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, true)

                _connectionState.value = "AUTHENTICATING"
                healthManager?.reportAuthenticating(ProviderHealthManager.PROVIDER_MSTOCK)
                safeLogD(TAG, "[MSTOCK_AUTHENTICATING] WebSocket open. Sending authentication handshake payload...")

                val token = sessionManager?.mstockAccessToken ?: ""
                webSocket.send("LOGIN:$token")

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
                safeLogW(TAG, "[MSTOCK_ERROR] m.Stock WebSocket closing: code=$code reason=$reason")
                handleDisconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                safeLogW(TAG, "[MSTOCK_ERROR] m.Stock WebSocket closed: code=$code reason=$reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                safeLogE(TAG, "[MSTOCK_ERROR] m.Stock WebSocket connection failure: ${t.localizedMessage}", t)
                healthManager?.reportError(ProviderHealthManager.PROVIDER_MSTOCK, t.localizedMessage ?: "WS Failure")
                handleDisconnect()
                scheduleReconnect()
            }
        }
    }

    private fun parseJsonSafely(text: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        try {
            val json = JSONObject(text)
            map["type"] = (if (json.has("type")) json.optString("type") else if (json.has("action")) json.optString("action") else "").lowercase()
            map["status"] = (if (json.has("status")) json.optString("status") else "").lowercase()
            map["code"] = json.optInt("code", 0)
            map["msg"] = if (json.has("message")) json.optString("message") else if (json.has("msg")) json.optString("msg") else ""
            map["token"] = if (json.has("token")) json.optString("token") else if (json.has("scripCode")) json.optString("scripCode") else ""
            map["exch"] = if (json.has("exchange")) json.optString("exchange") else "NSE"
            map["ltp"] = json.optDouble("ltp", json.optDouble("lastPrice", 0.0))
            map["open"] = json.optDouble("open", 0.0)
            map["high"] = json.optDouble("high", 0.0)
            map["low"] = json.optDouble("low", 0.0)
            map["close"] = json.optDouble("close", 0.0)
            map["volume"] = json.optLong("volume", 0L)
            map["has_ltp"] = json.has("ltp") || json.has("lastPrice")
            return map
        } catch (_: Throwable) {
            val type = Regex(""""(?:type|action)"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.lowercase() ?: ""
            val status = Regex(""""status"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.lowercase() ?: ""
            val code = Regex(""""code"\s*:\s*(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val msg = Regex(""""(?:message|msg)"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1) ?: ""
            val token = Regex(""""(?:token|scripCode)"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1) ?: ""
            val exch = Regex(""""exchange"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1) ?: "NSE"
            val ltp = Regex(""""(?:ltp|lastPrice)"\s*:\s*([\d.]+)""").find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

            map["type"] = type
            map["status"] = status
            map["code"] = code
            map["msg"] = msg
            map["token"] = token
            map["exch"] = exch
            map["ltp"] = ltp
            map["open"] = 0.0
            map["high"] = 0.0
            map["low"] = 0.0
            map["close"] = 0.0
            map["volume"] = 0L
            map["has_ltp"] = ltp > 0.0
            return map
        }
    }

    fun parseTextMessage(text: String) {
        try {
            val jsonMap = parseJsonSafely(text)
            val type = jsonMap["type"] as String
            val status = jsonMap["status"] as String
            val code = jsonMap["code"] as Int
            val msg = jsonMap["msg"] as String

            // Strict Authentication Response Validation according to m.Stock protocol
            val isExplicitAuthType = type in listOf("auth", "login", "auth_response", "login_response", "cn", "connect") ||
                    msg.contains("login", ignoreCase = true) || msg.contains("auth", ignoreCase = true)

            val isAuthSuccess = isExplicitAuthType && (
                    status in listOf("success", "ok", "authenticated") ||
                    code == 200 ||
                    msg.contains("successful", ignoreCase = true)
            ) && !status.contains("fail") && !status.contains("error")

            val isAuthFailed = (isExplicitAuthType || type in listOf("error", "auth_failed", "unauthorized")) && (
                    status in listOf("failed", "error", "unauthorized") ||
                    (code != 0 && code != 200) ||
                    msg.contains("fail", ignoreCase = true) || msg.contains("invalid", ignoreCase = true)
            )

            if (isAuthSuccess) {
                onAuthenticationSuccess()
            } else if (isAuthFailed) {
                val errMsg = if (msg.isNotBlank()) msg else "Authentication failed"
                safeLogE(TAG, "[MSTOCK_ERROR] Authentication failed: $errMsg")
                isAuthenticated.set(false)
                _connectionState.value = "AUTH_FAILED"
                healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, false, errMsg)
            } else if (type in listOf("tick", "quote", "ltp") || (jsonMap["has_ltp"] as Boolean)) {
                val token = jsonMap["token"] as String
                val exch = jsonMap["exch"] as String
                val ltp = jsonMap["ltp"] as Double
                val open = jsonMap["open"] as Double
                val high = jsonMap["high"] as Double
                val low = jsonMap["low"] as Double
                val close = jsonMap["close"] as Double
                val volume = jsonMap["volume"] as Long

                if (ltp > 0.0 && !ltp.isNaN() && !ltp.isInfinite()) {
                    processRealTick(exch, token, ltp, open, high, low, close, volume)
                }
            } else if (type in listOf("pong", "heartbeat") || status == "pong") {
                // Heartbeat frame
            }
        } catch (e: Exception) {
            val trimmedText = text.trim()
            if (trimmedText.equals("LOGIN_SUCCESS", ignoreCase = true) || trimmedText.equals("AUTH_OK", ignoreCase = true)) {
                onAuthenticationSuccess()
            } else if (trimmedText.equals("AUTH_FAILED", ignoreCase = true) || trimmedText.contains("INVALID_TOKEN", ignoreCase = true)) {
                safeLogE(TAG, "[MSTOCK_ERROR] Text auth failed: $text")
                isAuthenticated.set(false)
                _connectionState.value = "AUTH_FAILED"
                healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, false, text)
            } else {
                safeLogE(TAG, "[MSTOCK_ERROR] Failed to parse m.Stock text frame: ${e.localizedMessage}", e)
            }
        }
    }

    fun onAuthenticationSuccess() {
        if (isAuthenticated.compareAndSet(false, true)) {
            safeLogI(TAG, "[MSTOCK_AUTHENTICATED] m.Stock authentication verified!")
            _connectionState.value = "AUTHENTICATED"
            healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, true)
            MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "AUTHENTICATED")

            _connectionState.value = "SUBSCRIBING"
            healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_MSTOCK)
            safeLogI(TAG, "[MSTOCK_SUBSCRIBED] Subscribed to registered tokens")

            resubscribeAll()

            _connectionState.value = if (hasFirstTick) "LIVE" else "SUBSCRIBED"
            healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_MSTOCK, subscribedTokens.size)
        }
    }

    /**
     * Parses m.Stock Binary Market Data Packets using Little Endian byte order.
     * Safely handles truncated, malformed, and concatenated multi-packet binary frames.
     */
    fun parseBinaryPacket(bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.size < 12) {
            safeLogW(TAG, "[MSTOCK_ERROR] Binary packet too short (${bytes.size} bytes)")
            return
        }

        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            while (buffer.remaining() >= 12) {
                val startPos = buffer.position()
                val packetLen = buffer.short.toInt() and 0xFFFF

                if (packetLen < 12 || packetLen > buffer.remaining() + 2) {
                    safeLogW(TAG, "[MSTOCK_ERROR] Invalid packet length: $packetLen (remaining=${buffer.remaining() + 2})")
                    break
                }

                val mode = buffer.get().toInt() and 0xFF
                val exchangeCode = buffer.get().toInt() and 0xFF
                val token = buffer.int

                val exchange = mapExchangeCode(exchangeCode)
                if (exchange == "UNKNOWN") {
                    safeLogW(TAG, "[MSTOCK_ERROR] Unknown exchange code $exchangeCode for token $token")
                    val bytesRead = buffer.position() - startPos
                    val bytesToSkip = packetLen - bytesRead
                    if (bytesToSkip > 0 && bytesToSkip <= buffer.remaining()) {
                        buffer.position(buffer.position() + bytesToSkip)
                    } else {
                        break
                    }
                    continue
                }

                var ltp = 0.0
                var open = 0.0
                var high = 0.0
                var low = 0.0
                var close = 0.0
                var volume = 0L

                if (buffer.remaining() >= 4) {
                    ltp = buffer.int / 100.0
                }

                if (ltp <= 0.0 || ltp.isNaN() || ltp.isInfinite()) {
                    val bytesRead = buffer.position() - startPos
                    val bytesToSkip = packetLen - bytesRead
                    if (bytesToSkip > 0 && bytesToSkip <= buffer.remaining()) {
                        buffer.position(buffer.position() + bytesToSkip)
                    } else {
                        break
                    }
                    continue
                }

                if (buffer.remaining() >= 16 && (buffer.position() - startPos + 16 <= packetLen)) {
                    open = buffer.int / 100.0
                    high = buffer.int / 100.0
                    low = buffer.int / 100.0
                    close = buffer.int / 100.0
                }

                if (buffer.remaining() >= 8 && (buffer.position() - startPos + 8 <= packetLen)) {
                    volume = buffer.long
                } else if (buffer.remaining() >= 4 && (buffer.position() - startPos + 4 <= packetLen)) {
                    volume = buffer.int.toLong()
                }

                val bytesRead = buffer.position() - startPos
                if (bytesRead < packetLen && (packetLen - bytesRead) <= buffer.remaining()) {
                    buffer.position(startPos + packetLen)
                }

                processRealTick(exchange, token.toString(), ltp, open, high, low, close, volume)
            }
        } catch (e: Exception) {
            safeLogE(TAG, "[MSTOCK_ERROR] Failed to parse m.Stock binary frame: ${e.localizedMessage}", e)
        }
    }

    private fun processRealTick(
        exchange: String,
        tokenStr: String,
        ltp: Double,
        open: Double,
        high: Double,
        low: Double,
        close: Double,
        volume: Long
    ) {
        val symbol = resolveSymbol(exchange, tokenStr)
        val now = System.currentTimeMillis()

        safeLogD(TAG, "[MSTOCK_TICK_RECEIVED] exch=$exchange token=$tokenStr ltp=$ltp")

        if (!hasFirstTick) {
            hasFirstTick = true
            safeLogI(TAG, "[MSTOCK_FIRST_REAL_TICK] First valid m.Stock real tick received! symbol=$symbol ltp=$ltp")
            safeLogI(TAG, "[MSTOCK_LIVE] m.Stock feed is now LIVE")
        }

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
                    safeLogW(TAG, "[MSTOCK_ERROR] Failed to send m.Stock heartbeat: ${e.localizedMessage}")
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
        isAuthenticated.set(false)
        hasSubscription = false
        heartbeatJob?.cancel()
        staleCheckJob?.cancel()
        _connectionState.value = "DISCONNECTED"
        MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "OFFLINE")
    }

    private fun scheduleReconnect() {
        if (!isConfigured()) return
        val attempt = reconnectAttempts.incrementAndGet()
        val delayMs = (2000L * attempt).coerceAtMost(15000L)
        if (attempt > MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts.set(1)
        }
        safeLogD(TAG, "Scheduling m.Stock reconnect attempt $attempt in ${delayMs}ms...")
        scope.launch {
            delay(delayMs)
            reconnect()
        }
    }

    fun subscribe(exchange: String, tokens: List<String>, mode: Int = 1) {
        val newTokensToSend = mutableListOf<String>()

        tokens.forEach { tok ->
            val trimmed = tok.trim()
            if (trimmed.isNotBlank()) {
                // Prevent duplicate subscriptions in registry
                val existingExch = subscribedTokens[trimmed]
                if (existingExch == null || existingExch != exchange) {
                    subscribedTokens[trimmed] = exchange
                    newTokensToSend.add(trimmed)
                }
            } else {
                safeLogW(TAG, "[MSTOCK_ERROR] Subscription failed: token blank")
            }
        }

        // Do not send WebSocket payload if not authenticated yet;
        // tokens are saved in registry and will be subscribed upon authentication success!
        if (!isAuthenticated.get() || !isConnected.get()) {
            safeLogD(TAG, "Stored tokens in registry. Subscription will be sent upon authentication.")
            return
        }

        if (newTokensToSend.isEmpty()) return

        sendSubscriptionPayload(exchange, newTokensToSend, mode)
    }

    private fun sendSubscriptionPayload(exchange: String, tokens: List<String>, mode: Int = 1) {
        try {
            val validIntTokens = tokens.mapNotNull { it.toIntOrNull() }
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
            healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_MSTOCK)

            webSocket?.send(subMsg.toString())
            webSocket?.send(modeMsg.toString())

            hasSubscription = true
            _connectionState.value = if (hasFirstTick) "LIVE" else "SUBSCRIBED"
            healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_MSTOCK, subscribedTokens.size)

            safeLogD(TAG, "[MSTOCK_SUBSCRIBED] tokens=$tokens exch=$exchange mode=$mode")
        } catch (e: Exception) {
            safeLogE(TAG, "[MSTOCK_ERROR] Failed to send m.Stock subscribe frame", e)
        }
    }

    fun unsubscribe(exchange: String, tokens: List<String>) {
        val removedTokens = mutableListOf<String>()
        tokens.forEach {
            val trimmed = it.trim()
            if (subscribedTokens.containsKey(trimmed)) {
                subscribedTokens.remove(trimmed)
                removedTokens.add(trimmed)
            }
        }

        if (!isConnected.get() || !isAuthenticated.get() || removedTokens.isEmpty()) return

        try {
            val unsubMsg = JSONObject().apply {
                put("action", "unsubscribe")
                put("exchange", exchange)
                put("tokens", JSONArray(removedTokens))
            }
            webSocket?.send(unsubMsg.toString())
        } catch (e: Exception) {
            safeLogE(TAG, "[MSTOCK_ERROR] Failed to send m.Stock unsubscribe frame", e)
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

        val exchangeGroups = ConcurrentHashMap<String, MutableList<String>>()
        subscribedTokens.forEach { (token, exch) ->
            exchangeGroups.getOrPut(exch) { mutableListOf() }.add(token)
        }

        exchangeGroups.forEach { (exch, tokens) ->
            sendSubscriptionPayload(exch, tokens)
        }
    }

    fun disconnect() {
        handleDisconnect()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "[MSTOCK_ERROR] Error closing m.Stock WebSocket", e)
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

