import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/BrokerManager.kt"

with open(filepath, "r") as f:
    content = f.read()

replacement = """
    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return marketDataEngine.getOptionExpiries(symbol)
    }
"""

# Replace the old getOptionExpiries body
start_idx = content.find("suspend fun getOptionExpiries")
end_idx = content.find("suspend fun getHistoricalCandles")

content = content[:start_idx] + replacement.strip() + "\n\n    " + content[end_idx:]

with open(filepath, "w") as f:
    f.write(content)
