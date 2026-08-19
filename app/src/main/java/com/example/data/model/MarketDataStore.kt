package com.example.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class MarketDataState(
    val symbol: String,
    val token: String,
    val exchange: String,
    val ltp: Double,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val previousClose: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val volume: Long = 0L,
    val timestamp: Long = 0L,
    val source: String = "AngelOne",
    val state: String = "LIVE" // "LIVE", "CLOSED", "STALE", "UNAVAILABLE"
)

object MarketDataStore {
    private val _marketData = MutableStateFlow<Map<String, MarketDataState>>(emptyMap())
    val marketData: StateFlow<Map<String, MarketDataState>> = _marketData.asStateFlow()

    private val internalMap = ConcurrentHashMap<String, MarketDataState>()
    private val tokenMap = ConcurrentHashMap<String, MarketDataState>() // key: "$exchange:$token" or "$exchangeType:$token"

    fun updateTick(
        symbol: String,
        token: String,
        exchange: String,
        ltp: Double,
        timestamp: Long,
        open: Double = 0.0,
        high: Double = 0.0,
        low: Double = 0.0,
        close: Double = 0.0,
        volume: Long = 0L
    ) {
        val existing = internalMap[symbol]
        val prevClose = if (close > 0.0) close else (existing?.previousClose ?: if (existing?.ltp != null && existing.ltp > 0) existing.ltp else ltp)
        val change = if (prevClose > 0.0) ltp - prevClose else 0.0
        val changePct = if (prevClose > 0.0) (change / prevClose) * 100.0 else 0.0
        
        val newState = MarketDataState(
            symbol = symbol,
            token = token,
            exchange = exchange,
            ltp = ltp,
            open = if (open > 0.0) open else (existing?.open ?: 0.0),
            high = if (high > 0.0) high else (existing?.high ?: 0.0),
            low = if (low > 0.0) low else (existing?.low ?: 0.0),
            previousClose = prevClose,
            change = change,
            changePercent = changePct,
            volume = if (volume > 0L) volume else (existing?.volume ?: 0L),
            timestamp = timestamp,
            source = "AngelOne",
            state = "LIVE"
        )
        internalMap[symbol] = newState
        if (token.isNotBlank()) {
            tokenMap["$exchange:$token"] = newState
        }
        _marketData.value = internalMap.toMap()
    }

    fun getTick(symbol: String): MarketDataState? = internalMap[symbol]

    fun getTickByToken(exchange: String, token: String): MarketDataState? = tokenMap["$exchange:$token"]
    
    fun setPreviousClose(symbol: String, prevClose: Double) {
        val existing = internalMap[symbol]
        if (existing != null) {
            val change = existing.ltp - prevClose
            val changePct = if (prevClose > 0.0) (change / prevClose) * 100.0 else 0.0
            internalMap[symbol] = existing.copy(previousClose = prevClose, change = change, changePercent = changePct)
        } else {
            internalMap[symbol] = MarketDataState(
                symbol = symbol, token = "", exchange = "", ltp = 0.0, previousClose = prevClose,
                change = 0.0, changePercent = 0.0, timestamp = 0L, source = "AngelOne", state = "UNAVAILABLE"
            )
        }
        _marketData.value = internalMap.toMap()
    }
}

