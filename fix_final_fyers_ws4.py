import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace('webSocket.send(okio.ByteString.of(*buffer.array()))', 'webSocket?.send(okio.ByteString.of(*buffer.array()))')

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
