package com.example.util

object InstrumentMapUtil {
    fun getAngelTokenForSymbol(symbol: String, exchange: String = "NSE"): String {
        return getAngelSymbolToken(symbol, exchange)
    }
    
    fun getDhanTokenForSymbol(symbol: String, exchange: String = "NSE"): String {
        return getDhanSecurityId(symbol, exchange)
    }
    
    fun getAngelSymbolToken(symbol: String, exchange: String = "NSE"): String {
        val upper = symbol.uppercase().trim()
        return when (upper) {
            "NIFTY", "NIFTY 50", "NIFTY50" -> "99926000"
            "BANKNIFTY" -> "99926009"
            "FINNIFTY" -> "99926037"
            "MIDCPNIFTY" -> "99926014"
            else -> if (upper.all { it.isDigit() }) upper else ""
        }
    }
    
    fun getDhanSecurityId(symbol: String, exchange: String = "NSE"): String {
        val upper = symbol.uppercase().trim()
        return when (upper) {
            "NIFTY", "NIFTY 50", "NIFTY50" -> "13"
            "BANKNIFTY" -> "25"
            "FINNIFTY" -> "27"
            "MIDCPNIFTY" -> "31"
            else -> if (upper.all { it.isDigit() }) upper else ""
        }
    }
}
