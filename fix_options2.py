import re
content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

# Just split the file and truncate it, then append the correct methods
idx = content.find("override suspend fun getOptionExpiries")
content = content[:idx]

content += """override suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return runCatching {
            val apiResponse = api.getOptionExpiries(symbol)
            val liveExpiries = if (apiResponse.isSuccessful) {
                apiResponse.body()?.data ?: emptyList()
            } else {
                emptyList()
            }
            com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol, liveExpiries)
        }
    }

    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> {
        return runCatching {
            val instrument = instrumentMaster.resolveIndexToken(symbol)
            if (instrument == null) throw Exception("Option Chain unavailable for this instrument.")
            
            val exchangeForOptions = when (instrument.exch_seg) {
                "MCX" -> "MCX"
                "BSE" -> "BFO"
                else -> "NFO"
            }
            
            val request = AngelOptionChainRequest(
                exchange = exchangeForOptions,
                symboltoken = instrument.token,
                expirydate = expiry
            )
            val response = api.getOptionChain(request)
            if (response.isSuccessful && response.body()?.status == true) {
                response.body()?.data?.map { item ->
                    OptionStrikeItem(
                        strikePrice = item.strikePrice?.toDoubleOrNull() ?: 0.0,
                        callOi = "${item.callOi ?: 0.0}",
                        callChgOi = "${item.callChgOi ?: 0.0}",
                        callIv = 0.0,
                        callLtp = item.callLtp ?: 0.0,
                        callDelta = 0.0,
                        putDelta = 0.0,
                        putLtp = item.putLtp ?: 0.0,
                        putIv = 0.0,
                        putChgOi = "${item.putChgOi ?: 0.0}",
                        putOi = "${item.putOi ?: 0.0}",
                        callVolume = "${item.callVolume ?: 0.0}",
                        putVolume = "${item.putVolume ?: 0.0}"
                    )
                } ?: emptyList()
            } else {
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }
}
"""

open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)
