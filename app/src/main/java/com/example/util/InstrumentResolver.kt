package com.example.util

import com.example.data.model.ResolvedInstrument
import com.example.data.model.MarketUniverse
import com.example.data.network.InstrumentMasterService

object InstrumentResolver {

    fun resolve(
        rawSymbol: String,
        master: InstrumentMasterService?
    ): ResolvedInstrument? {
        val upper = rawSymbol.uppercase().trim()
        
        var exch = "NSE"
        var token = ""
        
        if (upper.contains("|")) {
            val parts = upper.split("|")
            if (parts.size == 2) {
                exch = InstrumentMasterService.normalizeExchange(parts[0])
                token = parts[1]
            }
        } else if (upper.all { it.isDigit() }) {
            token = upper
            exch = "NFO" // default guess for pure token
        }
        
        if (master != null && master.isLoaded && token.isNotBlank()) {
            val inst = master.getInstrument(exch, token)
                ?: master.getInstrument("NFO", token)
                ?: master.getInstrument("NSE", token)
                ?: master.getInstrument("BFO", token)
                ?: master.getInstrument("MCX", token)
                
            if (inst != null) {
                val underlying = MarketUniverse.getCanonicalUnderlying(inst.name)
                val isOpt = inst.symbol.endsWith("CE") || inst.symbol.endsWith("PE")
                val optType = if (inst.symbol.endsWith("CE")) "CE" else if (inst.symbol.endsWith("PE")) "PE" else ""
                
                return ResolvedInstrument(
                    brokerToken = token,
                    exchange = inst.exch_seg,
                    segment = inst.exch_seg,
                    securityId = token, // Dhan generally uses exchange token as securityId for options
                    tradingSymbol = inst.symbol,
                    underlying = underlying,
                    instrumentType = inst.instrumenttype,
                    expiry = inst.expiry,
                    strike = inst.strike.toDoubleOrNull() ?: 0.0,
                    optionType = optType,
                    lotSize = inst.lotsize.toIntOrNull() ?: MarketUniverse.getLotSize(underlying)
                )
            }
        }
        
        // Fallback for basic underlying matching if not found
        val underlying = MarketUniverse.getCanonicalUnderlying(upper)
        if (MarketUniverse.isApprovedUnderlying(underlying)) {
            val ex = MarketUniverse.getExchangeForUnderlying(underlying)
            val secId = InstrumentMapUtil.getDhanSecurityId(underlying, ex)
            return ResolvedInstrument(
                brokerToken = secId,
                exchange = ex,
                segment = ex,
                securityId = secId,
                tradingSymbol = upper,
                underlying = underlying,
                instrumentType = "",
                expiry = "",
                strike = 0.0,
                optionType = "",
                lotSize = MarketUniverse.getLotSize(underlying)
            )
        }
        
        return null
    }
}
