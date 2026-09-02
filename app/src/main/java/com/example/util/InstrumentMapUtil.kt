package com.example.util

import com.example.data.network.InstrumentMasterService

object InstrumentMapUtil {
    @Volatile
    private var instrumentMasterService: InstrumentMasterService? = null

    val symbolToUnderlyingCache = java.util.concurrent.ConcurrentHashMap<String, String>()

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
    
    fun getAngelSymbolToken(symbol: String, exchange: String = "NSE"): String {
        return getAngelTokenForSymbol(symbol, exchange)
    }

    fun getDhanTokenForSymbol(symbol: String, exchange: String = "NSE"): String {
        return getDhanSecurityId(symbol, exchange)
    }

    fun getDhanSecurityId(symbol: String, exchange: String = "NSE"): String {
        val upper = symbol.uppercase().trim()
        
        // P0-2: Fix Dhan Option Order Security-ID Mapping
        // Must resolve real option contracts using InstrumentMasterService
        val master = instrumentMasterService
        if (master != null && master.isLoaded) {
            val resolved = master.resolveDhanSecurityId(symbol, exchange)
            if (!resolved.isNullOrBlank()) return resolved
            
            // If the symbol is like "NSE_FO|42634", extract the token
            if (upper.contains("|")) {
                val parts = upper.split("|")
                if (parts.size == 2) {
                    val token = parts[1]
                    if (token.all { it.isDigit() }) return token
                }
            }
        }

        return when (upper) {
            "NIFTY", "NIFTY 50", "NIFTY50" -> "13"
            "BANKNIFTY" -> "25"
            "FINNIFTY" -> "27"
            "MIDCPNIFTY" -> "31"
            "SENSEX" -> "51"
            "BANKEX" -> "17"
            "CRUDEOIL" -> "418042"
            "CRUDEOIL M" -> "418043"
            else -> if (upper.all { it.isDigit() }) upper else ""
        }
    }

    fun resolveCanonicalUnderlying(rawSymbol: String): String {
        val master = instrumentMasterService
        val upper = rawSymbol.uppercase().trim()

        val cached = symbolToUnderlyingCache[upper]
        if (cached != null) {
            return com.example.data.model.MarketUniverse.getCanonicalUnderlying(cached)
        }

        if (master != null && master.isLoaded) {
            if (upper.contains("|")) {
                val parts = upper.split("|")
                if (parts.size == 2) {
                    val exch = when (parts[0]) {
                        "NSE_FO", "NFO" -> "NFO"
                        "BSE_FO", "BFO" -> "BFO"
                        "MCX_FO", "MCX" -> "MCX"
                        "NSE_EQ", "NSE" -> "NSE"
                        "BSE_EQ", "BSE" -> "BSE"
                        else -> parts[0]
                    }
                    val token = parts[1]
                    val inst = master.getInstrument(exch, token)
                    if (inst != null) {
                        return com.example.data.model.MarketUniverse.getCanonicalUnderlying(inst.name)
                    }
                }
            }

            if (upper.all { it.isDigit() }) {
                val inst = master.getInstrument("NFO", upper) 
                           ?: master.getInstrument("NSE", upper)
                           ?: master.getInstrument("BFO", upper)
                           ?: master.getInstrument("BSE", upper)
                if (inst != null) {
                     return com.example.data.model.MarketUniverse.getCanonicalUnderlying(inst.name)
                }
            }
            
            val instBySymbol = master.getActiveContract(upper)
            if (instBySymbol != null) {
                return com.example.data.model.MarketUniverse.getCanonicalUnderlying(instBySymbol.name)
            }
        }

        // Try direct parse if no instrument matched
        return com.example.data.model.MarketUniverse.getCanonicalUnderlying(upper)
    }
}
