import re

content = """
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

with open('app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'r') as f:
    text = f.read()

# Just replace from the first getOptionChain to the end
idx = text.find("override suspend fun getOptionChain")
if idx != -1:
    text = text[:idx] + content
else:
    # maybe it was getOptionExpiries first
    idx = text.find("override suspend fun getOptionExpiries")
    if idx != -1:
        text = text[:idx] + content

with open('app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'w') as f:
    f.write(text)

