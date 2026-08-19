import re

content = open("app/src/main/java/com/example/data/network/IBrokerService.kt").read()
if "getHistoricalCandles" not in content:
    content = content.replace("suspend fun getOptionExpiries(symbol: String): Result<List<String>>",
"""suspend fun getOptionExpiries(symbol: String): Result<List<String>>
    suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<com.example.ui.components.CandleData>>""")
open("app/src/main/java/com/example/data/network/IBrokerService.kt", "w").write(content)

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()
if "override suspend fun getHistoricalCandles" not in content:
    func = """
    override suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<com.example.ui.components.CandleData>> {
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
                fromdate = fromdate,
                todate = todate
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
                            volume = row[5].toString().toLong()
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
"""
    content = content.replace("}\n", "}\n" + func)
open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)

content = open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt").read()
# fix getHistoricalCandles
content = re.sub(
    r'suspend fun getHistoricalCandles\(symbol: String, interval: String = "15m"\): Result<List<com\.example\.ui\.components\.CandleData>> \{(.*?)\}',
    r'''suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<com.example.ui.components.CandleData>> {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val toDate = format.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -5)
        val fromDate = format.format(cal.time)
        
        val angelInterval = when (interval) {
            "1m" -> "ONE_MINUTE"
            "5m" -> "FIVE_MINUTE"
            "15m" -> "FIFTEEN_MINUTE"
            "30m" -> "THIRTY_MINUTE"
            "1h" -> "ONE_HOUR"
            "1d" -> "ONE_DAY"
            else -> "FIFTEEN_MINUTE"
        }
        
        return angelOneService.getHistoricalCandles(symbol, angelInterval, fromDate, toDate)
    }''',
    content,
    flags=re.DOTALL
)
open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt", "w").write(content)

