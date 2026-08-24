import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# WatchlistItem needs lotSize and isPositive
fix_item = """                WatchlistItem(
                    symbol = quote.v?.original_name ?: quote.n ?: "",
                    exchange = quote.v?.exchange ?: "NSE",
                    ltp = quote.v?.lp ?: 0.0,
                    change = quote.v?.ch ?: 0.0,
                    changePercent = quote.v?.chp ?: 0.0,
                    lotSize = 1,
                    isPositive = (quote.v?.ch ?: 0.0) >= 0
                )"""

content = re.sub(r"                WatchlistItem\([\s\S]*?changePercent = quote\.v\?\.chp \?: 0\.0\n                \)", fix_item, content)

with open(filepath, "w") as f:
    f.write(content)

