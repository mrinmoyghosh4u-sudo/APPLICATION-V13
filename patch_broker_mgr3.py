import os

filepath = "app/src/main/java/com/example/data/network/BrokerManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
    "val fyersMarketDataService = FyersMarketDataService(sessionManager, marketDataEngine)",
    "val fyersMarketDataService = FyersMarketDataService(sessionManager, marketDataEngine).apply { marketDataEngine.fyersMarketDataService = this }"
)

with open(filepath, "w") as f:
    f.write(content)
