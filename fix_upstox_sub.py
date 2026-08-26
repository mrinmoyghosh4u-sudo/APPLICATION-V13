import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

subscribe_code = """
    suspend fun subscribeToMarketData(symbols: List<String>) = withContext(Dispatchers.IO) {
        if (!isConfigured() || symbols.isEmpty()) return@withContext
        val newKeys = symbols.mapNotNull { UpstoxSymbolMapper.toUpstoxInstrumentKey(it) }
        subscribedInstrumentKeys.addAll(newKeys)
        
        if (isConnected && webSocket != null) {
            try {
                val json = org.json.JSONObject()
                val data = org.json.JSONObject()
                data.put("instrumentKeys", org.json.JSONArray(newKeys))
                json.put("guid", java.util.UUID.randomUUID().toString())
                json.put("method", "sub")
                json.put("data", data)
                
                val payload = json.toString().toByteArray(Charsets.UTF_8)
                webSocket?.send(okio.ByteString.of(*payload))
                android.util.Log.i(TAG, "[UPSTOX_SUB_SENT] Subscribed to ${newKeys.size} instruments dynamically")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error sending subscription", e)
            }
        }
    }
"""

content = content.replace("    fun isConfigured(): Boolean {", subscribe_code + "\n    fun isConfigured(): Boolean {")

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
