import re

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

# The class ends around line 315 in the original file, before the duplicate appends.
# Let's find the first getHistoricalCandles
matches = list(re.finditer(r'override suspend fun getHistoricalCandles', content))
if len(matches) > 1:
    first_match = matches[0]
    content = content[:first_match.start()]
    # add one instance of the function and close the class
    func = """    override suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<com.example.ui.components.CandleData>> {
        return runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated")
            val token = instrumentMaster.resolveAngelToken(symbol, "NSE") ?: throw Exception("Token not found")
            val ex = when {
                symbol.contains("CRUDE", ignoreCase = true) -> "MCX"
                symbol.contains("SENSEX", ignoreCase = true) || symbol.contains("BANKEX", ignoreCase = true) -> "BSE"
                else -> "NSE"
            }
            
            val request = AngelHistoricalRequest(
                exchange = ex,
                symboltoken = token,
                interval = interval,
                fromdate = fromDate,
                todate = toDate
            )
            val response = api.getHistoricalData(request)
            if (response.isSuccessful && response.body()?.status == true) {
                val data = response.body()?.data ?: emptyList()
                data.mapNotNull { row ->
                    try {
                        com.example.ui.components.CandleData(
                            timestamp = row[0].toString(),
                            open = row[1].toString().toDouble(),
                            high = row[2].toString().toDouble(),
                            low = row[3].toString().toDouble(),
                            close = row[4].toString().toDouble(),
                            volume = row[5].toString().toDouble().toLong()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e("AngelOneBrokerService", "Historical API Error: $errorMsg")
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }
}
"""
    content += func
    
open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)

