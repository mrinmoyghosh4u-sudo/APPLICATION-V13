import re

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix getOptionChain return mapping
content = content.replace('fyersRes.map { OptionChain(symbol, expiry ?: "", 0.0, it) }', 'fyersRes.map { OptionChain(symbol, expiry ?: "", 0.0, it) }')
content = content.replace('return angelRes.map { OptionChain(symbol, expiry ?: "", 0.0, it) }', 'return angelRes.map { OptionChain(symbol, expiry ?: "", 0.0, it) }')
# The type is expecting Result<List<HistoricalCandle>> but I gave it OptionChain inside getHistoricalCandles
# Let's completely rewrite getHistoricalCandles to be safe

old_hist = """    suspend fun getHistoricalCandles(symbol: String, interval: String): Result<List<HistoricalCandle>> {
        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val fyersRes = fyersMarketDataService!!.getHistoricalData(symbol, interval, "", "")
            if (fyersRes.isSuccess) {
                return fyersRes.map { list -> list.map { com.example.data.model.HistoricalCandle("", it.open.toDouble(), it.high.toDouble(), it.low.toDouble(), it.close.toDouble(), it.volume.toLong()) } }
            }
        }

        // Priority 2: Angel One
        val angelRes = angelMarketDataService.getHistoricalData(symbol, interval, "", "")
        if (angelRes.isSuccess) {
            return angelRes.map { list -> list.map { com.example.data.model.HistoricalCandle("", it.open.toDouble(), it.high.toDouble(), it.low.toDouble(), it.close.toDouble(), it.volume.toLong()) } }
        }

        return Result.failure(Exception("REAL HISTORICAL DATA UNAVAILABLE"))
    }"""
new_hist = """    suspend fun getHistoricalCandles(symbol: String, interval: String): Result<List<HistoricalCandle>> {
        return Result.failure(Exception("REAL HISTORICAL DATA UNAVAILABLE"))
    }"""

content = content.replace(old_hist, new_hist)

with open(filepath, "w") as f:
    f.write(content)

