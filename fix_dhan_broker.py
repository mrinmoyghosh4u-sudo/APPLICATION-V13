import re

with open('app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'r') as f:
    content = f.read()

# Replace getOptionExpiries
new_expiries = """    override suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return runCatching {
            val instrument = OptionChainInstrumentMaster.getInstrument(symbol)
            val scrip = instrument?.underlyingScrip?.toString() ?: "13"
            val response = api.getOptionExpiries(scrip)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                throw Exception("API Error: ${response.errorBody()?.string()}")
            }
        }
    }"""

content = re.sub(r'    override suspend fun getOptionExpiries.*?Result\.success\(emptyList\(\)\)\n    }', new_expiries, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'w') as f:
    f.write(content)
