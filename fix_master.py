import re

with open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt", "r") as f:
    content = f.read()

search_fn = """
    fun searchInstruments(query: String): List<Instrument> {
        val q = query.trim().uppercase()
        if (q.isBlank()) return emptyList()
        val tokens = q.split(" ").filter { it.isNotBlank() }
        
        return instrumentMap.values.filter { inst ->
            val sym = inst.symbol.uppercase()
            val name = inst.name.uppercase()
            tokens.all { t -> sym.contains(t) || name.contains(t) }
        }.take(30)
    }
"""

if "fun searchInstruments" not in content:
    content = content.replace("fun getInstrumentByToken", search_fn + "\n    fun getInstrumentByToken")

with open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt", "w") as f:
    f.write(content)
