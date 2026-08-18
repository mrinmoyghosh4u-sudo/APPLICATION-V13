package com.example.data.network

import android.util.Log
import com.example.data.model.MarketDataState
import com.example.data.model.MarketDataStore
import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem
import com.example.ui.components.CandleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * KING KHAN AI TRADE - Centralized Market Data Manager
 * Primary Market Data Provider: Angel One
 * Secondary Market Data Provider: m.Stock (Fallback)
 *
 * Responsibilities:
 * - WebSocket streaming & health monitoring
 * - Seamless automatic failover (Angel One -> m.Stock -> Unavailable)
 * - Automatic reconnection back to Angel One when primary link recovers
 * - Strict Zero Fallback Prevention (No ₹0.00 / fake data)
 */
class MarketDataManager(
    private val angelMarketDataService: AngelOneMarketDataService,
    private val mStockMarketDataService: MStockMarketDataService,
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _activeProvider = MutableStateFlow("Angel One")
    val activeProvider: StateFlow<String> = _activeProvider.asStateFlow()

    private val _connectionStatus = MutableStateFlow("CONNECTING")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow("")
    val lastUpdateTime: StateFlow<String> = _lastUpdateTime.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val quoteStream: StateFlow<Map<String, MarketDataState>> = MarketDataStore.marketData

    private var monitorJob: Job? = null

    init {
        startConnectionMonitoring()
    }

    private fun startConnectionMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            angelMarketDataService.connectionState.collectLatest { state ->
                Log.d("MarketDataManager", "Angel One WebSocket State: $state")
                when (state) {
                    "CONNECTED", "SUBSCRIBED" -> {
                        _activeProvider.value = "Angel One"
                        _connectionStatus.value = "LIVE"
                        _error.value = null
                        updateTimestamp()
                    }
                    "CONNECTING" -> {
                        if (_activeProvider.value == "Angel One") {
                            _connectionStatus.value = "CONNECTING"
                        }
                    }
                    "DISCONNECTED", "ERROR" -> {
                        // Angel One failed or disconnected -> Attempt failover to m.Stock if configured
                        if (mStockMarketDataService.isConfigured()) {
                            Log.w("MarketDataManager", "Angel One link down. Failing over to m.Stock...")
                            _activeProvider.value = "m.Stock"
                            _connectionStatus.value = "FAILOVER"
                            _error.value = "Angel One connection lost. Switched to m.Stock fallback."
                        } else {
                            _activeProvider.value = "Angel One"
                            _connectionStatus.value = "DISCONNECTED"
                            _error.value = "Primary market data provider disconnected."
                        }
                    }
                }
            }
        }

        // Also update lastUpdateTime whenever a new tick arrives in MarketDataStore
        scope.launch {
            MarketDataStore.marketData.collect {
                if (it.isNotEmpty()) {
                    updateTimestamp()
                }
            }
        }
    }

    private fun updateTimestamp() {
        _lastUpdateTime.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    /**
     * Primary Quote Fetching with Automatic Failover
     */
    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        // 1. Try Angel One (Primary Provider)
        val angelResult = angelMarketDataService.getMarketQuotes(symbols)
        if (angelResult.isSuccess) {
            _activeProvider.value = "Angel One"
            _connectionStatus.value = "LIVE"
            updateTimestamp()
            return angelResult
        }

        Log.w("MarketDataManager", "Angel One getMarketQuotes failed: ${angelResult.exceptionOrNull()?.message}")

        // 2. Failover to m.Stock (Secondary Provider)
        if (mStockMarketDataService.isConfigured()) {
            val mStockResult = mStockMarketDataService.getMarketQuotes(symbols)
            if (mStockResult.isSuccess) {
                _activeProvider.value = "m.Stock"
                _connectionStatus.value = "LIVE"
                updateTimestamp()
                return mStockResult
            }
            Log.w("MarketDataManager", "m.Stock getMarketQuotes failed: ${mStockResult.exceptionOrNull()?.message}")
        }

        // 3. Unavailable State
        _activeProvider.value = "Market Data Unavailable"
        _connectionStatus.value = "DISCONNECTED"
        _error.value = "Market data unavailable across all configured providers."
        return Result.failure(Exception("Market data unavailable from Angel One or m.Stock."))
    }

    /**
     * Option Chain Fetching with Automatic Failover
     */
    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        val angelResult = angelMarketDataService.getOptionChain(symbol, expiry)
        if (angelResult.isSuccess) {
            _activeProvider.value = "Angel One"
            return angelResult
        }

        if (mStockMarketDataService.isConfigured()) {
            val mStockResult = mStockMarketDataService.getOptionChain(symbol, expiry)
            if (mStockResult.isSuccess) {
                _activeProvider.value = "m.Stock"
                return mStockResult
            }
        }

        return Result.failure(Exception("Option Chain data unavailable from Angel One or m.Stock."))
    }

    /**
     * Option Expiries
     */
    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return angelMarketDataService.getOptionExpiries(symbol)
    }

    /**
     * Fetch Real Historical Candle Data
     */
    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<CandleData>> {
        // Attempts real REST call for candles from Angel One
        return angelMarketDataService.getHistoricalCandles(symbol, interval)
    }

    fun retryConnection() {
        _connectionStatus.value = "CONNECTING"
        angelMarketDataService.reconnect()
    }
}
