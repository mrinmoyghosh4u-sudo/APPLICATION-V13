package com.example.util.indicators

import android.util.Log
import com.example.ui.components.CandleData
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe In-Memory Store for Real OHLC Candle Series
 * Feeds the real Technical Indicator Engine for AlgoEngine.
 */
object CandleStore {
    private const val TAG = "CandleStore"
    private const val MAX_CANDLES = 500

    // Key: "$normalizedSymbol:$timeframe"
    private val candleMap = ConcurrentHashMap<String, CopyOnWriteArrayList<RealCandle>>()

    fun makeKey(symbol: String, timeframe: String): String {
        val cleanSym = normalizeSymbol(symbol)
        val cleanTf = normalizeTimeframe(timeframe)
        return "$cleanSym:$cleanTf"
    }

    fun normalizeSymbol(symbol: String): String {
        val upper = symbol.trim().uppercase()
        return when {
            upper == "NIFTY" || upper == "NIFTY 50" || upper == "NIFTY50" -> "NIFTY 50"
            upper == "BANKNIFTY" || upper == "NIFTY BANK" || upper == "BANK NIFTY" -> "BANKNIFTY"
            upper == "FINNIFTY" || upper == "NIFTY FIN SERVICE" || upper == "FIN NIFTY" -> "FINNIFTY"
            upper.contains("MID SELECT") || upper == "MIDCPNIFTY" || upper == "MIDCAP NIFTY" -> "MIDCPNIFTY"
            upper == "SENSEX" || upper == "BSESN" || upper == "BSE SENSEX" -> "SENSEX"
            upper == "BANKEX" || upper == "BSE BANKEX" -> "BANKEX"
            upper.startsWith("CRUDEOILM") || upper == "CRUDEOIL M" -> "CRUDEOIL M"
            upper.startsWith("CRUDEOIL") -> "CRUDEOIL"
            else -> upper
        }
    }

    fun normalizeTimeframe(timeframe: String): String {
        val upper = timeframe.trim().uppercase()
        return when {
            upper.contains("1") && (upper.contains("M") || upper.contains("MIN")) -> "1 MIN"
            upper.contains("5") && (upper.contains("M") || upper.contains("MIN")) -> "5 MIN"
            upper.contains("15") && (upper.contains("M") || upper.contains("MIN")) -> "15 MIN"
            upper.contains("30") && (upper.contains("M") || upper.contains("MIN")) -> "30 MIN"
            upper.contains("DAY") || upper.contains("1D") || upper.contains("DAILY") -> "1 DAY"
            else -> "5 MIN"
        }
    }

    fun getTimeframeIntervalMs(timeframe: String): Long {
        return when (normalizeTimeframe(timeframe)) {
            "1 MIN" -> 60_000L
            "5 MIN" -> 300_000L
            "15 MIN" -> 900_000L
            "30 MIN" -> 1_800_000L
            "1 DAY" -> 86_400_000L
            else -> 300_000L
        }
    }

    /**
     * Replaces or initializes candle history with real candles from historical API.
     */
    fun setHistoricalCandles(symbol: String, timeframe: String, candles: List<RealCandle>) {
        if (candles.isEmpty()) return
        val key = makeKey(symbol, timeframe)
        val sorted = candles.sortedBy { it.timestamp }.takeLast(MAX_CANDLES)
        candleMap[key] = CopyOnWriteArrayList(sorted)
        Log.d(TAG, "[CANDLE_STORE_SET] Stored ${sorted.size} real historical candles for key $key")
    }

    /**
     * Converts UI CandleData list to RealCandle list and stores it.
     */
    fun setHistoricalCandleData(symbol: String, timeframe: String, candles: List<CandleData>) {
        if (candles.isEmpty()) return
        val intervalMs = getTimeframeIntervalMs(timeframe)
        val now = System.currentTimeMillis()
        val count = candles.size
        
        val realCandles = candles.mapIndexed { idx, c ->
            val candleTime = now - ((count - 1 - idx) * intervalMs)
            RealCandle(
                timestamp = candleTime,
                open = c.open.toDouble(),
                high = c.high.toDouble(),
                low = c.low.toDouble(),
                close = c.close.toDouble(),
                volume = c.volume.toDouble()
            )
        }
        setHistoricalCandles(symbol, timeframe, realCandles)
    }

    /**
     * Ingests a live tick (LTP + Volume) and updates the current active candle or creates a new one.
     */
    @Synchronized
    fun onLiveTick(symbol: String, ltp: Double, volume: Long, timestamp: Long = System.currentTimeMillis(), timeframe: String = "5 MIN") {
        if (ltp <= 0.0) return
        val key = makeKey(symbol, timeframe)
        val list = candleMap.getOrPut(key) { CopyOnWriteArrayList() }
        val intervalMs = getTimeframeIntervalMs(timeframe)
        val bucketStart = (timestamp / intervalMs) * intervalMs

        if (list.isEmpty()) {
            list.add(
                RealCandle(
                    timestamp = bucketStart,
                    open = ltp,
                    high = ltp,
                    low = ltp,
                    close = ltp,
                    volume = if (volume > 0L) volume.toDouble() else 0.0
                )
            )
            return
        }

        val lastCandle = list.last()
        val lastBucket = (lastCandle.timestamp / intervalMs) * intervalMs

        if (bucketStart == lastBucket) {
            // Update current bar
            val updated = lastCandle.copy(
                high = maxOf(lastCandle.high, ltp),
                low = minOf(lastCandle.low, ltp),
                close = ltp,
                volume = if (volume > 0L) maxOf(lastCandle.volume, volume.toDouble()) else lastCandle.volume
            )
            list[list.size - 1] = updated
        } else if (bucketStart > lastBucket) {
            // Finalize previous candle and start new candle
            val newCandle = RealCandle(
                timestamp = bucketStart,
                open = ltp,
                high = ltp,
                low = ltp,
                close = ltp,
                volume = if (volume > 0L) (volume - lastCandle.volume.toLong()).coerceAtLeast(0L).toDouble() else 0.0
            )
            list.add(newCandle)
            if (list.size > MAX_CANDLES) {
                list.removeAt(0)
            }
        }
    }

    /**
     * Returns defensive copy of real candles for symbol and timeframe.
     */
    fun getCandles(symbol: String, timeframe: String = "5 MIN"): List<RealCandle> {
        val key = makeKey(symbol, timeframe)
        return candleMap[key]?.toList() ?: emptyList()
    }

    /**
     * Checks whether we have sufficient real candles for indicator computation.
     */
    fun hasSufficientCandles(symbol: String, timeframe: String = "5 MIN", minCount: Int = 14): Boolean {
        val candles = getCandles(symbol, timeframe)
        return candles.size >= minCount
    }

    fun clear(symbol: String? = null) {
        if (symbol == null) {
            candleMap.clear()
        } else {
            val cleanSym = normalizeSymbol(symbol)
            val keysToRemove = candleMap.keys.filter { it.startsWith("$cleanSym:") }
            keysToRemove.forEach { candleMap.remove(it) }
        }
    }
}
