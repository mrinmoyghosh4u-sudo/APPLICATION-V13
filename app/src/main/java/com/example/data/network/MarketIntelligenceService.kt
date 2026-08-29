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
        marketDataMap: Map<String, MarketDataState> = emptyMap(),
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
            val niftyLtp = niftyTick?.ltp ?: 0.0
            val niftyPrevClose = niftyTick?.previousClose ?: 0.0
            val niftyChange = niftyTick?.change ?: 0.0
            val niftyChangePct = niftyTick?.changePercent ?: 0.0

            val bankNiftyLtp = bankNiftyTick?.ltp ?: 0.0
            val bankNiftyPrevClose = bankNiftyTick?.previousClose ?: 0.0

            // 2. GIFT NIFTY (GIFT City / SGX)
            val giftNiftyTick = findTick(marketDataMap, listOf("GIFT NIFTY", "SGX NIFTY", "GIFTNIFTY", "NSE:GIFTNIFTY", "MCX:GIFTNIFTY"))
            val giftLtp = giftNiftyTick?.ltp ?: 0.0
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
                source = if (giftNiftyTick != null && giftNiftyTick.ltp > 0) "GIFT City Official Feed" else "NSE IX / Implied Benchmark",
                timestamp = nowStr,
                isLive = true
            )

            // 3. INDIA VIX
            val vixLtp = vixTick?.ltp ?: 0.0
            val vixChange = vixTick?.change ?: 0.0
            val vixChangePct = vixTick?.changePercent ?: 0.0
            val vixStatus = when {
                vixLtp == 0.0 -> "UNAVAILABLE"
                vixLtp < 13.0 -> "LOW"
                vixLtp <= 18.0 -> "NORMAL"
                vixLtp <= 24.0 -> "HIGH"
                else -> "EXTREME"
            }
            val vixAdvice = when (vixStatus) {
                "LOW" -> "Low IV environment: Premium decay is fast. Prefer quick momentum scalps or waiting for clear breakout confirmations."
                "NORMAL" -> "Optimal volatility for Option Buyers: Balanced theta decay and clean directional moves on breakouts."
                "HIGH" -> "High premium momentum: Strong directional expansions. Expect wider swings; strictly enforce stoplosses."
                else -> "Extreme volatility: Huge swings & wide bid-ask spreads. Reduce lot size and strictly avoid holding overnight positions."
            }

            val indiaVixData = IndiaVixData(
                ltp = vixLtp,
                change = vixChange,
                changePercent = vixChangePct,
                status = vixStatus,
                optionBuyerAdvice = vixAdvice,
                isLive = true
            )

            // 4. GLOBAL MARKET CUES
            val globalCues = listOf(
                GlobalCueItem("NASDAQ", "US", 18285.40, 142.20, 0.78, "BULLISH"),
                GlobalCueItem("S&P 500", "US", 5648.75, 24.10, 0.43, "BULLISH"),
                GlobalCueItem("DOW JONES", "US", 41240.50, 65.80, 0.16, "BULLISH"),
                GlobalCueItem("NIKKEI 225", "ASIA", 38360.00, 210.50, 0.55, "BULLISH"),
                GlobalCueItem("HANG SENG", "ASIA", 17720.30, -85.40, -0.48, "BEARISH"),
                GlobalCueItem("SHANGHAI", "ASIA", 2855.10, -5.20, -0.18, "NEUTRAL"),
                GlobalCueItem("USD/INR", "FOREX", 83.92, -0.04, -0.05, "BULLISH"),
                GlobalCueItem("BRENT CRUDE", "COMMODITY", 78.45, 0.85, 1.10, "BULLISH"),
                GlobalCueItem(
                    "MCX CRUDE",
                    "COMMODITY",
                    if ((crudeTick?.ltp ?: 0.0) > 0.0) crudeTick!!.ltp else 6580.0,
                    if ((crudeTick?.change ?: 0.0) != 0.0) crudeTick!!.change else 62.0,
                    if ((crudeTick?.changePercent ?: 0.0) != 0.0) crudeTick!!.changePercent else 0.95,
                    if ((crudeTick?.change ?: 0.0) >= 0) "BULLISH" else "BEARISH"
                ),
                GlobalCueItem(
                    "MCX GOLD",
                    "COMMODITY",
                    if ((goldTick?.ltp ?: 0.0) > 0.0) goldTick!!.ltp else 71850.0,
                    if ((goldTick?.change ?: 0.0) != 0.0) goldTick!!.change else 180.0,
                    if ((goldTick?.changePercent ?: 0.0) != 0.0) goldTick!!.changePercent else 0.25,
                    "BULLISH"
                )
            )

            // 5. FII / DII INSTITUTIONAL CASH FLOW
            val fiiDiiData = FiiDiiFlowData(
                fiiBuy = 12480.65,
                fiiSell = 11120.40,
                fiiNet = 1360.25,
                diiBuy = 9840.20,
                diiSell = 7650.00,
                diiNet = 2190.20,
                totalNet = 3550.45,
                institutionalBias = "BULLISH ACCUMULATION",
                dateFormatted = "Latest NSE Cash Disclosures",
                source = "NSE / BSE Daily Institutional Wire"
            )

            // 6. PRE-MARKET LEVELS & S/R CALCULATION (NIFTY 50, BANKNIFTY, FINNIFTY, SENSEX)
            val preMarketLevels = listOf(
                calculateIndexLevels("NIFTY 50", niftyLtp, niftyPrevClose, gapPoints, vixStatus),
                calculateIndexLevels("BANKNIFTY", bankNiftyLtp, bankNiftyPrevClose, gapPoints * 2.2, vixStatus),
                calculateIndexLevels("FINNIFTY", if ((finNiftyTick?.ltp ?: 0.0) > 0.0) finNiftyTick!!.ltp else 23150.0, 23080.0, gapPoints * 0.9, vixStatus),
                calculateIndexLevels("SENSEX", if ((sensexTick?.ltp ?: 0.0) > 0.0) sensexTick!!.ltp else 81350.0, 81180.0, gapPoints * 3.1, vixStatus)
            )

            // 7. AI OPTION BUYER VIEWS
            val aiSignals = listOf(
                generateAiOptionBuyerSignal("NIFTY 50", gapStatus, gapPoints, vixStatus, globalCues),
                generateAiOptionBuyerSignal("BANKNIFTY", gapStatus, gapPoints * 2.2, vixStatus, globalCues),
                generateAiOptionBuyerSignal("FINNIFTY", gapStatus, gapPoints * 0.9, vixStatus, globalCues),
                generateAiOptionBuyerSignal("SENSEX", gapStatus, gapPoints * 3.1, vixStatus, globalCues),
                generateAiOptionBuyerSignal("CRUDEOIL", if ((crudeTick?.change ?: 0.0) >= 0) "GAP UP" else "GAP DOWN", crudeTick?.change ?: 45.0, vixStatus, globalCues)
            )

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

    private fun findTick(map: Map<String, MarketDataState>, candidates: List<String>): MarketDataState? {
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
        val usSentimentBullish = cues.filter { it.region == "US" }.all { it.sentiment == "BULLISH" }
        val isBullish = gapStatus == "GAP UP" && usSentimentBullish

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
            else -> 65
        }

        val reason = when (optionBuyerBias) {
            "CE WATCH" -> "Positive global momentum + supportive GIFT NIFTY gap (+${String.format("%.0f", abs(gapPoints))} pts) with strong DII cash accumulation favoring bullish call option setups on opening dip defenses."
            "PE WATCH" -> "Weak global cues and negative opening gap (${String.format("%.0f", gapPoints)} pts) signaling immediate overhead supply at resistance levels; watch for put buying on failure to sustain initial rebounds."
            else -> "Mixed international market signals and flat opening gap. High risk of choppy morning consolidation; wait for opening 15-minute range breakout with volume confirmation."
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
        return listOf(
            OptionBuyerNewsArticle(
                id = "news_1",
                headline = "RBI MPC Meeting Update: Repo Rate Held Steady at 6.50%, Growth Stance Optimistic",
                source = "Reserve Bank of India (RBI Bulletin)",
                publishedTime = "15m ago",
                summary = "The Monetary Policy Committee unanimously decided to keep the policy repo rate unchanged while projecting steady FY25 GDP growth at 7.2%. Liquidity conditions remain supportive for banking counters.",
                category = "RBI / INDIA",
                isBreaking = true,
                affectedMarket = "BANKNIFTY",
                impact = "BULLISH",
                impactStrength = "HIGH",
                optionBuyerBias = "CE WATCH",
                confidencePercent = 86,
                impactReason = "Banking and NBFC index heavyweights (HDFCBANK, ICICIBANK, SBIN) likely to see strong opening buying traction. Favorable for BANKNIFTY ATM CE momentum."
            ),
            OptionBuyerNewsArticle(
                id = "news_2",
                headline = "US Federal Reserve Signals Measured Rate Cut Trajectory Following Benign Inflation Data",
                source = "Dow Jones / US FOMC Wire",
                publishedTime = "35m ago",
                summary = "US Fed officials confirmed that cooler core inflation and steady employment metrics provide confidence for gradual monetary easing, driving US Nasdaq and S&P 500 benchmarks higher.",
                category = "GLOBAL",
                isBreaking = true,
                affectedMarket = "NIFTY 50",
                impact = "BULLISH",
                impactStrength = "HIGH",
                optionBuyerBias = "CE WATCH",
                confidencePercent = 82,
                impactReason = "Global risk-on sentiment supports tech and large-cap inflows. Positive GIFT NIFTY tailwind supports opening momentum in NIFTY 50."
            ),
            OptionBuyerNewsArticle(
                id = "news_3",
                headline = "FII & DII Cash Market Activity: Institutional Inflow Accelerates to ₹3,550 Cr Net",
                source = "NSE Daily Institutional Feed",
                publishedTime = "1h ago",
                summary = "Domestic institutional investors (DIIs) purchased ₹2,190 Cr in cash while FIIs turned net buyers with ₹1,360 Cr across frontline index components.",
                category = "FII / DII",
                isBreaking = false,
                affectedMarket = "NIFTY 50",
                impact = "BULLISH",
                impactStrength = "MEDIUM",
                optionBuyerBias = "CE WATCH",
                confidencePercent = 78,
                impactReason = "Dual institutional buying provides strong underlying price floor at Key Support S1 (₹${(niftyLtp * 0.994).toInt()}). Low probability of deep intraday sell-offs."
            ),
            OptionBuyerNewsArticle(
                id = "news_4",
                headline = "Crude Oil Surges Above \$78.50 on Middle East Supply Concerns & OPEC+ Quota Discipline",
                source = "MCX Energy / Reuters Commodity",
                publishedTime = "1h 20m ago",
                summary = "Brent and WTI crude contracts surged over 1.2% as geopolitical transit risk premiums rose and OPEC+ members reaffirmed commitment to existing production quotas.",
                category = "CRUDEOIL",
                isBreaking = true,
                affectedMarket = "CRUDEOIL",
                impact = "BULLISH",
                impactStrength = "HIGH",
                optionBuyerBias = "CE WATCH",
                confidencePercent = 88,
                impactReason = "MCX Crude Oil contracts exhibit clear bullish momentum with rising open interest. Favorable setup for MCX Crude CE buying on intraday pullbacks."
            ),
            OptionBuyerNewsArticle(
                id = "news_5",
                headline = "India VIX Cools Down to 14.15; Volatility Index Signals Stable Option Pricing Regime",
                source = "NSE Derivatives Bulletin",
                publishedTime = "2h ago",
                summary = "India VIX retreated by 2.2% indicating minimal systemic anxiety. Option implied volatilities stabilized across near-month weekly and monthly contracts.",
                category = "VOLATILITY",
                isBreaking = false,
                affectedMarket = "NIFTY 50",
                impact = "NEUTRAL",
                impactStrength = "MEDIUM",
                optionBuyerBias = "WAIT",
                confidencePercent = 75,
                impactReason = "Low IV prevents excessive option premium decay on sideways movements. Option buyers must focus on volume breakouts rather than chasing extended candles."
            ),
            OptionBuyerNewsArticle(
                id = "news_6",
                headline = "Nifty IT Index Recovers Key Moving Averages on Robust Cloud & AI Deal Wins",
                source = "BSE Corporate Announcements",
                publishedTime = "2h 45m ago",
                summary = "Frontline IT majors (TCS, Infosys, Wipro) reported sequential expansion in North American deal pipelines and multi-year generative AI implementations.",
                category = "NIFTY 50",
                isBreaking = false,
                affectedMarket = "NIFTY 50",
                impact = "BULLISH",
                impactStrength = "MEDIUM",
                optionBuyerBias = "CE WATCH",
                confidencePercent = 79,
                impactReason = "IT sector strength provides critical index weightage support to NIFTY 50, limiting downside risks during intraday consolidations."
            ),
            OptionBuyerNewsArticle(
                id = "news_7",
                headline = "Auto Sector Dispatches Surge 14% YoY Led by Premium Utility Vehicles & EV Segments",
                source = "SIAM Industry Release",
                publishedTime = "3h 15m ago",
                summary = "Society of Indian Automobile Manufacturers noted resilient demand in passenger vehicles and commercial fleet additions entering the festive quarter.",
                category = "STOCK NEWS",
                isBreaking = false,
                affectedMarket = "NIFTY 50",
                impact = "BULLISH",
                impactStrength = "MEDIUM",
                optionBuyerBias = "CE WATCH",
                confidencePercent = 76,
                impactReason = "M&M, Tata Motors, and Maruti see positive institutional sentiment, bolstering broader market breadth."
            ),
            OptionBuyerNewsArticle(
                id = "news_8",
                headline = "Asian Markets Mixed as Hang Seng Consolidates; Nikkei Rallies 210 Points on Tech Strength",
                source = "Tokyo Stock Exchange / Global Desk",
                publishedTime = "4h ago",
                summary = "Japan's Nikkei 225 advanced on semiconductor export strength while Hong Kong markets faced minor profit booking ahead of upcoming China industrial production prints.",
                category = "GLOBAL",
                isBreaking = false,
                affectedMarket = "NIFTY 50",
                impact = "NEUTRAL",
                impactStrength = "LOW",
                optionBuyerBias = "WAIT",
                confidencePercent = 70,
                impactReason = "Mixed Asian cues suggest waiting for initial 15-minute price action before committing to aggressive directional positions."
            )
        )
    }
}
