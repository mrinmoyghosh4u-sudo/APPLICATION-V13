package com.example.util

import com.example.data.network.InstrumentMasterService

object InstrumentMapUtil {
    @Volatile
    private var instrumentMasterService: InstrumentMasterService? = null

    fun setInstrumentMaster(service: InstrumentMasterService) {
        instrumentMasterService = service
    }

    fun getAngelTokenForSymbol(symbol: String, exchange: String = "NSE"): String {
        val master = instrumentMasterService
        if (master != null && master.isLoaded) {
            val resolved = master.resolveAngelToken(symbol, exchange)
            if (!resolved.isNullOrBlank()) return resolved
        }
        val upper = symbol.uppercase().trim()
        return if (upper.all { it.isDigit() }) upper else ""
    }
    
    fun getDhanTokenForSymbol(symbol: String, exchange: String = "NSE"): String {
        return getDhanSecurityId(symbol, exchange)
    }
    
    fun getAngelSymbolToken(symbol: String, exchange: String = "NSE"): String {
        return getAngelTokenForSymbol(symbol, exchange)
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

