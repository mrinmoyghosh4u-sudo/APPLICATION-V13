import re

filepath = "app/src/main/java/com/example/data/network/BrokerManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("DhanTradingService", "DhanBrokerService")

with open(filepath, "w") as f:
    f.write(content)
