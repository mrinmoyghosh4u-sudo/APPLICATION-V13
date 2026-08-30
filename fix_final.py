import re

# 1. MarketDataEngine.kt
filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix updateTick block
content = re.sub(
    r'MarketDataStore\.updateTick\(\s*source = "FYERS"[\s\S]*?state = "LIVE"\s*\)',
    r'MarketDataStore.updateTick(com.example.data.model.RealTimePriceTick(symbol = tick.symbol, price = tick.ltp, timestamp = System.currentTimeMillis(), source = "FYERS"))',
    content
)
# Fix logFailover and report*
content = re.sub(r'MarketDataStore\.report\w+\(.*?\)', '', content)
content = re.sub(r'MarketDataStore\.logFailover\(.*?\)', '', content)
# Fix previousClose etc from getTick(symbol)
content = content.replace("current?.open", "0.0")
content = content.replace("current?.high", "0.0")
content = content.replace("current?.low", "0.0")
content = content.replace("current?.previousClose", "0.0")
content = content.replace("current?.volume", "0L")
with open(filepath, "w") as f:
    f.write(content)

# 2. UpstoxMarketDataService.kt
filepath = "app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()
content = content.replace('setUpstoxHealth("Upstox", ', 'setUpstoxHealth(')
content = content.replace('MarketDataStore.updateTick(source=', 'MarketDataStore.updateTick(com.example.data.model.RealTimePriceTick(source=')
content = content.replace('updateTick(', 'updateTick(') # Hmm, wait, I already replaced updateUpstoxTick to updateTick.
# If there's an updateTick leftover, I will manually patch it. Let's see if updateUpstoxTick still existed on line 513
content = content.replace('updateUpstoxTick', 'updateTick')
with open(filepath, "w") as f:
    f.write(content)

