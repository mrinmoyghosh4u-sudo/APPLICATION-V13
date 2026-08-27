import re

with open("app/src/main/java/com/example/ui/screens/MarketScreen.kt", "r") as f:
    content = f.read()

# Remove generateSearchInstrumentPool
content = re.sub(r'private fun generateSearchInstrumentPool\(\): List<SearchInstrumentItem> \{.*?\}\n\}', '', content, flags=re.DOTALL)
# also it might end with `items.add(SearchInstrumentItem("COPPER", "MCX", "FUTURES", "30 SEP", 2500))`
content = re.sub(r'private fun generateSearchInstrumentPool\(\).*?^}', '', content, flags=re.DOTALL|re.MULTILINE)

# We will remove `searchInstruments` entirely
content = re.sub(r'private fun searchInstruments\(.*?^}', '', content, flags=re.DOTALL|re.MULTILINE)

# Replace the remember block that generates the pool
content = re.sub(r'val searchInstrumentPool = remember \{\s*generateSearchInstrumentPool\(\)\s*\}', '', content, flags=re.DOTALL)

# Replace the searchResults block
new_search = """
    val searchResults = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            emptyList<SearchInstrumentItem>()
        } else {
            val master = com.example.data.network.InstrumentMasterService.instance
            if (master != null) {
                master.searchInstruments(q).map { inst ->
                    SearchInstrumentItem(
                        symbol = inst.symbol,
                        exchange = com.example.data.network.InstrumentMasterService.normalizeExchange(inst.exch_seg),
                        category = inst.instrumenttype,
                        expiry = inst.expiry,
                        lotSize = inst.lotsize.toIntOrNull() ?: 1,
                        token = inst.token
                    )
                }
            } else {
                emptyList<SearchInstrumentItem>()
            }
        }
    }
"""
content = re.sub(r'val searchResults = remember\(searchQuery, searchInstrumentPool\) \{.*?\}', new_search, content, flags=re.DOTALL)

# Wait, `SearchInstrumentItem` needs to have `token: String = ""`
content = content.replace("val lotSize: Int", "val lotSize: Int,\n    val token: String = \"\"")

with open("app/src/main/java/com/example/ui/screens/MarketScreen.kt", "w") as f:
    f.write(content)
