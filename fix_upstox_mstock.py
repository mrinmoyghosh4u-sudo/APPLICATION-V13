import re

# Fix MStock
with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "r") as f:
    mstock_content = f.read()

mstock_fix = """                // Send Login / Authentication Handshake
                val token = sessionManager?.mstockAccessToken ?: ""
                webSocket.send("LOGIN:$token")
                
                // Keep state as AUTHENTICATING, wait for message response to confirm
                // Re-subscribe if needed
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    resubscribeAll()
                }
"""

mstock_content = re.sub(
    r'// Send Login / Authentication Handshake[\s\S]*?_connectionState\.value = "SUBSCRIBING"',
    mstock_fix.strip(),
    mstock_content
)

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "w") as f:
    f.write(mstock_content)


# Fix Upstox
with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    upstox_content = f.read()

upstox_content = upstox_content.replace(
    'data.put("instrumentKeys", org.json.JSONArray(newKeys))',
    'data.put("instrumentKeys", org.json.JSONArray(newKeys))\n                data.put("mode", "full")'
)

upstox_content = upstox_content.replace(
    'data.put("instrumentKeys", org.json.JSONArray(keys))',
    'data.put("instrumentKeys", org.json.JSONArray(keys))\n                        data.put("mode", "full")'
)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(upstox_content)

