import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

# Extract the subscription part from onOpen
sub_pattern = r'(// 2\. Send Lite mode message.*?Log\.i\(TAG, "\[FYERS_SUB_SENT\] Sent subscription for \$\{symbolsToSub\.size\} symbols"\))'
match = re.search(sub_pattern, content, flags=re.DOTALL)
if match:
    sub_code = match.group(1)
    
    # Remove from onOpen
    content = content.replace(sub_code, 'Log.i(TAG, "Waiting for auth response before subscribing...")\n                    isAuthSent = true')
    
    # Add variable
    content = content.replace('private var hasFirstTick = false', 'private var hasFirstTick = false\n    private var isAuthSent = false\n    private var isSubscribed = false')
    
    # Create sendSubscription method
    send_sub_method = f"""    private fun sendSubscription() {{
        if (webSocket == null || !isConnected) return
        try {{
            val hsmToken = sessionManager.fyersAccessToken ?: "" // We need the token here too. Let's just retrieve it from sessionManager
            var actualToken = hsmToken
            if (actualToken.contains(":")) {{
                actualToken = actualToken.split(":")[1]
            }}
            val tokenParts = actualToken.split(".")
            val resolvedToken = if (tokenParts.size >= 2) {{
                val payloadBytes = android.util.Base64.decode(tokenParts[1], android.util.Base64.URL_SAFE)
                org.json.JSONObject(String(payloadBytes)).optString("hsm_key", actualToken)
            }} else actualToken
            val source = "PythonSDK-1.0.0"
            
{sub_code}
        }} catch (e: Exception) {{
            Log.e(TAG, "Failed to send Fyers subscribe: ${{e.message}}")
        }}
    }}"""
    
    # Insert method
    content = content.replace('private fun handleBinaryMessage(bytes: ByteArray) {', f'{send_sub_method}\n\n    private fun handleBinaryMessage(bytes: ByteArray) {{')
    
    # Call sendSubscription on first message
    content = content.replace('private fun handleBinaryMessage(bytes: ByteArray) {\n        try {', 'private fun handleBinaryMessage(bytes: ByteArray) {\n        if (isAuthSent && !isSubscribed) {\n            isSubscribed = true\n            _connectionState.value = "AUTHENTICATED"\n            healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)\n            _connectionState.value = "SUBSCRIBING"\n            healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_FYERS)\n            sendSubscription()\n        }\n        try {')
    content = content.replace('private fun handleTextMessage(text: String) {\n        try {', 'private fun handleTextMessage(text: String) {\n        if (isAuthSent && !isSubscribed) {\n            isSubscribed = true\n            _connectionState.value = "AUTHENTICATED"\n            healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)\n            _connectionState.value = "SUBSCRIBING"\n            healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_FYERS)\n            sendSubscription()\n        }\n        try {')

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
