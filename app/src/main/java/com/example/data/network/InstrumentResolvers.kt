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
            token = mappedKey,
            lotSize = if (lotSize > 0) lotSize else 1
        )
    }
}

class AngelOneInstrumentResolver(private val instrumentMaster: InstrumentMasterService?) {
    fun resolve(symbol: String, exchange: String = "NSE"): CanonicalInstrument? {
        val cleanSym = symbol.trim().uppercase(Locale.ENGLISH)
        val inst = instrumentMaster?.resolveIndexToken(cleanSym)
            ?: instrumentMaster?.searchInstruments(cleanSym)?.firstOrNull()

        val token = inst?.token ?: when (cleanSym) {
            "NIFTY 50", "NIFTY50", "NIFTY" -> "99926000"
            "BANKNIFTY", "NIFTY BANK", "BANK NIFTY" -> "99926009"
            "FINNIFTY", "NIFTY FIN SERVICE", "FIN NIFTY" -> "99926037"
            "MIDCPNIFTY", "NIFTY MID SELECT", "MIDCP NIFTY" -> "99926074"
            "SENSEX", "BSESN", "BSE SENSEX" -> "99919000"
            "BANKEX", "BSE BANKEX" -> "99919012"
            else -> ""
        }

        if (token.isBlank()) return null

        val exchSeg = inst?.exch_seg?.ifBlank { exchange } ?: when {
            cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX") -> "BSE"
            cleanSym.contains("CRUDE") || cleanSym.contains("GOLD") || cleanSym.contains("SILVER") -> "MCX"
            else -> exchange
        }

        return CanonicalInstrument(
            exchange = InstrumentMasterService.normalizeExchange(exchSeg),
            segment = if (inst != null && inst.instrumenttype.isNotBlank()) inst.instrumenttype else "IDX",
            symbol = inst?.symbol?.ifBlank { cleanSym } ?: cleanSym,
            displayName = inst?.name?.ifBlank { cleanSym } ?: cleanSym,
            instrumentType = inst?.instrumenttype ?: "INDEX",
            instrumentKey = token,
            token = token,
            lotSize = inst?.lotsize?.toIntOrNull() ?: instrumentMaster?.getLotSizeForSymbol(cleanSym) ?: 1
        )
    }
}

class FyersInstrumentResolver(private val instrumentMaster: InstrumentMasterService?) {
    fun resolve(symbol: String, exchange: String = "NSE"): CanonicalInstrument? {
        val cleanSym = symbol.trim().uppercase(Locale.ENGLISH)
        val fyersSym = FyersSymbolMapper.toFyersSymbol(cleanSym, exchange)
        if (fyersSym.isBlank()) return null
        val normExch = if (cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX")) "BSE" else if (cleanSym.contains("CRUDE") || cleanSym.contains("GOLD") || cleanSym.contains("SILVER")) "MCX" else exchange
        val lotSize = instrumentMaster?.getLotSizeForSymbol(cleanSym) ?: 1
        return CanonicalInstrument(
            exchange = normExch,
            segment = if (cleanSym.contains("NIFTY") || cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX")) "IDX" else if (normExch == "MCX") "COMM" else "EQ",
            symbol = cleanSym,
            displayName = cleanSym,
            instrumentType = if (cleanSym.contains("NIFTY") || cleanSym.contains("SENSEX") || cleanSym.contains("BANKEX")) "INDEX" else "EQUITY",
            instrumentKey = fyersSym,
            token = fyersSym,
            lotSize = if (lotSize > 0) lotSize else 1
        )
    }
}

class DhanInstrumentResolver(private val instrumentMaster: InstrumentMasterService?) {
    fun resolve(symbol: String, exchange: String = "NSE"): CanonicalInstrument? {
        val cleanSym = symbol.trim().uppercase(Locale.ENGLISH)
        val normExch = InstrumentMasterService.normalizeExchange(exchange)
        val secId = com.example.util.InstrumentMapUtil.getDhanSecurityId(cleanSym, normExch)
        if (secId.isBlank()) return null
        val isOption = cleanSym.endsWith("CE") || cleanSym.endsWith("PE")
        val lotSize = instrumentMaster?.getLotSizeForSymbol(cleanSym) ?: 1
        return CanonicalInstrument(
            exchange = normExch,
            segment = when {
                isOption -> "OPT"
                normExch == "NFO" || normExch == "BFO" -> "FUT"
                normExch == "MCX" -> "COMM"
                else -> "EQ"
            },
            symbol = cleanSym,
            displayName = cleanSym,
            instrumentType = if (isOption) "OPTION" else "EQUITY",
            instrumentKey = secId,
            token = secId,
            lotSize = if (lotSize > 0) lotSize else 1
        )
    }
}
