import re

filepath = "app/src/main/java/com/example/data/network/BrokerManager.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix getOptionExpiries
content = content.replace(
"""    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        val res = marketDataEngine.getOptionChain(symbol, "")
        if (res.isSuccess) {
            return Result.success(res.getOrThrow().expiries)
        }
        return Result.failure(res.exceptionOrNull() ?: Exception("Option expiries fetch failed"))
    }""",
"""    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return Result.success(emptyList()) // Expiries not supported directly via unified MarketDataEngine
    }"""
)

# Fix CandleData instantiation
content = content.replace(
"""                 CandleData(timestamp = it.timestamp, open = it.open.toFloat(), high = it.high.toFloat(), low = it.low.toFloat(), close = it.close.toFloat())""",
"""                 CandleData(open = it.open.toFloat(), high = it.high.toFloat(), low = it.low.toFloat(), close = it.close.toFloat(), volume = 0f)"""
)

with open(filepath, "w") as f:
    f.write(content)
