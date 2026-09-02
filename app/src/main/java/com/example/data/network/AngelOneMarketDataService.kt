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
    val instrumentMaster: InstrumentMasterService,
    private val healthManager: ProviderHealthManager? = null
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    
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

    private var monitorJob: Job? = null

    init {
        monitorJob = scope.launch {
            monitorConnection()
        }
        scope.launch {
            launch {
                instrumentMaster.isLoadedFlow.collect { loaded ->
                    val ws = webSocket
                    if (loaded && ws != null && (_connectionState.value == "CONNECTED" || _connectionState.value == "SUBSCRIBED" || _connectionState.value == "LIVE")) {
                        resubscribeAll(ws)
                    }
                }
            }
            instrumentMaster.loadMaster()
            connectWebSocket()
        }
    }


    fun isConfigured(): Boolean {
        val token = sessionManager.angelJwtToken?.takeIf { it.isNotBlank() } ?: sessionManager.angelAuthToken
        val cCode = sessionManager.angelClientCode.takeIf { it.isNotBlank() } ?: sessionManager.angelClientId
        return !token.isNullOrBlank() && !cCode.isNullOrBlank() && !sessionManager.angelFeedToken.isNullOrBlank()
    }

    fun isConnectingOrLive(): Boolean {
        val state = _connectionState.value
        return state == "CONNECTING" || state == "CONNECTED" || state == "SUBSCRIBING" || state == "SUBSCRIBED" || state == "LIVE"
    }

    private fun connectWebSocket(force: Boolean = false) {
        if (!force && webSocket != null && isConnectingOrLive()) {
            Log.d("SmartStream", "WebSocket connection already active in state ${_connectionState.value}. Skipping duplicate connect request.")
            return
        }

        webSocket?.cancel()
        webSocket = null

        val token = sessionManager.angelJwtToken?.takeIf { it.isNotBlank() } ?: sessionManager.angelAuthToken
        val clientCode = sessionManager.angelClientCode.takeIf { it.isNotBlank() } ?: sessionManager.angelClientId
        val feedToken = sessionManager.angelFeedToken
        
        if (token.isNullOrBlank() || clientCode.isNullOrBlank() || feedToken.isNullOrBlank()) {
            _connectionState.value = "DISCONNECTED"
            return
        }

        _connectionState.value = "CONNECTING"
        healthManager?.reportConnecting(ProviderHealthManager.PROVIDER_ANGEL_ONE)
        if (force) {
            hasFirstTick = false
            isSubscribed = false
        }
        
        val cleanAuth = token.removePrefix("Bearer ").removePrefix("bearer ").trim()
        val request = Request.Builder()
            .url("wss://smartapisocket.angelone.in/smart-stream")
            .header("Authorization", cleanAuth)
            .header("x-api-key", sessionManager.angelApiKey)
            .header("x-client-code", clientCode)
            .header("x-feed-token", feedToken)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "CONNECTED"
                reconnectAttempt = 0
                healthManager?.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, true)
                Log.d("SmartStream", "[WEBSOCKET_CONNECTED]")
                
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
                healthManager?.reportDisconnected(ProviderHealthManager.PROVIDER_ANGEL_ONE)
                com.example.data.model.MarketDataStore.setAngelHealth("OFFLINE")
                Log.d("SmartStream", "WebSocket Closed: $reason, code: $code")
                if (code == 4401 || code == 4403 || code == 4001) {
                    _connectionState.value = "AUTH_FAILED"
                    return
                }
                if (code != 1000 && code != 1008 && code != 1001) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = "ERROR"
                isSubscribed = false
                hasFirstTick = false
                healthManager?.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, false)
                com.example.data.model.MarketDataStore.setAngelHealth("ERROR")
                Log.e("SmartStream", "WebSocket Failure: ${t.message}, code=${response?.code}")
                if (response?.code == 401 || response?.code == 403) {
                    Log.w("SmartStream", "Angel One WebSocket Auth Failed (code ${response.code}). Breaking reconnect loop.")
                    _connectionState.value = "AUTH_FAILED"
                    return
                }
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

        // Register core indices and stock universe via AngelOneInstrumentResolver
        val resolver = AngelOneInstrumentResolver(instrumentMaster)
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
        for ((sym, exch) in universe) {
            val resolved = resolver.resolve(sym, exch)
            if (resolved != null && resolved.token.isNotBlank()) {
                val exType = InstrumentMasterService.getExchangeType(resolved.exchange)
                activeSubscribedTokens.getOrPut(exType) { ConcurrentHashMap.newKeySet() }.add(resolved.token)
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
                com.example.data.model.MarketDataStore.reportProviderSubscribed(com.example.data.model.MarketDataProviders.ANGEL_ONE, totalTokens)
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
        isSubscribed = true
        healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_ANGEL_ONE, currentSet.size)
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
            val exchange = inst?.let { InstrumentMasterService.normalizeExchange(it.exch_seg) } ?: when (exchangeType) {
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
                com.example.data.model.MarketDataStore.setAngelHealth( "LIVE")
                Log.d("SmartStream", "[LIVE]")
            }
            lastTickTimestamp = System.currentTimeMillis()
            lastTickSymbol = symbol
            lastTickLtp = ltp
            
            Log.d("SmartStream", "[REAL_TICK_RECEIVED] symbol=$symbol exch=$exchange ltp=$ltp ts=$exchangeTimestamp")
            
            MarketDataStore.updateTick(
                com.example.data.model.RealTimePriceTick(
                    symbol = symbol,
                    price = ltp,
                    timestamp = if (exchangeTimestamp > 0) exchangeTimestamp else System.currentTimeMillis(),
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    volume = volume,
                    exchange = exchange
                )
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

            val sym = when (token) {
                crudeInst?.token -> "CRUDEOIL"
                crudeMInst?.token -> "CRUDEOIL M"
                niftyInst?.token -> "NIFTY 50"
                bankNiftyInst?.token -> "BANKNIFTY"
                finNiftyInst?.token -> "FINNIFTY"
                midcpInst?.token -> "MIDCPNIFTY"
                sensexInst?.token -> "SENSEX"
                bankexInst?.token -> "BANKEX"
                else -> symbol
            }
            val exch = if (sym == "SENSEX" || sym == "BANKEX") "BSE" else if (sym.contains("CRUDE")) "MCX" else "NSE"
            MarketDataStore.updateTick(
                com.example.data.model.RealTimePriceTick(
                    symbol = sym,
                    price = ltp,
                    timestamp = System.currentTimeMillis(),
                    source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                    volume = volume,
                    exchange = exch
                )
            )
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
        monitorJob?.cancel()
        _connectionState.value = "DISCONNECTED"
        com.example.data.model.MarketDataStore.setAngelHealth("OFFLINE")
    }

    fun reconnect() {
        connectWebSocket(force = true)
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<com.example.data.model.HistoricalCandle>> {
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
                        
                        val ceLive = if (ceOpt != null) MarketDataStore.getTick(ceOpt.symbol) else null
                        val peLive = if (peOpt != null) MarketDataStore.getTick(peOpt.symbol) else null
                        
                        item.copy(
                            callLtp = ceLive?.price ?: item.callLtp,
                            putLtp = peLive?.price ?: item.putLtp,
                            callToken = ceOpt?.token ?: "",
                            putToken = peOpt?.token ?: "",
                            callSymbol = ceOpt?.symbol ?: "",
                            putSymbol = peOpt?.symbol ?: "",
                            expiry = expiry,
                            underlying = symbol
                        )
                    }
                    return Result.success(strikes)
                } else {
                    // Extract strikes directly from real Master Option contracts
                    val strikeMap = mutableMapOf<Double, Pair<Instrument?, Instrument?>>()
                    options.forEach { opt ->
                        val rawStrike = opt.strike.toDoubleOrNull() ?: 0.0
                        val sp = if (rawStrike > 100000) rawStrike / 100.0 else if (rawStrike > 10000 && (symbol.contains("NIFTY", true) || symbol.contains("SENSEX", true) || symbol.contains("BANKEX", true))) rawStrike / 100.0 else rawStrike
                        if (sp > 0.0) {
                            val current = strikeMap.getOrDefault(sp, Pair(null, null))
                            if (opt.symbol.endsWith("CE") || opt.symbol.contains("CE")) {
                                strikeMap[sp] = Pair(opt, current.second)
                            } else if (opt.symbol.endsWith("PE") || opt.symbol.contains("PE")) {
                                strikeMap[sp] = Pair(current.first, opt)
                            }
                        }
                    }
                    val strikes = strikeMap.map { (sp, pair) ->
                        val ceOpt = pair.first
                        val peOpt = pair.second
                        val ceLive = if (ceOpt != null) MarketDataStore.getTick(ceOpt.symbol) else null
                        val peLive = if (peOpt != null) MarketDataStore.getTick(peOpt.symbol) else null
                        OptionStrikeItem(
                            strikePrice = sp,
                            callLtp = ceLive?.price ?: 0.0,
                            putLtp = peLive?.price ?: 0.0,
                            callToken = ceOpt?.token ?: "",
                            putToken = peOpt?.token ?: "",
                            callSymbol = ceOpt?.symbol ?: "",
                            putSymbol = peOpt?.symbol ?: "",
                            expiry = expiry,
                            underlying = symbol
                        )
                    }.sortedBy { it.strikePrice }
                    return Result.success(strikes)
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
                    com.example.data.model.MarketDataStore.setAngelHealth("STALE")
                    Log.w("AngelOneMarketData", "No ticks received for 15s. Marking connection STALE.")
                }
            }
        }
    }
}

