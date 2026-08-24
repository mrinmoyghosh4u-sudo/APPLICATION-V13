import os

filepath = "app/src/main/java/com/example/data/network/BrokerManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("""    val networkClient = BrokerNetworkClient(sessionManager)
    val fyersAuthManager = FyersAuthManager(sessionManager, networkClient.fyersApi)
    val fyersMarketDataService = FyersMarketDataService(sessionManager, marketDataEngine)""", "")

fyers_after_engine = """    val networkClient = BrokerNetworkClient(sessionManager)
    val fyersAuthManager = FyersAuthManager(sessionManager, networkClient.fyersApi)
    val fyersMarketDataService = FyersMarketDataService(sessionManager, marketDataEngine)"""

content = content.replace("    // Central Order Execution Manager", fyers_after_engine + "\n    // Central Order Execution Manager")

with open(filepath, "w") as f:
    f.write(content)

