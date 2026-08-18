import re

with open('app/src/main/java/com/example/data/network/InstrumentMasterService.kt', 'r') as f:
    content = f.read()

new_fun = """    fun getOptionInstruments(name: String, expiry: String): List<Instrument> {
        val uppercaseName = name.uppercase()
        val nfoName = if (uppercaseName == "NIFTY 50") "NIFTY" else uppercaseName
        
        return instrumentMap.values.filter {
            it.exch_seg == "NFO" && it.name == nfoName && it.expiry == expiry
        }
    }
    
    fun getOptionExpiries(name: String): List<String> {
        val uppercaseName = name.uppercase()
        val nfoName = if (uppercaseName == "NIFTY 50") "NIFTY" else uppercaseName
        
        val expiries = instrumentMap.values.filter {
            it.exch_seg == "NFO" && it.name == nfoName
        }.map { it.expiry }.distinct().sortedBy {
            // Need a quick way to sort date strings if possible, or just string sort which might be flawed
            // Let's rely on standard format or return them to be sorted outside
            it
        }
        return expiries
    }
"""

content = content.replace("fun resolveIndexToken(indexName: String): Instrument? {", new_fun + "\n    fun resolveIndexToken(indexName: String): Instrument? {")

with open('app/src/main/java/com/example/data/network/InstrumentMasterService.kt', 'w') as f:
    f.write(content)
