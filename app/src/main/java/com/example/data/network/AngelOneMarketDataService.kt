package com.example.data.network

import android.util.Log
import com.example.data.model.MarketDataStore
import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AngelOneMarketDataService(
    private val angelOneService: AngelOneBrokerService,
    private val sessionManager: SessionManager,
    private val instrumentMaster: InstrumentMasterService
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _connectionState = MutableStateFlow("DISCONNECTED")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()
    
    private var reconnectAttempt = 0
    private var isSubscribed = false
    private var hasFirstTick = false
    private var lastTickTimestamp: Long = 0L
    private var lastTickSymbol: String = ""
    private var lastTickLtp: Double = 0.0

    // Deduplicated token registry per exchange
    private val activeSubscribedTokens = ConcurrentHashMap<Int, MutableSet<String>>()

    init {
        scope.launch {
            monitorConnection()
        }
        scope.launch {
            launch {
                instrumentMaster.isLoadedFlow.collect { loaded ->
                    if (loaded && webSocket != null && (_connectionState.value == "CONNECTED" || _connectionState.value == "SUBSCRIBED" || _connectionState.value == "LIVE")) {
                        resubscribeAll(webSocket!!)
                    }
                }
            }
            instrumentMaster.loadMaster()
            connectWebSocket()
        }
    }

    fun isConnectingOrLive(): Boolean {
        val state = _connectionState.value
        return state == "CONNECTING" || state == "CONNECTED" || state == "SUBSCRIBING" || state == "SUBSCRIBED" || state == "LIVE"
    }

    private var pingJob: Job? = null

    private fun connectWebSocket(force: Boolean = false) {
        if (!force && webSocket != null && isConnectingOrLive()) {
            Log.d("SmartStream", "WebSocket connection already active in state ${_connectionState.value}. Skipping duplicate connect request.")
            return
        }

        webSocket?.cancel()
        webSocket = null
        pingJob?.cancel()

        val token = sessionManager.angelJwtToken
        val clientCode = sessionManager.angelClientId
        val feedToken = sessionManager.angelFeedToken
        
        if (token.isNullOrEmpty() || clientCode.isNullOrEmpty() || feedToken.isNullOrEmpty()) {
            _connectionState.value = "DISCONNECTED"
            return
        }

        _connectionState.value = "CONNECTING"
        if (force) {
            hasFirstTick = false
            isSubscribed = false
        }
        
        val request = Request.Builder()
            .url("wss://smartapisocket.angelone.in/smart-stream")
            .header("Authorization", "Bearer $token")
            .header("x-api-key", sessionManager.angelApiKey)
            .header("x-client-code", clientCode)
            .header("x-feed-token", feedToken)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "CONNECTED"
                reconnectAttempt = 0
                Log.d("SmartStream", "[WEBSOCKET_CONNECTED]")
                
                pingJob = scope.launch {
                    while (true) {
                        delay(30_000)
                        try {
                            webSocket.send("ping")
                        } catch (e: Exception) {
                            break
                        }
                    }
                }
                
                if (instrumentMaster.isLoaded) {
                    resubscribeAll(webSocket)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.optBoolean("status", false)) {
                        _connectionState.value = "SUBSCRIBED"
                        isSubscribed = true
                        Log.d("SmartStream", "[SUBSCRIPTION_RESPONSE] status=true")
                    }
                } catch (e: Exception) {
                    Log.e("AngelOneMarketDataService", "Error parsing text message", e)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleBinaryTick(bytes.toByteArray())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "DISCONNECTED"
                isSubscribed = false
                hasFirstTick = false
                com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.ANGEL_ONE, "OFFLINE")
                Log.d("SmartStream", "WebSocket Closed: $reason")
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = "ERROR"
                isSubscribed = false
                hasFirstTick = false
                com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.ANGEL_ONE, "OFFLINE")
                Log.e("SmartStream", "WebSocket Failure: ${t.message}")
                scheduleReconnect()
            }
        })
    }
    
    private fun scheduleReconnect() {
        if (reconnectAttempt > 5) {
            _connectionState.value = "DISCONNECTED"
            reconnectAttempt = 5
        }
        val delayTime = (1 shl reconnectAttempt) * 1000L
        reconnectAttempt++
        _connectionState.value = "RECONNECTING"
        
        scope.launch {
            delay(delayTime)
            connectWebSocket(force = true)
        }
    }

    private fun resubscribeAll(ws: WebSocket) {
        if (!instrumentMaster.isLoaded) {
            Log.d("SmartStream", "Instrument Master not loaded yet. Delaying subscription until loaded.")
            return
        }

        // Register core indices via Instrument Master lookup
        val indices = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX", "CRUDEOIL", "CRUDEOIL M")
        for (index in indices) {
            val inst = instrumentMaster.resolveIndexToken(index)
            if (inst != null) {
                val exType = InstrumentMasterService.getExchangeType(inst.exch_seg)
                activeSubscribedTokens.getOrPut(exType) { ConcurrentHashMap.newKeySet() }.add(inst.token)
            }
        }

        // Register default stocks via Instrument Master lookup
        val watchlistStockSymbols = listOf("RELIANCE", "TCS", "INFY", "SBIN", "HDFCBANK", "ICICIBANK", "TATAMOTORS", "TATASTEEL")
        for (sym in watchlistStockSymbols) {
            val token = instrumentMaster.resolveAngelToken(sym, "NSE")
            if (!token.isNullOrBlank()) {
                activeSubscribedTokens.getOrPut(1) { ConcurrentHashMap.newKeySet() }.add(token)
            }
        }

        if (activeSubscribedTokens.isNotEmpty()) {
            val tokenListJson = JSONArray()
            var totalTokens = 0
            for ((exType, tokens) in activeSubscribedTokens) {
                if (tokens.isNotEmpty()) {
                    totalTokens += tokens.size
                    tokenListJson.put(JSONObject().apply {
                        put("exchangeType", exType)
                        put("tokens", JSONArray(tokens.toList()))
                    })
                }
            }
            if (tokenListJson.length() > 0) {
                val req = JSONObject().apply {
                    put("correlationID", "market_sub_${System.currentTimeMillis()}")
                    put("action", 1)
                    put("params", JSONObject().apply {
                        put("mode", 1) // Mode 1: LTP
                        put("tokenList", tokenListJson)
                    })
                }
                ws.send(req.toString())
                _connectionState.value = "SUBSCRIBING"
                isSubscribed = true
                Log.d("SmartStream", "[SUBSCRIPTION_SENT] count=$totalTokens")
            }
        }
    }

    fun subscribeToTokens(exchangeType: Int, tokens: List<String>) {
        if (tokens.isEmpty()) return
        val currentSet = activeSubscribedTokens.getOrPut(exchangeType) { ConcurrentHashMap.newKeySet() }
        val newTokens = tokens.filter { !currentSet.contains(it) }
        if (newTokens.isEmpty()) return // Deduplicated

        currentSet.addAll(newTokens)
        val ws = webSocket ?: return

        val req = JSONObject().apply {
            put("correlationID", "dynamic_sub_${System.currentTimeMillis()}")
            put("action", 1)
            put("params", JSONObject().apply {
                put("mode", 1)
                put("tokenList", JSONArray().apply {
                    put(JSONObject().apply {
                        put("exchangeType", exchangeType)
                        put("tokens", JSONArray(newTokens))
                    })
                })
            })
        }
        ws.send(req.toString())
        Log.d("SmartStream", "[SUBSCRIPTION_SENT] dynamic_tokens=${newTokens.size} exchangeType=$exchangeType")
    }

    private fun handleBinaryTick(bytes: ByteArray) {
        try {
            if (bytes.size < 51) {
                return
            }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val subscriptionMode = buffer.get()
            val exchangeType = buffer.get().toInt()
            
            if (exchangeType !in 1..13) return

            val tokenBytes = ByteArray(25)
            buffer.get(tokenBytes)
            val token = String(tokenBytes, Charsets.US_ASCII).trimEnd('\u0000', ' ').trim()
            
            if (token.isBlank()) return

            val sequenceNumber = buffer.getLong()
            val exchangeTimestamp = buffer.getLong()
            val ltpInt = buffer.getLong() // 8-byte integer for LTP
            
            if (ltpInt <= 0) return
            
            val divisor = if (exchangeType == 9 || exchangeType == 13) 10000000.0 else 100.0
            val ltp = ltpInt / divisor
            if (ltp <= 0.0) return

            var open = 0.0
            var high = 0.0
            var low = 0.0
            var close = 0.0
            var volume = 0L

            if (bytes.size >= 123) {
                val lastTradedQty = buffer.getLong()
                val avgPriceInt = buffer.getLong()
                volume = buffer.getLong()
                val totalBuyQty = buffer.getLong()
                val totalSellQty = buffer.getLong()
                val openInt = buffer.getLong()
                val highInt = buffer.getLong()
                val lowInt = buffer.getLong()
                val closeInt = buffer.getLong()

                open = openInt / divisor
                high = highInt / divisor
                low = lowInt / divisor
                close = closeInt / divisor
            }
            
            // Composite lookup: "$exchangeType:$token"
            val inst = instrumentMaster.getInstrumentByToken(token, exchangeType)
            val symbol = inst?.symbol ?: token
            val exchange = inst?.exch_seg ?: when (exchangeType) {
                1 -> "NSE"
                2 -> "NFO"
                3 -> "BSE"
                4 -> "BFO"
                5 -> "MCX"
                else -> "NSE"
            }
            
            if (!hasFirstTick || _connectionState.value != "LIVE") {
                hasFirstTick = true
                _connectionState.value = "LIVE"
                Log.d("SmartStream", "[LIVE]")
            }
            lastTickTimestamp = System.currentTimeMillis()
            lastTickSymbol = symbol
            lastTickLtp = ltp
            
            Log.d("SmartStream", "[REAL_TICK_RECEIVED] symbol=$symbol exch=$exchange ltp=$ltp ts=$exchangeTimestamp")
            
            MarketDataStore.updateTick(
                source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                symbol = symbol,
                token = token,
                exchange = exchange,
                ltp = ltp,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume,
                exchangeTimestamp = if (exchangeTimestamp > 0) exchangeTimestamp else System.currentTimeMillis(),
                receivedTimestamp = System.currentTimeMillis(),
                state = "LIVE",
                sequenceNumber = sequenceNumber
            )

            // Also map to alias index names if this token matches a known index/commodity contract
            val crudeInst = instrumentMaster.resolveIndexToken("CRUDEOIL")
            val crudeMInst = instrumentMaster.resolveIndexToken("CRUDEOIL M")
            val niftyInst = instrumentMaster.resolveIndexToken("NIFTY")
            val bankNiftyInst = instrumentMaster.resolveIndexToken("BANKNIFTY")
            val finNiftyInst = instrumentMaster.resolveIndexToken("FINNIFTY")
            val midcpInst = instrumentMaster.resolveIndexToken("MIDCPNIFTY")
            val sensexInst = instrumentMaster.resolveIndexToken("SENSEX")
            val bankexInst = instrumentMaster.resolveIndexToken("BANKEX")

            when (token) {
                crudeInst?.token -> MarketDataStore.updateTick(
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    symbol = "CRUDEOIL",
                    token = token,
                    exchange = "MCX",
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    receivedTimestamp = System.currentTimeMillis(),
                    state = "LIVE",
                    sequenceNumber = sequenceNumber
                )
                crudeMInst?.token -> MarketDataStore.updateTick(
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    symbol = "CRUDEOIL M",
                    token = token,
                    exchange = "MCX",
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    receivedTimestamp = System.currentTimeMillis(),
                    state = "LIVE",
                    sequenceNumber = sequenceNumber
                )
                niftyInst?.token -> MarketDataStore.updateTick(
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    symbol = "NIFTY 50",
                    token = token,
                    exchange = "NSE",
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    receivedTimestamp = System.currentTimeMillis(),
                    state = "LIVE",
                    sequenceNumber = sequenceNumber
                )
                bankNiftyInst?.token -> MarketDataStore.updateTick(
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    symbol = "BANKNIFTY",
                    token = token,
                    exchange = "NSE",
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    receivedTimestamp = System.currentTimeMillis(),
                    state = "LIVE",
                    sequenceNumber = sequenceNumber
                )
                finNiftyInst?.token -> MarketDataStore.updateTick(
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    symbol = "FINNIFTY",
                    token = token,
                    exchange = "NSE",
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    receivedTimestamp = System.currentTimeMillis(),
                    state = "LIVE",
                    sequenceNumber = sequenceNumber
                )
                midcpInst?.token -> MarketDataStore.updateTick(
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    symbol = "MIDCPNIFTY",
                    token = token,
                    exchange = "NSE",
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    receivedTimestamp = System.currentTimeMillis(),
                    state = "LIVE",
                    sequenceNumber = sequenceNumber
                )
                sensexInst?.token -> MarketDataStore.updateTick(
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    symbol = "SENSEX",
                    token = token,
                    exchange = "BSE",
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    receivedTimestamp = System.currentTimeMillis(),
                    state = "LIVE",
                    sequenceNumber = sequenceNumber
                )
                bankexInst?.token -> MarketDataStore.updateTick(
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    symbol = "BANKEX",
                    token = token,
                    exchange = "BSE",
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    receivedTimestamp = System.currentTimeMillis(),
                    state = "LIVE",
                    sequenceNumber = sequenceNumber
                )
            }
            Log.d("SmartStream", "[MARKET_DATA_STORE_UPDATED] symbol=$symbol ltp=$ltp")
            
        } catch (e: Exception) {
            Log.e("SmartStreamParser", "Error parsing binary tick: ${e.message}")
        }
    }

    fun isConnectionLive(): Boolean {
        val age = getTickAgeMs()
        return (_connectionState.value == "LIVE" || _connectionState.value == "SUBSCRIBED" || _connectionState.value == "CONNECTED") && age >= 0L && age <= 15000L
    }

    fun hasFirstTickReceived(): Boolean = hasFirstTick || lastTickTimestamp > 0L

    fun hasActiveSubscription(): Boolean = isSubscribed

    fun getTickAgeMs(): Long {
        if (lastTickTimestamp <= 0L) return -1L
        return (System.currentTimeMillis() - lastTickTimestamp).coerceAtLeast(0L)
    }

    fun getLastUpdatedTime(): String {
        return if (lastTickTimestamp > 0) {
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastTickTimestamp))
        } else ""
    }

    fun connect() {
        connectWebSocket()
    }

    fun disconnect() {
        webSocket?.cancel()
        webSocket = null
        pingJob?.cancel()
        _connectionState.value = "DISCONNECTED"
    }

    fun reconnect() {
        connectWebSocket(force = true)
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<com.example.ui.components.CandleData>> {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val toDate = format.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -5)
        val fromDate = format.format(cal.time)
        
        val angelInterval = when (interval) {
            "1m" -> "ONE_MINUTE"
            "5m" -> "FIVE_MINUTE"
            "15m" -> "FIFTEEN_MINUTE"
            "30m" -> "THIRTY_MINUTE"
            "1h" -> "ONE_HOUR"
            "1d" -> "ONE_DAY"
            else -> "FIFTEEN_MINUTE"
        }
        
        return angelOneService.getHistoricalCandles(symbol, angelInterval, fromDate, toDate)
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return angelOneService.getMarketQuotes(symbols)
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        val httpResult = angelOneService.getOptionChain(symbol, expiry)
        
        if (instrumentMaster.isLoaded) {
            val options = instrumentMaster.getOptionInstruments(symbol, expiry)
            if (options.isNotEmpty()) {
                val exchSeg = options.firstOrNull()?.exch_seg ?: "NFO"
                val exchType = InstrumentMasterService.getExchangeType(exchSeg)
                val tokensToSubscribe = options.map { it.token }
                subscribeToTokens(exchType, tokensToSubscribe)
                
                if (httpResult.isSuccess && httpResult.getOrDefault(emptyList()).isNotEmpty()) {
                    val strikes = httpResult.getOrDefault(emptyList()).map { item ->
                        val ceOpt = options.find { 
                            val s = (it.strike.toDoubleOrNull() ?: 0.0).let { raw -> if (raw > 10000 && item.strikePrice < 10000) raw / 100.0 else if (raw > 100000 && item.strikePrice < 100000) raw / 100.0 else raw }
                            kotlin.math.abs(s - item.strikePrice) < 0.01 && (it.symbol.endsWith("CE") || it.symbol.contains("CE"))
                        }
                        val peOpt = options.find { 
                            val s = (it.strike.toDoubleOrNull() ?: 0.0).let { raw -> if (raw > 10000 && item.strikePrice < 10000) raw / 100.0 else if (raw > 100000 && item.strikePrice < 100000) raw / 100.0 else raw }
                            kotlin.math.abs(s - item.strikePrice) < 0.01 && (it.symbol.endsWith("PE") || it.symbol.contains("PE"))
                        }
                        
                        val ceLive = if (ceOpt != null) MarketDataStore.getTick(ceOpt.symbol) ?: MarketDataStore.getTickByToken(exchSeg, ceOpt.token) else null
                        val peLive = if (peOpt != null) MarketDataStore.getTick(peOpt.symbol) ?: MarketDataStore.getTickByToken(exchSeg, peOpt.token) else null
                        
                        item.copy(
                            callLtp = ceLive?.ltp ?: item.callLtp,
                            putLtp = peLive?.ltp ?: item.putLtp
                        )
                    }
                    return Result.success(strikes)
                } else {
                    // Assemble option strike items from official Instrument Master loaded instruments
                    val groupedByStrike = options.groupBy { inst ->
                        val rawStrike = inst.strike.toDoubleOrNull() ?: 0.0
                        if (rawStrike > 100000) rawStrike / 100.0 else rawStrike
                    }
                    val strikes = groupedByStrike.map { (strike, instList) ->
                        val ceInst = instList.find { it.symbol.endsWith("CE") || it.symbol.contains("CE") }
                        val peInst = instList.find { it.symbol.endsWith("PE") || it.symbol.contains("PE") }
                        val ceLive = if (ceInst != null) MarketDataStore.getTick(ceInst.symbol) ?: MarketDataStore.getTickByToken(exchSeg, ceInst.token) else null
                        val peLive = if (peInst != null) MarketDataStore.getTick(peInst.symbol) ?: MarketDataStore.getTickByToken(exchSeg, peInst.token) else null
                        
                        OptionStrikeItem(
                            strikePrice = strike,
                            callOi = "-",
                            callChgOi = "-",
                            callIv = 0.0,
                            callLtp = ceLive?.ltp ?: 0.0,
                            callDelta = 0.0,
                            putDelta = 0.0,
                            putLtp = peLive?.ltp ?: 0.0,
                            putIv = 0.0,
                            putChgOi = "-",
                            putOi = "-",
                            callVolume = "${ceLive?.volume ?: 0}",
                            putVolume = "${peLive?.volume ?: 0}"
                        )
                    }.sortedBy { it.strikePrice }
                    if (strikes.isNotEmpty()) {
                        return Result.success(strikes)
                    }
                }
            }
        }
        return httpResult
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        val httpResult = angelOneService.getOptionExpiries(symbol)
        if (httpResult.isSuccess && httpResult.getOrDefault(emptyList()).isNotEmpty()) {
            return httpResult
        }
        if (instrumentMaster.isLoaded) {
            val expiries = instrumentMaster.getOptionExpiries(symbol)
            if (expiries.isNotEmpty()) {
                return Result.success(expiries)
            }
        }
        return httpResult
    }

    private suspend fun monitorConnection() {
        while (true) {
            delay(15000)
            if (_connectionState.value == "LIVE") {
                if (System.currentTimeMillis() - lastTickTimestamp > 15000) {
                    _connectionState.value = "STALE"
                    Log.w("AngelOneMarketData", "No ticks received for 15s. Marking connection STALE.")
                }
            }
        }
    }
}

