import re

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

new_block = """
                    open = 0.0,
                    high = 0.0,
                    low = 0.0,
                    previousClose = (item.ltp - item.change),
"""

content = re.sub(
    r'open = state\?\.open \?: 0\.0,\s*high = state\?\.high \?: 0\.0,\s*low = state\?\.low \?: 0\.0,\s*previousClose = state\?\.previousClose \?: \(item\.ltp - item\.change\),',
    new_block.strip() + ',',
    content
)

# And fix the fyersMarketDataService getHistoricalCandles call
content = content.replace("fyersMarketDataService!!.getHistoricalCandles(symbol, interval)", "fyersMarketDataService!!.getHistoricalCandles(symbol, interval, \"\", \"\")")

# And Angel
content = content.replace("angelMarketDataService.getOptionChain(symbol, expiry)", "angelMarketDataService.getOptionChain(symbol, expiry ?: \"\")")
content = content.replace("fyersMarketDataService!!.getOptionChain(symbol, expiry)", "fyersMarketDataService!!.getOptionChain(symbol, expiry ?: \"\")")

with open(filepath, "w") as f:
    f.write(content)
