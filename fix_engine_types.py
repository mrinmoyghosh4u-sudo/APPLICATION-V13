import re

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix getOptionChain return mapping for Fyers and Angel
# fyersRes is used like `return fyersRes` -> `return fyersRes.map { it.let { OptionChain(symbol, expiry ?: "", 0.0, it) } }`
content = content.replace("return fyersRes", "return fyersRes.map { OptionChain(symbol, expiry ?: \"\", 0.0, it) }")
content = content.replace("return angelRes", "return angelRes.map { OptionChain(symbol, expiry ?: \"\", 0.0, it) }")

# Fix getHistoricalData missing params
content = content.replace("fyersMarketDataService!!.getHistoricalData(symbol, interval)", "fyersMarketDataService!!.getHistoricalData(symbol, interval, \"\", \"\")")
content = content.replace("angelMarketDataService.getHistoricalData(symbol, interval)", "angelMarketDataService.getHistoricalData(symbol, interval, \"\", \"\")")

# Fix getHistoricalData return type
content = content.replace(
    "return fyersHist",
    "return fyersHist.map { list -> list.map { com.example.data.model.HistoricalCandle(\"\", it.open.toDouble(), it.high.toDouble(), it.low.toDouble(), it.close.toDouble(), it.volume.toLong()) } }"
)
content = content.replace(
    "return angelHist",
    "return angelHist.map { list -> list.map { com.example.data.model.HistoricalCandle(\"\", it.open.toDouble(), it.high.toDouble(), it.low.toDouble(), it.close.toDouble(), it.volume.toLong()) } }"
)

with open(filepath, "w") as f:
    f.write(content)
