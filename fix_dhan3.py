import re

with open('app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'r') as f:
    text = f.read()

idx = text.find("override suspend fun getMarketQuotes")

if idx != -1:
    end_of_func = text.find("}", idx) + 1
    # Actually wait, getMarketQuotes has its own body.
    # Let's just find "override suspend fun getMarketQuotes" and replace from there to the end.
    text = text[:idx] + """override suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return Result.success(emptyList())
    }

    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> {
        return Result.failure(Exception("Dhan Option Chain disabled"))
    }
    
    override suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return Result.failure(Exception("Dhan Option Chain disabled"))
    }
    
    suspend fun searchInstrument(query: String): Result<List<WatchlistItem>> {
        return Result.success(emptyList())
    }
}
"""

with open('app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'w') as f:
    f.write(text)

