import re

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'r') as f:
    content = f.read()

new_find = """
                        val ceOpt = options.find { (it.strike.toDoubleOrNull() ?: 0.0) / 100.0 == item.strikePrice && it.symbol.endsWith("CE") }
                        val peOpt = options.find { (it.strike.toDoubleOrNull() ?: 0.0) / 100.0 == item.strikePrice && it.symbol.endsWith("PE") }
                        
                        // the identifier in MarketDataStore is 'symbol' from instrument master
                        val ceLive = if (ceOpt != null) MarketDataStore.marketData.value[ceOpt.symbol] else null
                        val peLive = if (peOpt != null) MarketDataStore.marketData.value[peOpt.symbol] else null
"""

content = re.sub(r'val strikeFormatted.*val peLive = if \(peOpt != null\) MarketDataStore\.marketData\.value\[peOpt\.symbol\] else null', new_find.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'w') as f:
    f.write(content)
