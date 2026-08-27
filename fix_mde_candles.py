import re

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "r") as f:
    content = f.read()

# Replace getHistoricalCandles(symbol, interval) to resolve the token first!
new_hist = """
    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m", exchange: String = "NSE"): Result<List<CandleData>> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val toDate = format.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -5)
        val fromDate = format.format(cal.time)

        val master = InstrumentMasterService.instance
        val inst = master?.resolveIndexToken(symbol) ?: master?.getInstrumentByToken(master.resolveAngelToken(symbol, exchange) ?: symbol, InstrumentMasterService.getExchangeType(exchange))
        val instrumentKey = inst?.token ?: symbol
        val exactExchange = inst?.exch_seg?.let { InstrumentMasterService.normalizeExchange(it) } ?: exchange

        // Priority 1: Upstox
        if (upstoxMarketDataService?.isConfigured() == true) {
            val startUpstox = System.currentTimeMillis()
            val upstoxRes = upstoxMarketDataService!!.getHistoricalCandles(instrumentKey, interval, fromDate, toDate)
            if (upstoxRes.isSuccess && upstoxRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_UPSTOX, System.currentTimeMillis() - startUpstox)
                return upstoxRes
            }
        }

        // Priority 2: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getHistoricalCandles(instrumentKey, interval, fromDate, toDate)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis() - startFyers)
                return fyersRes
            }
        }

        // Priority 3: Angel One
        if (angelMarketDataService?.isConfigured() == true) {
            val startAngel = System.currentTimeMillis()
            val angelRes = angelMarketDataService.getHistoricalCandles(instrumentKey, interval)
            if (angelRes.isSuccess && angelRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_ANGEL_ONE, System.currentTimeMillis() - startAngel)
                return angelRes
            }
        }

        // Priority 4: m.Stock
        if (mStockMarketDataService?.isConfigured() == true) {
            val startMStock = System.currentTimeMillis()
            val mStockRes = mStockMarketDataService.getHistoricalCandles(instrumentKey, interval)
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager?.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                val mapped = mStockRes.getOrDefault(emptyList()).map {
                    CandleData(open = it.open.toFloat(), high = it.high.toFloat(), low = it.low.toFloat(), close = it.close.toFloat(), volume = it.volume.toFloat())
                }
                return Result.success(mapped)
            }
        }

        return Result.failure(Exception("REAL HISTORICAL DATA UNAVAILABLE"))
    }
"""

content = re.sub(r'suspend fun getHistoricalCandles\(symbol: String, interval: String = "15m"\): Result<List<CandleData>> \{.*?(?=suspend fun getMarketBreadth)', new_hist, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "w") as f:
    f.write(content)
