package com.example.util.indicators

import android.util.Log
import com.example.data.model.OptionStrikeItem
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class RealCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class SupertrendResult(
    val value: Double,
    val isBullish: Boolean,
    val upperBand: Double,
    val lowerBand: Double
)

data class VolumeAnalysis(
    val isAvailable: Boolean,
    val currentVolume: Double,
    val avgVolume: Double,
    val isSurging: Boolean
)

data class OiAnalysis(
    val isAvailable: Boolean,
    val totalCallOi: Long,
    val totalPutOi: Long,
    val pcr: Double,
    val isBullishSupport: Boolean,
    val isBearishResistance: Boolean
)

data class IndicatorSnapshot(
    val symbol: String,
    val timeframe: String,
    val ltp: Double,
    val candleCount: Int,
    val ema9: Double?,
    val ema20: Double?,
    val vwap: Double?,
    val rsi: Double?,
    val supertrend: SupertrendResult?,
    val volumeAnalysis: VolumeAnalysis,
    val oiAnalysis: OiAnalysis,
    val isEma9Bullish: Boolean,
    val isEma20Bullish: Boolean,
    val isVwapBullish: Boolean,
    val isRsiBullish: Boolean,
    val isRsiBearish: Boolean,
    val isSupertrendBullish: Boolean,
    val isSupertrendBearish: Boolean
)

object TechnicalIndicators {
    private const val TAG = "TechnicalIndicators"

    // =========================================================================
    // 1. EXPONENTIAL MOVING AVERAGE (EMA)
    // Formula: Multiplier = 2 / (period + 1)
    // EMA = (Close - PrevEMA) * Multiplier + PrevEMA
    // =========================================================================

    fun calculateEma(candles: List<RealCandle>, period: Int): Double? {
        if (candles.size < period || period <= 0) return null
        val closes = candles.map { it.close }
        val multiplier = 2.0 / (period + 1.0)

        // Seed with Simple Moving Average (SMA) of first `period` bars
        var ema = closes.take(period).sum() / period.toDouble()

        for (i in period until closes.size) {
            ema = (closes[i] * multiplier) + (ema * (1.0 - multiplier))
        }
        return ema
    }

    fun calculateEmaSeries(candles: List<RealCandle>, period: Int): List<Double>? {
        if (candles.size < period || period <= 0) return null
        val closes = candles.map { it.close }
        val multiplier = 2.0 / (period + 1.0)

        var ema = closes.take(period).sum() / period.toDouble()
        val result = mutableListOf<Double>()
        result.add(ema)

        for (i in period until closes.size) {
            ema = (closes[i] * multiplier) + (ema * (1.0 - multiplier))
            result.add(ema)
        }
        return result
    }

    // =========================================================================
    // 2. VOLUME WEIGHTED AVERAGE PRICE (VWAP)
    // Formula: VWAP = Sum(Typical Price * Volume) / Sum(Volume)
    // Typical Price = (High + Low + Close) / 3
    // Session VWAP resets at daily session start
    // =========================================================================

    fun calculateVwap(candles: List<RealCandle>): Double? {
        if (candles.isEmpty()) return null

        // Filter today's session candles (or use all available intraday candles)
        val latestTime = candles.last().timestamp
        val calLatest = Calendar.getInstance().apply { timeInMillis = latestTime }
        val sessionDay = calLatest.get(Calendar.DAY_OF_YEAR)
        val sessionYear = calLatest.get(Calendar.YEAR)

        val sessionCandles = candles.filter { c ->
            val cal = Calendar.getInstance().apply { timeInMillis = c.timestamp }
            cal.get(Calendar.DAY_OF_YEAR) == sessionDay && cal.get(Calendar.YEAR) == sessionYear
        }.ifEmpty { candles }

        var cumulativePv = 0.0
        var cumulativeVol = 0.0

        for (c in sessionCandles) {
            if (c.volume > 0.0) {
                val typicalPrice = (c.high + c.low + c.close) / 3.0
                cumulativePv += typicalPrice * c.volume
                cumulativeVol += c.volume
            }
        }

        if (cumulativeVol <= 0.0) {
            // If individual candle volumes are unavailable, fallback to volume from candles or return null
            return null
        }

        return cumulativePv / cumulativeVol
    }

    // =========================================================================
    // 3. RELATIVE STRENGTH INDEX (RSI)
    // Formula: Standard Wilder's Smoothed RSI (Period 14)
    // RS = Avg Gain / Avg Loss
    // RSI = 100 - (100 / (1 + RS))
    // =========================================================================

    fun calculateRsi(candles: List<RealCandle>, period: Int = 14): Double? {
        if (candles.size <= period || period <= 0) return null

        val closes = candles.map { it.close }
        val changes = mutableListOf<Double>()
        for (i in 1 until closes.size) {
            changes.add(closes[i] - closes[i - 1])
        }

        if (changes.size < period) return null

        var avgGain = 0.0
        var avgLoss = 0.0

        // Initial simple average of gains and losses
        for (i in 0 until period) {
            val chg = changes[i]
            if (chg > 0.0) avgGain += chg
            else if (chg < 0.0) avgLoss += abs(chg)
        }
        avgGain /= period.toDouble()
        avgLoss /= period.toDouble()

        // Wilder's exponential smoothing for subsequent bars
        for (i in period until changes.size) {
            val chg = changes[i]
            val gain = if (chg > 0.0) chg else 0.0
            val loss = if (chg < 0.0) abs(chg) else 0.0

            avgGain = ((avgGain * (period - 1)) + gain) / period.toDouble()
            avgLoss = ((avgLoss * (period - 1)) + loss) / period.toDouble()
        }

        if (avgLoss == 0.0) {
            return if (avgGain == 0.0) 50.0 else 100.0
        }

        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    // =========================================================================
    // 4. SUPERTREND
    // Parameters: Period = 10, Multiplier = 3.0
    // ATR = Wilder's Smoothed Average True Range
    // Bands: Basic Upper/Lower, Final Upper/Lower with Trailing Lock
    // =========================================================================

    fun calculateSupertrend(
        candles: List<RealCandle>,
        period: Int = 10,
        multiplier: Double = 3.0
    ): SupertrendResult? {
        if (candles.size <= period || period <= 0) return null

        // 1. Calculate True Range (TR) for each candle
        val trList = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val h = candles[i].high
            val l = candles[i].low
            val prevC = candles[i - 1].close
            val tr = max(h - l, max(abs(h - prevC), abs(l - prevC)))
            trList.add(tr)
        }

        if (trList.size < period) return null

        // 2. Initial ATR (Simple average of first `period` TRs)
        var atr = trList.take(period).sum() / period.toDouble()
        val atrList = mutableListOf<Double>()
        atrList.add(atr)

        // Wilder's ATR Smoothing
        for (i in period until trList.size) {
            atr = ((atr * (period - 1)) + trList[i]) / period.toDouble()
            atrList.add(atr)
        }

        // 3. Compute Bands and Trend Trailing
        var prevFinalUpper = 0.0
        var prevFinalLower = 0.0
        var isBullish = true
        var supertrendValue = 0.0

        val candleOffset = period // index in candles corresponding to atrList[0]

        for (j in atrList.indices) {
            val candleIdx = candleOffset + j
            val c = candles[candleIdx]
            val prevC = candles[candleIdx - 1].close
            val curAtr = atrList[j]

            val hl2 = (c.high + c.low) / 2.0
            val basicUpper = hl2 + (multiplier * curAtr)
            val basicLower = hl2 - (multiplier * curAtr)

            val finalUpper = if (j == 0 || basicUpper < prevFinalUpper || prevC > prevFinalUpper) basicUpper else prevFinalUpper
            val finalLower = if (j == 0 || basicLower > prevFinalLower || prevC < prevFinalLower) basicLower else prevFinalLower

            // Trend determination
            isBullish = if (j == 0) {
                c.close >= finalLower
            } else {
                if (isBullish) {
                    c.close >= finalLower
                } else {
                    c.close > finalUpper
                }
            }

            supertrendValue = if (isBullish) finalLower else finalUpper
            prevFinalUpper = finalUpper
            prevFinalLower = finalLower
        }

        return SupertrendResult(
            value = supertrendValue,
            isBullish = isBullish,
            upperBand = prevFinalUpper,
            lowerBand = prevFinalLower
        )
    }

    // =========================================================================
    // 5. VOLUME ANALYSIS
    // Compares current bar volume with 20-period average volume
    // =========================================================================

    fun calculateVolumeAnalysis(candles: List<RealCandle>, currentVolume: Long): VolumeAnalysis {
        val nonZeroVolumes = candles.map { it.volume }.filter { it > 0.0 }
        val curVol = if (currentVolume > 0L) currentVolume.toDouble() else candles.lastOrNull()?.volume ?: 0.0

        if (nonZeroVolumes.isEmpty() && curVol <= 0.0) {
            return VolumeAnalysis(
                isAvailable = false,
                currentVolume = 0.0,
                avgVolume = 0.0,
                isSurging = false
            )
        }

        val recentVol = nonZeroVolumes.takeLast(20)
        val avgVol = if (recentVol.isNotEmpty()) recentVol.sum() / recentVol.size.toDouble() else curVol

        val isSurging = avgVol > 0.0 && curVol >= (avgVol * 0.75)

        return VolumeAnalysis(
            isAvailable = true,
            currentVolume = curVol,
            avgVolume = avgVol,
            isSurging = isSurging
        )
    }

    // =========================================================================
    // 6. OPTION CHAIN OI ANALYSIS
    // Calculates Total Call/Put OI, PCR, and near ATM Support/Resistance
    // =========================================================================

    fun calculateOptionOiAnalysis(optionChain: List<OptionStrikeItem>?, underlyingLtp: Double): OiAnalysis {
        if (optionChain.isNullOrEmpty()) {
            return OiAnalysis(
                isAvailable = false,
                totalCallOi = 0L,
                totalPutOi = 0L,
                pcr = 1.0,
                isBullishSupport = false,
                isBearishResistance = false
            )
        }

        var totalCallOi = 0L
        var totalPutOi = 0L
        var atmCallOi = 0L
        var atmPutOi = 0L

        val atmThreshold = if (underlyingLtp > 0.0) underlyingLtp * 0.02 else 500.0

        for (item in optionChain) {
            val callOiVal = item.callOi.replace(",", "").trim().toLongOrNull() ?: (item.callOi.toDoubleOrNull()?.toLong() ?: 0L)
            val putOiVal = item.putOi.replace(",", "").trim().toLongOrNull() ?: (item.putOi.toDoubleOrNull()?.toLong() ?: 0L)

            totalCallOi += callOiVal
            totalPutOi += putOiVal

            if (underlyingLtp > 0.0 && abs(item.strikePrice - underlyingLtp) <= atmThreshold) {
                atmCallOi += callOiVal
                atmPutOi += putOiVal
            }
        }

        if (totalCallOi <= 0L && totalPutOi <= 0L) {
            return OiAnalysis(
                isAvailable = false,
                totalCallOi = 0L,
                totalPutOi = 0L,
                pcr = 1.0,
                isBullishSupport = false,
                isBearishResistance = false
            )
        }

        val pcr = if (totalCallOi > 0L) totalPutOi.toDouble() / totalCallOi.toDouble() else 1.0
        val isBullishSupport = pcr >= 0.95 || (atmPutOi > atmCallOi && atmPutOi > 0L)
        val isBearishResistance = pcr <= 1.05 || (atmCallOi > atmPutOi && atmCallOi > 0L)

        return OiAnalysis(
            isAvailable = true,
            totalCallOi = totalCallOi,
            totalPutOi = totalPutOi,
            pcr = pcr,
            isBullishSupport = isBullishSupport,
            isBearishResistance = isBearishResistance
        )
    }

    // =========================================================================
    // 7. COMPREHENSIVE INDICATOR SNAPSHOT
    // =========================================================================

    fun computeSnapshot(
        symbol: String,
        timeframe: String,
        candles: List<RealCandle>,
        optionChain: List<OptionStrikeItem>?,
        currentLtp: Double,
        currentVolume: Long = 0L
    ): IndicatorSnapshot {
        val effectiveLtp = if (currentLtp > 0.0) currentLtp else candles.lastOrNull()?.close ?: 0.0
        val ema9 = calculateEma(candles, 9)
        val ema20 = calculateEma(candles, 20)
        val vwap = calculateVwap(candles)
        val rsi = calculateRsi(candles, 14)
        val supertrend = calculateSupertrend(candles, 10, 3.0)
        val volumeAnalysis = calculateVolumeAnalysis(candles, currentVolume)
        val oiAnalysis = calculateOptionOiAnalysis(optionChain, effectiveLtp)

        val isEma9Bullish = ema9 != null && effectiveLtp >= ema9
        val isEma20Bullish = ema20 != null && ((ema9 != null && ema9 >= ema20) || effectiveLtp >= ema20)
        val isVwapBullish = vwap != null && effectiveLtp >= vwap
        val isRsiBullish = rsi != null && rsi in 45.0..75.0
        val isRsiBearish = rsi != null && rsi in 25.0..55.0
        val isSupertrendBullish = supertrend?.isBullish == true
        val isSupertrendBearish = supertrend?.isBullish == false

        return IndicatorSnapshot(
            symbol = symbol,
            timeframe = timeframe,
            ltp = effectiveLtp,
            candleCount = candles.size,
            ema9 = ema9,
            ema20 = ema20,
            vwap = vwap,
            rsi = rsi,
            supertrend = supertrend,
            volumeAnalysis = volumeAnalysis,
            oiAnalysis = oiAnalysis,
            isEma9Bullish = isEma9Bullish,
            isEma20Bullish = isEma20Bullish,
            isVwapBullish = isVwapBullish,
            isRsiBullish = isRsiBullish,
            isRsiBearish = isRsiBearish,
            isSupertrendBullish = isSupertrendBullish,
            isSupertrendBearish = isSupertrendBearish
        )
    }
}
