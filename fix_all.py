import os

# 1. MarketDataStore
filepath = "/app/applet/app/src/main/java/com/example/data/model/MarketDataStore.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix lastTradeSmart and lastNse usage
content = content.replace("""                if (now - lastTradeSmart > 30000 && _unusedHealth.value == "TRADESMART") {
                    _unusedHealth.value = "STALE"
                }""", "")
content = content.replace("""                if (now - lastNse > 30000 && _unusedHealth.value == "NSE") {
                    _unusedHealth.value = "STALE"
                }""", "")
content = content.replace("val lastTradeSmart", "//val lastTradeSmart")
content = content.replace("val lastNse", "//val lastNse")
content = content.replace("now - lastTradeSmart", "false")
content = content.replace("now - lastNse", "false")
content = content.replace("_fyersHealth", "fyersHealth")
content = content.replace("fyersHealth.value", "_fyersHealth.value")
# Wait, let me just replace all `fyersHealth` back to `_fyersHealth` and make sure it's defined.
# I had replaced `private val _angelOneHealth...` with `private val _fyersHealth... \n private val _angelOneHealth...` in fix_data_source.py
# Let's just define them if they are missing
if "_fyersHealth = MutableStateFlow" not in content:
    content = content.replace("private val _angelOneHealth = MutableStateFlow(\"DISCONNECTED\")", 
    "private val _fyersHealth = MutableStateFlow(\"DISCONNECTED\")\n    val fyersHealth = _fyersHealth.asStateFlow()\n    private val _angelOneHealth = MutableStateFlow(\"DISCONNECTED\")")

with open(filepath, "w") as f:
    f.write(content)

# 2. FyersAuthManager
filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("pin = pin", "pin = sessionManager.fyersPin")
with open(filepath, "w") as f:
    f.write(content)

# 3. TradeSmartMarketDataService
filepath = "/app/applet/app/src/main/java/com/example/data/network/TradeSmartMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()
content = content.replace("MarketDataSourceNames.TRADESMART", '"TradeSmart"')
with open(filepath, "w") as f:
    f.write(content)

# 4. FyersMarketDataService - OptionStrikeItem
filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

# I used:
# item.callLtp = ... 
# but it's a copy
# I need to replace it with item = item.copy(...)
replace_block = """
                if (contract.option_type == "CE") {
                    strikesMap[strike] = item.copy(
                        callLtp = contract.ltp ?: 0.0,
                        callOi = (contract.oi ?: 0.0).toString(),
                        callVolume = (contract.volume ?: 0.0).toLong(),
                        callBid = contract.bid ?: 0.0,
                        callAsk = contract.ask ?: 0.0,
                        callSymbol = contract.symbol ?: ""
                    )
                } else if (contract.option_type == "PE") {
                    strikesMap[strike] = item.copy(
                        putLtp = contract.ltp ?: 0.0,
                        putOi = (contract.oi ?: 0.0).toString(),
                        putVolume = (contract.volume ?: 0.0).toLong(),
                        putBid = contract.bid ?: 0.0,
                        putAsk = contract.ask ?: 0.0,
                        putSymbol = contract.symbol ?: ""
                    )
                }
"""
import re
pattern = re.compile(r'if \(contract\.option_type == "CE"\) \{.*\} else if \(contract\.option_type == "PE"\) \{.*\}', re.DOTALL)
content = pattern.sub(replace_block.strip(), content)

with open(filepath, "w") as f:
    f.write(content)

