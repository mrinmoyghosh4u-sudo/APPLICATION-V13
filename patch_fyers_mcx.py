with open("app/src/main/java/com/example/data/network/FyersSymbolMapper.kt", "r") as f:
    content = f.read()

old_code = """                // Equity
                if (exchange.equals("NSE", ignoreCase = true)) {
                    "NSE:$symbol-EQ"
                } else if (exchange.equals("BSE", ignoreCase = true)) {
                    "BSE:$symbol-EQ"
                } else {
                    symbol
                }"""

new_code = """                // Equity
                if (exchange.equals("NSE", ignoreCase = true)) {
                    "NSE:$symbol-EQ"
                } else if (exchange.equals("BSE", ignoreCase = true)) {
                    "BSE:$symbol-EQ"
                } else if (exchange.equals("MCX", ignoreCase = true)) {
                    if (symbol.startsWith("MCX:")) symbol else "MCX:$symbol"
                } else {
                    symbol
                }"""

content = content.replace(old_code, new_code)
with open("app/src/main/java/com/example/data/network/FyersSymbolMapper.kt", "w") as f:
    f.write(content)
