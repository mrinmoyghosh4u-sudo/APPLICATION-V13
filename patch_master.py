import re

with open('/app/applet/app/src/main/java/com/example/data/model/OptionChainInstrument.kt', 'r') as f:
    content = f.read()

replacement = """package com.example.data.model

data class OptionChainInstrument(
    val symbol: String,
    val exchange: String,
    val underlyingScrip: Int,
    val underlyingSeg: String,
    val angelToken: String
)

object OptionChainInstrumentMaster {
    val supportedInstruments = listOf(
        OptionChainInstrument("NIFTY 50", "NSE", 13, "IDX_I", "99926000"),
        OptionChainInstrument("BANKNIFTY", "NSE", 25, "IDX_I", "99926009"),
        OptionChainInstrument("FINNIFTY", "NSE", 27, "IDX_I", "99926037"),
        OptionChainInstrument("MIDCAP NIFTY", "NSE", 26, "IDX_I", "99926074"),
        OptionChainInstrument("SENSEX", "BSE", 51, "IDX_I", "99919000"),
        OptionChainInstrument("BANKEX", "BSE", 52, "IDX_I", "99919012"),
        OptionChainInstrument("CRUDEOIL", "MCX", 118, "MCX_O", "235472"),
        OptionChainInstrument("CRUDEOIL M", "MCX", 119, "MCX_O", "235473")
    )
    
    fun getInstrument(symbol: String): OptionChainInstrument? {
        return supportedInstruments.find { it.symbol.equals(symbol, ignoreCase = true) }
    }
}
"""

with open('/app/applet/app/src/main/java/com/example/data/model/OptionChainInstrument.kt', 'w') as f:
    f.write(replacement)
print("OptionChainInstrument Patched!")
