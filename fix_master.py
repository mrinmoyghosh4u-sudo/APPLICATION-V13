content = open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt").read()

hardcoded = """
    private val hardcodedIndices = mapOf(
        "NIFTY" to Instrument("26000", "NIFTY", "NIFTY", "", "0", "1", "AMXIDX", "NSE", "0"),
        "BANKNIFTY" to Instrument("26009", "BANKNIFTY", "BANKNIFTY", "", "0", "1", "AMXIDX", "NSE", "0"),
        "FINNIFTY" to Instrument("26037", "FINNIFTY", "FINNIFTY", "", "0", "1", "AMXIDX", "NSE", "0"),
        "MIDCPNIFTY" to Instrument("26074", "MIDCPNIFTY", "MIDCPNIFTY", "", "0", "1", "AMXIDX", "NSE", "0"),
        "SENSEX" to Instrument("99926000", "SENSEX", "SENSEX", "", "0", "1", "AMXIDX", "BSE", "0"),
        "BANKEX" to Instrument("99926009", "BANKEX", "BANKEX", "", "0", "1", "AMXIDX", "BSE", "0")
    )

"""

if "hardcodedIndices" not in content:
    content = content.replace('private val indexSymbolMap = ConcurrentHashMap<String, Instrument>()',
    'private val indexSymbolMap = ConcurrentHashMap<String, Instrument>()\n' + hardcoded)

resolve_index = """
    fun resolveIndexToken(indexName: String): Instrument? {
        val nameToSymbol = mapOf(
            "NIFTY 50" to "NIFTY",
            "BANKNIFTY" to "BANKNIFTY",
            "FINNIFTY" to "FINNIFTY",
            "MIDCPNIFTY" to "MIDCPNIFTY",
            "SENSEX" to "SENSEX",
            "BANKEX" to "BANKEX",
            "CRUDEOIL" to "CRUDEOIL",
            "CRUDEOIL M" to "CRUDEOIL M"
        )
        val symbol = nameToSymbol[indexName] ?: indexName
        return indexSymbolMap[symbol] ?: hardcodedIndices[symbol]
    }
"""
import re
content = re.sub(r'fun resolveIndexToken\(.*?\).*?\}', resolve_index.strip(), content, flags=re.DOTALL)

resolve_angel = """
    fun resolveAngelToken(symbol: String, exchange: String = "NSE"): String? {
        val uppercaseSymbol = symbol.uppercase().trim()
        
        // Check index symbol map first
        val mappedName = if (uppercaseSymbol == "NIFTY 50") "NIFTY" else uppercaseSymbol
        val indexInst = indexSymbolMap[mappedName] ?: hardcodedIndices[mappedName]
        if (indexInst != null) return indexInst.token
        
        // Exact token lookup if symbol is already a numeric token
        if (uppercaseSymbol.all { it.isDigit() }) return uppercaseSymbol

        // Search by symbol or name
        val match = instrumentMap.values.find {
            (it.symbol.equals(uppercaseSymbol, ignoreCase = true) || it.name.equals(uppercaseSymbol, ignoreCase = true)) &&
            (exchange.isBlank() || it.exch_seg.equals(exchange, ignoreCase = true))
        }
        return match?.token
    }
"""
content = re.sub(r'fun resolveAngelToken\(.*?\).*?\}', resolve_angel.strip(), content, flags=re.DOTALL)

open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt", "w").write(content)
