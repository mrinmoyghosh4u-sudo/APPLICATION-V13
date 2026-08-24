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
    private val upstoxApi: UpstoxApi
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

    private val _connectionState = MutableStateFlow("DISCONNECTED")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val subscribedInstrumentKeys = ConcurrentHashMap.newKeySet<String>()
    private var isConnected = false
    private var reconnectJob: Job? = null
    private var backoffDelayMs = 1000L
    private var hasFirstTick = false
    private var lastTickReceivedTime: Long = 0L

    fun isConnectionLive(): Boolean = isConnected && _connectionState.value == "LIVE"
    fun hasFirstTickReceived(): Boolean = hasFirstTick
    fun hasActiveSubscription(): Boolean = subscribedInstrumentKeys.isNotEmpty() || isConfigured()
    fun getTickAgeMs(): Long = if (lastTickReceivedTime <= 0L) -1L else (System.currentTimeMillis() - lastTickReceivedTime).coerceAtLeast(0L)
    fun getLastUpdatedTime(): String = if (lastTickReceivedTime <= 0L) "No ticks received yet" else java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(lastTickReceivedTime))

    fun isConfigured(): Boolean {
        return !sessionManager.upstoxApiKey.isNullOrBlank() && !sessionManager.upstoxAccessToken.isNullOrBlank()
    }

    private fun getAuthHeader(): String {
        val token = sessionManager.upstoxAccessToken ?: ""
        return if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
    }

    suspend fun connect() = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            _connectionState.value = "ERROR"
            Log.w(TAG, "Cannot connect to Upstox WebSocket: credentials missing")
            return@withContext
        }
        reconnectJob?.cancel()
        connectWebSocket()
    }

    private fun connectWebSocket() {
        if (isConnected) return
        _connectionState.value = "CONNECTING"

        val token = sessionManager.upstoxAccessToken ?: return
        val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"

        scope.launch {
            try {
                // First attempt to get the authorized redirect URI from Upstox V3 Auth endpoint
                var targetUrl = WS_BASE_URL
                try {
                    val authRes = upstoxApi.getWebSocketFeedAuth(authHeader)
                    if (authRes.isSuccessful && !authRes.body()?.data?.authorizedRedirectUri.isNullOrBlank()) {
                        targetUrl = authRes.body()!!.data!!.authorizedRedirectUri!!
                        Log.d(TAG, "Obtained Upstox authorized WebSocket URL")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Using direct WebSocket URL with Bearer token header")
                }

                val request = Request.Builder()
                    .url(targetUrl)
                    .header("Authorization", authHeader)
                    .header("Accept", "*/*")
                    .build()

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.i(TAG, "UPSTOX WebSocket V3 Connected")
                        isConnected = true
                        _connectionState.value = "LIVE"
                        backoffDelayMs = 1000L

                        // Subscribe to default index scrips & any pending subscriptions
                        val defaultKeys = listOf(
                            UpstoxSymbolMapper.KEY_NIFTY_50,
                            UpstoxSymbolMapper.KEY_BANK_NIFTY,
                            UpstoxSymbolMapper.KEY_FIN_NIFTY,
                            UpstoxSymbolMapper.KEY_MIDCP_NIFTY,
                            UpstoxSymbolMapper.KEY_SENSEX,
                            UpstoxSymbolMapper.KEY_BANKEX
                        )
                        subscribedInstrumentKeys.addAll(defaultKeys)
                        sendSubscription(subscribedInstrumentKeys.toList(), mode = "full")
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        handleBinaryFeed(bytes.toByteArray())
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        // Upstox sometimes sends text acks or heartbeats
                        Log.d(TAG, "Upstox text frame: $text")
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.w(TAG, "Upstox WebSocket closed: $code / $reason")
                        isConnected = false
                        _connectionState.value = "DISCONNECTED"
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "Upstox WebSocket failure: ${t.message}")
                        isConnected = false
                        _connectionState.value = "ERROR"
                        scheduleReconnect()
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating Upstox WebSocket: ${e.message}")
                _connectionState.value = "ERROR"
                scheduleReconnect()
            }
        }
    }

    private fun handleBinaryFeed(bytes: ByteArray) {
        try {
            val response = UpstoxProtobufDecoder.decode(bytes)
            if (response.feeds.isEmpty()) return

            val now = System.currentTimeMillis()
            hasFirstTick = true
            lastTickReceivedTime = now
            response.feeds.forEach { (instrumentKey, feed) ->
                val (sym, exch) = UpstoxSymbolMapper.fromUpstoxInstrumentKey(instrumentKey)

                if (feed.ltp > 0.0) {
                    val tick = MarketTick(
                        symbol = sym,
                        token = instrumentKey,
                        exchange = exch,
                        ltp = feed.ltp,
                        open = feed.open.takeIf { it > 0.0 } ?: feed.ltp,
                        high = feed.high.takeIf { it > 0.0 } ?: feed.ltp,
                        low = feed.low.takeIf { it > 0.0 } ?: feed.ltp,
                        close = feed.close.takeIf { it > 0.0 } ?: feed.ltp,
                        volume = feed.volume,
                        timestamp = if (feed.timestamp > 0L) feed.timestamp else now
                    )
                    scope.launch {
                        marketDataEngine.updateUpstoxTick(tick)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling Upstox binary feed: ${e.message}")
        }
    }

    private fun sendSubscription(instrumentKeys: List<String>, mode: String = "full") {
        if (!isConnected || webSocket == null || instrumentKeys.isEmpty()) return
        try {
            val payload = JSONObject().apply {
                put("guid", UUID.randomUUID().toString())
                put("method", "sub")
                put("data", JSONObject().apply {
                    put("mode", mode)
                    put("instrumentKeys", JSONArray(instrumentKeys))
                })
            }.toString()

            webSocket?.send(payload)
            Log.d(TAG, "Subscribed to Upstox instruments (${instrumentKeys.size}): $instrumentKeys")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send Upstox subscription: ${e.message}")
        }
    }

    fun subscribeMarketData(symbols: List<String>) {
        val keys = symbols.map { UpstoxSymbolMapper.toUpstoxInstrumentKey(it) }
        subscribedInstrumentKeys.addAll(keys)
        if (isConnected) {
            sendSubscription(keys, mode = "full")
        }
    }

    fun unsubscribeMarketData(symbols: List<String>) {
        val keys = symbols.map { UpstoxSymbolMapper.toUpstoxInstrumentKey(it) }
        subscribedInstrumentKeys.removeAll(keys.toSet())
        if (!isConnected || webSocket == null || keys.isEmpty()) return
        try {
            val payload = JSONObject().apply {
                put("guid", UUID.randomUUID().toString())
                put("method", "unsub")
                put("data", JSONObject().apply {
                    put("instrumentKeys", JSONArray(keys))
                })
            }.toString()
            webSocket?.send(payload)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send Upstox unsubscription: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(backoffDelayMs)
            backoffDelayMs = minOf(backoffDelayMs * 2, 30000L) // Exponential backoff up to 30s
            if (!isConnected && isConfigured()) {
                Log.d(TAG, "Attempting Upstox WebSocket reconnect...")
                connectWebSocket()
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
        _connectionState.value = "DISCONNECTED"
        subscribedInstrumentKeys.clear()
        Log.d(TAG, "Upstox WebSocket disconnected")
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
}
