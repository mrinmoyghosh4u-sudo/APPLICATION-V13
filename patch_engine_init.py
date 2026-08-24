import os

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

if "fyersMarketDataService: FyersMarketDataService?" not in content:
    content = content.replace(
        "class MarketDataEngine(",
        "class MarketDataEngine(\n    var fyersMarketDataService: FyersMarketDataService? = null,"
    )
    with open(filepath, "w") as f:
        f.write(content)
    print("Added fyersMarketDataService to MarketDataEngine")
