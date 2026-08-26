import re

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "r") as f:
    content = f.read()

# Fix onOpen authentication
on_open_old = """                // Send Login / Authentication Handshake
                val authPayload = JSONObject().apply {
                    put("action", "login")
                    put("apiKey", sessionManager?.mstockApiKey ?: "")
                    put("clientId", sessionManager?.mstockClientId ?: "")
                    put("token", sessionManager?.mstockAccessToken ?: "")
                    put("feedToken", sessionManager?.mstockFeedToken ?: "")
                    put("timestamp", System.currentTimeMillis())
                }
                webSocket.send(authPayload.toString())"""

on_open_new = """                // Send Login / Authentication Handshake
                val token = sessionManager?.mstockAccessToken ?: ""
                webSocket.send("LOGIN:$token")
                
                // Immediately assume authenticated since connection succeeded
                _connectionState.value = "AUTHENTICATED"
                healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, true)
                MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "AUTHENTICATED")
                
                _connectionState.value = "SUBSCRIBING"
                healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_MSTOCK)
                resubscribeAll()
                _connectionState.value = "WAITING_FOR_TICK"
                healthManager?.reportSubscribed(ProviderHealthManager.PROVIDER_MSTOCK, subscribedTokens.size)
"""
content = content.replace(on_open_old, on_open_new)

# Fix subscribe method
subscribe_old = """        try {
            val subMsg = JSONObject().apply {
                put("action", "subscribe")
                put("mode", mode) // 1: LTP, 2: QUOTE
                put("exchange", exchange)
                put("tokens", JSONArray(validTokens))
            }
            _connectionState.value = "SUBSCRIBING"
            webSocket?.send(subMsg.toString())"""

subscribe_new = """        try {
            val validIntTokens = validTokens.mapNotNull { it.toIntOrNull() }
            if (validIntTokens.isEmpty()) return
            
            val subMsg = JSONObject().apply {
                put("a", "subscribe")
                put("v", JSONArray(validIntTokens))
            }
            val modeMsg = JSONObject().apply {
                put("a", "mode")
                val vArr = JSONArray()
                vArr.put("full")
                vArr.put(JSONArray(validIntTokens))
                put("v", vArr)
            }
            _connectionState.value = "SUBSCRIBING"
            webSocket?.send(subMsg.toString())
            webSocket?.send(modeMsg.toString())"""
content = content.replace(subscribe_old, subscribe_new)

# Fix resubscribeAll
resubscribe_old = """        if (subscribedTokens.isEmpty()) return
        val grouped = subscribedTokens.entries.groupBy({ it.value }, { it.key })
        grouped.forEach { (exchange, tokens) ->
            subscribe(exchange, tokens)
        }"""

resubscribe_new = """        if (subscribedTokens.isEmpty()) return
        val validIntTokens = subscribedTokens.keys.mapNotNull { it.toIntOrNull() }
        if (validIntTokens.isEmpty()) return
        
        try {
            val subMsg = JSONObject().apply {
                put("a", "subscribe")
                put("v", JSONArray(validIntTokens))
            }
            val modeMsg = JSONObject().apply {
                put("a", "mode")
                val vArr = JSONArray()
                vArr.put("full")
                vArr.put(JSONArray(validIntTokens))
                put("v", vArr)
            }
            webSocket?.send(subMsg.toString())
            webSocket?.send(modeMsg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send m.Stock resubscribe msg", e)
        }"""
content = content.replace(resubscribe_old, resubscribe_new)

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "w") as f:
    f.write(content)
