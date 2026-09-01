package com.example.data.network

import android.util.Log
import com.example.data.model.HistoricalCandle
import com.example.data.model.IndexQuote
import com.example.data.model.MarketBreadth
import com.example.data.model.RealTimePriceTick
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
import kotlinx.coroutines.launch

/**
 * UNIFIED MARKET DATA ENGINE / REPOSITORY
 * 
 * Central orchestrator implementing automatic data failover:
 * Priority: 1. Upstox (Primary) -> 2. Fyers (Secondary) -> 3. Angel One (Third) -> SIGNAL PAUSED / UNAVAILABLE
 *
 * STRICT REQUIREMENTS:
 * - NO YAHOO FINANCE
 * - NO HARDCODED OR RANDOM / SYNTHETIC PRICES
 * - Return DATA UNAVAILABLE or fail Result if real live data is missing.
 * - Signal generation pauses when all live market data sources disconnect or go stale.
 */
class MarketDataEngine(
    var upstoxMarketDataService: UpstoxMarketDataService? = null,
    var fyersMarketDataService: FyersMarketDataService? = null,
    val angelMarketDataService: AngelOneMarketDataService? = null,
    private val sessionManager: SessionManager? = null,
    private val healthManager: ProviderHealthManager? = null
) {
    private val _providerState = MutableStateFlow(com.example.data.model.MarketDataProviderState())
    val providerState: StateFlow<com.example.data.model.MarketDataProviderState> = _providerState.asStateFlow()

    companion object {
        private const val TAG = "MarketDataEngine"
        private const val STALE_THRESHOLD_MS = 30_000L // 30 seconds
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _unifiedFeedStatus = MutableStateFlow("CONNECTING")
    val unifiedFeedStatus: StateFlow<String> = _unifiedFeedStatus.asStateFlow()

    private val _marketBreadth = MutableStateFlow<MarketBreadth?>(null)
    val marketBreadth: StateFlow<MarketBreadth?> = _marketBreadth.asStateFlow()

    private val _internalActiveProvider = MutableStateFlow(ProviderHealthManager.PROVIDER_UPSTOX)
    val internalActiveProvider: StateFlow<String> = _internalActiveProvider.asStateFlow()
    
    private val _lastTickTimeMs = MutableStateFlow(0L)
    val lastTickTimeMs: StateFlow<Long> = _lastTickTimeMs.asStateFlow()

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
                delay(10000)
                val lastTick = _lastTickTimeMs.value
                val now = System.currentTimeMillis()
                
                if (lastTick > 0 && now - lastTick > STALE_THRESHOLD_MS) {
                    Log.w(TAG, "Market Data Stale (>30s silence).")
                    if (_unifiedFeedStatus.value.contains("LIVE")) {
                        _unifiedFeedStatus.value = "STALE DATA"
                        _providerState.value = _providerState.value.copy(stale = true, live = false, status = "STALE")
                    }
                }
            }
        }
    }

    fun updateLastTickTime() {
        _lastTickTimeMs.value = System.currentTimeMillis()
        _providerState.value = _providerState.value.copy(
            stale = false,
            live = true,
            status = "CONNECTED",
            lastUpdate = System.currentTimeMillis(),
            provider = _internalActiveProvider.value
        )
    }

    // =========================================================================
    // 1. LIVE OPTION CHAIN
    // Priority: 1. Upstox -> 2. Fyers -> 3. Angel One -> 4. Unavailable
    // =========================================================================
    suspend fun getOptionChain(symbol: String, expiry: String? = null): Result<OptionChain> {
        // Priority 1: Upstox (Primary)
        val upstoxService = upstoxMarketDataService
        if (upstoxService?.isConfigured() == true) {
            val upstoxRes = upstoxService.getOptionChain(symbol, expiry ?: "")
            if (upstoxRes.isSuccess) {
                val strikes = upstoxRes.getOrDefault(emptyList())
                if (strikes.isNotEmpty()) {
                    _unifiedFeedStatus.value = "LIVE — UPSTOX"
                    _internalActiveProvider.value = ProviderHealthManager.PROVIDER_UPSTOX
                    updateLastTickTime()
                    val underlyingPrice = MarketDataStore.getTick(symbol)?.price ?: 0.0
                    return Result.success(OptionChain(symbol = symbol, expiry = expiry ?: "", underlyingLtp = underlyingPrice, strikes = strikes))
                }
            }
        }

        // Priority 2: Fyers (Secondary)
        val fyersService = fyersMarketDataService
        if (fyersService?.isConfigured() == true) {
            val fyersRes = fyersService.getOptionChain(symbol, expiry ?: "")
            if (fyersRes.isSuccess) {
                val strikes = fyersRes.getOrDefault(emptyList())
                if (strikes.isNotEmpty()) {
                    _unifiedFeedStatus.value = "LIVE — FYERS"
                    _internalActiveProvider.value = ProviderHealthManager.PROVIDER_FYERS
                    updateLastTickTime()
                    val underlyingPrice = MarketDataStore.getTick(symbol)?.price ?: 0.0
                    return Result.success(OptionChain(symbol = symbol, expiry = expiry ?: "", underlyingLtp = underlyingPrice, strikes = strikes))
                }
            }
        }

        // Priority 3: Angel One (Third)
        val angelRes = angelMarketDataService?.getOptionChain(symbol, expiry ?: "")
        if (angelRes?.isSuccess == true) {
            val strikes = angelRes.getOrDefault(emptyList())
            if (strikes.isNotEmpty()) {
                _unifiedFeedStatus.value = "LIVE — ANGEL ONE"
                _internalActiveProvider.value = ProviderHealthManager.PROVIDER_ANGEL_ONE
                updateLastTickTime()
                val underlyingPrice = MarketDataStore.getTick(symbol)?.price ?: 0.0
                return Result.success(OptionChain(symbol = symbol, expiry = expiry ?: "", underlyingLtp = underlyingPrice, strikes = strikes))
            }
        }

        _unifiedFeedStatus.value = "SIGNAL PAUSED — NO DATA"
        return Result.failure(Exception("REAL OPTION CHAIN UNAVAILABLE"))
    }

    // =========================================================================
    // 2. HISTORICAL DATA
    // Priority: 1. Upstox -> 2. Fyers -> 3. Angel One -> 4. Unavailable
    // =========================================================================
    suspend fun getHistoricalCandles(symbol: String, interval: String): Result<List<HistoricalCandle>> {
        // Priority 1: Upstox (Primary)
        val upstoxService = upstoxMarketDataService
        if (upstoxService?.isConfigured() == true) {
            val upstoxRes = upstoxService.getHistoricalCandles(symbol, interval)
            if (upstoxRes.isSuccess) {
                val candles = upstoxRes.getOrDefault(emptyList()).map {
                    HistoricalCandle(
                        time = "",
                        timestamp = 0L,
                        open = it.open.toDouble(),
                        high = it.high.toDouble(),
                        low = it.low.toDouble(),
                        close = it.close.toDouble(),
                        volume = it.volume.toLong()
                    )
                }
                if (candles.isNotEmpty()) {
                    return Result.success(candles)
                }
            }
        }

        // Priority 2: Fyers (Secondary)
        val fyersService = fyersMarketDataService
        if (fyersService?.isConfigured() == true) {
            val fyersRes = fyersService.getHistoricalCandles(symbol, interval, "", "")
            if (fyersRes.isSuccess) {
                val candles = fyersRes.getOrDefault(emptyList()).map {
                    HistoricalCandle(
                        time = "",
                        timestamp = 0L,
                        open = it.open.toDouble(),
                        high = it.high.toDouble(),
                        low = it.low.toDouble(),
                        close = it.close.toDouble(),
                        volume = it.volume.toLong()
                    )
                }
                if (candles.isNotEmpty()) {
                    return Result.success(candles)
                }
            }
        }

        // Priority 3: Angel One (Third)
        val angelRes = angelMarketDataService?.getHistoricalCandles(symbol, interval)
        if (angelRes?.isSuccess == true) {
            val candles = angelRes.getOrDefault(emptyList()).map {
                HistoricalCandle(
                    time = "",
                    timestamp = 0L,
                    open = it.open.toDouble(),
                    high = it.high.toDouble(),
                    low = it.low.toDouble(),
                    close = it.close.toDouble(),
                    volume = it.volume.toLong()
                )
            }
            if (candles.isNotEmpty()) {
                return Result.success(candles)
            }
        }

        return Result.failure(Exception("REAL HISTORICAL DATA UNAVAILABLE"))
    }

    // =========================================================================
    // 3. MARKET BREADTH
    // =========================================================================
    suspend fun getMarketBreadth(): Result<MarketBreadth> {
        val symbols = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX", "CRUDEOIL", "CRUDEOIL M")
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
    // Priority: 1. Upstox -> 2. Fyers -> 3. Angel One -> 4. Unavailable
    // =========================================================================
    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (symbols.isEmpty()) return Result.success(emptyList())

        // Priority 1: Upstox (Primary)
        val upstoxService = upstoxMarketDataService
        if (upstoxService?.isConfigured() == true) {
            val upstoxRes = upstoxService.getMarketQuotes(symbols)
            if (upstoxRes.isSuccess && upstoxRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = upstoxRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    _unifiedFeedStatus.value = "LIVE — UPSTOX"
                    _internalActiveProvider.value = ProviderHealthManager.PROVIDER_UPSTOX
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
        }
    
        // Priority 2: Fyers (Secondary)
        val fyersService = fyersMarketDataService
        if (fyersService?.isConfigured() == true) {
            val fyersRes = fyersService.getMarketQuotes(symbols)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = fyersRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    _unifiedFeedStatus.value = "LIVE — FYERS"
                    _internalActiveProvider.value = ProviderHealthManager.PROVIDER_FYERS
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
        }
        
        // Priority 3: Angel One (Third)
        val angelRes = angelMarketDataService?.getMarketQuotes(symbols)
        if (angelRes?.isSuccess == true && (angelRes.getOrDefault(emptyList())).isNotEmpty()) {
            val valid = (angelRes.getOrDefault(emptyList())).filter { it.ltp > 0.0 }
            if (valid.isNotEmpty()) {
                _unifiedFeedStatus.value = "LIVE — ANGEL ONE"
                _internalActiveProvider.value = ProviderHealthManager.PROVIDER_ANGEL_ONE
                updateLastTickTime()
                return Result.success(valid)
            }
        }

        _unifiedFeedStatus.value = "REAL MARKET DATA UNAVAILABLE"
        return Result.failure(Exception("REAL MARKET DATA UNAVAILABLE"))
    }

    suspend fun getIndexQuote(symbol: String, exchange: String = "NSE"): Result<IndexQuote> {
        val quotes = getMarketQuotes(listOf(symbol))
        val item = quotes.getOrNull()?.firstOrNull()
        return if (item != null && item.ltp > 0.0) {
            Result.success(
                IndexQuote(
                    symbol = item.symbol,
                    exchange = item.exchange,
                    ltp = item.ltp,
                    change = item.change,
                    changePercent = item.changePercent,
                    open = 0.0,
                    high = 0.0,
                    low = 0.0,
                    previousClose = (item.ltp - item.change),
                    timestamp = System.currentTimeMillis(),
                    isLive = _unifiedFeedStatus.value.contains("LIVE")
                )
            )
        } else {
            Result.failure(Exception("Index quote unavailable for $symbol"))
        }
    }

    suspend fun updateUpstoxTick(tick: MarketTick) {
        MarketDataStore.updateTick(
            RealTimePriceTick(
                symbol = tick.symbol,
                price = tick.ltp,
                timestamp = System.currentTimeMillis(),
                source = "UPSTOX"
            )
        )
        _unifiedFeedStatus.value = "LIVE — UPSTOX"
        _internalActiveProvider.value = ProviderHealthManager.PROVIDER_UPSTOX
        updateLastTickTime()
    }
    
    suspend fun updateFyersTick(tick: MarketTick) {
        MarketDataStore.updateTick(
            RealTimePriceTick(
                symbol = tick.symbol,
                price = tick.ltp,
                timestamp = System.currentTimeMillis(),
                source = "FYERS"
            )
        )
        _unifiedFeedStatus.value = "LIVE — FYERS"
        _internalActiveProvider.value = ProviderHealthManager.PROVIDER_FYERS
        updateLastTickTime()
    }

    suspend fun updateAngelTick(tick: MarketTick) {
        MarketDataStore.updateTick(
            RealTimePriceTick(
                symbol = tick.symbol,
                price = tick.ltp,
                timestamp = System.currentTimeMillis(),
                source = "ANGEL ONE"
            )
        )
        _unifiedFeedStatus.value = "LIVE — ANGEL ONE"
        _internalActiveProvider.value = ProviderHealthManager.PROVIDER_ANGEL_ONE
        updateLastTickTime()
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
    }
}
