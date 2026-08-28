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
    val fiiBuy: Double,
    val fiiSell: Double,
    val fiiNet: Double,
    val diiBuy: Double,
    val diiSell: Double,
    val diiNet: Double,
    val totalNet: Double,
    val institutionalBias: String, // "BULLISH ACCUMULATION", "BEARISH DISTRIBUTION", "NEUTRAL"
    val dateFormatted: String,
    val source: String = "NSE / BSE Daily Institutional Wire"
)

@Immutable
data class PreMarketIndexLevels(
    val symbol: String,
    val prevClose: Double,
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
    val confidence: Int, // e.g. 78
    val reason: String,
    val disclaimer: String = "Pre-market bias is for watchlist setup only. It MUST NOT automatically create an order. Live order requires live market price + option chain OI confirmation."
)

@Immutable
data class OptionBuyerNewsArticle(
    val id: String,
    val headline: String,
    val source: String,
    val publishedTime: String,
    val summary: String,
    val category: String, // "ALL", "BREAKING", "HIGH IMPACT", "NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX", "GLOBAL", "RBI / INDIA", "CRUDEOIL", "FII / DII", "VOLATILITY", "STOCK NEWS"
    val isBreaking: Boolean = false,
    val affectedMarket: String, // "NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX", "CRUDEOIL", "GLOBAL"
    val impact: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val impactStrength: String, // "LOW", "MEDIUM", "HIGH"
    val optionBuyerBias: String, // "CE WATCH", "PE WATCH", "WAIT"
    val confidencePercent: Int,
    val impactReason: String,
    val url: String? = null
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
    val fiiDii: FiiDiiFlowData? = null,
    val preMarketLevels: List<PreMarketIndexLevels> = emptyList(),
    val aiOptionBuyerSignals: List<AiPreMarketOptionBuyerSignal> = emptyList(),
    val newsArticles: List<OptionBuyerNewsArticle> = emptyList(),
    val breakingNews: List<OptionBuyerNewsArticle> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastUpdatedTime: String = ""
)
