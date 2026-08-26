import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace(
    '[FYERS_FIRST_REAL_TICK] First valid FYERS real tick received',
    '[FYERS_FIRST_REAL_TICK] / FYERS_LIVE First valid FYERS real tick received'
)

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
