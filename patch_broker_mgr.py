import os

filepath = "app/src/main/java/com/example/data/network/BrokerManager.kt"
with open(filepath, "r") as f:
    content = f.read()

network_client = """    val networkClient = BrokerNetworkClient(sessionManager)
    val fyersAuthManager = FyersAuthManager(sessionManager, networkClient.fyersApi)
    val fyersMarketDataService = FyersMarketDataService(sessionManager, marketDataEngine)"""

if "fyersMarketDataService" not in content:
    content = content.replace("val nseFeedService = NseAuthorizedFeedService(sessionManager)", 
    "val nseFeedService = NseAuthorizedFeedService(sessionManager)\n" + network_client)
    with open(filepath, "w") as f:
        f.write(content)
    print("Added Fyers dependencies to BrokerManager")
