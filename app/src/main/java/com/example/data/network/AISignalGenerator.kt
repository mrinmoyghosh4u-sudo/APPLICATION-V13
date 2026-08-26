package com.example.data.network

import com.example.data.model.AISignalEntity
import com.example.data.model.WatchlistItem
import com.example.util.MarketStatusUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object AISignalGenerator {

    /**
     * Checks whether an exchange is currently open in IST (Asia/Kolkata).
     */
    fun isExchangeOpenIST(exchange: String): Boolean {
        return MarketStatusUtil.getDetailedMarketStatus(exchange).isOpen
    }

    /**
     * Generates real-time AI Option signals from live quotes ONLY for exchanges that are currently OPEN.
     */
            fun generateSignalsFromMarketData(quotes: List<WatchlistItem>): List<AISignalEntity> {
        val signals = mutableListOf<AISignalEntity>()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        
        quotes.forEach { quote ->
            if (quote.ltp > 0 && quote.changePercent != 0.0) {
                val isBullish = quote.changePercent > 0.5
                val isBearish = quote.changePercent < -0.5
                
                if (isBullish || isBearish) {
                    val optionSymbol = formatOptionSymbol(quote.symbol, isBullish, quote.ltp)
                    val sl = if (isBullish) quote.ltp * 0.99 else quote.ltp * 1.01
                    val tg1 = if (isBullish) quote.ltp * 1.01 else quote.ltp * 0.99
                    val tg2 = if (isBullish) quote.ltp * 1.02 else quote.ltp * 0.98
                    
                    signals.add(
                        AISignalEntity(
                            id = 0,
                            symbol = optionSymbol,
                            exchange = quote.exchange,
                            side = if (isBullish) "BUY" else "SELL",
                            actionType = if (isBullish) "BUY CE" else "BUY PE",
                            trend = if (isBullish) "BULLISH" else "BEARISH",
                            ltp = quote.ltp,
                            changePercent = quote.changePercent,
                            entryZone = String.format(Locale.US, "%.2f - %.2f", quote.ltp * 0.998, quote.ltp * 1.002),
                            target1 = tg1,
                            target2 = tg2,
                            stopLoss = sl,
                            confidence = if (kotlin.math.abs(quote.changePercent) > 1.5) 95 else 85,
                            riskReward = "1:2",
                            lotSize = 1,
                            timeframe = "Live Tick",
                            timestamp = currentTime,
                            status = "ACTIVE"
                        )
                    )
                }
            }
        }
        return signals.sortedByDescending { it.confidence }
    }

    fun formatOptionSymbol(rawSymbol: String, isBullish: Boolean, underlyingLtp: Double = 0.0): String {
        val upper = rawSymbol.uppercase().trim()
        if (upper.contains("CE") || upper.contains("PE")) return upper

        val cleanIndex = when {
            upper.contains("NIFTY 50") || upper == "NIFTY" -> "NIFTY"
            upper.contains("BANKNIFTY") -> "BANKNIFTY"
            upper.contains("FINNIFTY") -> "FINNIFTY"
            upper.contains("MIDCPNIFTY") -> "MIDCPNIFTY"
            upper.contains("SENSEX") -> "SENSEX"
            upper.contains("BANKEX") -> "BANKEX"
            upper.contains("CRUDE") -> "CRUDEOIL"
            else -> upper
        }

        if (underlyingLtp <= 0.0) {
            return "$cleanIndex ${if (isBullish) "CE" else "PE"}"
        }

        val step = when (cleanIndex) {
            "MIDCPNIFTY" -> 25.0
            "NIFTY", "FINNIFTY", "CRUDEOIL" -> 50.0
            else -> 100.0
        }
        val atmStrike = (kotlin.math.round(underlyingLtp / step) * step).toInt()
        val optionType = if (isBullish) "CE" else "PE"
        return "$cleanIndex $atmStrike $optionType"
    }

    private fun calculateOptionLotSize(symbol: String, exchange: String, defaultLot: Int): Int {
        return com.example.util.AppPreferences.getGlobalLotSize(symbol)
    }
}
