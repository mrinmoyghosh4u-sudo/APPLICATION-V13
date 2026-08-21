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

        val master = InstrumentMasterService.instance
        if (master != null && master.isLoaded) {
            val expiries = master.getOptionExpiries(cleanIndex)
            if (expiries.isNotEmpty()) {
                val activeExpiry = expiries.first()
                val optInst = master.resolveOptionInstrument(cleanIndex, activeExpiry, atmStrike.toDouble(), optionType)
                if (optInst != null) {
                    return optInst.symbol
                }
            }
        }
        
        return "$cleanIndex $atmStrike $optionType"
    }

    private fun calculateOptionLotSize(symbol: String, exchange: String, defaultLot: Int): Int {
        return com.example.util.AppPreferences.getGlobalLotSize(symbol)
    }
}
