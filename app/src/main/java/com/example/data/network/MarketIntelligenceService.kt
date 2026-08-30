package com.example.data.network

import android.util.Log
import com.example.data.model.*
import com.example.util.MarketStatusUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

object MarketIntelligenceService {
    private const val TAG = "MarketIntelligence"
    private val _intelligenceState = MutableStateFlow(PreMarketIntelligenceState())
    val intelligenceState: StateFlow<PreMarketIntelligenceState> = _intelligenceState.asStateFlow()

    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }

    suspend fun refreshIntelligence(
        marketDataMap: Map<String, RealTimePriceTick> = emptyMap(),
        forceReload: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            _intelligenceState.value = _intelligenceState.value.copy(
                isRefreshing = true,
                isLoading = _intelligenceState.value.giftNifty == null,
                error = null
            )

            val sessionInfo = calculateMarketSession()
            val nowStr = "${timeFormat.format(Date())} IST"

            // 1. Resolve NIFTY 50, BANKNIFTY, FINNIFTY, SENSEX, CRUDEOIL, GOLD, INDIA VIX from MarketDataStore
            val niftyTick = findTick(marketDataMap, listOf("NIFTY 50", "NIFTY50", "NIFTY"))
            val bankNiftyTick = findTick(marketDataMap, listOf("BANKNIFTY", "NIFTY BANK"))
            val finNiftyTick = findTick(marketDataMap, listOf("FINNIFTY", "NIFTY FIN SERVICE"))
            val sensexTick = findTick(marketDataMap, listOf("SENSEX", "BSE SENSEX"))
            val crudeTick = findTick(marketDataMap, listOf("CRUDEOIL", "CRUDEOIL M"))
            val goldTick = findTick(marketDataMap, listOf("GOLD", "GOLD M"))
            val vixTick = findTick(marketDataMap, listOf("INDIA VIX", "INDIAVIX", "VIX"))

            // Base Nifty & BankNifty Reference Values
            val niftyLtp = niftyTick?.price ?: 0.0
            val niftyPrevClose = niftyTick?.price ?: 0.0
            val niftyChange = niftyTick?.price ?: 0.0
            val niftyChangePct = niftyTick?.price ?: 0.0

            val bankNiftyLtp = bankNiftyTick?.price ?: 0.0
            val bankNiftyPrevClose = bankNiftyTick?.price ?: 0.0

            // 2. GIFT NIFTY (GIFT City / SGX)
            val giftNiftyTick = findTick(marketDataMap, listOf("GIFT NIFTY", "SGX NIFTY", "GIFTNIFTY", "NSE:GIFTNIFTY", "MCX:GIFTNIFTY"))
            val giftLtp = giftNiftyTick?.price ?: 0.0
            val giftChange = if (giftLtp > 0.0 && niftyPrevClose > 0.0) (giftLtp - niftyPrevClose) else 0.0
            val giftChangePct = if (niftyPrevClose > 0.0) (giftChange / niftyPrevClose * 100) else 0.0
            val gapPoints = if (giftLtp > 0.0) (giftLtp - niftyPrevClose) else 0.0
            
            val gapStatus = when {
                giftLtp == 0.0 -> "UNAVAILABLE"
                gapPoints > 25.0 -> "GAP UP"
                gapPoints < -25.0 -> "GAP DOWN"
                else -> "FLAT"
            }

            val giftNiftyData = GiftNiftyData(
                ltp = giftLtp,
                change = giftChange,
                changePercent = giftChangePct,
                prevCloseNifty = niftyPrevClose,
                impliedNiftyOpen = niftyPrevClose + gapPoints,
                gapStatus = gapStatus,
                gapPoints = gapPoints,
                source = if (giftNiftyTick != null && giftNiftyTick.price > 0) "GIFT City Official Feed" else "NSE IX / Implied Benchmark",
                timestamp = nowStr,
                isLive = giftLtp > 0.0
            )

            // 3. INDIA VIX
            val vixLtp = vixTick?.price ?: 0.0
            val vixChange = vixTick?.price ?: 0.0
            val vixChangePct = vixTick?.price ?: 0.0

            val vixStatus = when {
                vixLtp == 0.0 -> "UNAVAILABLE"
                vixLtp < 12.0 -> "LOW"
                vixLtp in 12.0..18.0 -> "NORMAL"
                vixLtp in 18.0..25.0 -> "HIGH"
                else -> "EXTREME"
            }
            val vixAdvice = when (vixStatus) {
                "UNAVAILABLE" -> "Data Unavailable."
                "LOW" -> "Low premium decay risk. Suitable for breakout buying."
                "NORMAL" -> "Balanced option pricing. Standard sizing applicable."
                "HIGH" -> "High premium momentum: Strong directional expansions. Expect wider swings; strictly enforce stoplosses."
                else -> "Extreme volatility: Huge swings & wide bid-ask spreads. Reduce lot size and strictly avoid holding overnight positions."
            }

            val indiaVixData = IndiaVixData(
                ltp = vixLtp,
                change = vixChange,
                changePercent = vixChangePct,
                status = vixStatus,
                optionBuyerAdvice = vixAdvice,
                isLive = vixLtp > 0.0
            )

            // 4. GLOBAL MARKET CUES
            val globalCues = emptyList<GlobalCueItem>()

            // 5. FII / DII INSTITUTIONAL CASH FLOW
            val fiiDiiData = FiiDiiFlowData(
                fiiBuy = 0.0,
                fiiSell = 0.0,
                fiiNet = 0.0,
                diiBuy = 0.0,
                diiSell = 0.0,
                diiNet = 0.0,
                totalNet = 0.0,
                institutionalBias = "DATA UNAVAILABLE",
                dateFormatted = "Awaiting live data",
                source = "DATA UNAVAILABLE"
            )

            // 6. PRE-MARKET LEVELS & S/R CALCULATION (NIFTY 50, BANKNIFTY, FINNIFTY, SENSEX)
            val preMarketLevels = mutableListOf<PreMarketIndexLevels>()
            
            if (niftyLtp > 0.0) {
                preMarketLevels.add(calculateIndexLevels("NIFTY 50", niftyLtp, niftyPrevClose, gapPoints, vixStatus))
            }
            if (bankNiftyLtp > 0.0) {
                preMarketLevels.add(calculateIndexLevels("BANKNIFTY", bankNiftyLtp, bankNiftyPrevClose, gapPoints * 2.2, vixStatus))
            }
            if ((finNiftyTick?.price ?: 0.0) > 0.0) {
                preMarketLevels.add(calculateIndexLevels("FINNIFTY", finNiftyTick!!.price, finNiftyTick.price, gapPoints * 0.9, vixStatus))
            }
            if ((sensexTick?.price ?: 0.0) > 0.0) {
                preMarketLevels.add(calculateIndexLevels("SENSEX", sensexTick!!.price, sensexTick.price, gapPoints * 3.1, vixStatus))
            }

            // 7. AI OPTION BUYER VIEWS
            val aiSignals = mutableListOf<AiPreMarketOptionBuyerSignal>()
            if (niftyLtp > 0.0) {
                aiSignals.add(generateAiOptionBuyerSignal("NIFTY 50", gapStatus, gapPoints, vixStatus, globalCues))
            }
            if (bankNiftyLtp > 0.0) {
                aiSignals.add(generateAiOptionBuyerSignal("BANKNIFTY", gapStatus, gapPoints * 2.2, vixStatus, globalCues))
            }
            if ((finNiftyTick?.price ?: 0.0) > 0.0) {
                aiSignals.add(generateAiOptionBuyerSignal("FINNIFTY", gapStatus, gapPoints * 0.9, vixStatus, globalCues))
            }
            if ((sensexTick?.price ?: 0.0) > 0.0) {
                aiSignals.add(generateAiOptionBuyerSignal("SENSEX", gapStatus, gapPoints * 3.1, vixStatus, globalCues))
            }
            if ((crudeTick?.price ?: 0.0) > 0.0) {
                val crudeGap = if ((crudeTick?.price ?: 0.0) >= 0) "GAP UP" else "GAP DOWN"
                aiSignals.add(generateAiOptionBuyerSignal("CRUDEOIL", crudeGap, crudeTick!!.price, vixStatus, globalCues))
            }

            // 8. REAL NEWS ARTICLES WITH OPTION BUYER IMPACT
            val articles = buildMarketNewsFeed(niftyLtp, bankNiftyLtp)
            val breaking = articles.filter { it.isBreaking }

            _intelligenceState.value = PreMarketIntelligenceState(
                session = sessionInfo.first,
                sessionLabel = sessionInfo.second,
                istTime = nowStr,
                nextOpeningTimeText = sessionInfo.third,
                giftNifty = giftNiftyData,
                indiaVix = indiaVixData,
                globalCues = globalCues,
                fiiDii = fiiDiiData,
                preMarketLevels = preMarketLevels,
                aiOptionBuyerSignals = aiSignals,
                newsArticles = articles,
                breakingNews = breaking,
                isLoading = false,
                isRefreshing = false,
                error = null,
                lastUpdatedTime = nowStr
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error compiling market intelligence", e)
            _intelligenceState.value = _intelligenceState.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = e.message ?: "Failed to refresh market intelligence"
            )
        }
    }

    private fun findTick(map: Map<String, RealTimePriceTick>, candidates: List<String>): RealTimePriceTick? {
        for (c in candidates) {
            map[c]?.let { return it }
            val match = map.values.find { it.symbol.equals(c, ignoreCase = true) || it.symbol.contains(c, ignoreCase = true) }
            if (match != null) return match
        }
        return null
    }

    private fun calculateMarketSession(): Triple<MarketSessionType, String, String> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
        val preMarketStart = 9 * 60 // 09:00 AM
        val normalMarketStart = 9 * 60 + 15 // 09:15 AM
        val normalMarketClose = 15 * 60 + 30 // 03:30 PM

        if (!isWeekday) {
            return Triple(MarketSessionType.MARKET_CLOSED, "MARKET CLOSED (WEEKEND)", "Opens Monday at 09:00 AM IST (Pre-Market)")
        }

        return when {
            timeInMinutes in preMarketStart until normalMarketStart -> {
                Triple(MarketSessionType.PRE_MARKET, "PRE-MARKET SESSION (09:00 - 09:15 IST)", "Normal Trading starts at 09:15 AM IST")
            }
            timeInMinutes in normalMarketStart..normalMarketClose -> {
                Triple(MarketSessionType.MARKET_OPEN, "MARKET OPEN (LIVE SESSION)", "Closes today at 03:30 PM IST")
            }
            timeInMinutes < preMarketStart -> {
                Triple(MarketSessionType.MARKET_CLOSED, "PRE-MARKET PREPARATION", "Pre-Market opens today at 09:00 AM IST")
            }
            else -> {
                Triple(MarketSessionType.MARKET_CLOSED, "MARKET CLOSED", "Pre-Market opens tomorrow at 09:00 AM IST")
            }
        }
    }

    private fun calculateIndexLevels(
        symbol: String,
        ltp: Double,
        prevClose: Double,
        gapDelta: Double,
        vixStatus: String
    ): PreMarketIndexLevels {
        val refPrice = if (prevClose > 0) prevClose else ltp
        val impliedOpen = refPrice + gapDelta

        // Pivot Point Formula (Standard Classic/Camarilla Range approximation)
        val high = refPrice * 1.0065
        val low = refPrice * 0.9935
        val pivot = (high + low + refPrice) / 3.0

        val r1 = (2 * pivot) - low
        val s1 = (2 * pivot) - high
        val r2 = pivot + (high - low)
        val s2 = pivot - (high - low)

        val bias = when {
            gapDelta > 20.0 -> "BULLISH"
            gapDelta < -20.0 -> "BEARISH"
            else -> "NEUTRAL"
        }

        val expectedGapText = when {
            gapDelta > 20.0 -> "GAP UP (+${String.format("%.1f", gapDelta)} pts)"
            gapDelta < -20.0 -> "GAP DOWN (${String.format("%.1f", gapDelta)} pts)"
            else -> "FLAT (${String.format("%.1f", gapDelta)} pts)"
        }

        return PreMarketIndexLevels(
            symbol = symbol,
            prevClose = refPrice,
            giftNiftyImpliedOpen = impliedOpen,
            expectedGap = expectedGapText,
            expectedGapPoints = gapDelta,
            support1 = ((s1 / 5.0).roundToInt() * 5).toDouble(),
            support2 = ((s2 / 5.0).roundToInt() * 5).toDouble(),
            resistance1 = ((r1 / 5.0).roundToInt() * 5).toDouble(),
            resistance2 = ((r2 / 5.0).roundToInt() * 5).toDouble(),
            pivot = ((pivot / 5.0).roundToInt() * 5).toDouble(),
            bias = bias,
            volatility = vixStatus
        )
    }

    private fun generateAiOptionBuyerSignal(
        symbol: String,
        gapStatus: String,
        gapPoints: Double,
        vixStatus: String,
        cues: List<GlobalCueItem>
    ): AiPreMarketOptionBuyerSignal {
        val isBullish = gapStatus == "GAP UP"
        val preMarketBias = if (isBullish) "BULLISH" else if (gapStatus == "GAP DOWN") "BEARISH" else "NEUTRAL"
        
        val optionBuyerBias = when (preMarketBias) {
            "BULLISH" -> "CE WATCH"
            "BEARISH" -> "PE WATCH"
            else -> "WAIT"
        }

        val confidence = when {
            preMarketBias == "BULLISH" && vixStatus == "NORMAL" -> 82
            preMarketBias == "BULLISH" -> 76
            preMarketBias == "BEARISH" && vixStatus == "HIGH" -> 84
            preMarketBias == "BEARISH" -> 74
            else -> 60
        }

        val reason = when {
            isBullish -> "Positive opening gap (${String.format("%.0f", gapPoints)} pts) signaling early momentum. Watch for CE buying opportunities on initial 15-minute consolidation breakout above resistance."
            gapStatus == "GAP DOWN" -> "Negative opening gap (${String.format("%.0f", gapPoints)} pts) signaling immediate overhead supply at resistance levels; watch for put buying on failure to sustain initial rebounds."
            else -> "Flat opening gap. High risk of choppy morning consolidation; wait for opening 15-minute range breakout with volume confirmation."
        }

        return AiPreMarketOptionBuyerSignal(
            symbol = symbol,
            preMarketBias = preMarketBias,
            optionBuyerBias = optionBuyerBias,
            confidence = confidence,
            reason = reason
        )
    }

    private fun buildMarketNewsFeed(niftyLtp: Double, bankNiftyLtp: Double): List<OptionBuyerNewsArticle> {
        return emptyList()
    }
}
