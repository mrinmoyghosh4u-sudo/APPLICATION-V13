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
    val change: Double,
    val changePercent: Double,
    val timestamp: Long,
    val source: String,
    val state: String // "LIVE", "CLOSED", "UNAVAILABLE"
)

object MarketDataStore {
    private val _marketData = MutableStateFlow<Map<String, MarketDataState>>(emptyMap())
    val marketData: StateFlow<Map<String, MarketDataState>> = _marketData.asStateFlow()

    private val internalMap = ConcurrentHashMap<String, MarketDataState>()

    fun updateTick(symbol: String, token: String, exchange: String, ltp: Double, timestamp: Long) {
        val existing = internalMap[symbol]
        val prevClose = existing?.previousClose ?: ltp // We don't have prev close initially
        val change = if (existing?.previousClose != null && existing.previousClose > 0) ltp - existing.previousClose else 0.0
        val changePct = if (existing?.previousClose != null && existing.previousClose > 0) (change / existing.previousClose) * 100 else 0.0
        
        val newState = MarketDataState(
            symbol = symbol,
            token = token,
            exchange = exchange,
            ltp = ltp,
            previousClose = existing?.previousClose ?: 0.0,
            change = change,
            changePercent = changePct,
            timestamp = timestamp,
            source = "AngelOne",
            state = "LIVE"
        )
        internalMap[symbol] = newState
        _marketData.value = internalMap.toMap()
    }
    
    fun setPreviousClose(symbol: String, prevClose: Double) {
        val existing = internalMap[symbol]
        if (existing != null) {
            val change = existing.ltp - prevClose
            val changePct = (change / prevClose) * 100
            internalMap[symbol] = existing.copy(previousClose = prevClose, change = change, changePercent = changePct)
        } else {
            internalMap[symbol] = MarketDataState(
                symbol = symbol, token = "", exchange = "", ltp = 0.0, previousClose = prevClose,
                change = 0.0, changePercent = 0.0, timestamp = 0, source = "AngelOne", state = "UNAVAILABLE"
            )
        }
        _marketData.value = internalMap.toMap()
    }
}
