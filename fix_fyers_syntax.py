import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

new_content = """    private var backoffDelayMs = 2000L
    
    suspend fun connect() {
        if (!isConfigured()) {
            _connectionState.value = "NOT_CONFIGURED"
            healthManager?.reportConfigured(ProviderHealthManager.PROVIDER_FYERS, false)
            Log.e(TAG, "[FYERS_AUTH_FAILED] Cannot connect: Fyers credentials missing")
            return
        }
        _connectionState.value = "CONNECTING"
        reconnectJob?.cancel()
        backoffDelayMs = 2000L
        
        connectWebSocket()
    }

    private fun connectWebSocket() {"""

content = re.sub(
    r'    private var backoffDelayMs = 2000L.*?private fun connectWebSocket\(\) \{',
    new_content,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
