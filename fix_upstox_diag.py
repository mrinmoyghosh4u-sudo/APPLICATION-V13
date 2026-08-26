import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace(
    'webSocket?.send(okio.ByteString.of(*buffer.array()))',
    'webSocket?.send(okio.ByteString.of(*buffer.array()))\n                        healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, subscribedInstrumentKeys.size)\n                        healthManager?.reportWaitingForTick(ProviderHealthManager.PROVIDER_UPSTOX)'
)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
