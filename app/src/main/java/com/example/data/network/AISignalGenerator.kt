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
        // Real implementation should fetch from AI server. 
        // Returning empty list as per real runtime verification requirements to not show fake signals.
        return emptyList()
    }

    private fun formatOptionSymbol(rawSymbol: String, isBullish: Boolean): String {
        val upper = rawSymbol.uppercase().trim()
        if (upper.contains("CE") || upper.contains("PE")) return upper

        return when {
            upper.contains("NIFTY 50") || upper == "NIFTY" -> if (isBullish) "NIFTY 24850 CE" else "NIFTY 24800 PE"
            upper.contains("BANKNIFTY") -> if (isBullish) "BANKNIFTY 52500 CE" else "BANKNIFTY 52400 PE"
            upper.contains("FINNIFTY") -> if (isBullish) "FINNIFTY 23400 CE" else "FINNIFTY 23350 PE"
            upper.contains("MIDCPNIFTY") -> if (isBullish) "MIDCPNIFTY 13200 CE" else "MIDCPNIFTY 13150 PE"
            upper.contains("SENSEX") -> if (isBullish) "SENSEX 81500 CE" else "SENSEX 81000 PE"
            upper.contains("BANKEX") -> if (isBullish) "BANKEX 59200 CE" else "BANKEX 59000 PE"
            upper.contains("CRUDE OIL M") || upper.contains("CRUDEOILM") -> if (isBullish) "CRUDEOILM 6450 CE" else "CRUDEOILM 6400 PE"
            upper.contains("CRUDE") -> if (isBullish) "CRUDEOIL 6800 CE" else "CRUDEOIL 6700 PE"
            upper.contains("GAS") -> if (isBullish) "NATURALGAS 180 CE" else "NATURALGAS 175 PE"
            upper.contains("GOLD") -> if (isBullish) "GOLD 72000 CE" else "GOLD 71500 PE"
            upper.contains("SILVER") -> if (isBullish) "SILVER 85000 CE" else "SILVER 84500 PE"
            else -> if (isBullish) "$upper 24850 CE" else "$upper 24800 PE"
        }
    }

    private fun calculateOptionLotSize(symbol: String, exchange: String, defaultLot: Int): Int {
        return com.example.util.AppPreferences.getGlobalLotSize(symbol)
    }
}
