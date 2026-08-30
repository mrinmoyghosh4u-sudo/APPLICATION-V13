import re

filepath = "app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt"
try:
    with open(filepath, "r") as f:
        content = f.read()

    # Replacements
    content = re.sub(r'MarketDataStore\.report\w+\(.*?\)', '', content)
    content = content.replace("MarketDataStore.setSourceHealth(\"Upstox\", \"DISCONNECTED\")", "MarketDataStore.setUpstoxHealth(\"DISCONNECTED\")")
    content = content.replace("MarketDataStore.setSourceHealth(\"Upstox\", \"ERROR\")", "MarketDataStore.setUpstoxHealth(\"ERROR\")")
    content = content.replace("MarketDataStore.setSourceHealth(\"Upstox\", \"LIVE\")", "MarketDataStore.setUpstoxHealth(\"LIVE\")")
    content = content.replace("MarketDataStore.updateUpstoxTick", "MarketDataStore.updateTick")
    content = content.replace("MarketDataStore.updateTick(", "MarketDataStore.updateTick(")

    with open(filepath, "w") as f:
        f.write(content)
except Exception as e:
    print(e)
