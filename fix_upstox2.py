import re

filepath = "app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

content = re.sub(
    r'com\.example\.data\.model\.MarketDataStore\.updateTick\([^\n]+\n\s*exchange = [^\n]+\n\s*ltp = [^\n]+\n\s*receivedTimestamp = [^\n]+\n\s*state = [^\n]+\n\s*\)',
    r'com.example.data.model.MarketDataStore.updateTick(com.example.data.model.RealTimePriceTick(symbol = symbol, price = q.ltp, timestamp = now, source = "upstox"))',
    content, flags=re.DOTALL
)

with open(filepath, "w") as f:
    f.write(content)
