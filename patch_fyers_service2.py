import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
    "class FyersMarketDataService(\n    private val sessionManager: SessionManager,\n    private val marketDataEngine: MarketDataEngine\n)",
    "class FyersMarketDataService(\n    private val sessionManager: SessionManager,\n    private val marketDataEngine: MarketDataEngine,\n    private val fyersApi: FyersApi\n)"
)

impl_v2 = """
    private fun getFyersSymbol(symbol: String): String {
        return if (symbol.contains(":")) symbol else "NSE:$symbol-EQ" // Simplistic mapping
    }

    override suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val fyersAppId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val token = sessionManager.fyersAccessToken ?: throw Exception("Token missing")
            val auth = "$fyersAppId:$token"
            
            val fyersSymbols = symbols.map { getFyersSymbol(it) }.joinToString(",")
            val response = fyersApi.getQuotes(auth, fyersSymbols)
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response")
            if (body.s != "ok" || body.d == null) throw Exception("Fyers API Error")
            
            body.d.map { quote ->
                WatchlistItem(
                    symbol = quote.v?.original_name ?: quote.n ?: "",
                    exchange = quote.v?.exchange ?: "NSE",
                    ltp = quote.v?.lp ?: 0.0,
                    change = quote.v?.ch ?: 0.0,
                    changePercent = quote.v?.chp ?: 0.0
                )
            }
        }
    }

    override suspend fun getHistoricalCandles(symbol: String, interval: String): Result<List<CandleData>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val fyersAppId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val token = sessionManager.fyersAccessToken ?: throw Exception("Token missing")
            val auth = "$fyersAppId:$token"
            
            val fyersSymbol = getFyersSymbol(symbol)
            val res = interval.replace("m", "")
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val toDate = java.util.Date()
            val fromDate = java.util.Date(toDate.time - (10 * 24 * 60 * 60 * 1000L)) // 10 days
            
            val response = fyersApi.getHistory(
                auth = auth,
                symbol = fyersSymbol,
                resolution = res,
                dateFormat = 1,
                from = sdf.format(fromDate),
                to = sdf.format(toDate)
            )
            
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response")
            if (body.s != "ok" || body.candles == null) throw Exception("Fyers API Error")
            
            body.candles.map { c ->
                CandleData(
                    open = c[1].toFloat(),
                    high = c[2].toFloat(),
                    low = c[3].toFloat(),
                    close = c[4].toFloat(),
                    volume = c[5].toFloat()
                )
            }
        }
    }
    
    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> = Result.failure(Exception("Not implemented"))
    override suspend fun getOptionExpiries(symbol: String): Result<List<String>> = Result.failure(Exception("Not implemented"))
"""

import re
content = re.sub(r'    override suspend fun getMarketQuotes.*?Result\.failure\(Exception\("Not implemented"\)\)', impl_v2, content, flags=re.DOTALL)

with open(filepath, "w") as f:
    f.write(content)
