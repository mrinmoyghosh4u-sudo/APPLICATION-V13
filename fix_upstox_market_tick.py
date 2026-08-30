import re

filepath = "app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("""                val tick = MarketTick(
                    symbol = standardSym,
                    token = key,
                    exchange = exch,
                    ltp = ltp,
                    open = if (feed.open > 0.0) feed.open else ltp,
                    high = if (feed.high > 0.0) feed.high else ltp,
                    low = if (feed.low > 0.0) feed.low else ltp,
                    close = if (feed.close > 0.0) feed.close else ltp,
                    volume = feed.volume,
                    timestamp = if (feed.timestamp > 0L) feed.timestamp else now
                )""", """                val tick = com.example.data.model.RealTimePriceTick(
                    symbol = standardSym,
                    price = ltp,
                    volume = feed.volume,
                    timestamp = if (feed.timestamp > 0L) feed.timestamp else now,
                    source = "UPSTOX"
                )""")

with open(filepath, "w") as f:
    f.write(content)
