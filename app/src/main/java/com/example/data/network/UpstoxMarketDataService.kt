package com.example.data.network

import android.util.Log
import com.example.data.model.MarketTick
import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem
import com.example.ui.components.CandleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class UpstoxMarketDataService(
    private val sessionManager: SessionManager,
    private val marketDataEngine: MarketDataEngine,
    private val upstoxApi: UpstoxApi,
    private val healthManager: ProviderHealthManager? = null
) {
    companion object {
        private const val TAG = "UpstoxMarketDataService"
        private const val WS_BASE_URL = "wss://api.upstox.com/v3/feed/market-data-feed"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow("NOT_CONFIGURED")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val subscribedInstrumentKeys = ConcurrentHashMap.newKeySet<String>()
    private var isConnected = false
    private var reconnectJob: Job? = null
    private var restPollingJob: Job? = null
    private var backoffDelayMs = 1000L
    private var hasFirstTick = false
    private var lastTickReceivedTime: Long = 0L

    fun isConnectionLive(): Boolean = isConnected && _connectionState.value == "LIVE"
    fun hasFirstTickReceived(): Boolean = hasFirstTick
    fun hasActiveSubscription(): Boolean = subscribedInstrumentKeys.isNotEmpty() || isConfigured()
    fun getTickAgeMs(): Long = if (lastTickReceivedTime <= 0L) -1L else (System.currentTimeMillis() - lastTickReceivedTime).coerceAtLeast(0L)
    fun getLastUpdatedTime(): String = if (lastTickReceivedTime <= 0L) "No ticks received yet" else java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(lastTickReceivedTime))


    suspend fun subscribeToMarketData(symbols: List<String>) = withContext(Dispatchers.IO) {
        if (!isConfigured() || symbols.isEmpty()) return@withContext
        val newKeys = symbols.mapNotNull { UpstoxSymbolMapper.toUpstoxInstrumentKey(it) }
        val keysToSend = newKeys.filter { !subscribedInstrumentKeys.contains(it) }
        subscribedInstrumentKeys.addAll(newKeys)

        if (isConnected && webSocket != null && keysToSend.isNotEmpty()) {
            try {
                val json = JSONObject().apply {
                    put("guid", UUID.randomUUID().toString())
                    put("method", "sub")
                    put("data", JSONObject().apply {
                        put("mode", "ltpc")
                        put("instrumentKeys", JSONArray(keysToSend))
                    })
                }

                val payload = json.toString().toByteArray(Charsets.UTF_8)
                webSocket?.send(ByteString.of(*payload))
                Log.i(TAG, "[UPSTOX_SUBSCRIBE_SENT] Dynamic subscription sent for ${keysToSend.size} instrument(s) (mode=ltpc)")
            } catch (e: Exception) {
                Log.e(TAG, "[UPSTOX_ERROR] Error sending dynamic subscription: ${e.message}", e)
            }
        }
        healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, subscribedInstrumentKeys.size)
    }

    suspend fun unsubscribeMarketData(symbols: List<String>) = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext
        val keys = symbols.mapNotNull { UpstoxSymbolMapper.toUpstoxInstrumentKey(it) }
        subscribedInstrumentKeys.removeAll(keys.toSet())

        if (isConnected && webSocket != null && keys.isNotEmpty()) {
            try {
                val json = JSONObject().apply {
                    put("guid", UUID.randomUUID().toString())
                    put("method", "unsub")
                    put("data", JSONObject().apply {
                        put("instrumentKeys", JSONArray(keys))
                    })
                }
                val payload = json.toString().toByteArray(Charsets.UTF_8)
                webSocket?.send(ByteString.of(*payload))
                Log.i(TAG, "[UPSTOX_UNSUB_SENT] Unsubscribed from ${keys.size} instrument(s)")
            } catch (e: Exception) {
                Log.e(TAG, "[UPSTOX_ERROR] Error sending unsubscribe: ${e.message}", e)
            }
        }
        healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, subscribedInstrumentKeys.size)
    }

    fun isConfigured(): Boolean {
        return !sessionManager.upstoxApiKey.isNullOrBlank() && !sessionManager.upstoxAccessToken.isNullOrBlank()
    }

    private fun getAuthHeader(): String {
        val token = sessionManager.upstoxAccessToken ?: ""
        return if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
    }

    private fun startRestPolling() {
        if (restPollingJob?.isActive == true) return
        restPollingJob = scope.launch {
            while (isConfigured()) {
                if (!isConnectionLive()) {
                    try {
                        val symbolsToFetch = if (subscribedInstrumentKeys.isNotEmpty()) {
                            subscribedInstrumentKeys.map { UpstoxSymbolMapper.fromUpstoxInstrumentKey(it).first }
                        } else {
                            listOf(
                                UpstoxSymbolMapper.KEY_NIFTY_50,
                                UpstoxSymbolMapper.KEY_BANK_NIFTY,
                                UpstoxSymbolMapper.KEY_FIN_NIFTY,
                                UpstoxSymbolMapper.KEY_MIDCP_NIFTY
                            )
                        }
                        val quotes = getMarketQuotes(symbolsToFetch).getOrNull()
                        if (quotes != null) {
                            val now = System.currentTimeMillis()
                            for (q in quotes) {
                                if (q.ltp > 0.0) {
                                    val tick = MarketTick(
                                        symbol = q.symbol,
                                        token = UpstoxSymbolMapper.toUpstoxInstrumentKey(q.symbol),
                                        exchange = q.exchange,
                                        ltp = q.ltp,
                                        open = q.ltp,
                                        high = q.ltp,
                                        low = q.ltp,
                                        close = q.ltp,
                                        volume = 0L,
                                        timestamp = now
                                    )
                                    marketDataEngine.updateUpstoxTick(tick)
                                }
                            }
                            // REST polling MUST NOT mark WebSocket state as LIVE or set hasFirstTick = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[UPSTOX_REST_POLL_WARN] Upstox REST polling error: ${e.message}")
                    }
                }
                delay(5000L)
            }
        }
    }

    suspend fun connect() = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            _connectionState.value = "NOT_CONFIGURED"
            healthManager?.reportConfigured(ProviderHealthManager.PROVIDER_UPSTOX, false)
            Log.w(TAG, "[UPSTOX_ERROR] Cannot connect: credentials missing")
            return@withContext
        }
        reconnectJob?.cancel()
        restPollingJob?.cancel()
        backoffDelayMs = 1000L
        startRestPolling()
        connectWebSocket()
    }

    private fun connectWebSocket() {
        if (isConnected || _connectionState.value == "CONNECTING" || _connectionState.value == "RECONNECTING" || _connectionState.value == "SUBSCRIBING") {
            Log.d(TAG, "[UPSTOX_WS_CONNECTING_SKIP] Connection or connection attempt already active (state=${_connectionState.value})")
            return
        }

        _connectionState.value = "CONNECTING"
        healthManager?.reportConnecting(ProviderHealthManager.PROVIDER_UPSTOX)

        val token = sessionManager.upstoxAccessToken
        if (token.isNullOrBlank()) {
            _connectionState.value = "AUTH_FAILED"
            healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, false, "Token missing")
            Log.e(TAG, "[UPSTOX_ERROR] Access Token is missing")
            return
        }
        val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"

        scope.launch {
            try {
                Log.i(TAG, "[UPSTOX_AUTHORIZE] Requesting V3 authorized WebSocket URL from Upstox API...")
                val authRes = upstoxApi.getWebSocketFeedAuth(authHeader)
                if (!authRes.isSuccessful) {
                    _connectionState.value = "AUTH_FAILED"
                    healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, false, "HTTP ${authRes.code()}")
                    Log.e(TAG, "[UPSTOX_ERROR] Failed to obtain V3 WebSocket auth: HTTP ${authRes.code()}")
                    return@launch
                }

                val wsUrl = authRes.body()?.data?.redirectUri
                if (wsUrl.isNullOrBlank()) {
                    _connectionState.value = "AUTH_FAILED"
                    healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, false, "Authorized URL missing")
                    Log.e(TAG, "[UPSTOX_ERROR] Authorized redirect URI missing in Upstox auth response")
                    return@launch
                }

                Log.i(TAG, "[UPSTOX_AUTHORIZE] Fresh V3 authorized redirect URI obtained successfully")
                Log.i(TAG, "[UPSTOX_WS_CONNECTING] Connecting to V3 WebSocket endpoint...")

                val request = Request.Builder()
                    .url(wsUrl)
                    .build()

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        isConnected = true
                        hasFirstTick = false
                        // 1. WebSocket OPEN and AUTHENTICATED/LIVE are distinct.
                        // Socket OPEN sets state to CONNECTED (pending genuine tick validation)
                        _connectionState.value = "CONNECTED"
                        Log.i(TAG, "[UPSTOX_WS_CONNECTED] WebSocket connected successfully. Waiting for subscription & ticks.")
                        healthManager?.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)

                        _connectionState.value = "SUBSCRIBING"
                        healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_UPSTOX)

                        // 3 & 4. Send V3 JSON subscription payload with exact Upstox instrument keys
                        val keys = if (subscribedInstrumentKeys.isNotEmpty()) {
                            subscribedInstrumentKeys.toList()
                        } else {
                            listOf(UpstoxSymbolMapper.KEY_NIFTY_50) // Initial test: single key NIFTY 50
                        }
                        subscribedInstrumentKeys.addAll(keys)

                        val json = JSONObject().apply {
                            put("guid", UUID.randomUUID().toString())
                            put("method", "sub")
                            put("data", JSONObject().apply {
                                put("mode", "ltpc")
                                put("instrumentKeys", JSONArray(keys))
                            })
                        }

                        val payload = json.toString().toByteArray(Charsets.UTF_8)
                        webSocket.send(ByteString.of(*payload))
                        _connectionState.value = "SUBSCRIBED"
                        healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, keys.size)
                        Log.i(TAG, "[UPSTOX_SUBSCRIBE_SENT] Subscription payload sent for ${keys.size} instrument(s): mode=ltpc, keys=$keys")
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        Log.d(TAG, "[UPSTOX_BINARY_RECEIVED] Received ${bytes.size} binary bytes from WebSocket")
                        parseBinaryPacket(bytes.toByteArray())
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        Log.d(TAG, "[UPSTOX_TEXT_RECEIVED] Upstox WebSocket text message: $text")
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        isConnected = false
                        this@UpstoxMarketDataService.webSocket = null
                        _connectionState.value = "DISCONNECTED"
                        Log.w(TAG, "[UPSTOX_ERROR] Upstox WebSocket closed (code=$code, reason=$reason)")
                        healthManager?.reportDisconnected(ProviderHealthManager.PROVIDER_UPSTOX)
                        com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.UPSTOX, "OFFLINE")
                        scheduleReconnect()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        isConnected = false
                        this@UpstoxMarketDataService.webSocket = null
                        _connectionState.value = "ERROR"
                        Log.e(TAG, "[UPSTOX_ERROR] Upstox WebSocket failure: ${t.message}", t)
                        healthManager?.reportError(ProviderHealthManager.PROVIDER_UPSTOX, t.message ?: "WebSocket failure")
                        com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.UPSTOX, "ERROR")
                        scheduleReconnect()
                    }
                })
            } catch (e: Exception) {
                _connectionState.value = "ERROR"
                Log.e(TAG, "[UPSTOX_ERROR] Exception in connectWebSocket: ${e.message}", e)
                healthManager?.reportError(ProviderHealthManager.PROVIDER_UPSTOX, e.message ?: "Connect exception")
            }
        }
    }

    fun parseBinaryPacket(bytes: ByteArray): Int {
        val decoded = UpstoxProtobufDecoder.decode(bytes)
        Log.d(TAG, "[UPSTOX_PROTOBUF_DECODED] Decoded ${decoded.feeds.size} feed items (feedType=${decoded.feedType})")
        var tickCount = 0
        val now = System.currentTimeMillis()

        for ((key, feed) in decoded.feeds) {
            val (standardSym, exch) = UpstoxSymbolMapper.fromUpstoxInstrumentKey(key)
            val ltp = feed.ltp
            if (ltp > 0.0) {
                tickCount++
                Log.d(TAG, "[UPSTOX_LTP_RECEIVED] Key $key ($standardSym) LTP=$ltp")

                // 7. First real tick validation
                if (!hasFirstTick) {
                    hasFirstTick = true
                    Log.i(TAG, "[UPSTOX_FIRST_REAL_TICK] First valid Upstox real tick received: $key = $ltp")
                }

                lastTickReceivedTime = now
                backoffDelayMs = 1000L

                if (_connectionState.value != "LIVE") {
                    _connectionState.value = "LIVE"
                    Log.i(TAG, "[UPSTOX_LIVE] Upstox WebSocket market data feed state is now LIVE")
                }

                healthManager?.reportTickReceived(ProviderHealthManager.PROVIDER_UPSTOX, if (feed.timestamp > 0L) feed.timestamp else now)

                val tick = MarketTick(
                    symbol = standardSym,
                    token = key,
                    exchange = exch,
                    ltp = ltp,
                    open = if (feed.open > 0.0) feed.open else ltp,
                    high = if (feed.high > 0.0) feed.high else ltp,
                    low = if (feed.low > 0.0) feed.low else ltp,
                    close = if (feed.close > 0.0) feed.close else ltp,
                    volume = feed.volume,
                    timestamp = if (feed.timestamp > 0L) feed.timestamp else now
                )
                scope.launch {
                    marketDataEngine.updateUpstoxTick(tick)
                }
            }
        }
        return tickCount
    }

    fun disconnect() {
        reconnectJob?.cancel()
        restPollingJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
        _connectionState.value = "DISCONNECTED"
        com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.UPSTOX, "OFFLINE")
        subscribedInstrumentKeys.clear()
        Log.i(TAG, "[UPSTOX_DISCONNECTED] Upstox WebSocket disconnected by user")
    }

    // =========================================================================
    // REST API METHODS
    // =========================================================================

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isConfigured()) throw Exception("Upstox credentials not configured")
            val auth = getAuthHeader()
            val keys = symbols.map { UpstoxSymbolMapper.toUpstoxInstrumentKey(it) }.joinToString(",")

            val response = upstoxApi.getMarketQuotes(token = auth, instrumentKeys = keys)
            if (!response.isSuccessful) throw Exception("Upstox HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response body")
            if (body.status != "success" || body.data == null) throw Exception("Upstox API Error: ${body.errors?.firstOrNull()?.message ?: "Unknown error"}")

            body.data.map { (key, quote) ->
                val (sym, exch) = UpstoxSymbolMapper.fromUpstoxInstrumentKey(key)
                val ltp = quote.lastPrice ?: quote.ohlc?.close ?: 0.0
                val close = quote.ohlc?.close ?: 0.0
                val change = quote.netChange ?: (if (close > 0.0) ltp - close else 0.0)
                val changePercent = if (close > 0.0) (change / close) * 100.0 else 0.0

                WatchlistItem(
                    symbol = quote.symbol?.ifBlank { sym } ?: sym,
                    exchange = exch,
                    ltp = ltp,
                    change = change,
                    changePercent = changePercent,
                    lotSize = 1,
                    isPositive = change >= 0
                )
            }
        }
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isConfigured()) throw Exception("Upstox credentials not configured")
            val auth = getAuthHeader()
            val instKey = UpstoxSymbolMapper.toUpstoxInstrumentKey(symbol)

            val targetExpiry = if (expiry.isNotBlank()) expiry else {
                // Fetch default expiry if not provided
                val expiries = getOptionExpiries(symbol).getOrNull()
                expiries?.firstOrNull() ?: throw Exception("No option expiries available for $symbol")
            }

            val response = upstoxApi.getOptionChain(
                token = auth,
                instrumentKey = instKey,
                expiryDate = targetExpiry
            )

            if (!response.isSuccessful) throw Exception("Upstox HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response body")
            if (body.status != "success" || body.data == null) throw Exception("Upstox Option Chain Error: ${body.errors?.firstOrNull()?.message ?: "Unknown error"}")

            val chainData = body.data
            val strikesMap = mutableMapOf<Double, OptionStrikeItem>()

            chainData.forEach { item ->
                val strike = item.strikePrice ?: return@forEach
                val strikeItem = strikesMap.getOrPut(strike) {
                    OptionStrikeItem(strikePrice = strike)
                }

                val callOpt = item.callOptions
                val putOpt = item.putOptions

                var updated = strikeItem
                if (callOpt != null) {
                    val mData = callOpt.marketData
                    val greeks = callOpt.optionGreeks
                    updated = updated.copy(
                        callLtp = mData?.ltp ?: 0.0,
                        callOi = (mData?.oi ?: 0.0).toString(),
                        callVolume = (mData?.volume ?: 0L).toString(),
                        callBid = mData?.bidPrice ?: 0.0,
                        callAsk = mData?.askPrice ?: 0.0,
                        callIv = greeks?.iv ?: 0.0,
                        callDelta = greeks?.delta ?: 0.0,
                        callTheta = greeks?.theta ?: 0.0,
                        callGamma = greeks?.gamma ?: 0.0,
                        callVega = greeks?.vega ?: 0.0,
                        callSymbol = callOpt.instrumentKey ?: ""
                    )
                }

                if (putOpt != null) {
                    val mData = putOpt.marketData
                    val greeks = putOpt.optionGreeks
                    updated = updated.copy(
                        putLtp = mData?.ltp ?: 0.0,
                        putOi = (mData?.oi ?: 0.0).toString(),
                        putVolume = (mData?.volume ?: 0L).toString(),
                        putBid = mData?.bidPrice ?: 0.0,
                        putAsk = mData?.askPrice ?: 0.0,
                        putIv = greeks?.iv ?: 0.0,
                        putDelta = greeks?.delta ?: 0.0,
                        putTheta = greeks?.theta ?: 0.0,
                        putGamma = greeks?.gamma ?: 0.0,
                        putVega = greeks?.vega ?: 0.0,
                        putSymbol = putOpt.instrumentKey ?: ""
                    )
                }

                strikesMap[strike] = updated
            }

            strikesMap.values.toList().sortedBy { it.strikePrice }
        }
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isConfigured()) throw Exception("Upstox credentials not configured")
            val auth = getAuthHeader()
            val instKey = UpstoxSymbolMapper.toUpstoxInstrumentKey(symbol)

            val response = upstoxApi.getOptionContracts(token = auth, instrumentKey = instKey)
            if (!response.isSuccessful) throw Exception("Upstox HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response body")
            if (body.status != "success" || body.data == null) throw Exception("Upstox Expiries Error: ${body.errors?.firstOrNull()?.message ?: "Unknown error"}")

            val expiries = body.data.mapNotNull { it.expiry }.distinct().sorted()
            if (expiries.isEmpty()) throw Exception("No active option expiries found for $symbol")
            expiries
        }
    }

    suspend fun getHistoricalCandles(
        symbol: String,
        interval: String = "15m",
        fromDate: String = "",
        toDate: String = ""
    ): Result<List<CandleData>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isConfigured()) throw Exception("Upstox credentials not configured")
            val auth = getAuthHeader()
            val instKey = UpstoxSymbolMapper.toUpstoxInstrumentKey(symbol)

            val apiInterval = when (interval.lowercase(Locale.ENGLISH)) {
                "1m", "1min", "1minute" -> "1minute"
                "5m", "5min", "5minute" -> "5minute"
                "15m", "15min", "15minute" -> "15minute"
                "30m", "30min", "30minute" -> "30minute"
                "1d", "day", "daily" -> "day"
                "1w", "week", "weekly" -> "week"
                "1mth", "month", "monthly" -> "month"
                else -> "15minute"
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val toDateStr = if (toDate.isNotBlank()) toDate else sdf.format(Date())
            val fromDateStr = if (fromDate.isNotBlank()) fromDate else {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -10)
                sdf.format(cal.time)
            }

            val response = upstoxApi.getHistoricalCandles(
                token = auth,
                instrumentKey = instKey,
                interval = apiInterval,
                toDate = toDateStr,
                fromDate = fromDateStr
            )

            if (!response.isSuccessful) throw Exception("Upstox HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response body")
            if (body.status != "success" || body.data?.candles == null) throw Exception("Upstox Historical Error: ${body.errors?.firstOrNull()?.message ?: "Unknown error"}")

            body.data.candles.mapNotNull { row ->
                // Format: [timestamp, open, high, low, close, volume, oi]
                if (row.size >= 5) {
                    val open = (row[1] as? Number)?.toFloat() ?: 0f
                    val high = (row[2] as? Number)?.toFloat() ?: 0f
                    val low = (row[3] as? Number)?.toFloat() ?: 0f
                    val close = (row[4] as? Number)?.toFloat() ?: 0f
                    val volume = if (row.size >= 6) (row[5] as? Number)?.toFloat() ?: 0f else 0f
                    CandleData(open = open, high = high, low = low, close = close, volume = volume)
                } else null
            }.reversed() // Upstox returns newest first; reverse for chronological order
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            _connectionState.value = "RECONNECTING"
            Log.i(TAG, "[UPSTOX_WS_RECONNECTING] Scheduling Upstox WebSocket reconnection in 5s...")
            delay(5000L)
            if (!isConnected && isConfigured()) {
                Log.i(TAG, "[UPSTOX_AUTHORIZE] Initiating reconnection: requesting new V3 authorized URL...")
                connectWebSocket()
            }
        }
    }

}
