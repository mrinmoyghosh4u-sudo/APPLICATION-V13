import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

new_onopen = """                    override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                        isConnected = true
                        _connectionState.value = "AUTHENTICATED"
                        healthManager?.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
                        healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
                        
                        _connectionState.value = "SUBSCRIBING"
                        healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_UPSTOX)
                        
                        // Send subscription message"""

content = re.sub(
    r'                    override fun onOpen\(webSocket: okhttp3.WebSocket, response: okhttp3.Response\) \{.*?// Send subscription message',
    new_onopen,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
