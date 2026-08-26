import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace('resolvedToken', 'hsmToken')
content = content.replace('currentWebSocket', 'webSocket')

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
