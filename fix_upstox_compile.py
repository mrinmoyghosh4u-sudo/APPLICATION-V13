import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

# Fix the method signature
content = content.replace("private fun startRestPolling()\n        connectWebSocket() {", "private fun connectWebSocket() {")

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
