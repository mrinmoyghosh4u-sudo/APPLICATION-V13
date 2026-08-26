with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

import re

# Update URL candidates
content = re.sub(
    r'private val WS_URL_CANDIDATES = listOf\([^)]+\)',
    'private val WS_URL_CANDIDATES = listOf("wss://socket.fyers.in/hsm/v1-5/prod")',
    content,
    flags=re.MULTILINE
)

# Insert the binary auth sending logic inside onOpen
old_on_open = """                healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_FYERS)
                
                // Subscribe using official JSON format with type 'lite'
                val symbolsToSub = if (subscribedSymbols.isNotEmpty()) {
                    subscribedSymbols.toList()
                } else {
                    listOf("NSE:NIFTY50-INDEX")
                }
                subscribedSymbols.addAll(symbolsToSub)

                val payload = JSONObject().apply {
                    put("symbol", JSONArray(symbolsToSub))
                    put("type", "lite")
                }.toString()
                
                webSocket.send(payload)"""

new_on_open = """                healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_FYERS)
                
                // Send binary authentication message
                try {
                    val tokenParts = token.split(".")
                    val hsmToken = if (tokenParts.size >= 2) {
                        val payloadBytes = android.util.Base64.decode(tokenParts[1], android.util.Base64.URL_SAFE)
                        JSONObject(String(payloadBytes)).optString("hsm_key", token)
                    } else token

                    val source = "PythonSDK-1.0.0"
                    val authBufferSize = 18 + hsmToken.length + source.length
                    val buffer = java.nio.ByteBuffer.allocate(authBufferSize)
                    buffer.order(java.nio.ByteOrder.BIG_ENDIAN)
                    
                    buffer.putShort((authBufferSize - 2).toShort())
                    buffer.put(1.toByte()) // ReqType
                    buffer.put(4.toByte()) // FieldCount
                    
                    // Field 1
                    buffer.put(1.toByte())
                    buffer.putShort(hsmToken.length.toShort())
                    buffer.put(hsmToken.toByteArray(Charsets.UTF_8))
                    
                    // Field 2
                    buffer.put(2.toByte())
                    buffer.putShort(1.toShort())
                    buffer.put(78.toByte())
                    
                    // Field 3
                    buffer.put(3.toByte())
                    buffer.putShort(1.toShort())
                    buffer.put(1.toByte())
                    
                    // Field 4
                    buffer.put(4.toByte())
                    buffer.putShort(source.length.toShort())
                    buffer.put(source.toByteArray(Charsets.UTF_8))
                    
                    webSocket.send(okio.ByteString.of(*buffer.array()))
                    Log.i(TAG, "[FYERS_AUTH_SENT] Sent binary authentication message")
                    
                    // Send binary subscribe message
                    val symbolsToSub = if (subscribedSymbols.isNotEmpty()) {
                        subscribedSymbols.toList()
                    } else {
                        listOf("NSE:NIFTY50-INDEX")
                    }
                    subscribedSymbols.addAll(symbolsToSub)
                    
                    val subPayload = JSONObject().apply {
                        put("T", "SUB_L2")
                        put("L2LIST", JSONArray(symbolsToSub))
                        put("SUB_T", 1) // 1=Subscribe
                    }
                    webSocket.send(subPayload.toString())
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send Fyers auth/subscribe: ${e.message}")
                }
"""
if old_on_open in content:
    content = content.replace(old_on_open, new_on_open)
else:
    print("Could not find on_open logic to replace.")

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
