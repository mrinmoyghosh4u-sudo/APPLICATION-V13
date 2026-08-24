import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/BrokerManager.kt"

with open(filepath, "r") as f:
    content = f.read()

replacement = """
    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        return marketDataEngine.getOptionChain(symbol, expiry)
    }
"""

start_idx = content.find("suspend fun getOptionChain")
end_idx = content.find("suspend fun getOptionExpiries")

content = content[:start_idx] + replacement.strip() + "\n\n    " + content[end_idx:]

with open(filepath, "w") as f:
    f.write(content)
