import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

# Add reportSubscribed and waiting for tick
content = content.replace(
    'webSocket?.send(okio.ByteString.of(*buffer.array()))',
    'webSocket?.send(okio.ByteString.of(*buffer.array()))\n                healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_FYERS, subscribedSymbols.size)\n                healthManager?.reportWaitingForTick(ProviderHealthManager.PROVIDER_FYERS)'
)

# And add the user-agent okhttp header we wrote before, but let's make sure it's applied!
# Wait, I already added the okhttp header in `fix_fyers_okhttp.py`!

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
