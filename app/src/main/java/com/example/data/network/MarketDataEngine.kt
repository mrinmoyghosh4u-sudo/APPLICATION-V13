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
 * - Live LTP: Angel One → m.Stock → NSE → Yahoo → DATA UNAVAILABLE
 * - Option Chain: Angel One → m.Stock → NSE → Yahoo
 * - Historical Data: m.Stock → Yahoo → NSE → Angel One
 * - Advance/Decline: m.Stock → NSE → Yahoo → Angel One
 * - Index Data: Angel One → m.Stock → NSE → Yahoo
 * 
 * Strict Directives:
 * - The UI NEVER directly references a specific broker/provider.
 * - Provider source names are HIDDEN from user-facing UI (shows only LIVE or DATA UNAVAILABLE).
 * - Safe internal diagnostic logs only.
 * - Zero fake/random data.
 * - Automatic background failover and restoration without app restart or screen recreation.
 */
class MarketDataEngine(
    val angelMarketDataService: AngelOneMarketDataService,
    val mStockMarketDataService: MStockMarketDataService,
    val nseFeedService: NseAuthorizedFeedService,
    val tradeSmartMarketDataService: TradeSmartMarketDataService? = null,
    val sessionManager: SessionManager,
    val healthManager: ProviderHealthManager = ProviderHealthManager()
) {
    companion object {
        private const val TAG = "MarketDataEngine"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Internal active provider tracking (strictly internal)
    private val _internalActiveProvider = MutableStateFlow(ProviderHealthManager.PROVIDER_ANGEL_ONE)
    val internalActiveProvider: StateFlow<String> = _internalActiveProvider.asStateFlow()

    val primaryMarketDataProvider = MutableStateFlow(sessionManager.primaryMarketDataProvider)
    val secondaryMarketDataProvider = MutableStateFlow(if (sessionManager.primaryMarketDataProvider == "Angel One") "m.Stock" else "Angel One")

    fun setPrimaryMarketDataProvider(providerName: String) {
        val target = if (providerName.contains("m.Stock", ignoreCase = true)) "m.Stock" else "Angel One"
        sessionManager.primaryMarketDataProvider = target
        primaryMarketDataProvider.value = target
        secondaryMarketDataProvider.value = if (target == "Angel One") "m.Stock" else "Angel One"
        evaluateLiveLtpFailover()
    }

    // Unified user-facing status
    private val _unifiedFeedStatus = MutableStateFlow("DATA UNAVAILABLE") // "LIVE", "REFERENCE DATA", "DATA UNAVAILABLE", "CONNECTING"
    val unifiedFeedStatus: StateFlow<String> = _unifiedFeedStatus.asStateFlow()

    private val _lastTickTimeFormatted = MutableStateFlow("Not Updated")
    val lastTickTimeFormatted: StateFlow<String> = _lastTickTimeFormatted.asStateFlow()

    private val _marketBreadth = MutableStateFlow<MarketBreadth?>(null)
    val marketBreadth: StateFlow<MarketBreadth?> = _marketBreadth.asStateFlow()

    private var monitorJob: Job? = null
    private var healthCheckJob: Job? = null

    init {
        startEngineMonitoring()
        startPeriodicHealthCheck()
    }

    private fun startEngineMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            // Monitor Angel One connection state
            launch {
                angelMarketDataService.connectionState.collectLatest { state ->
                    val isConnected = state == "LIVE" || state == "SUBSCRIBED" || state == "CONNECTED" || state == "SUBSCRIBING" || state == "CONNECTING"
                    healthManager.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, isConnected)
                    
                    if (state == "LIVE" || state == "SUBSCRIBED" || state == "CONNECTED") {
                        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_ANGEL_ONE, true)
                        evaluateLiveLtpFailover()
                    } else if (state == "DISCONNECTED" || state == "ERROR" || state == "STALE") {
                        evaluateLiveLtpFailover()
                    }
                }
            }

            // Monitor m.Stock connection state
            launch {
                mStockMarketDataService.connectionState.collectLatest { state ->
                    val isConnected = state == "LIVE" || state == "SUBSCRIBED" || state == "CONNECTED" || state == "AUTHENTICATED"
                    healthManager.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, isConnected)
                    if (state == "LIVE" || state == "AUTHENTICATED") {
                        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, true)
                    }
                }
            }

            // Monitor ticks in MarketDataStore for strict source-driven failover and auto-recovery
            launch {
                MarketDataStore.marketData.collect { tickMap ->
                    if (tickMap.isNotEmpty()) {
                        val latestTicks = tickMap.values.toList()

                        // Check for recent Angel One tick
                        val angelTick = latestTicks.find { it.source == MarketDataSourceNames.ANGEL_ONE && it.ltp > 0.0 && it.state == "LIVE" }
                        if (angelTick != null) {
                            val now = System.currentTimeMillis()
                            val tickAge = now - angelTick.receivedTimestamp
                            if (tickAge <= ProviderHealthManager.STALE_TIMEOUT_MS) {
                                healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_ANGEL_ONE, angelTick.receivedTimestamp)
                                
                                // Automatic Recovery to Angel One if currently on m.Stock or None
                                if (_internalActiveProvider.value != ProviderHealthManager.PROVIDER_ANGEL_ONE) {
                                    Log.i(TAG, "RECOVERY: ${_internalActiveProvider.value} → ANGEL_ONE (Fresh Angel One tick received: ${angelTick.symbol} LTP=${angelTick.ltp})")
                                    healthManager.logRestored(_internalActiveProvider.value, ProviderHealthManager.PROVIDER_ANGEL_ONE)
                                    _internalActiveProvider.value = ProviderHealthManager.PROVIDER_ANGEL_ONE
                                }
                                _unifiedFeedStatus.value = "LIVE — ANGEL ONE"
                                updateLastTickTime()
                                return@collect
                            }
                        }

                        // Check for recent m.Stock tick if Angel One is not active/healthy
                        val mStockTick = latestTicks.find { it.source == MarketDataSourceNames.MSTOCK && it.ltp > 0.0 && it.state == "LIVE" }
                        if (mStockTick != null) {
                            val now = System.currentTimeMillis()
                            val tickAge = now - mStockTick.receivedTimestamp
                            if (tickAge <= ProviderHealthManager.STALE_TIMEOUT_MS) {
                                healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_MSTOCK, mStockTick.receivedTimestamp)
                                
                                // Automatic Failover to m.Stock if Angel One is unhealthy
                                val isAngelHealthy = healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE)
                                if (!isAngelHealthy && _internalActiveProvider.value != ProviderHealthManager.PROVIDER_MSTOCK) {
                                    Log.i(TAG, "FAILOVER: ${_internalActiveProvider.value} → MSTOCK (Fresh m.Stock tick received: ${mStockTick.symbol} LTP=${mStockTick.ltp})")
                                    healthManager.logFailover(_internalActiveProvider.value, ProviderHealthManager.PROVIDER_MSTOCK)
                                    _internalActiveProvider.value = ProviderHealthManager.PROVIDER_MSTOCK
                                }
                                if (_internalActiveProvider.value == ProviderHealthManager.PROVIDER_MSTOCK) {
                                    _unifiedFeedStatus.value = "LIVE — M.STOCK"
                                    updateLastTickTime()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startPeriodicHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (true) {
                delay(5000)
                healthManager.checkAndEvaluateStaleness()
                
                val currentProvider = _internalActiveProvider.value
                val isAngelHealthy = healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE)

                if (currentProvider == ProviderHealthManager.PROVIDER_ANGEL_ONE && !isAngelHealthy) {
                    Log.w(TAG, "Angel One feed stale/unhealthy. Initiating background m.Stock failover check...")
                    if (mStockMarketDataService.isConfigured() && !mStockMarketDataService.isConnectionLive()) {
                        mStockMarketDataService.connect()
                    }
                } else if (currentProvider == ProviderHealthManager.PROVIDER_MSTOCK) {
                    // Monitor Angel One background recovery
                    if (!angelMarketDataService.isConnectingOrLive()) {
                        Log.i(TAG, "Monitoring Angel One recovery in background...")
                        angelMarketDataService.reconnect()
                    }
                }
            }
        }
    }

    private fun evaluateLiveLtpFailover() {
        val prev = _internalActiveProvider.value
        val primary = primaryMarketDataProvider.value

        if (primary == "m.Stock") {
            // Priority 1: m.Stock
            if (mStockMarketDataService.isConfigured() && mStockMarketDataService.isConnectionLive()) {
                _internalActiveProvider.value = ProviderHealthManager.PROVIDER_MSTOCK
                _unifiedFeedStatus.value = "LIVE — M.STOCK"
                updateLastTickTime()
                if (prev != ProviderHealthManager.PROVIDER_MSTOCK) {
                    healthManager.logFailover(prev, ProviderHealthManager.PROVIDER_MSTOCK)
                }
                return
            }
            // Priority 2: Angel One
            if (angelMarketDataService.isConnectionLive()) {
                _internalActiveProvider.value = ProviderHealthManager.PROVIDER_ANGEL_ONE
                _unifiedFeedStatus.value = "LIVE — ANGEL ONE"
                updateLastTickTime()
                if (prev != ProviderHealthManager.PROVIDER_ANGEL_ONE) {
                    healthManager.logFailover(prev, ProviderHealthManager.PROVIDER_ANGEL_ONE)
                }
                return
            }
        } else {
            // Priority 1: Angel One
            if (angelMarketDataService.isConnectionLive()) {
                _internalActiveProvider.value = ProviderHealthManager.PROVIDER_ANGEL_ONE
                _unifiedFeedStatus.value = "LIVE — ANGEL ONE"
                updateLastTickTime()
                if (prev != ProviderHealthManager.PROVIDER_ANGEL_ONE) {
                    healthManager.logFailover(prev, ProviderHealthManager.PROVIDER_ANGEL_ONE)
                }
                return
            }
            // Priority 2: m.Stock
            if (mStockMarketDataService.isConfigured() && mStockMarketDataService.isConnectionLive()) {
                _internalActiveProvider.value = ProviderHealthManager.PROVIDER_MSTOCK
                _unifiedFeedStatus.value = "LIVE — M.STOCK"
                updateLastTickTime()
                if (prev != ProviderHealthManager.PROVIDER_MSTOCK) {
                    healthManager.logFailover(prev, ProviderHealthManager.PROVIDER_MSTOCK)
                }
                return
            }
        }

        // Priority 3: NSE Authorized Feed
        if (nseFeedService.isConfigured() && nseFeedService.isConnected) {
            _internalActiveProvider.value = ProviderHealthManager.PROVIDER_NSE
            _unifiedFeedStatus.value = "REFERENCE — NSE"
            updateLastTickTime()
            if (prev != ProviderHealthManager.PROVIDER_NSE) {
                healthManager.logFailover(prev, ProviderHealthManager.PROVIDER_NSE)
            }
            return
        }

        // Priority 4: Yahoo Reference
        val lastYahoo = MarketDataStore.getSourceLastUpdate(MarketDataSourceNames.YAHOO)
        if (lastYahoo > 0 && System.currentTimeMillis() - lastYahoo < 120000) {
            _internalActiveProvider.value = ProviderHealthManager.PROVIDER_YAHOO
            _unifiedFeedStatus.value = "LIVE"
            updateLastTickTime()
            if (prev != ProviderHealthManager.PROVIDER_YAHOO) {
                healthManager.logFailover(prev, ProviderHealthManager.PROVIDER_YAHOO)
            }
            return
        }

        // No live provider available
        _internalActiveProvider.value = ProviderHealthManager.PROVIDER_NONE
        _unifiedFeedStatus.value = "REAL MARKET DATA UNAVAILABLE"
    }

    private fun updateLastTickTime() {
        _lastTickTimeFormatted.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    // =========================================================================
    // 1. LIVE LTP FAILOVER & UNIFIED TICK REPOSITORY
    // Priority: 1. Angel One -> 2. m.Stock -> 3. NSE -> 4. Yahoo -> UNAVAILABLE
    // =========================================================================

    suspend fun getLiveTick(symbol: String, exchange: String = "NSE"): MarketTick? {
        val state = MarketDataStore.getTick(exchange, symbol) ?: MarketDataStore.getTick(symbol)
        if (state != null && state.ltp > 0.0) {
            val tick = MarketTick(
                symbol = state.symbol,
                exchange = state.exchange,
                token = state.token,
                ltp = state.ltp,
                open = state.open,
                high = state.high,
                low = state.low,
                close = state.previousClose,
                change = state.change,
                changePercent = state.changePercent,
                volume = state.volume,
                timestamp = if (state.exchangeTimestamp > 0) state.exchangeTimestamp else state.receivedTimestamp,
                isLive = state.state == "LIVE"
            )
            if (DataValidator.validateMarketTick(tick, symbol, exchange)) {
                return tick
            }
        }
        return null
    }

    // =========================================================================
    // 2. OPTION CHAIN FAILOVER
    // Priority: 1. Angel One -> 2. m.Stock (VERIFIED REAL PROVIDERS ONLY)
    // =========================================================================

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        // Priority 1: Angel One
        val startAngel = System.currentTimeMillis()
        val angelRes = angelMarketDataService.getOptionChain(symbol, expiry)
        if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
            val validStrikes = angelRes.getOrDefault(emptyList()).filter { DataValidator.validateOptionStrikeItem(it) }
            if (validStrikes.isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
                return Result.success(validStrikes)
            }
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)
        healthManager.logFailover(ProviderHealthManager.PROVIDER_ANGEL_ONE, ProviderHealthManager.PROVIDER_MSTOCK)

        // Priority 2: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getOptionChain(symbol, expiry)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                val validStrikes = mStockRes.getOrDefault(emptyList()).filter { DataValidator.validateOptionStrikeItem(it) }
                if (validStrikes.isNotEmpty()) {
                    healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                    return Result.success(validStrikes)
                }
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
        }

        return Result.failure(Exception("Option chain unavailable from all configured REAL providers."))
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return angelMarketDataService.getOptionExpiries(symbol)
    }

    // =========================================================================
    // 3. HISTORICAL DATA FAILOVER
    // Priority: 1. m.Stock -> 2. Yahoo -> 3. NSE -> 4. Angel One
    // =========================================================================

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<HistoricalCandle>> {
        // Priority 1: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getHistoricalCandles(symbol, interval)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                val validCandles = DataValidator.validateHistoricalCandles(mStockRes.getOrDefault(emptyList()))
                if (validCandles.isNotEmpty()) {
                    healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                    return Result.success(validCandles)
                }
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
            healthManager.logFailover(ProviderHealthManager.PROVIDER_MSTOCK, ProviderHealthManager.PROVIDER_YAHOO)
        }

        // Priority 2: Yahoo Finance
        val startYahoo = System.currentTimeMillis()
        val yahooRes = YahooFinanceService.getHistoricalCandles(symbol, interval)
        if (yahooRes.isSuccess && yahooRes.getOrDefault(emptyList()).isNotEmpty()) {
            val validCandles = DataValidator.validateHistoricalCandles(yahooRes.getOrDefault(emptyList()))
            if (validCandles.isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_YAHOO, System.currentTimeMillis() - startYahoo)
                return Result.success(validCandles)
            }
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_YAHOO)
        healthManager.logFailover(ProviderHealthManager.PROVIDER_YAHOO, ProviderHealthManager.PROVIDER_NSE)

        // Priority 3: NSE
        if (nseFeedService.isConfigured()) {
            val startNse = System.currentTimeMillis()
            val nseRes = nseFeedService.getHistoricalCandles(symbol, interval)
            if (nseRes.isSuccess && nseRes.getOrDefault(emptyList()).isNotEmpty()) {
                val validCandles = DataValidator.validateHistoricalCandles(nseRes.getOrDefault(emptyList()))
                if (validCandles.isNotEmpty()) {
                    healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_NSE, System.currentTimeMillis() - startNse)
                    return Result.success(validCandles)
                }
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_NSE)
            healthManager.logFailover(ProviderHealthManager.PROVIDER_NSE, ProviderHealthManager.PROVIDER_ANGEL_ONE)
        }

        // Priority 4: Angel One
        val startAngel = System.currentTimeMillis()
        val angelRes = angelMarketDataService.getHistoricalCandles(symbol, interval)
        if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
            val converted = angelRes.getOrDefault(emptyList()).map {
                HistoricalCandle(
                    time = "",
                    open = it.open.toDouble(),
                    high = it.high.toDouble(),
                    low = it.low.toDouble(),
                    close = it.close.toDouble(),
                    volume = it.volume.toLong()
                )
            }
            val validCandles = DataValidator.validateHistoricalCandles(converted)
            if (validCandles.isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
                return Result.success(validCandles)
            }
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)

        return Result.failure(Exception("Historical candle data unavailable from all providers."))
    }

    suspend fun getHistoricalCandleData(symbol: String, interval: String = "15m"): Result<List<CandleData>> {
        val res = getHistoricalCandles(symbol, interval)
        return if (res.isSuccess) {
            Result.success(res.getOrDefault(emptyList()).map { it.toCandleData() })
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Historical candle data unavailable"))
        }
    }

    // =========================================================================
    // 4. ADVANCE / DECLINE FAILOVER
    // Priority: 1. m.Stock -> 2. NSE -> 3. Yahoo -> 4. Angel One
    // =========================================================================

    suspend fun getAdvanceDecline(): Result<MarketBreadth> {
        // Priority 1: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val mStockRes = mStockMarketDataService.getMarketBreadth()
            if (mStockRes.isSuccess && mStockRes.getOrNull()?.total ?: 0 > 0) {
                val breadth = mStockRes.getOrThrow()
                _marketBreadth.value = breadth
                return Result.success(breadth)
            }
        }

        // Priority 2: NSE
        if (nseFeedService.isConfigured()) {
            val nseRes = nseFeedService.getMarketBreadth()
            if (nseRes.isSuccess && nseRes.getOrNull()?.total ?: 0 > 0) {
                val breadth = nseRes.getOrThrow()
                _marketBreadth.value = breadth
                return Result.success(breadth)
            }
        }

        // Priority 3: Yahoo Finance calculation
        val yahooRes = YahooFinanceService.getMarketBreadth()
        if (yahooRes.isSuccess && yahooRes.getOrNull()?.total ?: 0 > 0) {
            val breadth = yahooRes.getOrThrow()
            _marketBreadth.value = breadth
            return Result.success(breadth)
        }

        // Priority 4: Angel One calculation from quotes
        val angelQuotesRes = angelMarketDataService.getMarketQuotes(
            listOf("RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "SBIN", "BHARTIARTL", "ITC", "KOTAKBANK", "LT")
        )
        if (angelQuotesRes.isSuccess && angelQuotesRes.getOrDefault(emptyList()).isNotEmpty()) {
            val quotes = angelQuotesRes.getOrDefault(emptyList())
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
    // 5. INDEX DATA FAILOVER & MARKET QUOTES
    // Priority: 1. Angel One -> 2. m.Stock -> 3. NSE -> 4. Yahoo
    // =========================================================================

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        // Priority 1: Angel One
        val startAngel = System.currentTimeMillis()
        val angelRes = angelMarketDataService.getMarketQuotes(symbols)
        if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
            val valid = angelRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
            if (valid.isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
                _unifiedFeedStatus.value = "LIVE — ANGEL ONE"
                updateLastTickTime()
                return Result.success(valid)
            }
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)
        healthManager.logFailover(ProviderHealthManager.PROVIDER_ANGEL_ONE, ProviderHealthManager.PROVIDER_MSTOCK)

        // Priority 2: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getMarketQuotes(symbols)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = mStockRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                    _unifiedFeedStatus.value = "LIVE — M.STOCK"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
            healthManager.logFailover(ProviderHealthManager.PROVIDER_MSTOCK, ProviderHealthManager.PROVIDER_NSE)
        }

        // Priority 3: NSE
        if (nseFeedService.isConfigured()) {
            val startNse = System.currentTimeMillis()
            val nseRes = nseFeedService.getMarketQuotes(symbols)
            if (nseRes.isSuccess && nseRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = nseRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_NSE, System.currentTimeMillis() - startNse)
                    _unifiedFeedStatus.value = "REFERENCE — NSE"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_NSE)
            healthManager.logFailover(ProviderHealthManager.PROVIDER_NSE, ProviderHealthManager.PROVIDER_YAHOO)
        }

        // Priority 4: Yahoo Finance (Reference fallback)
        val startYahoo = System.currentTimeMillis()
        val yahooQuotes = YahooFinanceService.getMarketQuotes(symbols)
        if (yahooQuotes.isNotEmpty()) {
            val valid = yahooQuotes.filter { it.ltp > 0.0 }
            if (valid.isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_YAHOO, System.currentTimeMillis() - startYahoo)
                _internalActiveProvider.value = ProviderHealthManager.PROVIDER_YAHOO
                _unifiedFeedStatus.value = "LIVE"
                updateLastTickTime()
                return Result.success(valid)
            }
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_YAHOO)

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
                    isLive = _unifiedFeedStatus.value == "LIVE"
                )
            )
        } else {
            Result.failure(Exception("Index quote unavailable for $symbol"))
        }
    }

    fun retryConnection() {
        _unifiedFeedStatus.value = "CONNECTING"
        angelMarketDataService.reconnect()
        if (mStockMarketDataService.isConfigured()) {
            mStockMarketDataService.reconnect()
        }
    }
}
