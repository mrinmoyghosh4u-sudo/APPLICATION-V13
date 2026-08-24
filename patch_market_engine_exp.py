import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/MarketDataEngine.kt"

with open(filepath, "r") as f:
    content = f.read()

func = """
    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        val res = angelMarketDataService.getOptionExpiries(symbol)
        if (res.isSuccess) return res
        
        if (mStockMarketDataService.isConfigured()) {
            return mStockMarketDataService.getOptionExpiries(symbol)
        }
        return Result.failure(Exception("Expiries unavailable"))
    }
"""

# Insert before getHistoricalCandles
content = content.replace("suspend fun getHistoricalCandles", func + "\n    suspend fun getHistoricalCandles")

with open(filepath, "w") as f:
    f.write(content)
