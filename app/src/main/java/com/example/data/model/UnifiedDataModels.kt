package com.example.data.model

import com.example.ui.components.CandleData

/**
 * Unified Provider-Independent Data Models
 * 
 * The UI layer consumes ONLY these unified models and never directly references
 * provider-specific ticks or schemas.
 */

data class MarketTick(
    val symbol: String,
    val exchange: String,
    val token: String = "",
    val ltp: Double,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val close: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val volume: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = true
)

data class IndexQuote(
    val symbol: String,
    val exchange: String,
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val previousClose: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = true
)

data class OptionChain(
    val symbol: String,
    val expiry: String,
    val underlyingLtp: Double,
    val strikes: List<OptionStrikeItem>,
    val timestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = true
)

data class OptionContract(
    val strikePrice: Double,
    val callOi: String = "-",
    val callChgOi: String = "-",
    val callIv: Double = 0.0,
    val callLtp: Double = 0.0,
    val callDelta: Double = 0.0,
    val callGamma: Double = 0.0,
    val callTheta: Double = 0.0,
    val callVega: Double = 0.0,
    val callVolume: String = "0",
    val putOi: String = "-",
    val putChgOi: String = "-",
    val putIv: Double = 0.0,
    val putLtp: Double = 0.0,
    val putDelta: Double = 0.0,
    val putGamma: Double = 0.0,
    val putTheta: Double = 0.0,
    val putVega: Double = 0.0,
    val putVolume: String = "0",
    val isAtm: Boolean = false
)

data class HistoricalCandle(
    val time: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val timestamp: Long = 0L
) {
    fun toCandleData(): CandleData {
        return CandleData(
            open = open.toFloat(),
            high = high.toFloat(),
            low = low.toFloat(),
            close = close.toFloat(),
            volume = volume.toFloat()
        )
    }
}

data class MarketBreadth(
    val advances: Int,
    val declines: Int,
    val unchanged: Int,
    val total: Int,
    val advanceDeclineRatio: Double = if (declines > 0) advances.toDouble() / declines.toDouble() else advances.toDouble(),
    val advancingPercent: Double = if (total > 0) (advances.toDouble() / total.toDouble()) * 100.0 else 0.0,
    val decliningPercent: Double = if (total > 0) (declines.toDouble() / total.toDouble()) * 100.0 else 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class MarketDepthItem(
    val price: Double,
    val quantity: Int,
    val orders: Int = 1
)

data class MarketDepth(
    val symbol: String,
    val exchange: String,
    val totalBuyQty: Long = 0L,
    val totalSellQty: Long = 0L,
    val buyDepth: List<MarketDepthItem> = emptyList(),
    val sellDepth: List<MarketDepthItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class InstrumentInfo(
    val token: String,
    val symbol: String,
    val name: String,
    val exchange: String,
    val expiry: String = "",
    val strike: Double = 0.0,
    val optionType: String = "", // CE / PE
    val lotSize: Int = 1,
    val tickSize: Double = 0.05,
    val instrumentType: String = "EQUITY" // EQUITY, INDEX, FUTIDX, OPTIDX, FUTSTK, OPTSTK, COMMODITY
)

data class InstrumentIdentity(
    val exchange: String, // "NSE", "BSE", "MCX", "NFO", "BFO"
    val segment: String = "INDEX", // "INDEX", "EQ", "FO", "COMM"
    val instrumentKey: String, // Canonical key e.g. "NSE_INDEX|Nifty 50", "MCX_FO|CRUDEOIL"
    val symbol: String, // "NIFTY", "SENSEX", "CRUDEOIL", "NIFTY 24500 CE"
    val displayName: String = symbol,
    val instrumentType: String = "INDEX", // "INDEX", "EQUITY", "FUTURES", "OPTIONS", "COMMODITY"
    val underlying: String = symbol,
    val expiry: String = "",
    val strike: Double = 0.0,
    val optionType: String = "", // "CE", "PE", or ""
    val lotSize: Int = 1,
    val tickSize: Double = 0.05
) {
    val isIndex: Boolean
        get() = instrumentType.equals("INDEX", ignoreCase = true) || segment.equals("INDEX", ignoreCase = true)

    val isCommodity: Boolean
        get() = exchange.equals("MCX", ignoreCase = true) || instrumentType.equals("COMMODITY", ignoreCase = true)

    val isDerivative: Boolean
        get() = instrumentType.equals("OPTIONS", ignoreCase = true) || instrumentType.equals("FUTURES", ignoreCase = true) || segment.contains("FO", ignoreCase = true)

    val isOption: Boolean
        get() = instrumentType.equals("OPTIONS", ignoreCase = true) || optionType.isNotBlank()

    val supportsOptions: Boolean
        get() {
            val upper = (if (underlying.isNotBlank()) underlying else symbol).uppercase().trim()
            return when {
                upper in listOf("NIFTY", "NIFTY 50", "BANKNIFTY", "NIFTY BANK", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX") -> true
                exchange.equals("MCX", ignoreCase = true) && (upper.contains("CRUDE") || upper.contains("NATURALGAS") || upper.contains("GOLD") || upper.contains("SILVER") || upper.contains("COPPER")) -> true
                upper in listOf("RELIANCE", "TATASTEEL", "HDFCBANK", "INFY", "ICICIBANK", "SBIN", "TCS", "ITC", "AXISBANK", "LT", "BHARTIARTL", "KOTAKBANK", "BAJFINANCE", "MARUTI", "TATAMOTORS", "WIPRO", "HCLTECH", "ASIANPAINT", "TITAN", "SUNPHARMA") -> true
                else -> isDerivative
            }
        }
}


data class MarketDataProviderState(
    val provider: String = "UNKNOWN",
    val status: String = "DISCONNECTED",
    val live: Boolean = false,
    val stale: Boolean = false,
    val error: String? = null,
    val lastUpdate: Long = 0L,
    val ping: Long = 0L
)
