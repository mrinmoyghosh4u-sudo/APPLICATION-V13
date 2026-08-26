package com.example.data.network

import android.util.Log
import com.example.data.model.HistoricalCandle
import com.example.data.model.IndexQuote
import com.example.data.model.MarketBreadth
import com.example.data.model.MarketDataProviderState
import com.example.data.model.MarketDataState
import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import com.example.data.model.MarketTick
import com.example.data.model.OptionChain
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UNIFIED MARKET DATA ENGINE / REPOSITORY
 * 
 * Central orchestrator implementing automatic data failover:
 * Priority: 1. Upstox (Primary) -> 2. Fyers (Fallback #1) -> 3. Angel One (Fallback #2) -> 4. m.Stock (Fallback #3) -> REAL MARKET DATA UNAVAILABLE
 *
 * STRICT REQUIREMENTS:
 * - NO MOCK OR SYNTHETIC DATA
 * - NO HARDCODED OR RANDOM PRICES
 * - Return REAL MARKET DATA UNAVAILABLE or fail Result if real data is missing.
 */
class MarketDataEngine(
    var upstoxMarketDataService: UpstoxMarketDataService? = null,
    var fyersMarketDataService: FyersMarketDataService? = null,
    val angelMarketDataService: AngelOneMarketDataService? = null,
    val mStockMarketDataService: MStockMarketDataService? = null,
    private val sessionManager: SessionManager? = null,
    private val healthManager: ProviderHealthManager? = null
) {
    companion object {
        private const val TAG = "MarketDataEngine"
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    // Authoritative Provider State directly from MarketDataStore
    val providerState: StateFlow<MarketDataProviderState> = MarketDataStore.providerState

    private val _unifiedFeedStatus = MutableStateFlow("REAL MARKET DATA UNAVAILABLE")
    val unifiedFeedStatus: StateFlow<String> = _unifiedFeedStatus.asStateFlow()

    private val _marketBreadth = MutableStateFlow<MarketBreadth?>(null)
    val marketBreadth: StateFlow<MarketBreadth?> = _marketBreadth.asStateFlow()

    private val _internalActiveProvider = MutableStateFlow(ProviderHealthManager.PROVIDER_UPSTOX)
    val internalActiveProvider: StateFlow<String> = _internalActiveProvider.asStateFlow()
    
    private val _lastTickTimeMs = MutableStateFlow(0L)
    val lastTickTimeMs: StateFlow<Long> = _lastTickTimeMs.asStateFlow()
    
    private val _lastTickTimeFormatted = MutableStateFlow("--")
    val lastTickTimeFormatted: StateFlow<String> = _lastTickTimeFormatted.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private var heartbeatJob: Job? = null
    private var primaryProviderOverride: String? = null

    init {
        startHeartbeatMonitor()
        observeProviderState()
    }

    private fun observeProviderState() {
        scope.launch {
            MarketDataStore.providerState.collect { state ->
                _unifiedFeedStatus.value = state.displayStatus
                _internalActiveProvider.value = state.provider
                if (state.lastTickTimestamp > 0L) {
                    _lastTickTimeMs.value = state.lastTickTimestamp
                    _lastTickTimeFormatted.value = timeFormat.format(Date(state.lastTickTimestamp))
                }
            }
        }
    }

    fun setPrimaryMarketDataProvider(providerName: String) {
        primaryProviderOverride = providerName
    }

    private fun startHeartbeatMonitor() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(5000)
                val lastTick = _lastTickTimeMs.value
                val now = System.currentTimeMillis()
                
                if (lastTick > 0 && now - lastTick > 15000) {
                    Log.w(TAG, "Market Data Stale (>15s).")
                }
            }
        }
    }

    private fun updateLastTickTime() {
        val now = System.currentTimeMillis()
        _lastTickTimeFormatted.value = timeFormat.format(Date(now))
        _lastTickTimeMs.value = now
    }

    // =========================================================================
    // 1. LIVE OPTION CHAIN
    // Priority: 1. Upstox -> 2. Fyers -> 3. Angel One -> 4. m.Stock -> 5. Unavailable
    // =========================================================================
    suspend fun getOptionChain(symbol: String, expiry: String? = null): Result<List<OptionStrikeItem>> {
        // Priority 1: Upstox
        if (upstoxMarketDataService?.isConfigured() == true) {
            val startUpstox = System.currentTimeMillis()
            val upstoxRes = upstoxMarketDataService!!.getOptionChain(symbol, expiry ?: "")
            if (upstoxRes.isSuccess && upstoxRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_UPSTOX, System.currentTimeMillis() - startUpstox)
                return upstoxRes
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_UPSTOX)
            healthManager?.logFailover(ProviderHealthManager.PROVIDER_UPSTOX, ProviderHealthManager.PROVIDER_FYERS)
        }

        // Priority 2: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getOptionChain(symbol, expiry ?: "")
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis() - startFyers)
                return fyersRes
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_FYERS)
            healthManager?.logFailover(ProviderHealthManager.PROVIDER_FYERS, ProviderHealthManager.PROVIDER_ANGEL_ONE)
        }

        // Priority 3: Angel One
        if (angelMarketDataService?.isConfigured() == true) {
            val startAngel = System.currentTimeMillis()
            val angelRes = angelMarketDataService.getOptionChain(symbol, expiry ?: "")
            if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
                return angelRes
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)
            healthManager?.logFailover(ProviderHealthManager.PROVIDER_ANGEL_ONE, ProviderHealthManager.PROVIDER_MSTOCK)
        }

        // Priority 4: m.Stock
        if (mStockMarketDataService?.isConfigured() == true) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getOptionChain(symbol, expiry ?: "")
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                return mStockRes
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
        }

        return Result.failure(Exception("REAL OPTION CHAIN UNAVAILABLE"))
    }

    // =========================================================================
    // 2. HISTORICAL DATA & EXPIRIES
    // Priority: 1. Upstox -> 2. Fyers -> 3. Angel One -> 4. m.Stock -> 5. Unavailable
    // =========================================================================
    
    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        // Priority 1: Upstox
        if (upstoxMarketDataService?.isConfigured() == true) {
            val upstoxRes = upstoxMarketDataService!!.getOptionExpiries(symbol)
            if (upstoxRes.isSuccess && upstoxRes.getOrDefault(emptyList()).isNotEmpty()) return upstoxRes
        }

        // Priority 2: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val fyersRes = fyersMarketDataService!!.getOptionExpiries(symbol)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) return fyersRes
        }
        
        // Priority 3: Angel One
        if (angelMarketDataService?.isConfigured() == true) {
            val angelRes = angelMarketDataService.getOptionExpiries(symbol)
            if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) return angelRes
        }
        
        return Result.failure(Exception("REAL EXPIRIES UNAVAILABLE"))
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<CandleData>> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val toDate = format.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -5)
        val fromDate = format.format(cal.time)

        // Priority 1: Upstox
        if (upstoxMarketDataService?.isConfigured() == true) {
            val startUpstox = System.currentTimeMillis()
            val upstoxRes = upstoxMarketDataService!!.getHistoricalCandles(symbol, interval, fromDate, toDate)
            if (upstoxRes.isSuccess && upstoxRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_UPSTOX, System.currentTimeMillis() - startUpstox)
                return upstoxRes
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_UPSTOX)
        }

        // Priority 2: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getHistoricalCandles(symbol, interval, fromDate, toDate)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis() - startFyers)
                return fyersRes
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_FYERS)
        }

        // Priority 3: Angel One
        if (angelMarketDataService?.isConfigured() == true) {
            val startAngel = System.currentTimeMillis()
            val angelRes = angelMarketDataService.getHistoricalCandles(symbol, interval)
            if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
                return angelRes
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)
        }

        // Priority 4: m.Stock
        if (mStockMarketDataService?.isConfigured() == true) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getHistoricalCandles(symbol, interval)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                val mapped = mStockRes.getOrDefault(emptyList()).map {
                    CandleData(open = it.open.toFloat(), high = it.high.toFloat(), low = it.low.toFloat(), close = it.close.toFloat(), volume = it.volume.toFloat())
                }
                return Result.success(mapped)
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
        }

        return Result.failure(Exception("REAL HISTORICAL DATA UNAVAILABLE"))
    }

    suspend fun getMarketBreadth(): Result<MarketBreadth> {
        // Exchange-wide market breadth requires real upstream breadth feed from provider.
        // Never calculate exchange breadth from a small hardcoded sample list.
        return Result.failure(Exception("ADVANCE/DECLINE DATA UNAVAILABLE"))
    }

    // =========================================================================
    // 3. MARKET QUOTES & LIVE TICKS
    // Priority: 1. Upstox -> 2. Fyers -> 3. Angel One -> 4. m.Stock -> 5. Unavailable
    // =========================================================================
    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (symbols.isEmpty()) return Result.success(emptyList())

        // Priority 1: Upstox
        if (upstoxMarketDataService?.isConfigured() == true) {
            val startUpstox = System.currentTimeMillis()
            val upstoxRes = upstoxMarketDataService!!.getMarketQuotes(symbols)
            if (upstoxRes.isSuccess && upstoxRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = upstoxRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_UPSTOX, System.currentTimeMillis() - startUpstox)
                    _unifiedFeedStatus.value = "LIVE • UPSTOX"
                    _internalActiveProvider.value = "UPSTOX"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_UPSTOX)
        }
    
        // Priority 2: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getMarketQuotes(symbols)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = fyersRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis() - startFyers)
                    _unifiedFeedStatus.value = "REST_DATA_AVAILABLE • FYERS"
                    _internalActiveProvider.value = "FYERS"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_FYERS)
        }
        
        // Priority 3: Angel One
        if (angelMarketDataService?.isConfigured() == true) {
            val startAngel = System.currentTimeMillis()
            val angelRes = angelMarketDataService.getMarketQuotes(symbols)
            if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = angelRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
                    _unifiedFeedStatus.value = "LIVE • ANGEL ONE"
                    _internalActiveProvider.value = "ANGEL ONE"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)
        }

        // Priority 4: m.Stock
        if (mStockMarketDataService?.isConfigured() == true) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getMarketQuotes(symbols)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = mStockRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                    _unifiedFeedStatus.value = "LIVE • m.STOCK"
                    _internalActiveProvider.value = "m.STOCK"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager?.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
        }

        _unifiedFeedStatus.value = "REAL MARKET DATA UNAVAILABLE"
        return Result.failure(Exception("REAL MARKET DATA UNAVAILABLE"))
    }

    suspend fun getIndexQuote(symbol: String, exchange: String = "NSE"): Result<IndexQuote> {
        val quotes = getMarketQuotes(listOf(symbol))
        val item = quotes.getOrNull()?.firstOrNull()
        return if (item != null && item.ltp > 0.0) {
            val state = MarketDataStore.getTick(exchange, symbol)
            Result.success(
                IndexQuote(
                    symbol = item.symbol,
                    exchange = item.exchange,
                    ltp = item.ltp,
                    change = item.change,
                    changePercent = item.changePercent,
                    open = state?.open ?: 0.0,
                    high = state?.high ?: 0.0,
                    low = state?.low ?: 0.0,
                    previousClose = state?.previousClose ?: 0.0,
                    timestamp = System.currentTimeMillis(),
                    isLive = _unifiedFeedStatus.value.contains("LIVE")
                )
            )
        } else {
            Result.failure(Exception("Index quote unavailable for $symbol"))
        }
    }

    suspend fun updateUpstoxTick(tick: MarketTick) {
        val current = MarketDataStore.getTick(tick.exchange, tick.symbol)

        MarketDataStore.updateTick(
            source = MarketDataSourceNames.UPSTOX,
            symbol = tick.symbol,
            token = tick.token,
            exchange = if (tick.exchange.isNotBlank()) tick.exchange else "NSE",
            ltp = tick.ltp,
            open = tick.open.takeIf { it > 0.0 } ?: current?.open ?: 0.0,
            high = tick.high.takeIf { it > 0.0 } ?: current?.high ?: 0.0,
            low = tick.low.takeIf { it > 0.0 } ?: current?.low ?: 0.0,
            close = tick.close.takeIf { it > 0.0 } ?: current?.previousClose ?: 0.0,
            volume = tick.volume.takeIf { it > 0L } ?: current?.volume ?: 0L,
            exchangeTimestamp = tick.timestamp,
            receivedTimestamp = System.currentTimeMillis(),
            state = "LIVE"
        )

        healthManager?.reportTickReceived(ProviderHealthManager.PROVIDER_UPSTOX, tick.timestamp)
        _unifiedFeedStatus.value = "LIVE • UPSTOX"
        _internalActiveProvider.value = "UPSTOX"
        updateLastTickTime()
    }
    
    suspend fun updateFyersTick(tick: MarketTick) {
        val current = MarketDataStore.getTick(tick.exchange, tick.symbol)
        
        MarketDataStore.updateTick(
            source = MarketDataSourceNames.FYERS,
            symbol = tick.symbol,
            token = tick.token,
            exchange = if (tick.exchange.isNotBlank()) tick.exchange else "NSE",
            ltp = tick.ltp,
            open = tick.open.takeIf { it > 0.0 } ?: current?.open ?: 0.0,
            high = tick.high.takeIf { it > 0.0 } ?: current?.high ?: 0.0,
            low = tick.low.takeIf { it > 0.0 } ?: current?.low ?: 0.0,
            close = tick.close.takeIf { it > 0.0 } ?: current?.previousClose ?: 0.0,
            volume = tick.volume.takeIf { it > 0L } ?: current?.volume ?: 0L,
            exchangeTimestamp = tick.timestamp,
            receivedTimestamp = System.currentTimeMillis(),
            state = "LIVE"
        )
        
        healthManager?.reportTickReceived(ProviderHealthManager.PROVIDER_FYERS, tick.timestamp)
        _unifiedFeedStatus.value = "WEBSOCKET_LIVE • FYERS"
        _internalActiveProvider.value = "FYERS"
        updateLastTickTime()
    }


    suspend fun subscribeToTokens(exchange: String, symbols: List<String>) {
        if (symbols.isEmpty()) return
        
        if (upstoxMarketDataService?.isConfigured() == true) {
            upstoxMarketDataService?.subscribeToMarketData(symbols)
        }
        if (fyersMarketDataService?.isConfigured() == true) {
            fyersMarketDataService?.subscribeToMarketData(symbols)
        }
        if (angelMarketDataService?.isConfigured() == true) {
            val exchType = when(exchange.uppercase()) {
                "NSE" -> 1
                "NFO" -> 2
                "BSE" -> 3
                "BFO" -> 4
                "MCX" -> 5
                "CDS" -> 7
                else -> 1
            }
            angelMarketDataService.subscribeToTokens(exchType, symbols)
        }
        if (mStockMarketDataService?.isConfigured() == true) {
            mStockMarketDataService.subscribe(exchange, symbols)
        }
    }

    fun retryConnection() {
        _unifiedFeedStatus.value = "CONNECTING"
        if (upstoxMarketDataService?.isConfigured() == true) {
            CoroutineScope(Dispatchers.IO).launch { upstoxMarketDataService?.connect() }
        }
        if (fyersMarketDataService?.isConfigured() == true) {
            CoroutineScope(Dispatchers.IO).launch { fyersMarketDataService?.connect() }
        }
        angelMarketDataService?.reconnect()
        if (mStockMarketDataService?.isConfigured() == true) {
            mStockMarketDataService.reconnect()
        }
    }
}
