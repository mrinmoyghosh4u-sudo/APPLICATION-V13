import re

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'                // Keep state as AUTHENTICATING, wait for message response to confirm\s*// Re-subscribe if needed\s*CoroutineScope\(Dispatchers\.IO\)\.launch \{\s*delay\(500\)\s*resubscribeAll\(\)\s*\}',
    '                // Wait for auth confirmation in onMessage',
    content
)

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "w") as f:
    f.write(content)
