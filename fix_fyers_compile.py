import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

# Fix hsmToken conflict
content = content.replace(
    'val hsmToken = sessionManager.fyersAccessToken ?: "" // We need the token here too. Let\'s just retrieve it from sessionManager',
    'val sessionToken = sessionManager.fyersAccessToken ?: "" // We need the token here too. Let\'s just retrieve it from sessionManager'
)
content = content.replace('var actualToken = hsmToken', 'var actualToken = sessionToken')

# Fix webSocket smart cast
content = content.replace('webSocket.send(okio.ByteString.of(*liteData.array()))', 'webSocket?.send(okio.ByteString.of(*liteData.array()))')
content = content.replace('webSocket.send(okio.ByteString.of(*subMsg.array()))', 'webSocket?.send(okio.ByteString.of(*subMsg.array()))')

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
