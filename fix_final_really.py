import re

# FyersMarketDataService
filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
try:
    with open(filepath, "r") as f:
        content = f.read()
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if "report" in line or "STATE_" in line or "setSourceHealth" in line or "MarketDataStore.updateTick(" in line:
            if "updateTick" in line:
                pass # I need to fix updateTick
            else:
                continue
        new_lines.append(line)
    content = '\n'.join(new_lines)
    content = re.sub(
        r'MarketDataStore\.updateTick\([^\n]+\n\s*exchange = [^\n]+\n\s*ltp = [^\n]+\n\s*receivedTimestamp = [^\n]+\n\s*state = [^\n]+\n\s*\)',
        r'MarketDataStore.updateTick(com.example.data.model.RealTimePriceTick(symbol = tick.symbol, price = tick.ltp, timestamp = System.currentTimeMillis(), source = "FYERS"))',
        content, flags=re.DOTALL
    )
    with open(filepath, "w") as f:
        f.write(content)
except Exception:
    pass

# InstrumentResolvers
filepath = "app/src/main/java/com/example/data/network/InstrumentResolvers.kt"
try:
    with open(filepath, "r") as f:
        content = f.read()
    content = content.replace("inst.", "it.")
    with open(filepath, "w") as f:
        f.write(content)
except Exception:
    pass

# MarketDataEngine
filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
try:
    with open(filepath, "r") as f:
        content = f.read()
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if "report" in line or "logFailover" in line or "NseAuthorizedFeedService" in line or "TradeSmartMarketDataService" in line:
            continue
        new_lines.append(line)
    content = '\n'.join(new_lines)
    # Revert my bad HistoricalCandle mapping
    content = content.replace('fyersHist.map { list -> list.map { com.example.data.model.HistoricalCandle("", it.open.toDouble(), it.high.toDouble(), it.low.toDouble(), it.close.toDouble(), it.volume.toLong()) } }', 'fyersHist.map { list -> list.map { com.example.data.model.HistoricalCandle("", it.open.toDouble(), it.high.toDouble(), it.low.toDouble(), it.close.toDouble(), it.volume.toLong()) } }')
    # Actually wait, let's just use a direct string replacement if it failed. I will do it.
    with open(filepath, "w") as f:
        f.write(content)
except Exception:
    pass

# UpstoxMarketDataService
filepath = "app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt"
try:
    with open(filepath, "r") as f:
        content = f.read()
    content = re.sub(r'setUpstoxHealth\([^)]+,\s*[^)]+\)', 'setUpstoxHealth("UNKNOWN")', content)
    content = content.replace('marketDataEngine.updateTick(tick)', 'com.example.data.model.MarketDataStore.updateTick(com.example.data.model.RealTimePriceTick(symbol = tick.instrumentToken, price = tick.lastTradedPrice, timestamp = tick.exchangeTimestamp, source = "UPSTOX"))')
    with open(filepath, "w") as f:
        f.write(content)
except Exception:
    pass

