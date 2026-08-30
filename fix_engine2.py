import re

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix getHistoricalCandles
old_get_hist = """    suspend fun getHistoricalCandles(symbol: String, interval: String): Result<List<HistoricalCandle>> {
        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getHistoricalCandles(symbol, interval)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                return fyersRes.map { OptionChain(symbol, expiry ?: "", 0.0, it) }
            }
        }

        // Priority 2: Angel One
        val startAngel = System.currentTimeMillis()
        val angelRes = angelMarketDataService.getHistoricalCandles(symbol, interval)
        if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
            return angelRes.map { OptionChain(symbol, expiry ?: "", 0.0, it) }
        }

        // Priority 3: m.Stock
            
        return Result.failure(Exception("REAL HISTORICAL DATA UNAVAILABLE"))
    }"""
new_get_hist = """    suspend fun getHistoricalCandles(symbol: String, interval: String): Result<List<HistoricalCandle>> {
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
content = content.replace(old_get_hist, new_get_hist)

with open(filepath, "w") as f:
    f.write(content)
