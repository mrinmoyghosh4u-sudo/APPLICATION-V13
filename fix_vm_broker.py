import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
    "val isReady = brokerManager.upstox.isConnectionLive() ||",
    "val isReady = brokerManager.upstoxMarketDataService.isConnectionLive() ||"
)

content = content.replace(
    "//                     val fyersStatus = brokerManager.brokerAuthManager.statuses.value[\"Fyers\"]?.status",
    "                    val fyersStatus = brokerManager.brokerAuthManager.statuses.value[\"Fyers\"]?.status"
)

content = content.replace(
    "//                     val angelStatus = brokerManager.brokerAuthManager.statuses.value[\"Angel One\"]?.status",
    "                    val angelStatus = brokerManager.brokerAuthManager.statuses.value[\"Angel One\"]?.status"
)

with open(filepath, "w") as f:
    f.write(content)
