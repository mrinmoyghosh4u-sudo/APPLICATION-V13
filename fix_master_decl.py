content = open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt").read()

hardcoded = """
    private val hardcodedIndices = mapOf(
        "NIFTY" to Instrument("99926000", "NIFTY", "NIFTY", "", "0", "1", "AMXIDX", "NSE", "0"),
        "BANKNIFTY" to Instrument("99926009", "BANKNIFTY", "BANKNIFTY", "", "0", "1", "AMXIDX", "NSE", "0"),
        "FINNIFTY" to Instrument("99926037", "FINNIFTY", "FINNIFTY", "", "0", "1", "AMXIDX", "NSE", "0"),
        "MIDCPNIFTY" to Instrument("99926074", "MIDCPNIFTY", "MIDCPNIFTY", "", "0", "1", "AMXIDX", "NSE", "0"),
        "SENSEX" to Instrument("99919000", "SENSEX", "SENSEX", "", "0", "1", "AMXIDX", "BSE", "0"),
        "BANKEX" to Instrument("99919012", "BANKEX", "BANKEX", "", "0", "1", "AMXIDX", "BSE", "0")
    )
"""

content = content.replace('private val indexSymbolMap = mutableMapOf<String, Instrument>()',
'private val indexSymbolMap = mutableMapOf<String, Instrument>()\n' + hardcoded)

open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt", "w").write(content)
