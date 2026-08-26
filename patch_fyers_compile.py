import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace(
'''        private fun sendSubscription() {
        if (webSocket == null || !isConnected) return
        try {
            val hsmToken = sessionManager.fyersAccessToken ?: "" // We need the token here too. Let's just retrieve it from sessionManager
            var actualToken = hsmToken
            if (actualToken.contains(":")) {
                actualToken = actualToken.split(":")[1]
            }
            val tokenParts = actualToken.split(".")
            val hsmToken = if (tokenParts.size >= 2) {
                val payloadBytes = android.util.Base64.decode(tokenParts[1], android.util.Base64.URL_SAFE)
                org.json.JSONObject(String(payloadBytes)).optString("hsm_key", actualToken)
            } else actualToken
            val source = "PythonSDK-1.0.0"''', 
'''        private fun sendSubscription() {
        val currentWebSocket = webSocket
        if (currentWebSocket == null || !isConnected) return
        try {
            val hsmTokenStr = sessionManager.fyersAccessToken ?: "" // We need the token here too. Let's just retrieve it from sessionManager
            var actualToken = hsmTokenStr
            if (actualToken.contains(":")) {
                actualToken = actualToken.split(":")[1]
            }
            val tokenParts = actualToken.split(".")
            val resolvedToken = if (tokenParts.size >= 2) {
                val payloadBytes = android.util.Base64.decode(tokenParts[1], android.util.Base64.URL_SAFE)
                org.json.JSONObject(String(payloadBytes)).optString("hsm_key", actualToken)
            } else actualToken
            val source = "PythonSDK-1.0.0"'''
)

content = content.replace('val subDataLen = 18 + scripsLen + hsmToken.length + source.length', 'val subDataLen = 18 + scripsLen + resolvedToken.length + source.length')
content = content.replace('webSocket.send(okio.ByteString.of(*authData.array()))', 'currentWebSocket.send(okio.ByteString.of(*authData.array()))')
content = content.replace('webSocket.send(okio.ByteString.of(*liteData.array()))', 'currentWebSocket.send(okio.ByteString.of(*liteData.array()))')
content = content.replace('webSocket?.send(okio.ByteString.of(*subMsg.array()))', 'currentWebSocket.send(okio.ByteString.of(*subMsg.array()))')

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
