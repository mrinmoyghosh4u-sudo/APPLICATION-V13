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
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FyersMarketDataService(
    private val sessionManager: SessionManager,
    private val marketDataEngine: MarketDataEngine,
    private val fyersApi: FyersApi
) {

    private val TAG = "FyersMarketDataService"
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _connectionState = MutableStateFlow("DISCONNECTED")
    val connectionState: StateFlow<String> = _connectionState

    private val subscribedSymbols = mutableSetOf<String>()
    private var isConnected = false
    private var reconnectJob: Job? = null
    
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

    fun isConfigured(): Boolean {
        return !sessionManager.fyersAppId.isNullOrBlank() && !sessionManager.fyersAccessToken.isNullOrBlank()
    }

    suspend fun connect() {
        if (!isConfigured()) {
            _connectionState.value = "ERROR"
            Log.e(TAG, "Cannot connect: Fyers credentials missing")
            return
        }
        reconnectJob?.cancel()
        connectWebSocket()
    }

    private fun connectWebSocket() {
        if (isConnected) return
        _connectionState.value = "CONNECTING"
        
        val appId = sessionManager.fyersAppId ?: return
        val token = sessionManager.fyersAccessToken ?: return
        
        val fyersToken = "$appId:$token"
        
        val url = "wss://api.fyers.in/socket/v2/data/"
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", fyersToken)
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "FYERS WebSocket connected")
                isConnected = true
                _connectionState.value = "LIVE"
                
                // Resubscribe symbols
                if (subscribedSymbols.isNotEmpty()) {
                    subscribeSymbols(subscribedSymbols.toList(), "symbolUpdate")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                // FYERS sends binary data typically for ticks
                handleBinaryMessage(bytes.toByteArray())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                isConnected = false
                _connectionState.value = "DISCONNECTED"
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                isConnected = false
                _connectionState.value = "ERROR"
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(5000)
            if (!isConnected) {
                Log.d(TAG, "Attempting reconnect...")
                connectWebSocket()
            }
        }
    }

    suspend fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
        _connectionState.value = "DISCONNECTED"
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
        
        if (!ltp.isNaN()) {
            val tick = MarketTick(
                symbol = symbol,
                ltp = ltp,
                open = if (open.isNaN()) ltp else open,
                high = if (high.isNaN()) ltp else high,
                low = if (low.isNaN()) ltp else low,
                close = if (close.isNaN()) ltp else close,
                volume = vol,
                timestamp = System.currentTimeMillis(),
                exchange = "NSE"
            )
            scope.launch { marketDataEngine.updateFyersTick(tick) }
        }
    }

    private fun parseLiteMode(buffer: java.nio.ByteBuffer) {
        if (buffer.remaining() < 6) return
        val topicId = buffer.short.toInt()
        val ltpVal = buffer.int
        
        val symbol = topicToSymbolMap[topicId] ?: return
        val multiplier = topicToMultiplierMap[topicId]?.toDouble() ?: 100.0
        
        if (ltpVal != -2147483648) {
            val tick = MarketTick(
                symbol = symbol,
                ltp = ltpVal / multiplier,
                open = ltpVal / multiplier,
                high = ltpVal / multiplier,
                low = ltpVal / multiplier,
                close = ltpVal / multiplier,
                volume = 0L,
                timestamp = System.currentTimeMillis(),
                exchange = "NSE"
            )
            scope.launch { marketDataEngine.updateFyersTick(tick) }
        }
    }

    private fun parseJsonTick(json: JSONObject) {
        // If they send JSON ticks
        try {
            val symbol = json.optString("symbol", "")
            val ltp = json.optDouble("ltp", Double.NaN)
            val open = json.optDouble("open", Double.NaN).takeIf { !it.isNaN() }
            val high = json.optDouble("high", Double.NaN).takeIf { !it.isNaN() }
            val low = json.optDouble("low", Double.NaN).takeIf { !it.isNaN() }
            val close = json.optDouble("close", Double.NaN).takeIf { !it.isNaN() }
            val volume = json.optLong("volume", -1).takeIf { it != -1L }
            val ts = json.optLong("timestamp", System.currentTimeMillis())
            
            if (symbol.isNotEmpty() && !ltp.isNaN()) {
                val tick = MarketTick(
                    symbol = symbol,
                    ltp = ltp,
                    open = open ?: ltp,
                    high = high ?: ltp,
                    low = low ?: ltp,
                    close = close ?: ltp,
                    timestamp = ts,
                    volume = volume ?: 0L,
                    exchange = "NSE"
                )
                scope.launch {
                    marketDataEngine.updateFyersTick(tick)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing tick: ${e.message}")
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