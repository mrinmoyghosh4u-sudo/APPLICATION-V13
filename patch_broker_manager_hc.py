import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/BrokerManager.kt"

with open(filepath, "r") as f:
    content = f.read()

replacement = """
    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<CandleData>> {
        return marketDataEngine.getHistoricalCandles(symbol, interval)
    }
"""

start_idx = content.find("suspend fun getHistoricalCandles")
end_idx = content.find("suspend fun getMarketBreadth")

content = content[:start_idx] + replacement.strip() + "\n\n    " + content[end_idx:]

with open(filepath, "w") as f:
    f.write(content)
