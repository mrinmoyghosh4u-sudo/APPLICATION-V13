import re

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'r') as f:
    content = f.read()

merge_fun = """    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        val httpResult = angelOneService.getOptionChain(symbol, expiry)
        
        if (instrumentMaster.isLoaded) {
            val options = instrumentMaster.getOptionInstruments(symbol, expiry)
            if (options.isNotEmpty()) {
                val tokensToSubscribe = options.map { it.token }
                subscribeToTokens(2, tokensToSubscribe)
                
                if (httpResult.isSuccess) {
                    val strikes = httpResult.getOrDefault(emptyList()).map { item ->
                        val strikeFormatted = String.format("%.6f", item.strikePrice)
                        val ceOpt = options.find { it.strike == strikeFormatted && it.symbol.endsWith("CE") }
                        val peOpt = options.find { it.strike == strikeFormatted && it.symbol.endsWith("PE") }
                        
                        // the identifier in MarketDataStore is 'symbol' from instrument master
                        val ceLive = if (ceOpt != null) MarketDataStore.marketData.value[ceOpt.symbol] else null
                        val peLive = if (peOpt != null) MarketDataStore.marketData.value[peOpt.symbol] else null
                        
                        item.copy(
                            callLtp = ceLive?.ltp ?: item.callLtp,
                            putLtp = peLive?.ltp ?: item.putLtp
                        )
                    }
                    return Result.success(strikes)
                }
            }
        }
        return httpResult
    }"""

content = re.sub(r'suspend fun getOptionChain.*?\n\s+return angelOneService.getOptionChain\(symbol, expiry\)\n\s+}', merge_fun, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'w') as f:
    f.write(content)
