package com.example.util.indicators

import android.util.Log
import com.example.data.model.InstrumentIdentity
import com.example.ui.components.CandleData
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe In-Memory Store for Real OHLC Candle Series
 * Feeds the real Technical Indicator Engine and UI Charts.
 * Strictly maintains contract provenance using exact InstrumentIdentity / InstrumentKey.
 */
object CandleStore {
    private const val TAG = "CandleStore"
    private const val MAX_CANDLES = 500

    // Key: "$normalizedKey:$timeframe"
    private val candleMap = ConcurrentHashMap<String, CopyOnWriteArrayList<RealCandle>>()

    fun makeKey(symbolOrKey: String, timeframe: String): String {
        val cleanKey = normalizeKey(symbolOrKey)
        val cleanTf = normalizeTimeframe(timeframe)
        return "$cleanKey:$cleanTf"
    }

    fun makeKey(identity: InstrumentIdentity, timeframe: String): String {
        val key = if (identity.instrumentKey.isNotBlank()) identity.instrumentKey else "${identity.exchange}:${identity.symbol}"
        return "${key.trim().uppercase()}:${normalizeTimeframe(timeframe)}"
    }

    
    fun normalizeKey(symbolOrKey: String): String {
        val raw = symbolOrKey.trim().uppercase()
        try {
            val master = com.example.data.network.InstrumentMasterService.instance
            val token = master?.resolveAngelToken(raw) ?: master?.resolveAngelToken(raw.removePrefix("NSE:").removePrefix("BSE:").removePrefix("MCX:"))
            if (!token.isNullOrBlank()) {
                return token
            }
        } catch (e: Exception) {}
        
        if (raw.contains("|")) {
            return raw
        }
        val upper = raw
        if (!upper.contains(" CE") && !upper.contains(" PE") && !upper.contains(" FUT") && !upper.contains(" ")) {
            return when (upper) {
                "NIFTY", "NIFTY 50", "NIFTY50" -> "NIFTY 50"
                "BANKNIFTY", "NIFTY BANK", "BANK NIFTY" -> "BANKNIFTY"
                "FINNIFTY", "NIFTY FIN SERVICE", "FIN NIFTY" -> "FINNIFTY"
                "MIDCPNIFTY", "MIDCAP NIFTY" -> "MIDCPNIFTY"
                "SENSEX", "BSESN", "BSE SENSEX" -> "SENSEX"
                "BANKEX", "BSE BANKEX" -> "BANKEX"
                else -> upper
            }
        }
        return upper
    }
fun normalizeTimeframe(timeframe: String): String {
        val upper = timeframe.trim().uppercase()
        return when {
            upper == "1M" || (upper.contains("1") && (upper.contains("M") || upper.contains("MIN")) && !upper.contains("15") && !upper.contains("1H") && !upper.contains("1D")) -> "1 MIN"
            upper == "5M" || (upper.contains("5") && (upper.contains("M") || upper.contains("MIN"))) -> "5 MIN"
            upper == "15M" || (upper.contains("15") && (upper.contains("M") || upper.contains("MIN"))) -> "15 MIN"
            upper == "30M" || (upper.contains("30") && (upper.contains("M") || upper.contains("MIN"))) -> "30 MIN"
            upper == "1H" || upper == "60M" || upper.contains("HOUR") -> "1 HOUR"
            upper == "1D" || upper.contains("DAY") || upper.contains("DAILY") -> "1 DAY"
            else -> "5 MIN"
        }
    }

    fun getTimeframeIntervalMs(timeframe: String): Long {
        return when (normalizeTimeframe(timeframe)) {
            "1 MIN" -> 60_000L
            "5 MIN" -> 300_000L
            "15 MIN" -> 900_000L
            "30 MIN" -> 1_800_000L
            "1 HOUR" -> 3_600_000L
            "1 DAY" -> 86_400_000L
            else -> 300_000L
        }
    }

    /**
     * Replaces or initializes candle history with real candles from historical API.
     */
    fun setHistoricalCandles(symbolOrKey: String, timeframe: String, candles: List<RealCandle>) {
        if (candles.isEmpty()) return
        val key = makeKey(symbolOrKey, timeframe)
        val sorted = candles.sortedBy { it.timestamp }.takeLast(MAX_CANDLES)
        candleMap[key] = CopyOnWriteArrayList(sorted)
        Log.d(TAG, "[CANDLE_STORE_SET] Stored ${sorted.size} real historical candles for key $key")
    }

    /**
     * Converts UI CandleData list to RealCandle list and stores it.
     */
    fun setRealHistoricalCandles(symbolOrKey: String, timeframe: String, candles: List<com.example.data.model.HistoricalCandle>) {
        if (candles.isEmpty()) return
        val realCandles = candles.map { c ->
            RealCandle(
                timestamp = c.timestamp,
                open = c.open,
                high = c.high,
                low = c.low,
                close = c.close,
                volume = c.volume.toDouble()
            )
        }
        setHistoricalCandles(symbolOrKey, timeframe, realCandles)
    }

    fun setHistoricalCandleData(symbolOrKey: String, timeframe: String, candles: List<CandleData>) {
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
        setHistoricalCandles(symbolOrKey, timeframe, realCandles)
    }

    /**
     * Ingests a live tick (LTP + Volume) and updates the current active candle or creates a new one.
     */
    @Synchronized
    fun onLiveTick(symbolOrKey: String, ltp: Double, volume: Long, timestamp: Long = System.currentTimeMillis(), timeframe: String = "5 MIN") {
        if (ltp <= 0.0) return
        val key = makeKey(symbolOrKey, timeframe)
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
     * Returns defensive copy of real candles for symbol/key and timeframe.
     */
    fun getCandles(symbolOrKey: String, timeframe: String = "5 MIN"): List<RealCandle> {
        val key = makeKey(symbolOrKey, timeframe)
        val direct = candleMap[key]?.toList()
        if (!direct.isNullOrEmpty()) return direct

        // Fallback check by raw key if key normalization differed
        val rawKey = "${symbolOrKey.trim().uppercase()}:${normalizeTimeframe(timeframe)}"
        return candleMap[rawKey]?.toList() ?: emptyList()
    }

    /**
     * Checks whether we have sufficient real candles for indicator computation.
     */
    fun hasSufficientCandles(symbolOrKey: String, timeframe: String = "5 MIN", minCount: Int = 14): Boolean {
        val candles = getCandles(symbolOrKey, timeframe)
        return candles.size >= minCount
    }

    fun clear(symbolOrKey: String? = null) {
        if (symbolOrKey == null) {
            candleMap.clear()
        } else {
            val cleanKey = normalizeKey(symbolOrKey)
            val keysToRemove = candleMap.keys.filter { it.startsWith("$cleanKey:") || it.startsWith("${symbolOrKey.trim().uppercase()}:") }
            keysToRemove.forEach { candleMap.remove(it) }
        }
    }
}
