with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "r") as f:
    content = f.read()

old_code = """    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        val res = angelMarketDataService.getOptionExpiries(symbol)
        if (res.isSuccess) return res
        
        if (mStockMarketDataService.isConfigured()) {
            return Result.failure(Exception("Expiries unavailable"))
        }
        return Result.failure(Exception("Expiries unavailable"))
    }"""

new_code = """    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val fyersRes = fyersMarketDataService!!.getOptionExpiries(symbol)
            if (fyersRes.isSuccess) return fyersRes
        }
        
        // Priority 2: Angel One
        val angelRes = angelMarketDataService.getOptionExpiries(symbol)
        if (angelRes.isSuccess) return angelRes
        
        // Priority 3: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val mStockRes = mStockMarketDataService.getOptionExpiries(symbol)
            if (mStockRes.isSuccess) return mStockRes
        }
        
        return Result.failure(Exception("REAL EXPIRIES UNAVAILABLE"))
    }"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "w") as f:
    f.write(content)
