import os
import re

filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
"""    private fun getFyersSymbol(symbol: String): String {
        return if (symbol.contains(":")) symbol else "NSE:$symbol-EQ" // Simplistic mapping
    }""",
"""    private fun getFyersSymbol(symbol: String): String {
        return FyersSymbolMapper.toFyersSymbol(symbol)
    }"""
)

# And let's check OptionChain implementation in FyersMarketDataService.
# Prompt asks to "Implement real FYERS option-chain API. Use official: GET /data/options-chain-v3"
# Actually, wait, Fyers API doesn't have options-chain-v3? I should check FyersApi.kt.
with open(filepath, "w") as f:
    f.write(content)
