import os

filepath = "/app/applet/app/src/main/java/com/example/ui/screens/MarketScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# First, let's remove the broken `data class MarketMoverCardData` that was injected
content = content.replace("""data class MarketMoverCardData(
    val symbol: String,
    val exchange: String,
    val price: Double,
    val changePct: Double,
    val lotSize: Int,
    val expiry: String = "",
    val volume: Long = 0L,
    val oiChangePct: Double = 0.0
)""", "")

# Find where the broken function signature is:
# We know `val results = mutableListOf<SearchInstrumentItem>()` is the start of the body.
# Let's add the signature right before it.
sig = """
private fun searchInstruments(query: String, pool: List<SearchInstrumentItem>): List<SearchInstrumentItem> {
    val q = query.trim().uppercase()
    if (q.isBlank()) return emptyList()
    val tokens = q.split(" ").filter { it.isNotBlank() }
"""

content = content.replace("    val results = mutableListOf<SearchInstrumentItem>()", sig + "    val results = mutableListOf<SearchInstrumentItem>()")

content += """
data class MarketMoverCardData(
    val symbol: String,
    val exchange: String,
    val price: Double,
    val changePct: Double,
    val lotSize: Int,
    val expiry: String = "",
    val volume: Long = 0L,
    val oiChangePct: Double = 0.0
)
"""

with open(filepath, "w") as f:
    f.write(content)

