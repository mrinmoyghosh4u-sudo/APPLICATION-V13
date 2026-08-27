package com.example.data.network

import com.example.data.model.CanonicalInstrument
import java.util.Locale

class UpstoxInstrumentResolver(private val instrumentMaster: InstrumentMasterService?) {
    fun resolve(symbol: String, exchange: String = "NSE"): CanonicalInstrument? {
        val cleanSym = symbol.trim().uppercase(Locale.ENGLISH)
        val normExch = if (cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX")) {
            "BSE"
        } else if (cleanSym.contains("CRUDE") || cleanSym.contains("GOLD") || cleanSym.contains("SILVER") || cleanSym.contains("NATURAL")) {
            "MCX"
        } else {
            exchange.uppercase(Locale.ENGLISH)
        }
        
        // Mapped index name for Upstox V2/V3
        val mappedKey = when (cleanSym) {
            "NIFTY 50", "NIFTY50", "NIFTY" -> "NSE_INDEX|Nifty 50"
            "BANKNIFTY", "NIFTY BANK", "BANK NIFTY" -> "NSE_INDEX|Nifty Bank"
            "FINNIFTY", "NIFTY FIN SERVICE", "FIN NIFTY" -> "NSE_INDEX|Nifty Fin Service"
            "MIDCPNIFTY", "NIFTY MID SELECT", "MIDCP NIFTY" -> "NSE_INDEX|NIFTY MID SELECT"
            "NIFTY NEXT 50" -> "NSE_INDEX|Nifty Next 50"
            "NIFTY 100" -> "NSE_INDEX|Nifty 100"
            "NIFTY 200" -> "NSE_INDEX|Nifty 200"
            "NIFTY 500" -> "NSE_INDEX|Nifty 500"
            "NIFTY IT" -> "NSE_INDEX|Nifty IT"
            "NIFTY AUTO" -> "NSE_INDEX|Nifty Auto"
            "NIFTY PHARMA" -> "NSE_INDEX|Nifty Pharma"
            "NIFTY FMCG" -> "NSE_INDEX|Nifty FMCG"
            "NIFTY METAL" -> "NSE_INDEX|Nifty Metal"
            "NIFTY REALTY" -> "NSE_INDEX|Nifty Realty"
            "NIFTY PSU BANK" -> "NSE_INDEX|Nifty PSU Bank"
            "NIFTY PRIVATE BANK" -> "NSE_INDEX|Nifty Private Bank"
            "SENSEX", "BSESN", "BSE SENSEX" -> "BSE_INDEX|SENSEX"
            "BANKEX", "BSE BANKEX" -> "BSE_INDEX|BANKEX"
            else -> UpstoxSymbolMapper.toUpstoxInstrumentKey(cleanSym, normExch)
        }

        if (mappedKey.isBlank()) return null
        
        val isIndex = mappedKey.startsWith("NSE_INDEX|") || mappedKey.startsWith("BSE_INDEX|")
        val isMcx = normExch == "MCX"
        val segment = if (isIndex) "IDX" else if (isMcx) "FUT" else "EQ"
        val instType = if (isIndex) "INDEX" else if (isMcx) "FUT" else "EQUITY"
        val lotSize = instrumentMaster?.getLotSizeForSymbol(cleanSym) ?: 1
        
        return CanonicalInstrument(
            exchange = normExch,
            segment = segment,
            symbol = cleanSym,
            displayName = cleanSym,
            instrumentType = instType,
            instrumentKey = mappedKey,
            token = "",
            lotSize = if (lotSize > 0) lotSize else 1
        )
    }
}

class FyersInstrumentResolver(private val instrumentMaster: InstrumentMasterService?) {
    fun resolve(symbol: String, exchange: String = "NSE"): CanonicalInstrument? {
        val cleanSym = symbol.trim().uppercase(Locale.ENGLISH)
        val normExch = if (cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX")) {
            "BSE"
        } else if (cleanSym.contains("CRUDE") || cleanSym.contains("GOLD") || cleanSym.contains("SILVER") || cleanSym.contains("NATURAL")) {
            "MCX"
        } else {
            exchange.uppercase(Locale.ENGLISH)
        }
        
        val fyersSym = when (cleanSym) {
            "NIFTY 50", "NIFTY" -> "NSE:NIFTY50-INDEX"
            "BANKNIFTY", "NIFTY BANK", "BANK NIFTY" -> "NSE:NIFTYBANK-INDEX"
            "FINNIFTY", "NIFTY FIN SERVICE", "FIN NIFTY" -> "NSE:FINNIFTY-INDEX"
            "MIDCPNIFTY", "NIFTY MID SELECT", "MIDCP NIFTY" -> "NSE:MIDCPNIFTY-INDEX"
            "NIFTY NEXT 50" -> "NSE:NIFTYNEXT50-INDEX"
            "NIFTY 100" -> "NSE:NIFTY100-INDEX"
            "NIFTY 200" -> "NSE:NIFTY200-INDEX"
            "NIFTY 500" -> "NSE:NIFTY500-INDEX"
            "NIFTY IT" -> "NSE:NIFTYIT-INDEX"
            "NIFTY AUTO" -> "NSE:NIFTYAUTO-INDEX"
            "NIFTY PHARMA" -> "NSE:NIFTYPHARMA-INDEX"
            "NIFTY FMCG" -> "NSE:NIFTYFMCG-INDEX"
            "NIFTY METAL" -> "NSE:NIFTYMETAL-INDEX"
            "NIFTY REALTY" -> "NSE:NIFTYREALTY-INDEX"
            "NIFTY PSU BANK" -> "NSE:NIFTYPSUBANK-INDEX"
            "NIFTY PRIVATE BANK" -> "NSE:NIFTYPVTBANK-INDEX"
            "SENSEX", "BSESN", "BSE SENSEX" -> "BSE:SENSEX-INDEX"
            "BANKEX", "BSE BANKEX" -> "BSE:BANKEX-INDEX"
            else -> FyersSymbolMapper.toFyersSymbol(cleanSym, normExch)
        }

        if (fyersSym.isBlank()) return null
        
        val isIndex = fyersSym.contains("-INDEX")
        val isMcx = normExch == "MCX"
        val segment = if (isIndex) "IDX" else if (isMcx) "FUT" else "EQ"
        val instType = if (isIndex) "INDEX" else if (isMcx) "FUT" else "EQUITY"
        val lotSize = instrumentMaster?.getLotSizeForSymbol(cleanSym) ?: 1
        
        return CanonicalInstrument(
            exchange = normExch,
            segment = segment,
            symbol = cleanSym,
            displayName = cleanSym,
            instrumentType = instType,
            instrumentKey = "",
            token = fyersSym,
            lotSize = if (lotSize > 0) lotSize else 1
        )
    }
}

class AngelOneInstrumentResolver(private val instrumentMaster: InstrumentMasterService?) {
    fun resolve(symbol: String, exchange: String = "NSE"): CanonicalInstrument? {
        val cleanSym = symbol.trim().uppercase(Locale.ENGLISH)
        val normExch = if (cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX")) {
            "BSE"
        } else if (cleanSym.contains("CRUDE") || cleanSym.contains("GOLD") || cleanSym.contains("SILVER") || cleanSym.contains("NATURAL")) {
            "MCX"
        } else {
            exchange.uppercase(Locale.ENGLISH)
        }
        
        // Manual override for common indices to ensure 100% resolution
        val mappedToken = when (cleanSym) {
            "NIFTY 50", "NIFTY50", "NIFTY" -> "99926000"
            "BANKNIFTY", "NIFTY BANK", "BANK NIFTY" -> "99926009"
            "FINNIFTY", "NIFTY FIN SERVICE", "FIN NIFTY" -> "99926037"
            "MIDCPNIFTY", "NIFTY MID SELECT", "MIDCP NIFTY" -> "99926074"
            "SENSEX", "BSESN", "BSE SENSEX" -> "99919000"
            "BANKEX", "BSE BANKEX" -> "99919012"
            "CRUDEOIL" -> "260012" // Or lookup from instrumentMaster
            else -> ""
        }
        
        val token = if (mappedToken.isNotBlank()) mappedToken else {
            instrumentMaster?.resolveAngelToken(cleanSym, normExch) ?: ""
        }
        
        if (token.isBlank()) return null
        
        val inst = instrumentMaster?.getInstrumentByToken(token, InstrumentMasterService.getExchangeType(normExch))
        val isIndex = cleanSym.contains("NIFTY") || cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX") || token in listOf("99926000", "99926009", "99926037", "99926074", "99919000", "99919012")
        val isMcx = normExch == "MCX"
        val segment = inst?.exch_seg ?: (if (isIndex) "IDX" else if (isMcx) "FUT" else "EQ")
        val instType = inst?.instrumenttype ?: (if (isIndex) "INDEX" else if (isMcx) "FUT" else "EQUITY")
        val lotSize = inst?.lotsize?.toIntOrNull() ?: instrumentMaster?.getLotSizeForSymbol(cleanSym) ?: 1
        
        return CanonicalInstrument(
            exchange = normExch,
            segment = segment,
            symbol = cleanSym,
            displayName = inst?.name ?: cleanSym,
            instrumentType = instType,
            instrumentKey = "",
            token = token,
            lotSize = if (lotSize > 0) lotSize else 1
        )
    }
}

class MStockInstrumentResolver(private val instrumentMaster: InstrumentMasterService?) {
    fun resolve(symbol: String, exchange: String = "NSE"): CanonicalInstrument? {
        val cleanSym = symbol.trim().uppercase(Locale.ENGLISH)
        val normExch = if (cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX")) {
            "BSE"
        } else if (cleanSym.contains("CRUDE") || cleanSym.contains("GOLD") || cleanSym.contains("SILVER") || cleanSym.contains("NATURAL")) {
            "MCX"
        } else {
            exchange.uppercase(Locale.ENGLISH)
        }
        
        // Match token (same logic as Angel One)
        val mappedToken = when (cleanSym) {
            "NIFTY 50", "NIFTY50", "NIFTY" -> "99926000"
            "BANKNIFTY", "NIFTY BANK", "BANK NIFTY" -> "99926009"
            "FINNIFTY", "NIFTY FIN SERVICE", "FIN NIFTY" -> "99926037"
            "MIDCPNIFTY", "NIFTY MID SELECT", "MIDCP NIFTY" -> "99926074"
            "SENSEX", "BSESN", "BSE SENSEX" -> "99919000"
            "BANKEX", "BSE BANKEX" -> "99919012"
            "CRUDEOIL" -> "260012"
            else -> ""
        }
        
        val token = if (mappedToken.isNotBlank()) mappedToken else {
            instrumentMaster?.resolveAngelToken(cleanSym, normExch) ?: ""
        }
        
        if (token.isBlank()) return null
        
        val inst = instrumentMaster?.getInstrumentByToken(token, InstrumentMasterService.getExchangeType(normExch))
        val isIndex = cleanSym.contains("NIFTY") || cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX") || token in listOf("99926000", "99926009", "99926037", "99926074", "99919000", "99919012")
        val isMcx = normExch == "MCX"
        val segment = inst?.exch_seg ?: (if (isIndex) "IDX" else if (isMcx) "FUT" else "EQ")
        val instType = inst?.instrumenttype ?: (if (isIndex) "INDEX" else if (isMcx) "FUT" else "EQUITY")
        val lotSize = inst?.lotsize?.toIntOrNull() ?: instrumentMaster?.getLotSizeForSymbol(cleanSym) ?: 1
        
        return CanonicalInstrument(
            exchange = normExch,
            segment = segment,
            symbol = cleanSym,
            displayName = inst?.name ?: cleanSym,
            instrumentType = instType,
            instrumentKey = "",
            token = token,
            lotSize = if (lotSize > 0) lotSize else 1
        )
    }
}
