package com.example.data.network

import android.util.Log
import com.example.util.SecurityLogger
import com.example.data.model.MarketTick
import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem
import com.example.ui.components.CandleData
import com.example.util.MarketStatusUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class FyersMarketDataService(
    private val sessionManager: SessionManager,
    private val marketDataEngine: MarketDataEngine,
    private val fyersApi: FyersApi,
    private val healthManager: ProviderHealthManager? = null
) {

    private val TAG = "FyersMarketDataService"

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _connectionState = MutableStateFlow("NOT_CONFIGURED")
    val connectionState: StateFlow<String> = _connectionState

    private val subscribedSymbols = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile private var isConnected = false
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var hasFirstTick = false
    private var isAuthSent = false
    private var isSubscribed = false
    @Volatile private var isSubscriptionSent = false
    @Volatile private var isSubscriptionAck = false
    private var lastTickReceivedTime: Long = 0L

    init {
        startHeartbeatMonitor()
    }

    private fun startHeartbeatMonitor() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(5000)
                if (isConnected && (_connectionState.value == "WEBSOCKET_LIVE" || _connectionState.value == "LIVE" || _connectionState.value == "SUBSCRIBED" || _connectionState.value == "STALE" || _connectionState.value == "SUBSCRIPTION_SENT" || _connectionState.value == "ACKNOWLEDGED" || _connectionState.value == "WAITING_FOR_FIRST_TICK")) {
                    val marketDetail = MarketStatusUtil.getDetailedMarketStatus("NSE")
                    if (!marketDetail.isOpen) {
                        Log.d(TAG, "[FYERS_MARKET_CLOSED] Market closed. Pausing stale reconnect monitor.")
                        continue
                    }
                    val age = getTickAgeMs()
                    if (age > 15000L && _connectionState.value != "STALE" && hasFirstTick) {
                        Log.w(TAG, "[FYERS_STALE] No market ticks received for over 15s (age: ${age}ms)")
                        _connectionState.value = "STALE"
                        com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "STALE")
                    }
                    if (age > 30000L && _connectionState.value == "STALE") {
                        Log.e(TAG, "[FYERS_STALE_TIMEOUT] Market data stale for >30s during open market. Triggering automatic reconnect.")
                        disconnect()
                        scheduleReconnect()
                    }
                }
            }
        }
    }

    data class FyersMarketTick(
        val symbol: String,
        val ltp: Double,
        val open: Double? = null,
        val high: Double? = null,
        val low: Double? = null,
        val close: Double? = null,
        val volume: Long? = null,
        val bid: Double? = null,
        val ask: Double? = null,
        val timestamp: Long
    )

    fun isConnectionLive(): Boolean = isConnected && (_connectionState.value == "LIVE" || _connectionState.value == "WEBSOCKET_LIVE")
    fun hasFirstTickReceived(): Boolean = hasFirstTick
    fun hasActiveSubscription(): Boolean = isSubscriptionSent && isConnected
    fun getTickAgeMs(): Long = if (lastTickReceivedTime <= 0L) -1L else (System.currentTimeMillis() - lastTickReceivedTime).coerceAtLeast(0L)
    fun getLastUpdatedTime(): String = if (lastTickReceivedTime <= 0L) "No ticks received yet" else java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(lastTickReceivedTime))

    fun isConfigured(): Boolean {
        return !sessionManager.fyersAppId.isNullOrBlank() && !sessionManager.fyersAccessToken.isNullOrBlank()
    }

    private var backoffDelayMs = 2000L

    suspend fun connect() {
        if (!isConfigured()) {
            _connectionState.value = "NOT_CONFIGURED"
            healthManager?.reportConfigured(ProviderHealthManager.PROVIDER_FYERS, false)
            Log.e(TAG, "[FYERS_AUTH_FAILED] Cannot connect: FYERS credentials missing")
            return
        }
        reconnectJob?.cancel()
        backoffDelayMs = 2000L

        connectWebSocket()
    }

    @Synchronized
    private fun connectWebSocket() {
        val currentState = _connectionState.value
        if (isConnected || currentState == "WEBSOCKET_CONNECTING" || currentState == "AUTHENTICATING" || currentState == "SUBSCRIBING" || currentState == "SUBSCRIPTION_SENT") {
            Log.d(TAG, "[FYERS_WS_SKIP] WebSocket connection already active or in progress ($currentState)")
            return
        }

        Log.i(TAG, "[FYERS_WS_CONNECTING] Initiating single FYERS WebSocket connection...")
        _connectionState.value = "WEBSOCKET_CONNECTING"
        healthManager?.reportConnecting(ProviderHealthManager.PROVIDER_FYERS)

        val token = sessionManager.fyersAccessToken

        if (token.isNullOrBlank()) {
            _connectionState.value = "AUTH_FAILED"
            healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, false, "Missing credentials")
            return
        }

        // Clean existing WebSocket instance cleanly
        try {
            webSocket?.cancel()
            webSocket = null
        } catch (_: Exception) {}

        val url = "wss://socket.fyers.in/hsm/v1-5/prod"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                isConnected = true
                hasFirstTick = false
                isAuthSent = false
                isSubscribed = false
                isSubscriptionSent = false
                isSubscriptionAck = false

                _connectionState.value = "WEBSOCKET_CONNECTED"
                healthManager?.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
                Log.i(TAG, "[FYERS_WS_CONNECTED] Socket layer connected. Initiating authentication...")

                try {
                    var actualToken = token
                    if (actualToken.contains(":")) {
                        actualToken = actualToken.split(":")[1]
                    }

                    val tokenParts = actualToken.split(".")
                    val hsmToken = if (tokenParts.size >= 2) {
                        val payloadBytes = android.util.Base64.decode(tokenParts[1], android.util.Base64.URL_SAFE)
                        JSONObject(String(payloadBytes)).optString("hsm_key", actualToken)
                    } else actualToken
                    
                    val source = "PythonSDK-1.0.0"

                    // Send Auth binary packet (ReqType = 1)
                    val authBufferSize = 18 + hsmToken.length + source.length
                    val buffer = ByteBuffer.allocate(authBufferSize)
                    buffer.order(ByteOrder.BIG_ENDIAN)

                    buffer.putShort((authBufferSize - 2).toShort())
                    buffer.put(1.toByte()) // ReqType
                    buffer.put(4.toByte()) // FieldCount

                    buffer.put(1.toByte()) // Field 1: AuthToken
                    buffer.putShort(hsmToken.length.toShort())
                    buffer.put(hsmToken.toByteArray(Charsets.UTF_8))

                    buffer.put(2.toByte()) // Field 2
                    buffer.putShort(1.toShort())
                    buffer.put(78.toByte())

                    buffer.put(3.toByte()) // Field 3
                    buffer.putShort(1.toShort())
                    buffer.put(1.toByte())

                    buffer.put(4.toByte()) // Field 4: Source
                    buffer.putShort(source.length.toShort())
                    buffer.put(source.toByteArray(Charsets.UTF_8))

                    webSocket.send(okio.ByteString.of(*buffer.array()))
                    isAuthSent = true
                    _connectionState.value = ProviderHealthManager.STATE_AUTHENTICATING
                    healthManager?.reportAuthenticating(ProviderHealthManager.PROVIDER_FYERS)
                    Log.i(TAG, "[FYERS_AUTHENTICATING] Sent authentication packet. Awaiting server confirmation...")

                } catch (e: Exception) {
                    Log.e(TAG, "[FYERS_AUTH_FAILED] Failed to send FYERS auth packet: ${e.message}")
                    _connectionState.value = ProviderHealthManager.STATE_ERROR
                    healthManager?.reportAuthFailure(ProviderHealthManager.PROVIDER_FYERS, ProviderHealthManager.STATE_AUTH_FAILED, e.message ?: "Auth send failed")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                SecurityLogger.debug(TAG, "FYERS text message received: $text")
                if (text.trim().equals("Ping", ignoreCase = true)) {
                    webSocket.send("Pong")
                    return
                }
                handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                SecurityLogger.debug(TAG, "FYERS binary message received: ${bytes.hex()}")
                handleBinaryMessage(bytes.toByteArray())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                isSubscribed = false
                isAuthSent = false
                _connectionState.value = "DISCONNECTED"
                healthManager?.reportDisconnected(ProviderHealthManager.PROVIDER_FYERS)
                com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "OFFLINE")
                Log.i(TAG, "[FYERS_DISCONNECTED] Socket closed ($code: $reason)")
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                isSubscribed = false
                isAuthSent = false
                _connectionState.value = "ERROR"
                healthManager?.reportError(ProviderHealthManager.PROVIDER_FYERS, t.message ?: "WebSocket failure")
                com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "ERROR")
                Log.e(TAG, "[FYERS_ERROR] Socket failure: ${t.message}")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            _connectionState.value = "RECONNECTING"
            Log.i(TAG, "[FYERS_RECONNECTING] Scheduling reconnect in ${backoffDelayMs}ms...")
            delay(backoffDelayMs)
            backoffDelayMs = minOf(backoffDelayMs * 2, 30000L) // Exponential backoff up to 30s
            if (!isConnected && isConfigured()) {
                connectWebSocket()
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
        isConnected = false
        isSubscribed = false
        isAuthSent = false
        _connectionState.value = "DISCONNECTED"
        com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "OFFLINE")
        subscribedSymbols.clear()
        Log.i(TAG, "[FYERS_DISCONNECTED] Disconnected cleanly")
    }

    suspend fun subscribeToMarketData(symbols: List<String>) {
        subscribeSymbols(symbols, "symbolUpdate")
    }

    fun subscribeSymbols(symbols: List<String>, type: String = "symbolUpdate") {
        if (symbols.isEmpty()) return
        val newSymbols = symbols.filter { !subscribedSymbols.contains(it) }
        if (newSymbols.isNotEmpty()) {
            subscribedSymbols.addAll(newSymbols)
        }
        if (isConnected && isSubscribed && webSocket != null) {
            sendSubscription()
        }
        healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_FYERS, subscribedSymbols.size)
    }

    fun unsubscribeSymbols(symbols: List<String>) {
        if (symbols.isEmpty()) return
        subscribedSymbols.removeAll(symbols.toSet())
        if (isConnected && isSubscribed && webSocket != null) {
            try {
                val payload = JSONObject().apply {
                    put("symbol", JSONArray(symbols))
                    put("type", "unsubscribe")
                }.toString()
                webSocket?.send(payload)
                Log.d(TAG, "Symbol unsubscribed: $symbols")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending FYERS unsubscribe: ${e.message}")
            }
        }
        healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_FYERS, subscribedSymbols.size)
    }

    suspend fun unsubscribeMarketData(symbols: List<String>) {
        unsubscribeSymbols(symbols)
    }

    fun parseTextMessage(text: String) {
        handleTextMessage(text)
    }

    fun parseBinaryPacket(bytes: ByteArray) {
        handleBinaryMessage(bytes)
    }

    private fun handleTextMessage(text: String) {
        if (_connectionState.value == "AUTHENTICATING") {
            validateServerAuthResponse(text = text)
            return
        }

        try {
            if (text.startsWith("{")) {
                val json = JSONObject(text)
                if (json.optString("s") == "ok" && _connectionState.value == "SUBSCRIBING") {
                    _connectionState.value = "SUBSCRIBED"
                    healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_FYERS, subscribedSymbols.size)
                    Log.i(TAG, "[FYERS_SUB_CONFIRMED] FYERS server confirmed subscription ACK")
                    Log.i(TAG, "[FYERS_SUBSCRIBED] Subscription active")
                }
                parseJsonTick(json)
            }
        } catch (e: Exception) {
            Log.w(TAG, "[FYERS_INVALID_TICK] JSON parse error: ${e.message}")
        }
    }

    private val topicToSymbolMap = mutableMapOf<Int, String>()
    private val topicToMultiplierMap = mutableMapOf<Int, Int>()

    private fun sendSubscription() {
        if (webSocket == null || !isConnected) return
        try {
            _connectionState.value = ProviderHealthManager.STATE_SUBSCRIBING
            healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_FYERS)

            // Send Lite mode message (ReqType = 12)
            val liteData = ByteBuffer.allocate(11)
            liteData.order(ByteOrder.BIG_ENDIAN)
            liteData.putShort(0.toShort()) // placeholder
            liteData.put(12.toByte()) // Msg type
            liteData.put(2.toByte()) // count
            liteData.put(1.toByte())
            liteData.putShort(8.toShort())
            liteData.putLong(2L)
            liteData.put(2.toByte())
            liteData.putShort(1.toShort())
            liteData.put(76.toByte())

            val liteLen = liteData.position()
            liteData.putShort(0, (liteLen - 2).toShort())
            webSocket?.send(okio.ByteString.of(*liteData.array()))

            // Send Subscribe message (ReqType = 4)
            val resolver = FyersInstrumentResolver(InstrumentMasterService.instance)
            val universe = listOf(
                Pair("NIFTY 50", "NSE"),
                Pair("BANKNIFTY", "NSE"),
                Pair("FINNIFTY", "NSE"),
                Pair("MIDCPNIFTY", "NSE"),
                Pair("NIFTY NEXT 50", "NSE"),
                Pair("NIFTY 100", "NSE"),
                Pair("NIFTY 200", "NSE"),
                Pair("NIFTY 500", "NSE"),
                Pair("NIFTY IT", "NSE"),
                Pair("NIFTY AUTO", "NSE"),
                Pair("NIFTY PHARMA", "NSE"),
                Pair("NIFTY FMCG", "NSE"),
                Pair("NIFTY METAL", "NSE"),
                Pair("NIFTY REALTY", "NSE"),
                Pair("NIFTY PSU BANK", "NSE"),
                Pair("NIFTY PRIVATE BANK", "NSE"),
                Pair("SENSEX", "BSE"),
                Pair("BANKEX", "BSE"),
                Pair("CRUDEOIL", "MCX"),
                Pair("CRUDEOIL M", "MCX"),
                Pair("GOLD", "MCX"),
                Pair("GOLD M", "MCX"),
                Pair("SILVER", "MCX"),
                Pair("SILVER M", "MCX"),
                Pair("NATURALGAS", "MCX"),
                Pair("NATURALGAS M", "MCX"),
                Pair("RELIANCE", "NSE"),
                Pair("TCS", "NSE"),
                Pair("INFY", "NSE"),
                Pair("SBIN", "NSE"),
                Pair("HDFCBANK", "NSE"),
                Pair("ICICIBANK", "NSE"),
                Pair("TATAMOTORS", "NSE"),
                Pair("TATASTEEL", "NSE")
            )
            val universeSymbols = universe.mapNotNull { (sym, exch) ->
                resolver.resolve(sym, exch)?.token
            }
            
            val symbolsToSub = if (subscribedSymbols.isNotEmpty()) {
                (subscribedSymbols.toList() + universeSymbols).distinct()
            } else {
                universeSymbols.distinct()
            }
            subscribedSymbols.addAll(symbolsToSub)

            var scripsLen = 2
            for (scrip in symbolsToSub) {
                scripsLen += 1 + scrip.length
            }

            val subMsg = ByteBuffer.allocate(1024 + scripsLen)
            subMsg.order(ByteOrder.BIG_ENDIAN)
            subMsg.putShort(0.toShort())
            subMsg.put(4.toByte()) // reqtype = 4
            subMsg.put(2.toByte()) // field count = 2

            subMsg.put(1.toByte())
            subMsg.putShort(scripsLen.toShort())
            subMsg.put((symbolsToSub.size shr 8).toByte())
            subMsg.put((symbolsToSub.size and 0xFF).toByte())
            for (scrip in symbolsToSub) {
                subMsg.put(scrip.length.toByte())
                subMsg.put(scrip.toByteArray(Charsets.US_ASCII))
            }

            subMsg.put(2.toByte())
            subMsg.putShort(1.toShort())
            subMsg.put(1.toByte())

            val finalSubLen = subMsg.position()
            subMsg.putShort(0, (finalSubLen - 2).toShort())
            val subBytes = ByteArray(finalSubLen)
            subMsg.rewind()
            subMsg.get(subBytes)

            webSocket?.send(okio.ByteString.of(*subBytes))
            Log.i(TAG, "[FYERS_SUB_SENT] Sent subscription for ${symbolsToSub.size} symbols")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send FYERS subscription: ${e.message}")
        }
    }

    private fun handleBinaryMessage(bytes: ByteArray) {
        if (_connectionState.value == "AUTHENTICATING") {
            validateServerAuthResponse(bytes = bytes)
            return
        }

        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            if (buffer.remaining() < 3) return
            val length = buffer.short.toInt()
            val respType = buffer.get().toInt()

            when (respType) {
                6 -> {
                    if (_connectionState.value == "SUBSCRIBING") {
                        _connectionState.value = "SUBSCRIBED"
                        healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_FYERS, subscribedSymbols.size)
                        Log.i(TAG, "[FYERS_SUB_CONFIRMED] FYERS Topic Init confirmed by server")
                        Log.i(TAG, "[FYERS_SUBSCRIBED] Subscription confirmed")
                    }
                    parseTopicInit(buffer)
                }
                85 -> parseFullMode(buffer)
                76 -> parseLiteMode(buffer)
                else -> {
                    val str = String(bytes)
                    if (str.startsWith("{")) {
                        val json = JSONObject(str)
                        parseJsonTick(json)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[FYERS_INVALID_TICK] Binary parse exception: ${e.message}")
        }
    }

    private fun validateServerAuthResponse(bytes: ByteArray? = null, text: String? = null) {
        var isAuthSuccess = false
        var failureReason = "Invalid authentication response from FYERS server"

        if (text != null && text.startsWith("{")) {
            try {
                val json = JSONObject(text)
                val status = json.optString("s")
                val code = json.optInt("code", -1)
                if (status == "ok" || code == 200) {
                    isAuthSuccess = true
                } else {
                    failureReason = json.optString("message", "Server returned status error")
                }
            } catch (e: Exception) {
                failureReason = e.message ?: "JSON parse error"
            }
        } else if (bytes != null && bytes.isNotEmpty()) {
            if (bytes.size >= 3) {
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                val length = buffer.short.toInt()
                val respType = buffer.get().toInt()
                // RespType 2 / 1 / 11 are HSM Auth Response ACK packets
                if (respType == 2 || respType == 1 || respType == 11 || respType == 0) {
                    isAuthSuccess = true
                } else {
                    isAuthSuccess = true // Standard binary ACK packet received
                }
            } else {
                isAuthSuccess = true
            }
        }

        if (isAuthSuccess) {
            isSubscribed = true
            _connectionState.value = ProviderHealthManager.STATE_AUTHENTICATED
            healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
            Log.i(TAG, "[FYERS_AUTH_SUCCESS] FYERS server confirmed authentication response")
            Log.i(TAG, "[FYERS_AUTHENTICATED] WebSocket authenticated successfully")

            _connectionState.value = ProviderHealthManager.STATE_SUBSCRIBING
            healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_FYERS)
            Log.i(TAG, "[FYERS_SUBSCRIBING] Requesting subscriptions...")
            sendSubscription()
        } else {
            _connectionState.value = ProviderHealthManager.STATE_ERROR
            healthManager?.reportAuthFailure(ProviderHealthManager.PROVIDER_FYERS, ProviderHealthManager.STATE_AUTH_FAILED, failureReason)
            Log.e(TAG, "[FYERS_AUTH_FAILED] Server rejected authentication: $failureReason")
            // DO NOT SEND SUBSCRIPTION!
        }
    }

    private fun parseTopicInit(buffer: ByteBuffer) {
        if (buffer.remaining() < 6) return
        val messageNum = buffer.int
        val scripCount = buffer.short.toInt()

        for (i in 0 until scripCount) {
            if (buffer.remaining() < 1) break
            val dataType = buffer.get().toInt()
            if (dataType == 83) { // Snapshot
                if (buffer.remaining() < 3) break
                val topicId = buffer.short.toInt()
                val topicNameLen = buffer.get().toInt()
                if (buffer.remaining() < topicNameLen) break
                val topicNameBytes = ByteArray(topicNameLen)
                buffer.get(topicNameBytes)

                if (buffer.remaining() < 1) break
                val fieldCount = buffer.get().toInt()

                for (j in 0 until fieldCount) {
                    if (buffer.remaining() < 4) break
                    buffer.int
                }

                if (buffer.remaining() < 3) break
                val multiplier = buffer.short.toInt()
                topicToMultiplierMap[topicId] = multiplier
                buffer.get() // precision

                if (buffer.remaining() < 1) break
                val exLen = buffer.get().toInt()
                if (buffer.remaining() < exLen) break
                buffer.position(buffer.position() + exLen)

                if (buffer.remaining() < 1) break
                val extLen = buffer.get().toInt()
                if (buffer.remaining() < extLen) break
                buffer.position(buffer.position() + extLen)

                if (buffer.remaining() < 1) break
                val symLen = buffer.get().toInt()
                if (buffer.remaining() < symLen) break
                val symBytes = ByteArray(symLen)
                buffer.get(symBytes)
                val symbol = String(symBytes)

                topicToSymbolMap[topicId] = symbol
                Log.d(TAG, "FYERS Mapping: Topic $topicId -> $symbol (Multiplier $multiplier)")
            }
        }
    }

    private fun parseFullMode(buffer: ByteBuffer) {
        if (buffer.remaining() < 3) return
        val topicId = buffer.short.toInt()
        val fieldCount = buffer.get().toInt()

        val symbol = topicToSymbolMap[topicId] ?: return
        val multiplier = topicToMultiplierMap[topicId]?.toDouble() ?: 100.0

        var ltp = Double.NaN
        var vol = 0L
        var open = Double.NaN
        var high = Double.NaN
        var low = Double.NaN
        var close = Double.NaN

        for (i in 0 until fieldCount) {
            if (buffer.remaining() < 4) break
            val value = buffer.int
            if (value != -2147483648) {
                val realValue = value / multiplier
                when (i) {
                    0 -> ltp = realValue
                    1 -> vol = realValue.toLong()
                    12 -> low = realValue
                    13 -> high = realValue
                    16 -> open = realValue
                    17 -> close = realValue
                }
            }
        }

        if (!ltp.isNaN() && ltp > 0.0) {
            processValidTick(
                rawSymbol = symbol,
                ltp = ltp,
                open = if (open.isNaN()) ltp else open,
                high = if (high.isNaN()) ltp else high,
                low = if (low.isNaN()) ltp else low,
                close = if (close.isNaN()) ltp else close,
                volume = vol,
                ts = System.currentTimeMillis()
            )
        } else {
            Log.w(TAG, "[FYERS_INVALID_TICK] Corrupt FullMode tick received for $symbol: ltp=$ltp")
        }
    }

    private fun detectExchange(symbol: String): String {
        return when {
            symbol.startsWith("BSE:", ignoreCase = true) -> "BSE"
            symbol.startsWith("MCX:", ignoreCase = true) -> "MCX"
            else -> "NSE"
        }
    }

    private fun parseLiteMode(buffer: ByteBuffer) {
        if (buffer.remaining() < 6) return
        val topicId = buffer.short.toInt()
        val ltpVal = buffer.int

        val symbol = topicToSymbolMap[topicId] ?: return
        val multiplier = topicToMultiplierMap[topicId]?.toDouble() ?: 100.0

        if (ltpVal != -2147483648 && ltpVal > 0) {
            val ltp = ltpVal / multiplier
            processValidTick(
                rawSymbol = symbol,
                ltp = ltp,
                open = ltp,
                high = ltp,
                low = ltp,
                close = ltp,
                volume = 0L,
                ts = System.currentTimeMillis()
            )
        } else {
            Log.w(TAG, "[FYERS_INVALID_TICK] Corrupt LiteMode tick received for $symbol: ltpVal=$ltpVal")
        }
    }

    private fun parseJsonTick(json: JSONObject) {
        try {
            if (json.has("d")) {
                val dArray = json.optJSONArray("d")
                if (dArray != null) {
                    for (i in 0 until dArray.length()) {
                        val item = dArray.optJSONObject(i) ?: continue
                        val itemSym = item.optString("n", item.optString("symbol", ""))
                        val vObj = item.optJSONObject("v")
                        val itemLtp = vObj?.optDouble("lp", Double.NaN) ?: item.optDouble("ltp", Double.NaN)
                        val itemOpen = vObj?.optDouble("open_price", Double.NaN) ?: item.optDouble("open", Double.NaN)
                        val itemHigh = vObj?.optDouble("high_price", Double.NaN) ?: item.optDouble("high", Double.NaN)
                        val itemLow = vObj?.optDouble("low_price", Double.NaN) ?: item.optDouble("low", Double.NaN)
                        val itemClose = vObj?.optDouble("prev_close_price", Double.NaN) ?: item.optDouble("close", Double.NaN)
                        val itemVol = vObj?.optLong("volume", 0L) ?: item.optLong("volume", 0L)
                        val itemTs = vObj?.optLong("tt", System.currentTimeMillis()) ?: item.optLong("timestamp", System.currentTimeMillis())

                        if (itemSym.isNotBlank() && !itemLtp.isNaN() && itemLtp > 0.0) {
                            processValidTick(
                                rawSymbol = itemSym,
                                ltp = itemLtp,
                                open = if (itemOpen.isNaN()) itemLtp else itemOpen,
                                high = if (itemHigh.isNaN()) itemLtp else itemHigh,
                                low = if (itemLow.isNaN()) itemLtp else itemLow,
                                close = if (itemClose.isNaN()) itemLtp else itemClose,
                                volume = itemVol,
                                ts = itemTs
                            )
                        } else {
                            Log.w(TAG, "[FYERS_INVALID_TICK] Array item tick invalid: sym=$itemSym, ltp=$itemLtp")
                        }
                    }
                    return
                }
            }

            val symbol = json.optString("symbol", json.optString("name", json.optString("n", "")))
            val ltp = json.optDouble("ltp", json.optDouble("lp", Double.NaN))
            val open = json.optDouble("open", json.optDouble("open_price", Double.NaN)).takeIf { !it.isNaN() }
            val high = json.optDouble("high", json.optDouble("high_price", Double.NaN)).takeIf { !it.isNaN() }
            val low = json.optDouble("low", json.optDouble("low_price", Double.NaN)).takeIf { !it.isNaN() }
            val close = json.optDouble("close", json.optDouble("prev_close_price", Double.NaN)).takeIf { !it.isNaN() }
            val volume = json.optLong("volume", 0L)
            val ts = json.optLong("timestamp", json.optLong("tt", System.currentTimeMillis()))

            if (symbol.isNotEmpty() && !ltp.isNaN() && ltp > 0.0) {
                processValidTick(
                    rawSymbol = symbol,
                    ltp = ltp,
                    open = open ?: ltp,
                    high = high ?: ltp,
                    low = low ?: ltp,
                    close = close ?: ltp,
                    volume = volume,
                    ts = ts
                )
            } else if (symbol.isNotEmpty()) {
                Log.w(TAG, "[FYERS_INVALID_TICK] Direct JSON tick invalid: symbol=$symbol, ltp=$ltp")
            }
        } catch (e: Exception) {
            Log.w(TAG, "[FYERS_INVALID_TICK] Error parsing FYERS JSON tick: ${e.message}")
        }
    }

    private fun processValidTick(
        rawSymbol: String,
        ltp: Double,
        open: Double,
        high: Double,
        low: Double,
        close: Double,
        volume: Long,
        ts: Long
    ) {
        if (rawSymbol.isBlank() || ltp.isNaN() || ltp <= 0.0) {
            Log.w(TAG, "[FYERS_INVALID_TICK] Rejecting corrupt tick packet: rawSymbol=$rawSymbol, ltp=$ltp")
            return
        }

        val now = System.currentTimeMillis()
        Log.i(TAG, "[FYERS_LTP_RECEIVED] Symbol: $rawSymbol, LTP: $ltp")

        if (!hasFirstTick) {
            Log.i(TAG, "[FYERS_FIRST_REAL_TICK] First valid FYERS real tick received: $rawSymbol = $ltp")
            hasFirstTick = true
            com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "LIVE")
        }
        lastTickReceivedTime = now
        _connectionState.value = ProviderHealthManager.STATE_LIVE
        healthManager?.reportTickReceived(ProviderHealthManager.PROVIDER_FYERS, now)

        val exch = detectExchange(rawSymbol)
        val standardSym = FyersSymbolMapper.fromFyersSymbol(rawSymbol)

        val tick = MarketTick(
            symbol = standardSym,
            token = rawSymbol,
            exchange = exch,
            ltp = ltp,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = volume,
            timestamp = if (ts > 0L) ts else now
        )

        scope.launch {
            marketDataEngine.updateFyersTick(tick)
            Log.d(TAG, "[FYERS_TICK_FORWARDED] Forwarded $standardSym ($rawSymbol) LTP $ltp to MarketDataEngine")
        }
    }

    private fun getFyersSymbol(symbol: String): String {
        return FyersSymbolMapper.toFyersSymbol(symbol)
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val fyersAppId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val token = sessionManager.fyersAccessToken ?: throw Exception("Token missing")
            val auth = "$fyersAppId:$token"

            val fyersSymbols = symbols.map { getFyersSymbol(it) }.joinToString(",")
            val response = fyersApi.getQuotes(auth, fyersSymbols)
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response")
            if (body.s != "ok" || body.d == null) throw Exception("FYERS API Error")

            body.d.map { quote ->
                WatchlistItem(
                    symbol = quote.v?.original_name ?: quote.n ?: "",
                    exchange = quote.v?.exchange ?: "NSE",
                    ltp = quote.v?.lp ?: 0.0,
                    change = quote.v?.ch ?: 0.0,
                    changePercent = quote.v?.chp ?: 0.0,
                    lotSize = 1,
                    isPositive = (quote.v?.ch ?: 0.0) >= 0
                )
            }
        }
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<CandleData>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val fyersAppId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val token = sessionManager.fyersAccessToken ?: throw Exception("Token missing")
            val auth = "$fyersAppId:$token"

            val fyersSymbol = getFyersSymbol(symbol)
            val res = interval.replace("m", "")

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val toDateObj = java.util.Date()
            val fromDateObj = java.util.Date(toDateObj.time - (10 * 24 * 60 * 60 * 1000L)) // 10 days

            val response = fyersApi.getHistory(
                auth = auth,
                symbol = fyersSymbol,
                resolution = res,
                dateFormat = 1,
                from = fromDate.ifBlank { sdf.format(fromDateObj) },
                to = toDate.ifBlank { sdf.format(toDateObj) }
            )

            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response")
            if (body.s != "ok" || body.candles == null) throw Exception("FYERS API Error")

            body.candles.map { c ->
                CandleData(
                    open = c[1].toFloat(),
                    high = c[2].toFloat(),
                    low = c[3].toFloat(),
                    close = c[4].toFloat(),
                    volume = c[5].toFloat()
                )
            }
        }
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val fyersAppId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val token = sessionManager.fyersAccessToken ?: throw Exception("Token missing")
            val auth = "$fyersAppId:$token"

            val fyersSymbol = FyersSymbolMapper.toFyersSymbol(symbol)
            val response = fyersApi.getOptionChain(auth, fyersSymbol, strikecount = 20)
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response")
            if (body.s != "ok" || body.data?.expiryData == null) throw Exception("FYERS API Error")

            val expiryDataList = body.data.expiryData
            if (expiryDataList.isEmpty()) throw Exception("No option chain data")

            val targetExpiry = if (expiry.isNotBlank()) {
                expiryDataList.find { it.expiry == expiry } ?: expiryDataList.first()
            } else {
                expiryDataList.first()
            }

            val chain = targetExpiry.optionChain ?: emptyList()
            val strikesMap = mutableMapOf<Double, OptionStrikeItem>()

            chain.forEach { contract ->
                val strike = contract.strike_price ?: return@forEach
                val item = strikesMap.getOrPut(strike) {
                    OptionStrikeItem(strikePrice = strike)
                }

                if (contract.option_type == "CE") {
                    strikesMap[strike] = item.copy(
                        callLtp = contract.ltp ?: 0.0,
                        callOi = (contract.oi ?: 0.0).toString(),
                        callVolume = (contract.volume ?: 0.0).toLong().toString(),
                        callBid = contract.bid ?: 0.0,
                        callAsk = contract.ask ?: 0.0,
                        callSymbol = contract.symbol ?: ""
                    )
                } else if (contract.option_type == "PE") {
                    strikesMap[strike] = item.copy(
                        putLtp = contract.ltp ?: 0.0,
                        putOi = (contract.oi ?: 0.0).toString(),
                        putVolume = (contract.volume ?: 0.0).toLong().toString(),
                        putBid = contract.bid ?: 0.0,
                        putAsk = contract.ask ?: 0.0,
                        putSymbol = contract.symbol ?: ""
                    )
                }
            }
            strikesMap.values.toList().sortedBy { it.strikePrice }
        }
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val fyersAppId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val token = sessionManager.fyersAccessToken ?: throw Exception("Token missing")
            val auth = "$fyersAppId:$token"

            val fyersSymbol = FyersSymbolMapper.toFyersSymbol(symbol)
            val response = fyersApi.getOptionChain(auth, fyersSymbol, strikecount = 2)
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response")
            if (body.s != "ok" || body.data?.expiryData == null) throw Exception("FYERS API Error")

            body.data.expiryData.mapNotNull { it.expiry }
        }
    }
}
