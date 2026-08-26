with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

# Replace connectWebSocket to do nothing, or just set to LIVE since polling handles it
old_connect = """    private fun connectWebSocket() {
        if (isConnected) return
        Log.d(TAG, "[FYERS_AUTH_START] Initiating FYERS WebSocket connection...")"""

new_connect = """    private fun connectWebSocket() {
        // Disabled WebSocket to rely strictly on REST polling
        isConnected = true
        _connectionState.value = "LIVE"
        com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.FYERS, "CONNECTED")
        healthManager?.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
        return
        Log.d(TAG, "[FYERS_AUTH_START] Initiating FYERS WebSocket connection...")"""

content = content.replace(old_connect, new_connect)

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
