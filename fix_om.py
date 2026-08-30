import re

filepath = "app/src/main/java/com/example/data/network/OrderManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
"""            if (storeTick != null && (storeTick.state == "STALE" || storeTick.state == "OFFLINE" || false)) {
                return@withContext Result.failure(Exception("Stale Data Protection: Cannot place order using stale or unverified market data."))
            }""",
"""            if (com.example.data.model.MarketDataStore.providerState.value.stale) {
                return@withContext Result.failure(Exception("Stale Data Protection: Cannot place order using stale or unverified market data."))
            }"""
)

with open(filepath, "w") as f:
    f.write(content)
