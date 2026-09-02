package com.example.data.model

import androidx.compose.runtime.Immutable

enum class MarketSessionType {
    PRE_MARKET,
    MARKET_OPEN,
    MARKET_CLOSED
}

@Immutable
data class GiftNiftyData(
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val prevCloseNifty: Double,
    val impliedNiftyOpen: Double,
    val gapStatus: String, // "GAP UP", "GAP DOWN", "FLAT"
    val gapPoints: Double,
    val source: String = "GIFT City / SGX Feed",
    val timestamp: String = "",
    val isLive: Boolean = true,
    val disclaimer: String = "Estimated gap is indicative based on GIFT NIFTY. Never a guaranteed opening price."
)

@Immutable
data class IndiaVixData(
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val status: String, // "LOW", "NORMAL", "HIGH", "EXTREME"
    val optionBuyerAdvice: String,
    val isLive: Boolean = true
)

@Immutable
data class GlobalCueItem(
    val symbol: String,
    val region: String, // "US", "ASIA", "COMMODITY", "FOREX"
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val sentiment: String // "BULLISH", "BEARISH", "NEUTRAL"
)

@Immutable
data class FiiDiiFlowData(
    val isDataAvailable: Boolean = false,
    val fiiBuy: Double? = null,
    val fiiSell: Double? = null,
    val fiiNet: Double? = null,
    val diiBuy: Double? = null,
    val diiSell: Double? = null,
    val diiNet: Double? = null,
    val totalNet: Double? = null,
    val institutionalBias: String = "DATA UNAVAILABLE", // "BULLISH ACCUMULATION", "BEARISH DISTRIBUTION", "NEUTRAL", "DATA UNAVAILABLE"
    val dateFormatted: String = "Awaiting official exchange report",
    val source: String = "NSE / BSE Daily Institutional Wire"
)

@Immutable
data class PreMarketIndexLevels(
    val symbol: String,
    val prevClose: Double,
    val prevCloseSource: String = "Verified Market Close",
    val giftNiftyImpliedOpen: Double,
    val expectedGap: String,
    val expectedGapPoints: Double,
    val support1: Double,
    val support2: Double,
    val resistance1: Double,
    val resistance2: Double,
    val pivot: Double,
    val bias: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val volatility: String // "LOW", "NORMAL", "HIGH", "EXTREME"
)

@Immutable
data class AiPreMarketOptionBuyerSignal(
    val symbol: String,
    val preMarketBias: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val optionBuyerBias: String, // "CE WATCH", "PE WATCH", "WAIT"
    val tradeConfirmation: String = "REQUIRED",
    val heuristicScore: Int, // e.g. 78 (Heuristic score, not statistical win probability)
    val confidence: Int = heuristicScore, // Backwards compatibility
    val reason: String,
    val disclaimer: String = "Pre-market bias is for watchlist setup only. It MUST NOT automatically create an order. Live order requires live market price + option chain OI confirmation."
)

@Immutable
data class OptionBuyerNewsArticle(
    val id: String,
    val headline: String,
    val source: String,
    val publishedTime: String,
    val publishedTimestampMs: Long = 0L,
    val summary: String,
    val category: String, // "ALL", "BREAKING", "HIGH IMPACT", "NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX", "GLOBAL", "RBI / INDIA", "CRUDEOIL", "FII / DII", "VOLATILITY", "STOCK NEWS"
    val isBreaking: Boolean = false,
    val affectedMarket: String, // "NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX", "CRUDEOIL", "GLOBAL"
    val impact: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val impactStrength: String, // "LOW", "MEDIUM", "HIGH"
    val optionBuyerBias: String, // "CE WATCH", "PE WATCH", "WAIT"
    val tradeConfirmation: String = "REQUIRED",
    val sentimentScore: Int = 70, // Heuristic Sentiment Strength (0-100), not statistical probability
    val confidencePercent: Int = sentimentScore,
    val impactReason: String,
    val url: String? = null,
    val freshness: String = "LIVE" // "LIVE", "CACHED", "STALE", "UNAVAILABLE"
)

@Immutable
data class PreMarketIntelligenceState(
    val session: MarketSessionType = MarketSessionType.MARKET_CLOSED,
    val sessionLabel: String = "MARKET CLOSED",
    val istTime: String = "",
    val nextOpeningTimeText: String = "",
    val giftNifty: GiftNiftyData? = null,
    val indiaVix: IndiaVixData? = null,
    val globalCues: List<GlobalCueItem> = emptyList(),
    val isGlobalCuesAvailable: Boolean = false,
    val fiiDii: FiiDiiFlowData? = null,
    val isFiiDiiAvailable: Boolean = false,
    val preMarketLevels: List<PreMarketIndexLevels> = emptyList(),
    val aiOptionBuyerSignals: List<AiPreMarketOptionBuyerSignal> = emptyList(),
    val newsArticles: List<OptionBuyerNewsArticle> = emptyList(),
    val breakingNews: List<OptionBuyerNewsArticle> = emptyList(),
    val newsFeedStatus: String = "UNAVAILABLE", // "HEALTHY", "DEGRADED", "UNAVAILABLE", "NETWORK_ERROR"
    val newsFreshnessStatus: String = "UNAVAILABLE", // "LIVE", "CACHED", "STALE", "UNAVAILABLE"
    val newsSource: String = "UNAVAILABLE",
    val newsLastSyncTime: Long = 0L,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastUpdatedTime: String = ""
)
