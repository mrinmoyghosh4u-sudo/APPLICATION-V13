import os

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

# getMarketQuotes
fyers_quotes = """        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getMarketQuotes(symbols)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                val valid = fyersRes.getOrDefault(emptyList()).filter { it.ltp > 0.0 }
                if (valid.isNotEmpty()) {
                    healthManager.reportSuccessfulRequest("FYERS", System.currentTimeMillis() - startFyers)
                    _unifiedFeedStatus.value = "LIVE — FYERS"
                    _internalActiveProvider.value = "FYERS"
                    updateLastTickTime()
                    return Result.success(valid)
                }
            }
            healthManager.reportError("FYERS")
            healthManager.logFailover("FYERS", ProviderHealthManager.PROVIDER_ANGEL_ONE)
        }
        
"""
if "Priority 1: Fyers" not in content:
    content = content.replace("        // Priority 1: Angel One", fyers_quotes + "        // Priority 2: Angel One")


# getHistoricalCandleData (or getHistoricalCandles)
fyers_history = """        // Priority 1: Fyers
        if (fyersMarketDataService?.isConfigured() == true) {
            val startFyers = System.currentTimeMillis()
            val fyersRes = fyersMarketDataService!!.getHistoricalCandles(symbol, interval)
            if (fyersRes.isSuccess && fyersRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager.reportSuccessfulRequest("FYERS", System.currentTimeMillis() - startFyers)
                return fyersRes
            }
            healthManager.reportError("FYERS")
            healthManager.logFailover("FYERS", ProviderHealthManager.PROVIDER_MSTOCK)
        }
        
"""
if "Priority 1: Fyers" not in content:
    content = content.replace("        // Priority 1: m.Stock", fyers_history + "        // Priority 2: m.Stock")


with open(filepath, "w") as f:
    f.write(content)
