import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/MarketDataEngine.kt"

with open(filepath, "r") as f:
    content = f.read()

replacement = """
    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<CandleData>> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val toDate = format.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -5)
        val fromDate = format.format(cal.time)

        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getHistoricalCandles(symbol, interval, fromDate, toDate)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis() - startFyers)
                return fyersRes
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_FYERS)
        }

        // Priority 2: Angel One
        val startAngel = System.currentTimeMillis()
        val angelRes = angelMarketDataService.getHistoricalCandles(symbol, interval)
        if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
            healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
            return angelRes
        }
        healthManager.reportError(ProviderHealthManager.PROVIDER_ANGEL_ONE)

        // Priority 3: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getHistoricalCandles(symbol, interval)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                return mStockRes
            }
            healthManager.reportError(ProviderHealthManager.PROVIDER_MSTOCK)
        }

        return Result.failure(Exception("REAL HISTORICAL DATA UNAVAILABLE"))
    }
"""

start_idx = content.find("suspend fun getHistoricalCandles")
end_idx = content.find("suspend fun getMarketBreadth")

content = content[:start_idx] + replacement.strip() + "\n\n    " + content[end_idx:]

with open(filepath, "w") as f:
    f.write(content)
