package com.example.data.network

import android.util.Log
import com.example.data.model.HistoricalCandle
import com.example.data.model.IndexQuote
import com.example.data.model.MarketBreadth
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
 * Central orchestrator implementing hidden automatic data failover:
 * Priority: 1. Fyers (Primary) -> 2. Angel One (Secondary) -> 3. m.Stock (Tertiary) -> DATA UNAVAILABLE
 *
 * STRICT REQUIREMENTS:
 * - NO YAHOO FINANCE
 * - NO HARDCODED OR RANDOM PRICES
 * - Return DATA UNAVAILABLE or fail Result if real data is missing.
 */
class MarketDataEngine(
    var fyersMarketDataService: FyersMarketDataService? = null,
    val angelMarketDataService: AngelOneMarketDataService,
    val mStockMarketDataService: MStockMarketDataService,
    
    val tradeSmartMarketDataService: TradeSmartMarketDataService? = null,
    private val sessionManager: SessionManager,
    private val healthManager: ProviderHealthManager
) {
    companion object {
        private const val TAG = "MarketDataEngine"
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _unifiedFeedStatus = MutableStateFlow("CONNECTING")
    val unifiedFeedStatus: StateFlow<String> = _unifiedFeedStatus.asStateFlow()

    private val _marketBreadth = MutableStateFlow<MarketBreadth?>(null)
    val marketBreadth: StateFlow<MarketBreadth?> = _marketBreadth.asStateFlow()

    private val _internalActiveProvider = MutableStateFlow(ProviderHealthManager.PROVIDER_FYERS)
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
    }

    fun setPrimaryMarketDataProvider(providerName: String) {
        primaryProviderOverride = providerName
    }

    private fun startHeartbeatMonitor() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(15000)
                val lastTick = _lastTickTimeMs.value
                val now = System.currentTimeMillis()
                
                if (lastTick > 0 && now - lastTick > 30000) {
                    Log.w(TAG, "Market Data Stale (>30s).")
                    if (_unifiedFeedStatus.value.contains("LIVE")) {
                        _unifiedFeedStatus.value = "STALE DATA"
                    }
                }
            }
        }
    }

    private fun updateLastTickTime() {
        val now = System.currentTimeMillis()
        _lastTickTimeFormatted.value = timeFormat.format(Date(now))

        _lastTickTimeMs.value = System.currentTimeMillis()
    }

    // =========================================================================
    // 1. LIVE OPTION CHAIN
    // Priority: 1. Fyers -> 2. Angel One -> 3. m.Stock -> 4. Unavailable
    // =========================================================================
    suspend fun getOptionChain(symbol: String, expiry: String? = null): Result<List<OptionStrikeItem>> {
        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getOptionChain(symbol, expiry ?: "")
            if (fyersRes.isSuccess) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis() - startFyers)
                return fyersRes
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_FYERS)
            healthManager.logFailover(ProviderHealthManager.PROVIDER_FYERS, ProviderHealthManager.PROVIDER_ANGEL_ONE)
        }

        // Priority 2: Angel One
        val startAngel = System.currentTimeMillis()
        val angelRes = angelMarketDataService.getOptionChain(symbol, expiry ?: "")
        if (angelRes.isSuccess) {
            healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
            return angelRes
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)
        healthManager.logFailover(ProviderHealthManager.PROVIDER_ANGEL_ONE, ProviderHealthManager.PROVIDER_MSTOCK)

        // Priority 3: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getOptionChain(symbol, expiry ?: "")
            if (mStockRes.isSuccess) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                return mStockRes
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
        }

        return Result.failure(Exception("REAL OPTION CHAIN UNAVAILABLE"))
    }

    // =========================================================================
    // 2. HISTORICAL DATA
    // Priority: 1. Fyers -> 2. Angel One -> 3. m.Stock -> 4. Unavailable
    // =========================================================================
    
    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val fyersRes = fyersMarketDataService!!.getOptionExpiries(symbol)
            if (fyersRes.isSuccess) return fyersRes
        }
        
        // Priority 2: Angel One
        val angelRes = angelMarketDataService.getOptionExpiries(symbol)
        if (angelRes.isSuccess) return angelRes
        
        // Priority 3: m.Stock
        // (mStock missing direct expiries method, handled gracefully)
        
        return Result.failure(Exception("REAL EXPIRIES UNAVAILABLE"))
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<CandleData>> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val toDate = format.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -5)
        val fromDate = format.format(cal.time)

        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getHistoricalCandles(symbol, interval, fromDate, toDate)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis() - startFyers)
                return fyersRes
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_FYERS)
        }

        // Priority 2: Angel One
        val startAngel = System.currentTimeMillis()
        val angelRes = angelMarketDataService.getHistoricalCandles(symbol, interval)
        if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
            healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
            return angelRes
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)

        // Priority 3: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getHistoricalCandles(symbol, interval)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                val mapped = mStockRes.getOrDefault(emptyList()).map {
                    CandleData(open = it.open.toFloat(), high = it.high.toFloat(), low = it.low.toFloat(), close = it.close.toFloat(), volume = it.volume.toFloat())
                }
                return Result.success(mapped)
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
        }

        return Result.failure(Exception("REAL HISTORICAL DATA UNAVAILABLE"))
    }

    suspend fun getMarketBreadth(): Result<MarketBreadth> {
        // Since Fyers/Angel doesn't have direct breadth api, we calculate from quotes
        val symbols = listOf("RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "SBIN", "BHARTIARTL", "ITC", "KOTAKBANK", "LT")
        val quotesRes = getMarketQuotes(symbols)
        if (quotesRes.isSuccess && quotesRes.getOrDefault(emptyList()).isNotEmpty()) {
            val quotes = quotesRes.getOrDefault(emptyList())
            var adv = 0
            var dec = 0
            var unch = 0
            quotes.forEach { item ->
                when {
                    item.change > 0.0 -> adv++
                    item.change < 0.0 -> dec++
                    else -> unch++
                }
            }
            val breadth = MarketBreadth(advances = adv, declines = dec, unchanged = unch, total = quotes.size, timestamp = System.currentTimeMillis())
            _marketBreadth.value = breadth
            return Result.success(breadth)
        }
        return Result.failure(Exception("Market breadth calculation unavailable from all providers."))
    }

    // =========================================================================
    // 4. INDEX DATA FAILOVER & MARKET QUOTES
    // Priority: 1. Fyers -> 2. Angel One -> 3. m.Stock -> 4. Unavailable
    // =========================================================================
    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (symbols.isEmpty()) return Result.success(emptyList())
    
        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getMarketQuotes(symbols)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = fyersRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis() - startFyers)
                    _unifiedFeedStatus.value = "LIVE — FYERS"
                    _internalActiveProvider.value = "FYERS"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_FYERS)
        }
        
        // Priority 2: Angel One
        val startAngel = System.currentTimeMillis()
        val angelRes = angelMarketDataService.getMarketQuotes(symbols)
        if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
            val valid = angelRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
            if (valid.isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
                _unifiedFeedStatus.value = "LIVE — ANGEL ONE"
                _internalActiveProvider.value = "ANGEL ONE"
                updateLastTickTime()
                return Result.success(valid)
            }
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)

        // Priority 3: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getMarketQuotes(symbols)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = mStockRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                    _unifiedFeedStatus.value = "LIVE — M.STOCK"
                    _internalActiveProvider.value = "M.STOCK"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
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
                    previousClose = state?.previousClose ?: (item.ltp - item.change),
                    timestamp = System.currentTimeMillis(),
                    isLive = _unifiedFeedStatus.value.contains("LIVE")
                )
            )
        } else {
            Result.failure(Exception("Index quote unavailable for $symbol"))
        }
    }
    
    suspend fun updateFyersTick(tick: MarketTick) {
        val current = MarketDataStore.getTick(tick.symbol)
        
        MarketDataStore.updateTick(
            source = "FYERS",
            symbol = tick.symbol,
            token = "",
            exchange = "NSE",
            ltp = tick.ltp,
            open = tick.open.takeIf { it > 0.0 } ?: current?.open ?: tick.ltp,
            high = tick.high.takeIf { it > 0.0 } ?: current?.high ?: tick.ltp,
            low = tick.low.takeIf { it > 0.0 } ?: current?.low ?: tick.ltp,
            close = tick.close.takeIf { it > 0.0 } ?: current?.previousClose ?: tick.ltp,
            volume = tick.volume.takeIf { it > 0L } ?: current?.volume ?: 0L,
            exchangeTimestamp = tick.timestamp,
            receivedTimestamp = System.currentTimeMillis(),
            state = "LIVE"
        )
        
        _unifiedFeedStatus.value = "LIVE — FYERS"
        _internalActiveProvider.value = "FYERS"
        updateLastTickTime()
    }

    fun retryConnection() {
        _unifiedFeedStatus.value = "CONNECTING"
        if (fyersMarketDataService?.isConfigured() == true) {
            // reconnect fyers
            CoroutineScope(Dispatchers.IO).launch { fyersMarketDataService?.connect() }
        }
        angelMarketDataService.reconnect()
        if (mStockMarketDataService.isConfigured()) {
            mStockMarketDataService.reconnect()
        }
    }
}
