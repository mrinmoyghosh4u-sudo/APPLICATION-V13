import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

import re
# The lines:
#                 val tick = MarketTick(
#                     symbol = symbol,
#                     token = symbol,
#                     ltp = ltp,
#                     open = openPrice,
#                     high = highPrice,
#                     low = lowPrice,
#                     close = closePrice,
#                     change = change,
#                     changePercent = changePercent,
#                     volume = volume,
#                     timestamp = timestamp,
#                     oi = 0L,
#                     source = "FYERS"
#                 )

content = content.replace("oi = 0L,\n                    source = \"FYERS\"\n                )", "exchange = \"NSE\"\n                )")
content = content.replace("oi = 0L,\n                    source = \"FYERS\"", "exchange = \"NSE\"")
content = content.replace("oi = 0L,", "exchange = \"NSE\",")

with open(filepath, "w") as f:
    f.write(content)
