import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Remove : IBrokerService
content = content.replace(") : IBrokerService {", ") {")
content = content.replace("override fun connect()", "fun connect()")
content = content.replace("override fun disconnect()", "fun disconnect()")
content = content.replace("override fun subscribeToMarketData", "fun subscribeToMarketData")
content = content.replace("override fun unsubscribeMarketData", "fun unsubscribeMarketData")
content = content.replace("override suspend fun getMarketQuotes", "suspend fun getMarketQuotes")
content = content.replace("override suspend fun getHistoricalCandles", "suspend fun getHistoricalCandles")
content = content.replace("override suspend fun getOptionChain", "suspend fun getOptionChain")
content = content.replace("override suspend fun getOptionExpiries", "suspend fun getOptionExpiries")

# And there are dummy methods for trading that I can just remove since it's not IBrokerService anymore
content = re.sub(r"    override val brokerName = \"Fyers\"\n[\s\S]*?override suspend fun getPositions\(\): Result<List<PortfolioHoldingEntity>> = Result\.failure\(Exception\(\"Fyers Market Data Only\"\)\)\n\n", "", content)
# wait, wait, what if I already removed some? I'll just remove 'override' keyword from all methods just in case.
content = content.replace("override val brokerName", "val brokerName")
content = content.replace("override suspend fun getProfile", "suspend fun getProfile")
content = content.replace("override suspend fun getFunds", "suspend fun getFunds")
content = content.replace("override suspend fun getOrders", "suspend fun getOrders")
content = content.replace("override suspend fun placeOrder", "suspend fun placeOrder")
content = content.replace("override suspend fun modifyOrder", "suspend fun modifyOrder")
content = content.replace("override suspend fun cancelOrder", "suspend fun cancelOrder")
content = content.replace("override suspend fun getHoldings", "suspend fun getHoldings")
content = content.replace("override suspend fun getPositions", "suspend fun getPositions")

with open(filepath, "w") as f:
    f.write(content)
