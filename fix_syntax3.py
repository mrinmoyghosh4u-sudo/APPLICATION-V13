import os

filepath = "/app/applet/app/src/main/java/com/example/ui/screens/MarketScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Remove the one I added if it's duplicated
content = content.replace("""
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
""", "")

# Add SearchInstrumentItem
item_class = """
data class SearchInstrumentItem(
    val symbol: String,
    val exchange: String,
    val category: String,
    val expiry: String,
    val lotSize: Int
)
"""

content = content.replace("private fun searchInstruments", item_class + "\nprivate fun searchInstruments")

with open(filepath, "w") as f:
    f.write(content)

