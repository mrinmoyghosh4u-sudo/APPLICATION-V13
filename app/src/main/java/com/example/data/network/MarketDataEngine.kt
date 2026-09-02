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
    // Single Source of Truth from MarketDataStore
    val providerState: StateFlow<com.example.data.model.MarketDataProviderState> = MarketDataStore.providerState

    companion object {
        private const val TAG = "MarketDataEngine"
        private const val STALE_THRESHOLD_MS = 30_000L // 30 seconds
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _unifiedFeedStatus = MutableStateFlow("CONNECTING")
    val unifiedFeedStatus: StateFlow<String> = _unifiedFeedStatus.asStateFlow()

    private val _marketBreadth = MutableStateFlow<MarketBreadth?>(null)
    val marketBreadth: StateFlow<MarketBreadth?> = _marketBreadth.asStateFlow()

    private val _internalActiveProvider = MutableStateFlow(com.example.data.model.MarketDataProviders.UPSTOX)
    val internalActiveProvider: StateFlow<String> = _internalActiveProvider.asStateFlow()
    
    private val _lastTickTimeMs = MutableStateFlow(0L)
    val lastTickTimeMs: StateFlow<Long> = _lastTickTimeMs.asStateFlow()

    private var heartbeatJob: Job? = null
    private var primaryProviderOverride: String? = null

    init {
        scope.launch {
            MarketDataStore.providerState.collect { state ->
                _lastTickTimeMs.value = state.lastUpdate
                _internalActiveProvider.value = state.provider
                _unifiedFeedStatus.value = when {
                    state.live && !state.stale -> "LIVE — ${com.example.data.model.MarketDataProviders.getDisplayName(state.provider).uppercase()}"
                    state.status == "MARKET CLOSED" -> "MARKET CLOSED"
                    state.stale -> "STALE DATA"
                    state.status == "WAITING_FOR_FIRST_TICK" -> "WAITING FOR FIRST TICK"
                    state.status == "CONNECTING" -> "CONNECTING"
                    else -> "REAL MARKET DATA UNAVAILABLE"
                }
            }
        }
    }

    fun setPrimaryMarketDataProvider(providerName: String) {
        primaryProviderOverride = providerName
    }

    private fun getProviderPriorityOrder(): List<String> {
        val rawSelected = primaryProviderOverride 
            ?: sessionManager?.primaryMarketDataProvider 
            ?: com.example.data.model.MarketDataProviders.UPSTOX
        val primary = com.example.data.model.MarketDataProviders.normalize(rawSelected)
        val defaultList = listOf(
            com.example.data.model.MarketDataProviders.UPSTOX,
            com.example.data.model.MarketDataProviders.FYERS,
            com.example.data.model.MarketDataProviders.ANGEL_ONE
        )
        return listOf(primary) + defaultList.filter { it != primary }
    }

    fun updateLastTickTime(provider: String = _internalActiveProvider.value) {
        val canonical = com.example.data.model.MarketDataProviders.normalize(provider)
        _lastTickTimeMs.value = System.currentTimeMillis()
        MarketDataStore.updateProviderLive(canonical, _lastTickTimeMs.value)
    }

    // =========================================================================
    // 1. LIVE OPTION CHAIN
    // Priority: Dynamic based on primary selection (Upstox / Fyers / Angel One)
    // =========================================================================
    suspend fun getOptionChain(symbol: String, expiry: String? = null, forceRefresh: Boolean = false): Result<OptionChain> {
        val activeProvider = com.example.data.model.MarketDataStore.providerState.value.provider
        val targetExpiry = expiry ?: ""
        
        if (!forceRefresh) {
            val cached = com.example.data.model.OptionChainCache.get(symbol, targetExpiry, activeProvider)
            if (cached != null) {
                return Result.success(cached)
            }
        }
        
        com.example.data.model.OptionChainCache.setLoading()
        
        val providers = getProviderPriorityOrder()
        
        for (provider in providers) {
            when (provider) {
                com.example.data.model.MarketDataProviders.UPSTOX -> {
                    val upstoxService = upstoxMarketDataService
                    if (upstoxService?.isConfigured() == true) {
                        val upstoxRes = upstoxService.getOptionChain(symbol, targetExpiry)
                        if (upstoxRes.isSuccess) {
                            val strikes = upstoxRes.getOrDefault(emptyList())
                            if (strikes.isNotEmpty()) {
                                val underlyingPrice = com.example.data.model.MarketDataStore.getTick(symbol)?.price ?: 0.0
                                val chain = OptionChain(symbol = symbol, expiry = targetExpiry, underlyingLtp = underlyingPrice, strikes = strikes)
                                com.example.data.model.OptionChainCache.update(chain, provider)
                                return Result.success(chain)
                            }
                        }
                    }
                }
                com.example.data.model.MarketDataProviders.FYERS -> {
                    val fyersService = fyersMarketDataService
                    if (fyersService?.isConfigured() == true) {
                        val fyersRes = fyersService.getOptionChain(symbol, targetExpiry)
                        if (fyersRes.isSuccess) {
                            val strikes = fyersRes.getOrDefault(emptyList())
                            if (strikes.isNotEmpty()) {
                                val underlyingPrice = com.example.data.model.MarketDataStore.getTick(symbol)?.price ?: 0.0
                                val chain = OptionChain(symbol = symbol, expiry = targetExpiry, underlyingLtp = underlyingPrice, strikes = strikes)
                                com.example.data.model.OptionChainCache.update(chain, provider)
                                return Result.success(chain)
                            }
                        }
                    }
                }
                com.example.data.model.MarketDataProviders.ANGEL_ONE -> {
                    val angelService = angelMarketDataService
                    if (angelService != null) {
                        val angelRes = angelService.getOptionChain(symbol, targetExpiry)
                        if (angelRes.isSuccess) {
                            val strikes = angelRes.getOrDefault(emptyList())
                            if (strikes.isNotEmpty()) {
                                val underlyingPrice = com.example.data.model.MarketDataStore.getTick(symbol)?.price ?: 0.0
                                val chain = OptionChain(symbol = symbol, expiry = targetExpiry, underlyingLtp = underlyingPrice, strikes = strikes)
                                com.example.data.model.OptionChainCache.update(chain, provider)
                                return Result.success(chain)
                            }
                        }
                    }
                }
            }
        }
        
        com.example.data.model.OptionChainCache.setError()
        return Result.failure(Exception("REAL OPTION CHAIN UNAVAILABLE"))
    }

    // =========================================================================
    // 2. HISTORICAL DATA
    // Priority: Dynamic based on primary selection (Upstox / Fyers / Angel One)
    // =========================================================================
    suspend fun getHistoricalCandles(symbol: String, interval: String): Result<List<HistoricalCandle>> {
        val providers = getProviderPriorityOrder()
        for (provider in providers) {
            when (provider) {
                com.example.data.model.MarketDataProviders.UPSTOX -> {
                    val upstoxService = upstoxMarketDataService
                    if (upstoxService?.isConfigured() == true) {
                        val upstoxRes = upstoxService.getHistoricalCandles(symbol, interval)
                        if (upstoxRes.isSuccess) {
                            val candles = upstoxRes.getOrDefault(emptyList())
                            if (candles.isNotEmpty()) {
                                return Result.success(candles)
                            }
                        }
                    }
                }
                com.example.data.model.MarketDataProviders.FYERS -> {
                    val fyersService = fyersMarketDataService
                    if (fyersService?.isConfigured() == true) {
                        val fyersRes = fyersService.getHistoricalCandles(symbol, interval, "", "")
                        if (fyersRes.isSuccess) {
                            val candles = fyersRes.getOrDefault(emptyList())
                            if (candles.isNotEmpty()) {
                                return Result.success(candles)
                            }
                        }
                    }
                }
                com.example.data.model.MarketDataProviders.ANGEL_ONE -> {
                    val angelService = angelMarketDataService
                    if (angelService?.isConfigured() == true) {
                        val angelRes = angelService.getHistoricalCandles(symbol, interval)
                        if (angelRes.isSuccess) {
                            val candles = angelRes.getOrDefault(emptyList())
                            if (candles.isNotEmpty()) {
                                return Result.success(candles)
                            }
                        }
                    }
                }
            }
        }
        return Result.failure(Exception("Historical data fetch failed from all providers"))
    }

    suspend fun getMarketBreadth(): Result<MarketBreadth> {
        val quotesRes = getMarketQuotes(listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX"))
        if (quotesRes.isSuccess) {
            val quotes = quotesRes.getOrDefault(emptyList())
            var adv = 0
            var dec = 0
            var unch = 0
            for (item in quotes) {
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
    // Priority: Dynamic based on primary selection (Upstox / Fyers / Angel One)
    // =========================================================================
    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (symbols.isEmpty()) return Result.success(emptyList())

        val providers = getProviderPriorityOrder()

        for (provider in providers) {
            when (provider) {
                com.example.data.model.MarketDataProviders.UPSTOX -> {
                    val upstoxService = upstoxMarketDataService
                    if (upstoxService?.isConfigured() == true) {
                        val upstoxRes = upstoxService.getMarketQuotes(symbols)
                        if (upstoxRes.isSuccess && upstoxRes.getOrDefault(emptyList()).isNotEmpty()) {
                            val valid = upstoxRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                            if (valid.isNotEmpty()) {
                                return Result.success(valid)
                            }
                        }
                    }
                }
                com.example.data.model.MarketDataProviders.FYERS -> {
                    val fyersService = fyersMarketDataService
                    if (fyersService?.isConfigured() == true) {
                        val fyersRes = fyersService.getMarketQuotes(symbols)
                        if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                            val valid = fyersRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                            if (valid.isNotEmpty()) {
                                return Result.success(valid)
                            }
                        }
                    }
                }
                com.example.data.model.MarketDataProviders.ANGEL_ONE -> {
                    val angelService = angelMarketDataService
                    if (angelService != null) {
                        val angelRes = angelService.getMarketQuotes(symbols)
                        if (angelRes.isSuccess && (angelRes.getOrDefault(emptyList())).isNotEmpty()) {
                            val valid = (angelRes.getOrDefault(emptyList())).filter { it.ltp > 0.0 }
                            if (valid.isNotEmpty()) {
                                return Result.success(valid)
                            }
                        }
                    }
                }
            }
        }

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
                source = com.example.data.model.MarketDataProviders.UPSTOX
            )
        )
    }
    
    suspend fun updateFyersTick(tick: MarketTick) {
        MarketDataStore.updateTick(
            RealTimePriceTick(
                symbol = tick.symbol,
                price = tick.ltp,
                timestamp = System.currentTimeMillis(),
                source = com.example.data.model.MarketDataProviders.FYERS
            )
        )
    }

    suspend fun updateAngelTick(tick: MarketTick) {
        MarketDataStore.updateTick(
            RealTimePriceTick(
                symbol = tick.symbol,
                price = tick.ltp,
                timestamp = System.currentTimeMillis(),
                source = com.example.data.model.MarketDataProviders.ANGEL_ONE
            )
        )
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

    suspend fun subscribeToMarketData(symbols: List<String>) {
        if (symbols.isEmpty()) return
        val providers = getProviderPriorityOrder()
        for (provider in providers) {
            when (provider) {
                com.example.data.model.MarketDataProviders.UPSTOX -> {
                    if (upstoxMarketDataService?.isConfigured() == true) {
                        upstoxMarketDataService?.subscribeToMarketData(symbols)
                    }
                }
                com.example.data.model.MarketDataProviders.FYERS -> {
                    if (fyersMarketDataService?.isConfigured() == true) {
                        fyersMarketDataService?.subscribeToMarketData(symbols)
                    }
                }
                com.example.data.model.MarketDataProviders.ANGEL_ONE -> {
                    val angelService = angelMarketDataService
                    if (angelService != null) {
                        symbols.forEach { sym ->
                            val exch = when {
                                sym.contains("CRUDE", ignoreCase = true) -> "MCX"
                                sym.contains("SENSEX", ignoreCase = true) || sym.contains("BANKEX", ignoreCase = true) -> "BSE"
                                else -> "NSE"
                            }
                            val token = angelService.instrumentMaster.resolveAngelToken(sym, exch)
                            if (token != null) {
                                val exType = InstrumentMasterService.getExchangeType(exch)
                                angelService.subscribeToTokens(exType, listOf(token))
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 5. OPTION EXPIRIES FAILOVER
    // Priority: Dynamic based on primary selection (Upstox / Fyers / Angel One) -> OptionExpiryUtil
    // =========================================================================
    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        val providers = getProviderPriorityOrder()

        for (provider in providers) {
            when (provider) {
                com.example.data.model.MarketDataProviders.UPSTOX -> {
                    val upstoxService = upstoxMarketDataService
                    if (upstoxService?.isConfigured() == true) {
                        val upstoxRes = upstoxService.getOptionExpiries(symbol)
                        if (upstoxRes.isSuccess && upstoxRes.getOrDefault(emptyList()).isNotEmpty()) {
                            val expiries = com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol, upstoxRes.getOrDefault(emptyList()))
                            if (expiries.isNotEmpty() && !expiries.contains("UNAVAILABLE")) {
                                return Result.success(expiries)
                            }
                        }
                    }
                }
                com.example.data.model.MarketDataProviders.FYERS -> {
                    val fyersService = fyersMarketDataService
                    if (fyersService?.isConfigured() == true) {
                        val fyersRes = fyersService.getOptionExpiries(symbol)
                        if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                            val expiries = com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol, fyersRes.getOrDefault(emptyList()))
                            if (expiries.isNotEmpty() && !expiries.contains("UNAVAILABLE")) {
                                return Result.success(expiries)
                            }
                        }
                    }
                }
                com.example.data.model.MarketDataProviders.ANGEL_ONE -> {
                    val angelService = angelMarketDataService
                    if (angelService?.isConfigured() == true) {
                        val angelRes = angelService.getOptionExpiries(symbol)
                        if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
                            val expiries = com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol, angelRes.getOrDefault(emptyList()))
                            if (expiries.isNotEmpty() && !expiries.contains("UNAVAILABLE")) {
                                return Result.success(expiries)
                            }
                        }
                    }
                }
            }
        }

        // Priority Fallback: InstrumentMaster / OptionExpiryUtil
        val expiries = com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol)
        if (expiries.isNotEmpty() && !expiries.contains("UNAVAILABLE")) {
            return Result.success(expiries)
        }

        return Result.failure(Exception("No valid option expiries available for $symbol"))
    }
}
