package com.example.data.network

import java.util.Locale

object UpstoxSymbolMapper {

    // Common Index Instrument Keys for Upstox V2 / V3
    const val KEY_NIFTY_50 = "NSE_INDEX|Nifty 50"
    const val KEY_BANK_NIFTY = "NSE_INDEX|Nifty Bank"
    const val KEY_FIN_NIFTY = "NSE_INDEX|Nifty Fin Service"
    const val KEY_MIDCP_NIFTY = "NSE_INDEX|NIFTY MID SELECT"
    const val KEY_SENSEX = "BSE_INDEX|SENSEX"
    const val KEY_BANKEX = "BSE_INDEX|BANKEX"

    // Primary Stock ISIN mapping for Upstox
    private val stockKeyMap = mapOf(
        "RELIANCE" to "NSE_EQ|INE002A01018",
        "TATASTEEL" to "NSE_EQ|INE081A01020",
        "HDFCBANK" to "NSE_EQ|INE040A01034",
        "INFY" to "NSE_EQ|INE009A01021",
        "ICICIBANK" to "NSE_EQ|INE090A01021",
        "SBIN" to "NSE_EQ|INE062A01020",
        "TCS" to "NSE_EQ|INE467B01029",
        "ITC" to "NSE_EQ|INE154A01025",
        "AXISBANK" to "NSE_EQ|INE238A01034",
        "LT" to "NSE_EQ|INE018A01030",
        "BHARTIARTL" to "NSE_EQ|INE397D01024",
        "KOTAKBANK" to "NSE_EQ|INE237A01028",
        "BAJFINANCE" to "NSE_EQ|INE296A01024",
        "MARUTI" to "NSE_EQ|INE585B01010",
        "TATAMOTORS" to "NSE_EQ|INE155A01022",
        "WIPRO" to "NSE_EQ|INE075A01022",
        "HCLTECH" to "NSE_EQ|INE860A01027",
        "ASIANPAINT" to "NSE_EQ|INE021A01026",
        "TITAN" to "NSE_EQ|INE280A01028",
        "SUNPHARMA" to "NSE_EQ|INE044A01036"
    )

    private val reverseKeyMap = mapOf(
        KEY_NIFTY_50 to "NIFTY 50",
        "NSE_INDEX|NIFTY 50" to "NIFTY 50",
        KEY_BANK_NIFTY to "BANKNIFTY",
        "NSE_INDEX|NIFTY BANK" to "BANKNIFTY",
        KEY_FIN_NIFTY to "FINNIFTY",
        "NSE_INDEX|FINNIFTY" to "FINNIFTY",
        KEY_MIDCP_NIFTY to "MIDCPNIFTY",
        "NSE_INDEX|MIDCPNIFTY" to "MIDCPNIFTY",
        KEY_SENSEX to "SENSEX",
        "BSE_INDEX|SENSEX" to "SENSEX",
        "BSE_INDEX|BSESN" to "SENSEX",
        KEY_BANKEX to "BANKEX",
        "BSE_INDEX|BANKEX" to "BANKEX"
    )

    fun toUpstoxInstrumentKey(symbol: String, exchange: String = "NSE"): String {
        val raw = symbol.trim()
        if (raw.isEmpty()) return ""

        // If already formatted as a valid Upstox instrument key (e.g. contains '|'), preserve exactly as-is
        if (raw.contains("|")) {
            return raw
        }

        val clean = raw.uppercase(Locale.ENGLISH)

        // Direct Index matching
        when {
            clean == "NIFTY" || clean == "NIFTY 50" || clean == "NIFTY50" -> return KEY_NIFTY_50
            clean == "BANKNIFTY" || clean == "NIFTY BANK" || clean == "BANK NIFTY" -> return KEY_BANK_NIFTY
            clean == "FINNIFTY" || clean == "NIFTY FIN SERVICE" || clean == "FIN NIFTY" -> return KEY_FIN_NIFTY
            clean == "MIDCPNIFTY" || clean == "NIFTY MID SELECT" || clean == "MIDCP NIFTY" -> return KEY_MIDCP_NIFTY
            clean == "SENSEX" || clean == "BSESN" || clean == "BSE SENSEX" -> return KEY_SENSEX
            clean == "BANKEX" || clean == "BSE BANKEX" -> return KEY_BANKEX
            clean == "CRUDEOIL" -> return "MCX_FO|CRUDEOIL"
            clean == "CRUDEOIL M" || clean == "CRUDEOILM" -> return "MCX_FO|CRUDEOILM"
            clean == "GOLD" -> return "MCX_FO|GOLD"
            clean == "GOLD M" || clean == "GOLDM" -> return "MCX_FO|GOLDM"
            clean == "SILVER" -> return "MCX_FO|SILVER"
            clean == "SILVER M" || clean == "SILVERM" -> return "MCX_FO|SILVERM"
            clean == "COPPER" -> return "MCX_FO|COPPER"
            clean == "COPPER M" || clean == "COPPERM" -> return "MCX_FO|COPPERM"
            clean == "NATURALGAS" || clean == "NATGAS" -> return "MCX_FO|NATURALGAS"
            clean == "NATURALGAS M" || clean == "NATURALGASM" -> return "MCX_FO|NATURALGASM"
        }

        // Check Stock ISIN map
        stockKeyMap[clean]?.let { return it }

        // Equities / Default format
        val normExch = when (exchange.trim().uppercase(Locale.ENGLISH)) {
            "BSE", "BFO" -> "BSE_EQ"
            "MCX" -> "MCX_FO"
            "NFO" -> "NSE_FO"
            else -> "NSE_EQ"
        }

        return "$normExch|$clean"
    }

    fun fromUpstoxInstrumentKey(instrumentKey: String): Pair<String, String> {
        val clean = instrumentKey.trim()

        reverseKeyMap[clean]?.let { sym ->
            val exch = if (clean.startsWith("BSE_")) "BSE" else "NSE"
            return Pair(sym, exch)
        }

        // Find in stock key map
        for ((stockSym, key) in stockKeyMap) {
            if (key.equals(clean, ignoreCase = true)) {
                return Pair(stockSym, "NSE")
            }
        }

        if (clean.contains("|")) {
            val parts = clean.split("|", limit = 2)
            val exchPart = parts[0].uppercase(Locale.ENGLISH)
            val symPart = parts[1]

            val exch = when {
                exchPart.startsWith("BSE") -> "BSE"
                exchPart.startsWith("MCX") -> "MCX"
                exchPart.startsWith("NFO") || exchPart.contains("_FO") -> "NFO"
                else -> "NSE"
            }
            return Pair(symPart, exch)
        }

        return Pair(clean, "NSE")
    }

    fun isIndexKey(key: String): Boolean {
        return key.startsWith("NSE_INDEX|") || key.startsWith("BSE_INDEX|") ||
                key.equals(KEY_NIFTY_50, ignoreCase = true) ||
                key.equals(KEY_BANK_NIFTY, ignoreCase = true) ||
                key.equals(KEY_FIN_NIFTY, ignoreCase = true) ||
                key.equals(KEY_MIDCP_NIFTY, ignoreCase = true) ||
                key.equals(KEY_SENSEX, ignoreCase = true) ||
                key.equals(KEY_BANKEX, ignoreCase = true)
    }

    /**
     * Checks if a string is a valid Upstox instrument key format.
     */
    fun isValidInstrumentKey(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        val clean = key.trim()
        if (!clean.contains("|")) return false
        val prefix = clean.substringBefore("|").uppercase(Locale.ENGLISH)
        return prefix in listOf("NSE_INDEX", "NSE_EQ", "NSE_FO", "BSE_INDEX", "BSE_EQ", "BSE_FO", "MCX_FO", "CDS_FO")
    }

    /**
     * Filters a list of instrument keys to ensure only valid, non-blank keys are included.
     */
    fun filterValidKeys(keys: List<String>): List<String> {
        return keys.mapNotNull { raw ->
            val mapped = toUpstoxInstrumentKey(raw)
            if (isValidInstrumentKey(mapped)) mapped else null
        }.distinct()
    }
}
