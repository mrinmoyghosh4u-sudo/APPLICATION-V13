import re

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace(
    '[MSTOCK_FIRST_REAL_TICK] First valid m.Stock real tick received',
    '[MSTOCK_FIRST_REAL_TICK] / MSTOCK_LIVE First valid m.Stock real tick received'
)
content = content.replace(
    '[MSTOCK_FIRST_REAL_TICK] First valid m.Stock binary real tick received',
    '[MSTOCK_FIRST_REAL_TICK] / MSTOCK_LIVE First valid m.Stock binary real tick received'
)

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "w") as f:
    f.write(content)
