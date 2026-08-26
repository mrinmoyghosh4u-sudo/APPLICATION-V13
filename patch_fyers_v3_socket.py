import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

# Replace the WS URL candidates
old_candidates = """    private val WS_URL_CANDIDATES = listOf(
        "wss://socket.fyers.in/socket/v2/data/",
        "wss://api-t1.fyers.in/socket/v2/data/",
        "wss://socket.fyers.in/hsm/v1-5/prod",
        "wss://api.fyers.in/socket/v2/data/"
    )
    private var currentUrlIndex = 0"""

new_candidates = """    private val WS_URL = "wss://socket.fyers.in/hsm/v1-5/prod"
"""

content = content.replace(old_candidates, new_candidates)

# Update connectWebSocket to use the correct URL
old_url_logic = """        val fyersToken = "$appId:$token"
        val url = WS_URL_CANDIDATES[currentUrlIndex % WS_URL_CANDIDATES.size]
        Log.i(TAG, "[FYERS_WS_CONNECTING] Connecting to $url...")
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", fyersToken)
            .build()"""

new_url_logic = """        val fyersToken = "$appId:$token"
        val url = WS_URL
        Log.i(TAG, "[FYERS_WS_CONNECTING] Connecting to $url...")
        
        val request = Request.Builder()
            .url(url)
            .build()"""
content = content.replace(old_url_logic, new_url_logic)

# In onOpen, we need to send the HSM token message (binary auth)
# the HSM token msg in python:
# buffer_size = 18 + len(hsm_token) + len("PythonSDK-1.0.0")
# byte_buffer.extend(struct.pack("!H", buffer_size - 2))
# byte_buffer.extend(bytes([1])) # ReqType
# byte_buffer.extend(bytes([4])) # FieldCount
# Field 1 (hsmToken): fieldCode 7, length len(hsmToken), hsmToken bytes
# Field 2 (source): fieldCode 14, length len(source), source bytes
# Field 3 (channel_num): fieldCode 12, length 2, channel_num bytes
# Field 4 (data_type): fieldCode 13, length 2, data_type bytes (0x02 0x00 ?)
# Wait, parsing JWT for HSM token is hard. Fyers actually released v3 json websockets as well on wss://api-t1.fyers.in/data-rest/v3/quotes/ws? No, I got 404 for that.
