import re

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "r") as f:
    content = f.read()

angel_fix = """        if (angelMarketDataService.isConfigured()) {
            val exchType = when(exchange.uppercase()) {
                "NSE" -> 1
                "NFO" -> 2
                "BSE" -> 3
                "BFO" -> 4
                "MCX" -> 5
                "CDS" -> 7
                else -> 1
            }
            angelMarketDataService.subscribeToTokens(exchType, symbols)
        }"""

content = re.sub(
    r'if \(angelMarketDataService\.isConfigured\(\)\) \{\s*angelMarketDataService\.subscribeToTokens\(1, symbols\) // Assuming NSE\s*\}',
    angel_fix,
    content
)

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "w") as f:
    f.write(content)

