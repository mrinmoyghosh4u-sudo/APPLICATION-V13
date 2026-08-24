import os

filepath = "/app/applet/app/src/main/java/com/example/data/model/MarketDataStore.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add FYERS and ensure exact names
replacement = """object MarketDataSourceNames {
    const val FYERS = "Fyers"
    const val ANGEL_ONE = "AngelOne"
    const val MSTOCK = "mStock"
}"""
# Wait, let's find the current MarketDataSourceNames
start = content.find("object MarketDataSourceNames {")
end = content.find("}", start) + 1
content = content[:start] + replacement + content[end:]

# Add FYERS health fields
content = content.replace("private val _angelOneHealth = MutableStateFlow(\"DISCONNECTED\")",
                          "private val _fyersHealth = MutableStateFlow(\"DISCONNECTED\")\n    val fyersHealth = _fyersHealth.asStateFlow()\n\n    private val _angelOneHealth = MutableStateFlow(\"DISCONNECTED\")")

# In checkHealth, track FYERS
content = content.replace("val lastAngel = sourceLastUpdate[MarketDataSourceNames.ANGEL_ONE] ?: 0L",
                          "val lastFyers = sourceLastUpdate[MarketDataSourceNames.FYERS] ?: 0L\n                val lastAngel = sourceLastUpdate[MarketDataSourceNames.ANGEL_ONE] ?: 0L")

content = content.replace("if (now - lastAngel > 30000 && _angelOneHealth.value == \"LIVE\") {",
                          "if (now - lastFyers > 30000 && _fyersHealth.value == \"LIVE\") {\n                    _fyersHealth.value = \"STALE\"\n                }\n                if (now - lastAngel > 30000 && _angelOneHealth.value == \"LIVE\") {")

# In setSourceHealth
content = content.replace("MarketDataSourceNames.ANGEL_ONE -> _angelOneHealth.value = health",
                          "MarketDataSourceNames.FYERS -> _fyersHealth.value = health\n            MarketDataSourceNames.ANGEL_ONE -> _angelOneHealth.value = health")

# In updateTick logic (ensure we prioritize correctly)
# The prompt says: "Ensure FYERS primary data cannot be overwritten incorrectly by lower-priority providers."
update_tick = """
        // 1. Never overwrite FYERS data with lower priority data if FYERS is LIVE.
        // Wait, in our engine, BrokerManager will handle switching feeds. But in Store:
        if (existing != null) {
            val isExistingFyersLive = existing.source == MarketDataSourceNames.FYERS && _fyersHealth.value == "LIVE"
            if (isExistingFyersLive && tick.source != MarketDataSourceNames.FYERS) {
                // Ignore lower priority tick if FYERS is active
                return
            }
        }
"""
content = content.replace("val existing = _marketData.value[tick.symbol]",
                          "val existing = _marketData.value[tick.symbol]\n" + update_tick)

# Remove old references in setSourceHealth/checkHealth
content = content.replace("MarketDataSourceNames.TRADESMART -> _tradeSmartHealth.value = health\n", "")
content = content.replace("MarketDataSourceNames.NSE -> _nseHealth.value = health\n", "")
content = content.replace("val lastTradeSmart = sourceLastUpdate[MarketDataSourceNames.TRADESMART] ?: 0L\n", "")
content = content.replace("val lastNse = sourceLastUpdate[MarketDataSourceNames.NSE] ?: 0L\n", "")

# In the health check
content = content.replace("if (now - lastTradeSmart > 30000 && _tradeSmartHealth.value == \"LIVE\") {\n                    _tradeSmartHealth.value = \"STALE\"\n                }\n", "")
content = content.replace("if (now - lastNse > 30000 && _nseHealth.value == \"LIVE\") {\n                    _nseHealth.value = \"STALE\"\n                }\n", "")

# Also remove TRADESMART and NSE fields from MarketDataStore
content = content.replace("private val _tradeSmartHealth = MutableStateFlow(\"DISCONNECTED\")\n    val tradeSmartHealth = _tradeSmartHealth.asStateFlow()\n", "")
content = content.replace("private val _nseHealth = MutableStateFlow(\"DISCONNECTED\")\n    val nseHealth = _nseHealth.asStateFlow()\n", "")

# Also fix setSourceHealth update
content = content.replace("MarketDataSourceNames.TRADESMART -> _tradeSmartHealth.value = \"LIVE\"\n", "")
content = content.replace("MarketDataSourceNames.NSE -> _nseHealth.value = \"LIVE\"\n", "")

content = content.replace("MarketDataSourceNames.ANGEL_ONE -> _angelOneHealth.value = \"LIVE\"",
                          "MarketDataSourceNames.FYERS -> _fyersHealth.value = \"LIVE\"\n            MarketDataSourceNames.ANGEL_ONE -> _angelOneHealth.value = \"LIVE\"")


# Also the check about existing source for debug log:
content = content.replace("if (existing != null && (existing.source == MarketDataSourceNames.ANGEL_ONE || existing.source == MarketDataSourceNames.MSTOCK || existing.source == MarketDataSourceNames.TRADESMART || existing.source == MarketDataSourceNames.NSE)) {",
                          "if (existing != null && (existing.source == MarketDataSourceNames.ANGEL_ONE || existing.source == MarketDataSourceNames.MSTOCK || existing.source == MarketDataSourceNames.FYERS)) {")


with open(filepath, "w") as f:
    f.write(content)

