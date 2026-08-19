import re

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

pattern_quotes = r"override suspend fun getMarketQuotes.*?    }"
replacement_quotes = """override suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return runCatching {
            val tokenMap = mutableMapOf<String, MutableList<String>>()
            symbols.forEach { sym ->
                val token = instrumentMaster.resolveAngelToken(sym, "NSE") ?: ""
                if (token.isNotBlank()) {
                    val ex = when {
                        sym.contains("CRUDE", ignoreCase = true) -> "MCX"
                        sym.contains("SENSEX", ignoreCase = true) || sym.contains("BANKEX", ignoreCase = true) -> "BSE"
                        else -> "NSE"
                    }
                    tokenMap.getOrPut(ex) { mutableListOf() }.add(token)
                }
            }

            val response = api.getQuotes(AngelQuoteRequest(exchangeTokens = tokenMap))
            if (response.isSuccessful && response.body()?.status == true) {
                val quotes = response.body()?.data?.fetched ?: emptyList()
                val unfetched = response.body()?.data?.unfetched ?: emptyList()
                unfetched.forEach { u -> android.util.Log.w("AngelOneBrokerService", "Unfetched quote: exchange=${u.exchange} token=${u.symbolToken} reason=${u.message}") }
                
                quotes.map { q ->
                    val sym = q.tradingSymbol.toStringOrDefault("")
                    val ex = q.exchange.toStringOrDefault("NSE")
                    val ltp = q.ltp.toDoubleOrDefault(0.0)
                    val change = q.netChange.toDoubleOrDefault(0.0)
                    val pct = q.percentChange.toDoubleOrDefault(0.0)
                    WatchlistItem(
                        symbol = sym,
                        exchange = ex,
                        ltp = ltp,
                        change = change,
                        changePercent = pct,
                        lotSize = com.example.util.AppPreferences.getGlobalLotSize(sym),
                        isPositive = change >= 0
                    )
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.body()?.message ?: "Unknown API Error"
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }"""

content = re.sub(pattern_quotes, replacement_quotes, content, flags=re.DOTALL)
open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)
