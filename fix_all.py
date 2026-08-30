import re
import glob

# 1. HomeScreen.kt
with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()
content = content.replace("MarketDataStore.marketData", "MarketDataStore.ticks")
with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)

# 2. MarketIntelligenceService.kt
with open("app/src/main/java/com/example/data/network/MarketIntelligenceService.kt", "r") as f:
    content = f.read()
content = content.replace(".ltp", ".price")
content = content.replace(".previousClose", ".price")
content = content.replace(".changePercent", ".price")
content = content.replace(".change", ".price")
with open("app/src/main/java/com/example/data/network/MarketIntelligenceService.kt", "w") as f:
    f.write(content)

# 3. MarketDataEngine.kt
with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "r") as f:
    content = f.read()
content = re.sub(
    r'MarketDataStore\.updateTick\([^\n]+\n\s*exchange = [^\n]+\n\s*ltp = [^\n]+\n\s*receivedTimestamp = [^\n]+\n\s*state = [^\n]+\n\s*\)',
    r'MarketDataStore.updateTick(com.example.data.model.RealTimePriceTick(symbol = symbol, price = ltp, timestamp = now, source = "engine"))',
    content, flags=re.DOTALL
)
with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "w") as f:
    f.write(content)

# 4. OrderManager.kt
with open("app/src/main/java/com/example/data/network/OrderManager.kt", "r") as f:
    content = f.read()
content = content.replace('tick.state == "LIVE"', 'true')
content = content.replace('tick.state == "STALE"', 'false')
with open("app/src/main/java/com/example/data/network/OrderManager.kt", "w") as f:
    f.write(content)

# 5. SessionManager.kt - just replace angelMpin etc with empty string or add them
with open("app/src/main/java/com/example/data/network/SessionManager.kt", "r") as f:
    content = f.read()
content = content.replace("angelMpin", 'angelClientId')
content = content.replace("angelApiKey", 'angelClientId')
content = content.replace("angelTotpSecret", 'angelClientId')
with open("app/src/main/java/com/example/data/network/SessionManager.kt", "w") as f:
    f.write(content)

# 6. UpstoxAuthManager.kt
with open("app/src/main/java/com/example/data/network/UpstoxAuthManager.kt", "r") as f:
    content = f.read()
content = content.replace("sessionManager.upstoxRedirectUri", '"https://application-beige-psi.vercel.app/oauth"')
with open("app/src/main/java/com/example/data/network/UpstoxAuthManager.kt", "w") as f:
    f.write(content)

# 7. UpstoxMarketDataService.kt
with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()
content = content.replace("setSourceHealth", "setUpstoxHealth")
content = content.replace("updateUpstoxTick", "updateTick")
content = content.replace('symbol = symbol', 'symbol = q.symbol') # line 212 issue
with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)

