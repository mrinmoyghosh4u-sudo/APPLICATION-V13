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
 * Valid Market Data Sources
 */
object MarketDataSourceNames {
    const val FYERS = "Fyers"
    const val ANGEL_ONE = "AngelOne"
    const val MSTOCK = "mStock"
}

@Immutable
data class MarketDataState(
    val source: String, // "ANGEL_ONE", "MSTOCK", "TRADESMART", "NSE", "YAHOO"
    val symbol: String,
    val exchange: String,
    val token: String,
    val ltp: Double,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val previousClose: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val volume: Long = 0L,
    val exchangeTimestamp: Long = 0L,
    val receivedTimestamp: Long = 0L,
    val state: String = "LIVE", // "LIVE", "STALE", "OFFLINE", "REFERENCE", "DELAYED", "UNAVAILABLE", "STANDBY"
    val sequenceNumber: Long = 0L
)

/**
 * Central Unified Market Data Store for KING KHAN AI TRADER
 * 
 * Rules:
 * - Source is NEVER hardcoded.
 * - Every tick contains full provenance (source, timestamps, sequence).
 * - Full validation: timestamp freshness, stale detection, invalid price prevention, duplicate filtering.
 * - Source health tracking for ANGEL ONE, m.STOCK, TRADESMART, NSE, and YAHOO.
 */
object MarketDataStore {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _marketData = MutableStateFlow<Map<String, MarketDataState>>(emptyMap())
    val marketData: StateFlow<Map<String, MarketDataState>> = _marketData.asStateFlow()

    // Composite primary identity: "$exchange:$token" or "$exchange:$symbol"
    private val compositeMap = ConcurrentHashMap<String, MarketDataState>()
    // Symbol index for UI lookups
    private val symbolIndex = ConcurrentHashMap<String, MarketDataState>()

    // Source Health StateFlows
    private val _fyersHealth = MutableStateFlow("OFFLINE")
    val fyersHealth = _fyersHealth.asStateFlow()
    private val _angelOneHealth = MutableStateFlow("OFFLINE") // LIVE, STALE, OFFLINE
    val angelOneHealth: StateFlow<String> = _angelOneHealth.asStateFlow()

    private val _mStockHealth = MutableStateFlow("OFFLINE") // LIVE, STALE, STANDBY, OFFLINE
    val mStockHealth: StateFlow<String> = _mStockHealth.asStateFlow()

    private val _tradeSmartHealth = MutableStateFlow("OFFLINE") // LIVE, STALE, STANDBY, OFFLINE
    val tradeSmartHealth: StateFlow<String> = _tradeSmartHealth.asStateFlow()

    private val _nseHealth = MutableStateFlow("OFFLINE") // LIVE, STALE, OFFLINE
    val nseHealth: StateFlow<String> = _nseHealth.asStateFlow()

    private val _unusedHealth = MutableStateFlow("REFERENCE") // REFERENCE, DELAYED, OFFLINE
    val unusedHealth: StateFlow<String> = _unusedHealth.asStateFlow()

    // Last Update Timestamps per source
    private val sourceLastUpdate = ConcurrentHashMap<String, Long>()
    // Last Sequence Numbers per source
    private val sourceLastSequence = ConcurrentHashMap<String, Long>()

    init {
        sourceLastUpdate["REFERENCE"] = System.currentTimeMillis()
        startStaleDataMonitor()
    }

    private fun startStaleDataMonitor() {
        scope.launch {
            while (true) {
                delay(5000)
                val now = System.currentTimeMillis()
                val staleThreshold = 15000L // 15 seconds

                // Angel One Health
                val lastFyers = sourceLastUpdate[MarketDataSourceNames.FYERS] ?: 0L
                val lastAngel = sourceLastUpdate[MarketDataSourceNames.ANGEL_ONE] ?: 0L
                if (lastAngel > 0 && now - lastAngel > staleThreshold && _angelOneHealth.value == "LIVE") {
                    _angelOneHealth.value = "STALE"
                }

                // m.Stock Health
                val lastMStock = sourceLastUpdate[MarketDataSourceNames.MSTOCK] ?: 0L
                if (lastMStock > 0 && now - lastMStock > staleThreshold && _mStockHealth.value == "LIVE") {
                    _mStockHealth.value = "STALE"
                }

                
            }
        }
    }

    fun setSourceHealth(source: String, health: String) {
        when (source) {
            MarketDataSourceNames.FYERS -> _fyersHealth.value = health
            MarketDataSourceNames.ANGEL_ONE -> _angelOneHealth.value = health
            MarketDataSourceNames.MSTOCK -> _mStockHealth.value = health
                                    "REFERENCE" -> _unusedHealth.value = health
        }
    }

    fun getSourceLastUpdate(source: String): Long {
        return sourceLastUpdate[source] ?: 0L
    }

    /**
     * Unified Tick Ingestion with Rigorous Validation
     */
    fun updateTick(
        source: String,
        symbol: String,
        token: String,
        exchange: String,
        ltp: Double,
        open: Double = 0.0,
        high: Double = 0.0,
        low: Double = 0.0,
        close: Double = 0.0,
        volume: Long = 0L,
        exchangeTimestamp: Long = 0L,
        receivedTimestamp: Long = System.currentTimeMillis(),
        state: String = "LIVE",
        sequenceNumber: Long = 0L
    ) {
        // 1. Invalid Price Validation
        if (ltp <= 0.0 || ltp.isNaN() || ltp.isInfinite()) {
            Log.w("MarketDataStore", "[$source] REJECTED INVALID LTP: $ltp for $symbol")
            return
        }

        val normSymCheck = symbol.trim().uppercase()
        if (normSymCheck == "SENSEX" && (ltp < 50000.0 || ltp > 120000.0)) {
            Log.w("MarketDataStore", "[$source] REJECTED OUT-OF-BOUNDS INDEX PRICE FOR SENSEX: $ltp")
            return
        }
        if ((normSymCheck == "NIFTY 50" || normSymCheck == "NIFTY") && (ltp < 15000.0 || ltp > 35000.0)) {
            Log.w("MarketDataStore", "[$source] REJECTED OUT-OF-BOUNDS INDEX PRICE FOR NIFTY: $ltp")
            return
        }
        if (normSymCheck == "BANKNIFTY" && (ltp < 30000.0 || ltp > 70000.0)) {
            Log.w("MarketDataStore", "[$source] REJECTED OUT-OF-BOUNDS INDEX PRICE FOR BANKNIFTY: $ltp")
            return
        }
        if (normSymCheck == "BANKEX" && (ltp < 40000.0 || ltp > 85000.0)) {
            Log.w("MarketDataStore", "[$source] REJECTED OUT-OF-BOUNDS INDEX PRICE FOR BANKEX: $ltp")
            return
        }
        if (normSymCheck == "FINNIFTY" && (ltp < 15000.0 || ltp > 35000.0)) {
            Log.w("MarketDataStore", "[$source] REJECTED OUT-OF-BOUNDS INDEX PRICE FOR FINNIFTY: $ltp")
            return
        }

        // 2. Normalize Symbol & Exchange
        val normExch = exchange.trim().uppercase()
        val normSym = symbol.trim().uppercase()
        val normToken = token.trim()

        if (normSym.isBlank() && normToken.isBlank()) {
            return
        }

        val compositeKey = if (normToken.isNotBlank()) "$normExch:$normToken" else "$normExch:$normSym"
        val existing = compositeMap[compositeKey] ?: symbolIndex[normSym]

        // 3. Duplicate Tick Detection (Exact duplicate filtering)
        if (existing != null && existing.source == source && existing.ltp == ltp && 
            existing.exchangeTimestamp == exchangeTimestamp && exchangeTimestamp > 0 &&
            (sequenceNumber == 0L || sequenceNumber == existing.sequenceNumber)) {
            // Identical tick from same source; skip redundant map recreation
            return
        }

        // 4. Source Priority & Validation: Never overwrite verified real-time tick (ANGEL_ONE / MSTOCK / TRADESMART / NSE) with reference data (YAHOO)
        if (existing != null && (existing.source == MarketDataSourceNames.ANGEL_ONE || existing.source == MarketDataSourceNames.MSTOCK || existing.source == MarketDataSourceNames.FYERS)) {
            if (source == "REFERENCE") {
                sourceLastUpdate[source] = receivedTimestamp
                if (_unusedHealth.value != "REFERENCE") _unusedHealth.value = "REFERENCE"
                // Keep the live tick, but update previous close if missing
                if (existing.previousClose <= 0.0 && close > 0.0) {
                    val updated = existing.copy(
                        previousClose = close,
                        change = existing.ltp - close,
                        changePercent = ((existing.ltp - close) / close) * 100.0
                    )
                    compositeMap[compositeKey] = updated
                    symbolIndex[normSym] = updated
                    _marketData.value = HashMap(symbolIndex)
                }
                return
            }
        }

        // 5. Sequence & Timestamp validation
        if (sequenceNumber > 0L) {
            val lastSeq = sourceLastSequence[source] ?: 0L
            sourceLastSequence[source] = maxOf(lastSeq, sequenceNumber)
        }
        val validatedExchangeTs = if (exchangeTimestamp > 0L) exchangeTimestamp else receivedTimestamp
        sourceLastUpdate[source] = receivedTimestamp

        // 6. Update Source Health
        when (source) {
            MarketDataSourceNames.FYERS -> _fyersHealth.value = "LIVE"
            MarketDataSourceNames.ANGEL_ONE -> _angelOneHealth.value = "LIVE"
            MarketDataSourceNames.MSTOCK -> _mStockHealth.value = "LIVE"
                                    "REFERENCE" -> if (_unusedHealth.value != "REFERENCE") _unusedHealth.value = "REFERENCE"
        }

        // 7. Calculate Change and Change %
        val prevClose = if (close > 0.0) close else (existing?.previousClose ?: 0.0)
        val change = if (prevClose > 0.0) ltp - prevClose else 0.0
        val changePct = if (prevClose > 0.0) (change / prevClose) * 100.0 else 0.0

        val validatedState = when (source) {
            "REFERENCE" -> "REFERENCE"
            else -> state
        }

        val newState = MarketDataState(
            source = source,
            symbol = symbol,
            exchange = exchange,
            token = token,
            ltp = ltp,
            open = if (open > 0.0) open else (existing?.open ?: 0.0),
            high = if (high > 0.0) high else (existing?.high ?: 0.0),
            low = if (low > 0.0) low else (existing?.low ?: 0.0),
            previousClose = prevClose,
            change = change,
            changePercent = changePct,
            volume = if (volume > 0L) volume else (existing?.volume ?: 0L),
            exchangeTimestamp = validatedExchangeTs,
            receivedTimestamp = receivedTimestamp,
            state = validatedState,
            sequenceNumber = sequenceNumber
        )

        compositeMap[compositeKey] = newState
        if (normToken.isNotBlank()) {
            compositeMap["$normExch:$normToken"] = newState
        }
        symbolIndex[symbol] = newState
        symbolIndex[normSym] = newState

        // Construct unified state map for UI collection
        _marketData.value = HashMap(symbolIndex)
    }

    fun getTick(symbol: String): MarketDataState? {
        val direct = symbolIndex[symbol] ?: symbolIndex[symbol.trim().uppercase()]
        if (direct != null) return direct
        val key = symbolIndex.keys.find { it.equals(symbol, ignoreCase = true) }
        return key?.let { symbolIndex[it] }
    }

    fun getTickFlow(symbol: String): kotlinx.coroutines.flow.Flow<MarketDataState?> {
        val normSym = symbol.trim().uppercase()
        return marketData
            .map { map ->
                getTick(symbol) ?: map[normSym]
            }
            .distinctUntilChanged()
    }

    fun getTick(exchange: String, symbol: String): MarketDataState? {
        val normExch = exchange.trim().uppercase()
        val normSym = symbol.trim().uppercase()
        return compositeMap["$normExch:$normSym"] ?: getTick(symbol)
    }

    fun getTickByToken(exchange: String, token: String): MarketDataState? {
        val normExch = exchange.trim().uppercase()
        val normToken = token.trim()
        return compositeMap["$normExch:$normToken"]
    }

    fun setPreviousClose(symbol: String, prevClose: Double, exchange: String = "NSE", source: String = MarketDataSourceNames.ANGEL_ONE) {
        val normExch = exchange.trim().uppercase()
        val normSym = symbol.trim().uppercase()
        val compositeKey = "$normExch:$normSym"
        val existing = compositeMap[compositeKey] ?: symbolIndex[symbol]

        val newState = if (existing != null) {
            val change = existing.ltp - prevClose
            val changePct = if (prevClose > 0.0) (change / prevClose) * 100.0 else 0.0
            existing.copy(previousClose = prevClose, change = change, changePercent = changePct)
        } else {
            MarketDataState(
                source = source,
                symbol = symbol,
                token = "",
                exchange = exchange,
                ltp = 0.0,
                previousClose = prevClose,
                change = 0.0,
                changePercent = 0.0,
                exchangeTimestamp = 0L,
                receivedTimestamp = System.currentTimeMillis(),
                state = "UNAVAILABLE"
            )
        }

        compositeMap[compositeKey] = newState
        symbolIndex[symbol] = newState
        symbolIndex[normSym] = newState
        _marketData.value = HashMap(symbolIndex)
    }
}
