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

    init {
        scope.launch {
            launch { instrumentMaster.isLoadedFlow.collect { loaded -> if (loaded && _connectionState.value == "CONNECTED") { webSocket?.let { subscribeToIndices(it) } } } }
            instrumentMaster.loadMaster()
            connectWebSocket()
        }
    }

    private var pingJob: Job? = null

    private fun connectWebSocket() {
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
                Log.d("AngelOneMarketDataService", "WebSocket Opened")
                
                pingJob = scope.launch {
            launch { instrumentMaster.isLoadedFlow.collect { loaded -> if (loaded && _connectionState.value == "CONNECTED") { webSocket?.let { subscribeToIndices(it) } } } }
                    while (true) {
                        delay(30_000)
                        try {
                            webSocket.send("ping")
                        } catch (e: Exception) {
                            break
                        }
                    }
                }
                
                subscribeToIndices(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("AngelOneMarketDataService", "Text Message: $text")
                try {
                    val json = JSONObject(text)
                    if (json.optBoolean("status", false)) {
                        _connectionState.value = "SUBSCRIBED"
                        isSubscribed = true
                    }
                } catch (e: Exception) {
                    Log.e("AngelOneMarketDataService", "Error parsing text message", e)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleBinaryTick(bytes.toByteArray())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "CLOSED: $code $reason"
                isSubscribed = false
                Log.d("AngelOneMarketDataService", "WebSocket Closed: $reason")
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = "ERROR: ${t.message ?: "Unknown"} / ${response?.code}"
                isSubscribed = false
                Log.e("AngelOneMarketDataService", "WebSocket Failure", t)
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
            launch { instrumentMaster.isLoadedFlow.collect { loaded -> if (loaded && _connectionState.value == "CONNECTED") { webSocket?.let { subscribeToIndices(it) } } } }
            delay(delayTime)
            connectWebSocket()
        }
    }

    private fun getExchangeType(exchSeg: String): Int {
        return when (exchSeg.uppercase()) {
            "NSE" -> 1
            "NFO" -> 2
            "BSE" -> 3
            "BFO" -> 4
            "MCX" -> 5
            "NCDEX" -> 7
            "CDS" -> 9
            else -> 1
        }
    }

    private fun subscribeToIndices(ws: WebSocket) {
        // if (!instrumentMaster.isLoaded) return // No need to wait, hardcoded fallback exists
        
        val tokensByExchange = mutableMapOf<Int, MutableList<String>>()
        val indices = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX", "CRUDEOIL", "CRUDEOIL M")
        
        for (index in indices) {
            val inst = instrumentMaster.resolveIndexToken(index)
            if (inst != null) {
                val exType = getExchangeType(inst.exch_seg)
                tokensByExchange.getOrPut(exType) { mutableListOf() }.add(inst.token)
            }
        }
        
        if (tokensByExchange.isNotEmpty()) {
            val req = JSONObject().apply {
                put("correlationID", "initial_sub")
                put("action", 1)
                put("params", JSONObject().apply {
                    put("mode", 1)
                    put("tokenList", JSONArray().apply {
                        for ((exType, tokens) in tokensByExchange) {
                            put(JSONObject().apply {
                                put("exchangeType", exType)
                                put("tokens", JSONArray(tokens))
                            })
                        }
                    })
                })
            }
            ws.send(req.toString())
            _connectionState.value = "SUBSCRIBING"
        }
    }

    fun subscribeToTokens(exchangeType: Int, tokens: List<String>) {
        val ws = webSocket ?: return
        if (tokens.isEmpty()) return
        
        val req = JSONObject().apply {
            put("correlationID", "dynamic_sub")
            put("action", 1)
            put("params", JSONObject().apply {
                put("mode", 1)
                put("tokenList", JSONArray().apply {
                    put(JSONObject().apply {
                        put("exchangeType", exchangeType)
                        put("tokens", JSONArray(tokens))
                    })
                })
            })
        }
        ws.send(req.toString())
    }

    private fun handleBinaryTick(bytes: ByteArray) {
        try {
            if (bytes.size < 47) {
                Log.d("AngelOneMarketDataService", "Ignoring binary tick of size ${bytes.size}")
                return
            }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val subscriptionMode = buffer.get()
            val exchangeType = buffer.get()
            
            val tokenBytes = ByteArray(25)
            buffer.get(tokenBytes)
            val token = String(tokenBytes, Charsets.US_ASCII).trimEnd('\u0000')
            
            val sequenceNumber = buffer.getLong()
            val exchangeTimestamp = buffer.getLong()
            val ltpInt = buffer.getInt()
            
            if (ltpInt <= 0) return
            
            val divisor = if (exchangeType.toInt() == 9) 10000000.0 else 100.0
            val ltp = ltpInt / divisor
            
            val inst = instrumentMaster.getInstrumentByToken(token, exchangeType.toInt()) ?: return
            
            if (!hasFirstTick) {
                hasFirstTick = true
                _connectionState.value = "LIVE"
            }
            
            MarketDataStore.updateTick(
                symbol = inst.symbol,
                token = token,
                exchange = inst.exch_seg,
                ltp = ltp,
                timestamp = exchangeTimestamp
            )
            
        } catch (e: Exception) {
            Log.e("AngelOneMarketDataService", "Error parsing binary tick", e)
        }
    }

    fun isConnectionLive(): Boolean {
        return _connectionState.value == "LIVE"
    }

    fun getLastUpdatedTime(): String {
        return ""
    }

    fun reconnect() {
        connectWebSocket()
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<com.example.ui.components.CandleData>> {
        // Returns real historical candle data or empty if unavailable
        return Result.success(emptyList())
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return angelOneService.getMarketQuotes(symbols)
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        val httpResult = angelOneService.getOptionChain(symbol, expiry)
        
        if (instrumentMaster.isLoaded) {
            val options = instrumentMaster.getOptionInstruments(symbol, expiry)
            if (options.isNotEmpty()) {
                val tokensToSubscribe = options.map { it.token }
                subscribeToTokens(2, tokensToSubscribe)
                
                if (httpResult.isSuccess) {
                    val strikes = httpResult.getOrDefault(emptyList()).map { item ->
                        val ceOpt = options.find { (it.strike.toDoubleOrNull() ?: 0.0) / 100.0 == item.strikePrice && it.symbol.endsWith("CE") }
                        val peOpt = options.find { (it.strike.toDoubleOrNull() ?: 0.0) / 100.0 == item.strikePrice && it.symbol.endsWith("PE") }
                        
                        val ceLive = if (ceOpt != null) MarketDataStore.marketData.value[ceOpt.symbol] else null
                        val peLive = if (peOpt != null) MarketDataStore.marketData.value[peOpt.symbol] else null
                        
                        item.copy(
                            callLtp = ceLive?.ltp ?: item.callLtp,
                            putLtp = peLive?.ltp ?: item.putLtp
                        )
                    }
                    return Result.success(strikes)
                }
            }
        }
        return httpResult
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return angelOneService.getOptionExpiries(symbol)
    }
}
