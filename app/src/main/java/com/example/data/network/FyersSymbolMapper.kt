package com.example.data.network

object FyersSymbolMapper {
    fun toFyersSymbol(symbol: String, exchange: String = "NSE"): String {
        return when (symbol.uppercase()) {
            "NIFTY 50", "NIFTY" -> "NSE:NIFTY50-INDEX"
            "BANKNIFTY" -> "NSE:NIFTYBANK-INDEX"
            "FINNIFTY" -> "NSE:FINNIFTY-INDEX"
            "MIDCPNIFTY" -> "NSE:MIDCPNIFTY-INDEX"
            "SENSEX" -> "BSE:SENSEX-INDEX"
            "BANKEX" -> "BSE:BANKEX-INDEX"
            else -> {
                // Check if it's an option (contains CE or PE and space)
                if (symbol.contains(" CE") || symbol.contains(" PE")) {
                    // Option contract, must be exact from instrument master, just format properly if needed
                    // Usually options are formatted as: NSE:NIFTY23OCT19500CE
                    // For now, return as is assuming it was fetched correctly, or assume format
                    // Prompt: "Options must use the exact instrument symbol from the instrument master. Never construct fake option symbols manually if an exact contract mapping is unavailable."
                    return symbol 
                }
                // Equity
                if (exchange.equals("NSE", ignoreCase = true)) {
                    "NSE:$symbol-EQ"
                } else if (exchange.equals("BSE", ignoreCase = true)) {
                    "BSE:$symbol-EQ"
                } else if (exchange.equals("MCX", ignoreCase = true)) {
                    if (symbol.startsWith("MCX:")) symbol else "MCX:$symbol"
                } else {
                    symbol
                }
            }
        }
    }

    fun fromFyersSymbol(fyersSymbol: String): String {
        var clean = fyersSymbol.substringAfter(":")
        clean = clean.replace("-INDEX", "")
        clean = clean.replace("-EQ", "")
        return when (clean) {
            "NIFTY50" -> "NIFTY 50"
            "NIFTYBANK" -> "BANKNIFTY"
            else -> clean
        }
    }
}
