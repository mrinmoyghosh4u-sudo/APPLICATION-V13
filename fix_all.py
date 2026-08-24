import os
import re

# 1. Fix MarketDataStore.kt
filepath = "/app/applet/app/src/main/java/com/example/data/model/MarketDataStore.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("_yahooHealth", "_unusedHealth")
with open(filepath, "w") as f:
    f.write(content)

# 2. Fix BrokerManager.kt
filepath = "/app/applet/app/src/main/java/com/example/data/network/BrokerManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("nseFeedService = null, // removed\n", "")
content = content.replace("MarketTick(symbol = item.symbol, ltp = item.ltp, timestamp = System.currentTimeMillis())", 
                          'MarketTick(symbol = item.symbol, ltp = item.ltp, timestamp = System.currentTimeMillis(), exchange = item.exchange)')
with open(filepath, "w") as f:
    f.write(content)

# 3. Fix MarketDataEngine.kt
filepath = "/app/applet/app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("val fyersRes = fyersMarketDataService!!.getOptionChain(symbol, expiry)", 'val fyersRes = fyersMarketDataService!!.getOptionChain(symbol, expiry ?: "")')
content = content.replace("val angelRes = angelMarketDataService.getOptionChain(symbol, expiry)", 'val angelRes = angelMarketDataService.getOptionChain(symbol, expiry ?: "")')
content = content.replace("val mStockRes = mStockMarketDataService.getOptionChain(symbol, expiry)", 'val mStockRes = mStockMarketDataService.getOptionChain(symbol, expiry ?: "")')

# Fix mStockMarketDataService getHistoricalCandles
# Wait, Angel returns Result<List<CandleData>>, Fyers returns Result<List<CandleData>>. If mStock returns HistoricalCandle, we need to map it.
content = content.replace(
"""
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                return mStockRes
            }
""",
"""
            if (mStockRes.isSuccess && mStockRes.getOrDefault(emptyList()).isNotEmpty()) {
                healthManager.reportSuccessfulRequest(ProviderHealthManager.PROVIDER_MSTOCK, System.currentTimeMillis() - startMStock)
                val mapped = mStockRes.getOrDefault(emptyList()).map {
                    CandleData(timestamp = it.timestamp, open = it.open.toFloat(), high = it.high.toFloat(), low = it.low.toFloat(), close = it.close.toFloat())
                }
                return Result.success(mapped)
            }
""")

# Fix mStockMarketDataService.getOptionExpiries
content = content.replace("return mStockMarketDataService.getOptionExpiries(symbol)", "return Result.failure(Exception(\"Expiries unavailable\"))")

with open(filepath, "w") as f:
    f.write(content)

# 4. Fix DiagnosticsScreen.kt
filepath = "/app/applet/app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = re.sub(r'DiagnosticItem\("NSE Data Role".*?\n', '', content)
content = re.sub(r'DiagnosticItem\("NSE Health".*?\n', '', content)
content = re.sub(r'DiagnosticItem\("Yahoo Data Role".*?\n', '', content)
content = re.sub(r'DiagnosticItem\("Yahoo Health".*?\n', '', content)

with open(filepath, "w") as f:
    f.write(content)
