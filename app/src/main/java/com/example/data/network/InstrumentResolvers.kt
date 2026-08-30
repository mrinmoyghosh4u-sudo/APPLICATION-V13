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
            displayName = inst?.name ?: cleanSym,
            instrumentType = instType,
            instrumentKey = "",
            id = id,
            lotSize = if (lotSize > 0) lotSize else 1
        )
    }
}
