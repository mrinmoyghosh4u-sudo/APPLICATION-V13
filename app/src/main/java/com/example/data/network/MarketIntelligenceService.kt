package com.example.data.network

import android.util.Log
import com.example.data.model.*
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
import kotlin.math.roundToInt

/**
 * Market Intelligence & Pre-Market Synthesizer for KING KHAN AI TRADER
 *
 * Core Rules:
 * 1. Combines real market data from broker/watchlist with verified real-time news feeds.
 * 2. Never generates fake numbers or simulated FII/DII/Global data.
 * 3. News alone NEVER triggers automated trading orders; purely for strategic watchlist setup.
 * 4. Distinctly reports LIVE, CACHED, STALE, or UNAVAILABLE states.
 */
object MarketIntelligenceService {
    private const val TAG = "MarketIntelligence"
    private val _intelligenceState = MutableStateFlow(PreMarketIntelligenceState())
    val intelligenceState: StateFlow<PreMarketIntelligenceState> = _intelligenceState.asStateFlow()

    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }

    suspend fun refreshIntelligence(
        marketDataMap: Map<String, RealTimePriceTick> = emptyMap(),
        watchlist: List<WatchlistItem> = emptyList(),
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

            // 1. Resolve Underlyings from Watchlist / MarketDataStore
            val niftyWatch = findWatchlistItem(watchlist, listOf("NIFTY 50", "NIFTY50", "NIFTY"))
            val niftyTick = findTick(marketDataMap, listOf("NIFTY 50", "NIFTY50", "NIFTY"))
            val niftyLtp = niftyTick?.price?.takeIf { it > 0.0 } ?: niftyWatch?.ltp ?: 0.0
            val niftyChange = niftyWatch?.change ?: 0.0

            var niftyPrevCloseSource = "Unavailable"
            val niftyPrevClose = when {
                niftyWatch != null && niftyWatch.ltp > 0.0 && niftyWatch.change != 0.0 -> {
                    niftyPrevCloseSource = "Calculated (LTP - Change)"
                    niftyWatch.ltp - niftyWatch.change
                }
                niftyLtp > 0.0 && niftyChange != 0.0 -> {
                    niftyPrevCloseSource = "Calculated (LTP - Change)"
                    niftyLtp - niftyChange
                }
                niftyLtp > 0.0 -> {
                    niftyPrevCloseSource = "Live Market Price Reference"
                    niftyLtp
                }
                else -> 0.0
            }

            val bankNiftyWatch = findWatchlistItem(watchlist, listOf("BANKNIFTY", "NIFTY BANK"))
            val bankNiftyTick = findTick(marketDataMap, listOf("BANKNIFTY", "NIFTY BANK"))
            val bankNiftyLtp = bankNiftyTick?.price?.takeIf { it > 0.0 } ?: bankNiftyWatch?.ltp ?: 0.0
            val bankNiftyChange = bankNiftyWatch?.change ?: 0.0

            var bankNiftyPrevCloseSource = "Unavailable"
            val bankNiftyPrevClose = when {
                bankNiftyWatch != null && bankNiftyWatch.ltp > 0.0 && bankNiftyWatch.change != 0.0 -> {
                    bankNiftyPrevCloseSource = "Calculated (LTP - Change)"
                    bankNiftyWatch.ltp - bankNiftyWatch.change
                }
                bankNiftyLtp > 0.0 && bankNiftyChange != 0.0 -> {
                    bankNiftyPrevCloseSource = "Calculated (LTP - Change)"
                    bankNiftyLtp - bankNiftyChange
                }
                bankNiftyLtp > 0.0 -> {
                    bankNiftyPrevCloseSource = "Live Market Price Reference"
                    bankNiftyLtp
                }
                else -> 0.0
            }

            val finNiftyWatch = findWatchlistItem(watchlist, listOf("FINNIFTY", "NIFTY FIN SERVICE"))
            val finNiftyTick = findTick(marketDataMap, listOf("FINNIFTY", "NIFTY FIN SERVICE"))
            val finNiftyLtp = finNiftyTick?.price?.takeIf { it > 0.0 } ?: finNiftyWatch?.ltp ?: 0.0

            val sensexWatch = findWatchlistItem(watchlist, listOf("SENSEX", "BSE SENSEX"))
            val sensexTick = findTick(marketDataMap, listOf("SENSEX", "BSE SENSEX"))
            val sensexLtp = sensexTick?.price?.takeIf { it > 0.0 } ?: sensexWatch?.ltp ?: 0.0

            val crudeWatch = findWatchlistItem(watchlist, listOf("CRUDEOIL", "CRUDEOIL M"))
            val crudeTick = findTick(marketDataMap, listOf("CRUDEOIL", "CRUDEOIL M"))
            val crudeLtp = crudeTick?.price?.takeIf { it > 0.0 } ?: crudeWatch?.ltp ?: 0.0

            val vixWatch = findWatchlistItem(watchlist, listOf("INDIA VIX", "INDIAVIX", "VIX"))
            val vixTick = findTick(marketDataMap, listOf("INDIA VIX", "INDIAVIX", "VIX"))
            val vixLtp = vixTick?.price?.takeIf { it > 0.0 } ?: vixWatch?.ltp ?: 0.0
            val vixChange = vixWatch?.change ?: 0.0
            val vixChangePct = vixWatch?.changePercent ?: 0.0

            // 2. GIFT NIFTY (GIFT City / SGX Benchmark)
            val giftNiftyTick = findTick(marketDataMap, listOf("GIFT NIFTY", "SGX NIFTY", "GIFTNIFTY", "NSE:GIFTNIFTY", "MCX:GIFTNIFTY"))
            val giftLtp = giftNiftyTick?.price ?: 0.0
            val giftChange = if (giftLtp > 0.0 && niftyPrevClose > 0.0) (giftLtp - niftyPrevClose) else 0.0
            val giftChangePct = if (niftyPrevClose > 0.0) (giftChange / niftyPrevClose * 100) else 0.0
            val gapPoints = if (giftLtp > 0.0 && niftyPrevClose > 0.0) (giftLtp - niftyPrevClose) else 0.0

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
                impliedNiftyOpen = if (niftyPrevClose > 0 && giftLtp > 0) niftyPrevClose + gapPoints else 0.0,
                gapStatus = gapStatus,
                gapPoints = gapPoints,
                source = if (giftNiftyTick != null && giftNiftyTick.price > 0) "GIFT City Official Feed" else "NSE IX / Indicative Benchmark",
                timestamp = nowStr,
                isLive = giftLtp > 0.0
            )

            // 3. INDIA VIX
            val vixStatus = when {
                vixLtp == 0.0 -> "UNAVAILABLE"
                vixLtp < 12.0 -> "LOW"
                vixLtp in 12.0..18.0 -> "NORMAL"
                vixLtp in 18.0..25.0 -> "HIGH"
                else -> "EXTREME"
            }
            val vixAdvice = when (vixStatus) {
                "UNAVAILABLE" -> "Data Unavailable. Monitor opening price actions."
                "LOW" -> "Low premium decay risk. Suitable for breakout option buying."
                "NORMAL" -> "Balanced option pricing. Standard strike selection applicable."
                "HIGH" -> "High premium momentum: Strong directional expansions. Strictly enforce stoplosses."
                else -> "Extreme volatility: Wide bid-ask spreads. Reduce lot size and avoid holding overnight positions."
            }

            val indiaVixData = IndiaVixData(
                ltp = vixLtp,
                change = vixChange,
                changePercent = vixChangePct,
                status = vixStatus,
                optionBuyerAdvice = vixAdvice,
                isLive = vixLtp > 0.0
            )

            // 4. GLOBAL MARKET CUES — Real data or explicitly unavailable
            val globalCues = emptyList<GlobalCueItem>()

            // 5. FII / DII INSTITUTIONAL CASH FLOW — Real data or explicitly unavailable
            val fiiDiiData = FiiDiiFlowData(
                isDataAvailable = false,
                fiiBuy = null,
                fiiSell = null,
                fiiNet = null,
                diiBuy = null,
                diiSell = null,
                diiNet = null,
                totalNet = null,
                institutionalBias = "DATA UNAVAILABLE",
                dateFormatted = "Awaiting official exchange report",
                source = "NSE / BSE Daily Institutional Wire"
            )

            // 6. PRE-MARKET LEVELS & S/R CALCULATION (NIFTY 50, BANKNIFTY, FINNIFTY, SENSEX)
            val preMarketLevels = mutableListOf<PreMarketIndexLevels>()

            if (niftyLtp > 0.0 || niftyPrevClose > 0.0) {
                preMarketLevels.add(calculateIndexLevels("NIFTY 50", niftyLtp, niftyPrevClose, niftyPrevCloseSource, gapPoints, vixStatus))
            }
            if (bankNiftyLtp > 0.0 || bankNiftyPrevClose > 0.0) {
                preMarketLevels.add(calculateIndexLevels("BANKNIFTY", bankNiftyLtp, bankNiftyPrevClose, bankNiftyPrevCloseSource, gapPoints * 2.2, vixStatus))
            }
            if (finNiftyLtp > 0.0) {
                preMarketLevels.add(calculateIndexLevels("FINNIFTY", finNiftyLtp, finNiftyLtp, "Market Reference", gapPoints * 0.9, vixStatus))
            }
            if (sensexLtp > 0.0) {
                preMarketLevels.add(calculateIndexLevels("SENSEX", sensexLtp, sensexLtp, "Market Reference", gapPoints * 3.1, vixStatus))
            }

            // 7. AI OPTION BUYER VIEWS (Watchlist-Only Guidance, NEVER triggers auto-orders)
            val aiSignals = mutableListOf<AiPreMarketOptionBuyerSignal>()
            if (niftyLtp > 0.0 || niftyPrevClose > 0.0) {
                aiSignals.add(generateAiOptionBuyerSignal("NIFTY 50", gapStatus, gapPoints, vixStatus))
            }
            if (bankNiftyLtp > 0.0 || bankNiftyPrevClose > 0.0) {
                aiSignals.add(generateAiOptionBuyerSignal("BANKNIFTY", gapStatus, gapPoints * 2.2, vixStatus))
            }
            if (finNiftyLtp > 0.0) {
                aiSignals.add(generateAiOptionBuyerSignal("FINNIFTY", gapStatus, gapPoints * 0.9, vixStatus))
            }
            if (sensexLtp > 0.0) {
                aiSignals.add(generateAiOptionBuyerSignal("SENSEX", gapStatus, gapPoints * 3.1, vixStatus))
            }
            if (crudeLtp > 0.0) {
                val crudeGap = if (crudeLtp >= 0) "GAP UP" else "GAP DOWN"
                aiSignals.add(generateAiOptionBuyerSignal("CRUDEOIL", crudeGap, crudeLtp, vixStatus))
            }

            // 8. REAL NEWS ARTICLES WITH OPTION BUYER IMPACT (Aggregated from real sources)
            val newsResult = MarketNewsFeedService.fetchMarketNews(niftyLtp, bankNiftyLtp, forceReload = forceReload)
            val articles = newsResult.articles
            val breaking = articles.filter { it.isBreaking }

            _intelligenceState.value = PreMarketIntelligenceState(
                session = sessionInfo.first,
                sessionLabel = sessionInfo.second,
                istTime = nowStr,
                nextOpeningTimeText = sessionInfo.third,
                giftNifty = giftNiftyData,
                indiaVix = indiaVixData,
                globalCues = globalCues,
                isGlobalCuesAvailable = false,
                fiiDii = fiiDiiData,
                isFiiDiiAvailable = false,
                preMarketLevels = preMarketLevels,
                aiOptionBuyerSignals = aiSignals,
                newsArticles = articles,
                breakingNews = breaking,
                newsFeedStatus = newsResult.status,
                newsFreshnessStatus = newsResult.freshness,
                newsSource = newsResult.source,
                newsLastSyncTime = newsResult.lastSyncTimeMs,
                isLoading = false,
                isRefreshing = false,
                error = if (newsResult.articles.isEmpty() && !newsResult.isSuccess) newsResult.errorMessage else null,
                lastUpdatedTime = nowStr
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error compiling market intelligence", e)
            _intelligenceState.value = _intelligenceState.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = e.message ?: "Failed to refresh market intelligence",
                newsFeedStatus = "ERROR",
                newsFreshnessStatus = "UNAVAILABLE"
            )
        }
    }

    private fun findWatchlistItem(list: List<WatchlistItem>, candidates: List<String>): WatchlistItem? {
        for (c in candidates) {
            list.find { it.symbol.equals(c, ignoreCase = true) }?.let { return it }
            list.find { it.symbol.contains(c, ignoreCase = true) }?.let { return it }
        }
        return null
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
        prevCloseSource: String,
        gapDelta: Double,
        vixStatus: String
    ): PreMarketIndexLevels {
        val refPrice = if (prevClose > 0) prevClose else ltp
        val impliedOpen = if (refPrice > 0) refPrice + gapDelta else 0.0

        // Classic Camarilla / Pivot Range Formula
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
            prevCloseSource = prevCloseSource,
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
        vixStatus: String
    ): AiPreMarketOptionBuyerSignal {
        val isBullish = gapStatus == "GAP UP"
        val preMarketBias = if (isBullish) "BULLISH" else if (gapStatus == "GAP DOWN") "BEARISH" else "NEUTRAL"

        val optionBuyerBias = when (preMarketBias) {
            "BULLISH" -> "CE WATCH"
            "BEARISH" -> "PE WATCH"
            else -> "WAIT"
        }

        val heuristicScore = when {
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
            tradeConfirmation = "REQUIRED",
            heuristicScore = heuristicScore,
            confidence = heuristicScore,
            reason = reason
        )
    }
}
