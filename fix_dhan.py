import re

with open('app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'r') as f:
    content = f.read()

content = re.sub(r'import com\.example\.data\.model\.OptionChainInstrumentMaster\n', '', content)
content = re.sub(r'override suspend fun getOptionChain.*?\n\s+\}', 
"""override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> {
        return Result.failure(Exception("Dhan Option Chain disabled"))
    }""", content, flags=re.DOTALL)

content = re.sub(r'override suspend fun getOptionExpiries.*?\n\s+\}', 
"""override suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return Result.failure(Exception("Dhan Option Chain disabled"))
    }""", content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'w') as f:
    f.write(content)
