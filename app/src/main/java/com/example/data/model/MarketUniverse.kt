package com.example.data.model

/**
 * Strict 8-Instrument Universe for KING KHAN AI TRADER
 *
 * ONLY these 8 underlying instruments are supported across the entire app:
 * 
 * NSE:
 * 1. NIFTY (NIFTY 50)
 * 2. BANKNIFTY
 * 3. FINNIFTY
 * 4. MIDCPNIFTY
 *
 * BSE:
 * 5. SENSEX
 * 6. BANKEX
 *
 * MCX:
 * 7. CRUDEOIL
 * 8. CRUDEOIL-M (CRUDEOIL M / CRUDEOILM)
 *
 * Everything else (stock options, non-approved indices, non-approved commodities) is rejected.
 */
object MarketUniverse {
    const val NIFTY = "NIFTY 50"
    const val BANKNIFTY = "BANKNIFTY"
    const val FINNIFTY = "FINNIFTY"
    const val MIDCPNIFTY = "MIDCPNIFTY"

    const val SENSEX = "SENSEX"
    const val BANKEX = "BANKEX"

    const val CRUDEOIL = "CRUDEOIL"
    const val CRUDEOIL_M = "CRUDEOIL M"

    val APPROVED_UNDERLYINGS = listOf(
        NIFTY,
        BANKNIFTY,
        FINNIFTY,
        MIDCPNIFTY,
        SENSEX,
        BANKEX,
        CRUDEOIL,
        CRUDEOIL_M
    )

    val NSE_UNDERLYINGS = listOf(NIFTY, BANKNIFTY, FINNIFTY, MIDCPNIFTY)
    val BSE_UNDERLYINGS = listOf(SENSEX, BANKEX)
    val MCX_UNDERLYINGS = listOf(CRUDEOIL, CRUDEOIL_M)

    fun isApprovedUnderlying(symbol: String): Boolean {
        val clean = cleanSymbol(symbol)
        return when {
            clean.contains("BANKNIFTY") || clean.contains("BANK NIFTY") -> true
            clean.contains("FINNIFTY") || clean.contains("FIN NIFTY") || clean.contains("NIFTY FIN") -> true
            clean.contains("MIDCPNIFTY") || clean.contains("MIDCAP") || clean.contains("MID SELECT") -> true
            clean.contains("NIFTY 50") || clean == "NIFTY" -> true
            clean.contains("BANKEX") -> true
            clean.contains("SENSEX") -> true
            clean.contains("CRUDEOIL M") || clean.contains("CRUDEOIL-M") || clean.contains("CRUDEOILM") || clean.contains("CRUDE OIL M") -> true
            clean.contains("CRUDEOIL") || clean.contains("CRUDE OIL") -> true
            else -> false
        }
    }

    fun isApprovedOrderSymbol(symbol: String): Boolean {
        return isApprovedUnderlying(symbol)
    }

    fun getCanonicalUnderlying(symbol: String): String {
        val clean = cleanSymbol(symbol)
        return when {
            clean.contains("BANKNIFTY") || clean.contains("BANK NIFTY") -> BANKNIFTY
            clean.contains("FINNIFTY") || clean.contains("FIN NIFTY") || clean.contains("NIFTY FIN") -> FINNIFTY
            clean.contains("MIDCPNIFTY") || clean.contains("MIDCAP") || clean.contains("MID SELECT") -> MIDCPNIFTY
            clean.contains("NIFTY") -> NIFTY
            clean.contains("BANKEX") -> BANKEX
            clean.contains("SENSEX") -> SENSEX
            clean.contains("CRUDEOIL M") || clean.contains("CRUDEOIL-M") || clean.contains("CRUDEOILM") || clean.contains("CRUDE OIL M") -> CRUDEOIL_M
            clean.contains("CRUDEOIL") || clean.contains("CRUDE OIL") -> CRUDEOIL
            else -> symbol.trim().uppercase()
        }
    }

    fun getExchangeForUnderlying(symbol: String): String {
        val canonical = getCanonicalUnderlying(symbol)
        return when (canonical) {
            SENSEX, BANKEX -> "BSE"
            CRUDEOIL, CRUDEOIL_M -> "MCX"
            else -> "NSE"
        }
    }

    fun getLotSize(symbol: String): Int {
        val canonical = getCanonicalUnderlying(symbol)
        return when (canonical) {
            NIFTY -> 65
            BANKNIFTY -> 30
            FINNIFTY -> 60
            MIDCPNIFTY -> 120
            SENSEX -> 20
            BANKEX -> 30
            CRUDEOIL -> 100
            CRUDEOIL_M -> 10
            else -> 1
        }
    }

    fun getReferenceClosingPrice(symbol: String): Double {
        val canonical = getCanonicalUnderlying(symbol)
        return when (canonical) {
            NIFTY -> 24850.0
            BANKNIFTY -> 51200.0
            FINNIFTY -> 23450.0
            MIDCPNIFTY -> 13100.0
            SENSEX -> 81400.0
            BANKEX -> 58200.0
            CRUDEOIL -> 6150.0
            CRUDEOIL_M -> 6150.0
            else -> 100.0
        }
    }

    private fun cleanSymbol(symbol: String): String {
        return symbol.trim().uppercase()
    }
}
