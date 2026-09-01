package com.example.data.model

import androidx.compose.runtime.Immutable
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Normalized Canonical Market Data Providers
 */
object MarketDataProviders {
    const val UPSTOX = "UPSTOX"
    const val FYERS = "FYERS"
    const val ANGEL_ONE = "ANGEL_ONE"
    const val DHAN = "DHAN" // ORDER EXECUTION ONLY
    const val UNKNOWN = "UNKNOWN"

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return UNKNOWN
        val clean = raw.trim().uppercase(java.util.Locale.ROOT).replace(" ", "_").replace("-", "_")
        return when {
            clean.contains("UPSTOX") -> UPSTOX
            clean.contains("FYERS") -> FYERS
            clean.contains("ANGEL") -> ANGEL_ONE
            clean.contains("DHAN") -> DHAN
            else -> clean
        }
    }

    fun getDisplayName(provider: String?): String {
        return when (normalize(provider)) {
            UPSTOX -> "Upstox"
            FYERS -> "Fyers"
            ANGEL_ONE -> "Angel One"
            DHAN -> "Dhan (Execution Only)"
            else -> "Unknown"
        }
    }
}

/**
 * Valid Market Data Sources (Legacy / API compatibility)
 */
object MarketDataSourceNames {
    const val UPSTOX = "upstox"
    const val FYERS = "fyers"
    const val ANGEL_ONE = "angelone"
    const val MOCK = "mock"
    const val CACHE = "cache"
}

@Immutable
data class OptionGreeks(
    val delta: Double = 0.0,
    val theta: Double = 0.0,
    val gamma: Double = 0.0,
    val vega: Double = 0.0,
    val rho: Double = 0.0,
    val iv: Double = 0.0
)

@Immutable
data class RealTimePriceTick(
    val symbol: String,
    val price: Double,
    val timestamp: Long,
    val source: String,
    val volume: Long = 0L,
    val oi: Long = 0L,
    val bid: Double = 0.0,
    val ask: Double = 0.0,
    val greeks: OptionGreeks = OptionGreeks(),
    val exchange: String = "NSE",
    val token: String = "",
    val state: String = ""
) {
    val ltp: Double get() = price
}

object MarketDataStore {
    private const val TAG = "MarketDataStore"
    private const val STALE_THRESHOLD_MS = 30_000L // 30s threshold

    private val _providerState = MutableStateFlow(MarketDataProviderState())
    val providerState: StateFlow<MarketDataProviderState> = _providerState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    // Current Ticks
    private val _ticks = MutableStateFlow<Map<String, RealTimePriceTick>>(emptyMap())
    val ticks: StateFlow<Map<String, RealTimePriceTick>> = _ticks.asStateFlow()

    // Upstox Fallback Health (Primary)
    private val _upstoxHealth = MutableStateFlow("UNKNOWN")
    val upstoxHealth: StateFlow<String> = _upstoxHealth.asStateFlow()

    // Fyers Fallback Health (#1)
    private val _fyersHealth = MutableStateFlow("UNKNOWN")
    val fyersHealth: StateFlow<String> = _fyersHealth.asStateFlow()

    // Angel One Fallback Health (#2)
    private val _angelHealth = MutableStateFlow("UNKNOWN")
    val angelHealth: StateFlow<String> = _angelHealth.asStateFlow()

    private val tickerJobs = ConcurrentHashMap<String, Job>()
    
    // Global lock flag for failover events to block mock data
    @Volatile
    var isFailoverActive: Boolean = false
        private set

    // Real tick listener for ProviderHealthManager or other subscribers
    var onTickReceivedListener: ((provider: String, timestamp: Long) -> Unit)? = null

    private var heartbeatJob: Job? = null

    init {
        startStalenessMonitor()
    }

    private fun startStalenessMonitor() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(5000L)
                checkStaleness()
            }
        }
    }

    fun checkStaleness(now: Long = System.currentTimeMillis()) {
        val current = _providerState.value
        if (current.live && current.lastUpdate > 0L) {
            val age = now - current.lastUpdate
            if (age > STALE_THRESHOLD_MS) {
                val isMarketOpen = com.example.util.MarketStatusUtil.getDetailedMarketStatus("NSE").isOpen ||
                        com.example.util.MarketStatusUtil.getDetailedMarketStatus("MCX").isOpen
                if (!isMarketOpen) {
                    _providerState.value = current.copy(
                        live = false,
                        stale = false,
                        status = "MARKET CLOSED"
                    )
                } else {
                    _providerState.value = current.copy(
                        live = false,
                        stale = true,
                        status = "STALE"
                    )
                    when (current.provider) {
                        MarketDataProviders.UPSTOX -> _upstoxHealth.value = "STALE"
                        MarketDataProviders.FYERS -> _fyersHealth.value = "STALE"
                        MarketDataProviders.ANGEL_ONE -> _angelHealth.value = "STALE"
                    }
                }
            }
        }
    }

    fun updateTick(
        source: String,
        symbol: String,
        token: String = "",
        exchange: String = "NSE",
        ltp: Double = 0.0,
        receivedTimestamp: Long = System.currentTimeMillis(),
        state: String = ""
    ) {
        updateTick(
            RealTimePriceTick(
                symbol = symbol,
                price = ltp,
                timestamp = receivedTimestamp,
                source = source,
                token = token,
                exchange = exchange,
                state = state
            )
        )
    }

    fun updateTick(tick: RealTimePriceTick) {
        // Drop MOCK ticks if we are during a failover transition to prevent UI noise
        if (isFailoverActive && tick.source.equals(MarketDataSourceNames.MOCK, ignoreCase = true)) {
            return
        }

        val currentMap = _ticks.value.toMutableMap()
        currentMap[tick.symbol] = tick
        _ticks.value = currentMap

        // Real tick handling: only valid real ticks with positive prices make the feed LIVE
        if (!tick.price.isNaN() && tick.price > 0.0) {
            val canonical = MarketDataProviders.normalize(tick.source)
            if (canonical != MarketDataProviders.UNKNOWN && 
                canonical != MarketDataProviders.DHAN && 
                !tick.source.equals(MarketDataSourceNames.MOCK, ignoreCase = true)) {
                
                val now = System.currentTimeMillis()
                val tickTs = if (tick.timestamp > 0L) tick.timestamp else now
                val pingMs = (now - tickTs).coerceAtLeast(0L)

                _providerState.value = MarketDataProviderState(
                    provider = canonical,
                    status = "LIVE",
                    live = true,
                    stale = false,
                    error = null,
                    lastUpdate = tickTs,
                    ping = pingMs
                )

                when (canonical) {
                    MarketDataProviders.UPSTOX -> _upstoxHealth.value = "LIVE"
                    MarketDataProviders.FYERS -> _fyersHealth.value = "LIVE"
                    MarketDataProviders.ANGEL_ONE -> _angelHealth.value = "LIVE"
                }

                onTickReceivedListener?.invoke(canonical, tickTs)
            }
        }
    }

    fun updateProviderLive(provider: String, timestamp: Long = System.currentTimeMillis()) {
        val canonical = MarketDataProviders.normalize(provider)
        if (canonical != MarketDataProviders.UNKNOWN && canonical != MarketDataProviders.DHAN) {
            val now = System.currentTimeMillis()
            val tickTs = if (timestamp > 0L) timestamp else now
            _providerState.value = MarketDataProviderState(
                provider = canonical,
                status = "LIVE",
                live = true,
                stale = false,
                error = null,
                lastUpdate = tickTs,
                ping = (now - tickTs).coerceAtLeast(0L)
            )
            when (canonical) {
                MarketDataProviders.UPSTOX -> _upstoxHealth.value = "LIVE"
                MarketDataProviders.FYERS -> _fyersHealth.value = "LIVE"
                MarketDataProviders.ANGEL_ONE -> _angelHealth.value = "LIVE"
            }
            onTickReceivedListener?.invoke(canonical, tickTs)
        }
    }

    fun reportProviderConnecting(provider: String) {
        val canonical = MarketDataProviders.normalize(provider)
        if (!_providerState.value.live) {
            _providerState.value = _providerState.value.copy(
                provider = canonical,
                status = "CONNECTING",
                live = false,
                stale = false
            )
        }
    }

    fun reportProviderSubscribed(provider: String, count: Int = 0) {
        val canonical = MarketDataProviders.normalize(provider)
        if (!_providerState.value.live) {
            _providerState.value = _providerState.value.copy(
                provider = canonical,
                status = "WAITING_FOR_FIRST_TICK",
                live = false,
                stale = false
            )
        }
    }

    fun reportProviderDisconnected(provider: String) {
        val canonical = MarketDataProviders.normalize(provider)
        if (_providerState.value.provider == canonical) {
            _providerState.value = _providerState.value.copy(
                status = "DISCONNECTED",
                live = false,
                stale = false
            )
        }
    }

    fun getTickFlow(symbol: String): kotlinx.coroutines.flow.Flow<RealTimePriceTick?> {
        return _ticks.map { it[symbol] }.distinctUntilChanged()
    }

    fun getTick(symbol: String): RealTimePriceTick? {
        return _ticks.value[symbol]
    }

    // Health Reporting Setters
    fun setUpstoxHealth(status: String) { _upstoxHealth.value = status }
    fun setFyersHealth(status: String) { _fyersHealth.value = status }
    fun setAngelHealth(status: String) { _angelHealth.value = status }

    fun setFailoverActive(isActive: Boolean) {
        isFailoverActive = isActive
        if (isActive) {
            Log.w(TAG, "Market Failover Transition Active. Mock feeds temporarily suspended.")
        } else {
            Log.i(TAG, "Market Failover Transition Complete. Feeds restored.")
        }
    }
}
