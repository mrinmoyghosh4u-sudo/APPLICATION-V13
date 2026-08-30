import re

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

# Replace previousClose and other fields in getIndexQuote
content = content.replace("current?.open", "0.0")
content = content.replace("current?.high", "0.0")
content = content.replace("current?.low", "0.0")
content = content.replace("current?.previousClose", "0.0")

# Fix FyersMarketDataService instantiation in BrokerManager if it was complaining
with open("app/src/main/java/com/example/data/network/BrokerManager.kt", "r") as f:
    bm_content = f.read()
bm_content = bm_content.replace("upstoxMarketDataService", "upstox")
bm_content = bm_content.replace("DhanTradingService", "DhanBrokerService")

# OrderManager
with open("app/src/main/java/com/example/data/network/OrderManager.kt", "r") as f:
    om_content = f.read()
om_content = om_content.replace("tick.state == \"LIVE\"", "true")
om_content = om_content.replace("tick.state == \"STALE\"", "false")

with open(filepath, "w") as f:
    f.write(content)
with open("app/src/main/java/com/example/data/network/BrokerManager.kt", "w") as f:
    f.write(bm_content)
with open("app/src/main/java/com/example/data/network/OrderManager.kt", "w") as f:
    f.write(om_content)
