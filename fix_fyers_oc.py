import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

replacement = """
    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val fyersAppId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val token = sessionManager.fyersAccessToken ?: throw Exception("Token missing")
            val auth = "$fyersAppId:$token"
            
            val fyersSymbol = FyersSymbolMapper.toFyersSymbol(symbol)
            val response = fyersApi.getOptionChain(auth, fyersSymbol, strikecount = 20)
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response")
            if (body.s != "ok" || body.data?.expiryData == null) throw Exception("Fyers API Error")
            
            val expiryDataList = body.data.expiryData
            if (expiryDataList.isEmpty()) throw Exception("No option chain data")
            
            val targetExpiry = if (expiry.isNotBlank()) {
                expiryDataList.find { it.expiry == expiry } ?: expiryDataList.first()
            } else {
                expiryDataList.first()
            }
            
            val chain = targetExpiry.optionChain ?: emptyList()
            val strikesMap = mutableMapOf<Double, OptionStrikeItem>()
            
            chain.forEach { contract ->
                val strike = contract.strike_price ?: return@forEach
                val item = strikesMap.getOrPut(strike) {
                    OptionStrikeItem(strikePrice = strike)
                }
                
                if (contract.option_type == "CE") {
                    item.callLtp = contract.ltp ?: 0.0
                    item.callOi = (contract.oi ?: 0.0).toString()
                    item.callVolume = (contract.volume ?: 0.0).toLong()
                    item.callBid = contract.bid ?: 0.0
                    item.callAsk = contract.ask ?: 0.0
                    item.callChangePct = contract.chp ?: 0.0
                    item.callSymbol = contract.symbol ?: ""
                } else if (contract.option_type == "PE") {
                    item.putLtp = contract.ltp ?: 0.0
                    item.putOi = (contract.oi ?: 0.0).toString()
                    item.putVolume = (contract.volume ?: 0.0).toLong()
                    item.putBid = contract.bid ?: 0.0
                    item.putAsk = contract.ask ?: 0.0
                    item.putChangePct = contract.chp ?: 0.0
                    item.putSymbol = contract.symbol ?: ""
                }
            }
            strikesMap.values.toList().sortedBy { it.strikePrice }
        }
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        runCatching {
            val fyersAppId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val token = sessionManager.fyersAccessToken ?: throw Exception("Token missing")
            val auth = "$fyersAppId:$token"
            
            val fyersSymbol = FyersSymbolMapper.toFyersSymbol(symbol)
            val response = fyersApi.getOptionChain(auth, fyersSymbol, strikecount = 2)
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val body = response.body() ?: throw Exception("Empty response")
            if (body.s != "ok" || body.data?.expiryData == null) throw Exception("Fyers API Error")
            
            body.data.expiryData.mapNotNull { it.expiry }
        }
    }
"""

start = content.find("suspend fun getOptionChain")
end = content.find("}", content.find("suspend fun getOptionExpiries", start)) + 1

content = content[:start] + replacement.strip() + "\n" + content[end:]

with open(filepath, "w") as f:
    f.write(content)

