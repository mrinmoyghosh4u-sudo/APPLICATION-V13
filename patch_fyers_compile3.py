import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace('webSocket.send(okio.ByteString.of(*subMsg.array()))', 'currentWebSocket.send(okio.ByteString.of(*subMsg.array()))')
content = content.replace('webSocket?.send(okio.ByteString.of(*liteData.array()))', 'currentWebSocket.send(okio.ByteString.of(*liteData.array()))')
content = content.replace('webSocket?.send(okio.ByteString.of(*subMsg.array()))', 'currentWebSocket.send(okio.ByteString.of(*subMsg.array()))')


with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
