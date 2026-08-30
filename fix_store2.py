content = """package com.example.data.model

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
 * Valid Market Data Sources
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
    val exchange: String = "NSE"
)

object MarketDataStore {
    private const val TAG = "MarketDataStore"
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

    fun updateTick(tick: RealTimePriceTick) {
        // Drop MOCK ticks if we are during a failover transition to prevent UI noise
        if (isFailoverActive && tick.source == MarketDataSourceNames.MOCK) {
            return
        }

        val currentMap = _ticks.value.toMutableMap()
        currentMap[tick.symbol] = tick
        _ticks.value = currentMap
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
"""
with open("app/src/main/java/com/example/data/model/MarketDataStore.kt", "w") as f:
    f.write(content)
