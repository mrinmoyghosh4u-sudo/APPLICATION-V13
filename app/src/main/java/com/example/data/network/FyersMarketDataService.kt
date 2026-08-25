package com.example.data.network

import android.util.Log
import com.example.data.model.HistoricalCandle
import com.example.data.model.IndexQuote
import com.example.data.model.MarketBreadth
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
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    private val subscribedSymbols = mutableSetOf<String>()
    private var isConnected = false
    private var reconnectJob: Job? = null
    private var hasFirstTick = false
    private var lastTickReceivedTime: Long = 0L

    // For parsing
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

    fun isConnectionLive(): Boolean = isConnected && _connectionState.value == "LIVE"
    fun hasFirstTickReceived(): Boolean = hasFirstTick
    fun hasActiveSubscription(): Boolean = subscribedSymbols.isNotEmpty() || isConfigured()
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
            Log.e(TAG, "[FYERS_AUTH_FAILED] Cannot connect: Fyers credentials missing")
            return
        }
        _connectionState.value = "AUTHENTICATED"
        reconnectJob?.cancel()
        backoffDelayMs = 2000L
        connectWebSocket()
    }

    private fun connectWebSocket() {
        if (isConnected) return
        Log.d(TAG, "[FYERS_AUTH_START] Initiating FYERS WebSocket connection...")
        _connectionState.value = "CONNECTING"
        healthManager?.reportConnecting(ProviderHealthManager.PROVIDER_FYERS)
        
        val appId = sessionManager.fyersAppId
        val token = sessionManager.fyersAccessToken
        if (appId.isNullOrBlank() || token.isNullOrBlank()) {
            _connectionState.value = "AUTH_FAILED"
            healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, false, "Credentials missing")
            Log.e(TAG, "[FYERS_AUTH_FAILED] Fyers appId or token missing")
            return
        }
        
        val fyersToken = "$appId:$token"
        val url = "wss://api.fyers.in/socket/v2/data/"
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", fyersToken)
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "[FYERS_WS_OPEN] FYERS WebSocket connected")
                isConnected = true
                backoffDelayMs = 2000L
                _connectionState.value = "CONNECTED"
                com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "CONNECTED")
                healthManager?.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
                healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)

                _connectionState.value = "SUBSCRIBING"
                healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_FYERS)
                
                // Subscribe using official JSON format with type 'lite'
                val symbolsToSub = if (subscribedSymbols.isNotEmpty()) {
                    subscribedSymbols.toList()
                } else {
                    listOf("NSE:NIFTY50-INDEX")
                }
                subscribedSymbols.addAll(symbolsToSub)

                val payload = JSONObject().apply {
                    put("symbol", JSONArray(symbolsToSub))
                    put("type", "lite")
                }.toString()
                
                webSocket.send(payload)
                Log.i(TAG, "[FYERS_SUBSCRIBE_SENT] Sent subscription for $symbolsToSub")

                _connectionState.value = "WAITING_FOR_FIRST_TICK"
                healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_FYERS, symbolsToSub.size)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "[FYERS_MESSAGE_RECEIVED] Text message received")
                handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                Log.d(TAG, "[FYERS_MESSAGE_RECEIVED] Binary message received (${bytes.size} bytes)")
                handleBinaryMessage(bytes.toByteArray())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "[FYERS_DISCONNECTED] WebSocket closed: $code / $reason")
                isConnected = false
                _connectionState.value = "DISCONNECTED"
                com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "OFFLINE")
                healthManager?.reportDisconnected(ProviderHealthManager.PROVIDER_FYERS)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "[FYERS_DISCONNECTED] WebSocket failure: ${t.message}")
                isConnected = false
                _connectionState.value = "ERROR"
                com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "OFFLINE")
                healthManager?.reportError(ProviderHealthManager.PROVIDER_FYERS, t.message ?: "WebSocket failure")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(backoffDelayMs)
            backoffDelayMs = minOf(backoffDelayMs * 2, 30000L) // Exponential backoff up to 30s
            if (!isConnected && isConfigured()) {
                Log.d(TAG, "Attempting FYERS WebSocket reconnect...")
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
        com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "OFFLINE")
        subscribedSymbols.clear()
        Log.d(TAG, "FYERS WebSocket disconnected")
    }

    suspend fun subscribeToMarketData(symbols: List<String>) {
        subscribeSymbols(symbols, "symbolUpdate")
    }

    fun subscribeSymbols(symbols: List<String>, type: String = "symbolUpdate") {
        if (!isConnected) {
            subscribedSymbols.addAll(symbols)
            return
        }
        
        val payload = JSONObject().apply {
            put("symbol", org.json.JSONArray(symbols))
            put("type", type)
        }.toString()
        
        webSocket?.send(payload)
        subscribedSymbols.addAll(symbols)
        Log.d(TAG, "Symbol subscribed: $symbols")
    }

    fun unsubscribeSymbols(symbols: List<String>) {
        // As per generic unsubscription, might just be 'unsubscribe' command in real Fyers, 
        // but for now we remove from our tracking.
        subscribedSymbols.removeAll(symbols.toSet())
        Log.d(TAG, "Symbol unsubscribed: $symbols")
    }

    suspend fun unsubscribeMarketData(symbols: List<String>) {
        unsubscribeSymbols(symbols)
    }

    private fun handleTextMessage(text: String) {
        try {
            if (text.startsWith("{")) {
                val json = JSONObject(text)
                parseJsonTick(json)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private val topicToSymbolMap = mutableMapOf<Int, String>()
    private val topicToMultiplierMap = mutableMapOf<Int, Int>()

    private fun handleBinaryMessage(bytes: ByteArray) {
        try {
            val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
            if (buffer.remaining() < 3) return
            val length = buffer.short.toInt()
            val respType = buffer.get().toInt()

            when (respType) {
                6 -> parseTopicInit(buffer)
                85 -> parseFullMode(buffer)
                76 -> parseLiteMode(buffer)
                else -> {
                    // Try JSON fallback for other types
                    val str = String(bytes)
                    if (str.startsWith("{")) {
                        val json = JSONObject(str)
                        parseJsonTick(json)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing FYERS binary message: ${e.message}")
        }
    }

    private fun parseTopicInit(buffer: java.nio.ByteBuffer) {
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
                
                // Fields
                for (j in 0 until fieldCount) {
                    if (buffer.remaining() < 4) break
                    buffer.int
                }
                
                if (buffer.remaining() < 3) break
                val multiplier = buffer.short.toInt()
                topicToMultiplierMap[topicId] = multiplier
                buffer.get() // precision
                
                // exchange
                if (buffer.remaining() < 1) break
                val exLen = buffer.get().toInt()
                if (buffer.remaining() < exLen) break
                buffer.position(buffer.position() + exLen)
                
                // exchange_token
                if (buffer.remaining() < 1) break
                val extLen = buffer.get().toInt()
                if (buffer.remaining() < extLen) break
                buffer.position(buffer.position() + extLen)
                
                // symbol
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

    private fun parseFullMode(buffer: java.nio.ByteBuffer) {
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
        }
    }

    private fun detectExchange(symbol: String): String {
        return when {
            symbol.startsWith("BSE:", ignoreCase = true) -> "BSE"
            symbol.startsWith("MCX:", ignoreCase = true) -> "MCX"
            else -> "NSE"
        }
    }

    private fun parseLiteMode(buffer: java.nio.ByteBuffer) {
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
        }
    }

    private fun parseJsonTick(json: JSONObject) {
        try {
            // Check for array in 'd' field
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
                        }
                    }
                    return
                }
            }

            // Direct object tick
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
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing FYERS JSON tick: ${e.message}")
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
        val now = System.currentTimeMillis()
        Log.i(TAG, "[FYERS_LTP_RECEIVED] Symbol: $rawSymbol, LTP: $ltp")

        if (!hasFirstTick) {
            Log.i(TAG, "[FYERS_FIRST_REAL_TICK] First valid FYERS real tick received: $rawSymbol = $ltp")
        }
        hasFirstTick = true
        lastTickReceivedTime = now
        _connectionState.value = "LIVE"
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
            Log.i(TAG, "[FYERS_TICK_FORWARDED] Forwarded $standardSym ($rawSymbol) LTP $ltp to MarketDataEngine")
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
            if (body.s != "ok" || body.d == null) throw Exception("Fyers API Error")
            
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
            if (body.s != "ok" || body.candles == null) throw Exception("Fyers API Error")
            
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
            if (body.s != "ok" || body.data?.expiryData == null) throw Exception("Fyers API Error")
            
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
            if (body.s != "ok" || body.data?.expiryData == null) throw Exception("Fyers API Error")
            
            body.data.expiryData.mapNotNull { it.expiry }
        }
    }
}