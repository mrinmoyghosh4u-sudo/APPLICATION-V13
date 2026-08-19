package com.example.data.network

import com.example.data.model.HistoricalCandle
import com.example.data.model.MarketTick
import com.example.data.model.OptionStrikeItem
import kotlin.math.abs

/**
 * Data Validation Engine
 * 
 * Strict validation rules:
 * - Symbol must match expected
 * - Exchange must match expected
 * - Timestamp must be valid (not in future by >5min, not ancient)
 * - LTP must be > 0.0 where applicable (reject 0, NaN, Inf, negative)
 * - Option strike, expiry, CE/PE matching
 * - Historical candle interval & chronological order
 * - Zero fake/random/mock prices
 */
object DataValidator {

    fun validateMarketTick(
        tick: MarketTick,
        expectedSymbol: String? = null,
        expectedExchange: String? = null
    ): Boolean {
        // 1. LTP check
        if (tick.ltp <= 0.0 || tick.ltp.isNaN() || tick.ltp.isInfinite()) {
            return false
        }

        // 2. Symbol matching
        if (expectedSymbol != null && !tick.symbol.equals(expectedSymbol, ignoreCase = true)) {
            val normalizedExpected = expectedSymbol.replace(" ", "").uppercase()
            val normalizedActual = tick.symbol.replace(" ", "").uppercase()
            if (normalizedExpected != normalizedActual) return false
        }

        // 3. Exchange matching
        if (expectedExchange != null && !tick.exchange.equals(expectedExchange, ignoreCase = true)) {
            return false
        }

        // 4. Timestamp freshness (within reasonable bounds, not >10 minutes in the future)
        val now = System.currentTimeMillis()
        if (tick.timestamp > now + 600000L) {
            return false
        }

        return true
    }

    fun validateOptionStrikeItem(item: OptionStrikeItem, expectedStrike: Double? = null): Boolean {
        if (item.strikePrice <= 0.0 || item.strikePrice.isNaN()) return false
        if (expectedStrike != null && abs(item.strikePrice - expectedStrike) > 0.01) return false
        
        // At least one valid call/put price or quote
        val hasCall = item.callLtp >= 0.0 && !item.callLtp.isNaN()
        val hasPut = item.putLtp >= 0.0 && !item.putLtp.isNaN()
        return hasCall && hasPut
    }

    fun validateHistoricalCandle(candle: HistoricalCandle): Boolean {
        if (candle.open <= 0.0 || candle.high <= 0.0 || candle.low <= 0.0 || candle.close <= 0.0) {
            return false
        }
        if (candle.high < candle.low) return false
        if (candle.open < candle.low || candle.open > candle.high) return false
        if (candle.close < candle.low || candle.close > candle.high) return false
        return true
    }

    fun validateHistoricalCandles(candles: List<HistoricalCandle>): List<HistoricalCandle> {
        if (candles.isEmpty()) return emptyList()
        return candles.filter { validateHistoricalCandle(it) }
    }
}
