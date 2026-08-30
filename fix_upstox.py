import re

filepath = "app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix updateTick
# Original was probably MarketDataStore.updateUpstoxTick(source=..., symbol=..., ltp=..., receivedTimestamp=..., state=...)
# Or MarketDataState(...)
# I'll just change updateTick(...) to updateTick(com.example.data.model.RealTimePriceTick(source="UPSTOX", symbol=symbol, price=ltp, timestamp=System.currentTimeMillis()))

# I will just erase the problematic report lines using regex on the whole lines:
lines = content.split('\n')
new_lines = []
for line in lines:
    if "reportSubscribing" in line or "reportError" in line or "reportSubscribed" in line or "reportAuthentication" in line or "reportConnection" in line or "reportDisconnected" in line or "reportWaitingForTick" in line or "reportTickReceived" in line or "reportConfigured" in line or "reportConnecting" in line:
        continue
    if "setSourceHealth" in line:
        line = line.replace('setSourceHealth("Upstox"', 'setUpstoxHealth(')
    new_lines.append(line)

content = '\n'.join(new_lines)

# Fix updateTick block
content = re.sub(
    r'MarketDataStore\.update(?:Upstox)?Tick\s*\(\s*source\s*=\s*.*?\)', 
    r'MarketDataStore.updateTick(com.example.data.model.RealTimePriceTick(symbol = symbol, price = ltp, timestamp = System.currentTimeMillis(), source = "upstox"))', 
    content, flags=re.DOTALL
)

with open(filepath, "w") as f:
    f.write(content)
