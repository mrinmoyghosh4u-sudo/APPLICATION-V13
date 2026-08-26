import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

new_onopen = """                    override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                        isConnected = true
                        _connectionState.value = "CONNECTED"
                        healthManager?.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
                        
                        _connectionState.value = "AUTHENTICATED"
                        healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
                        
                        _connectionState.value = "SUBSCRIBING"
                        healthManager?.reportSubscribing(ProviderHealthManager.PROVIDER_UPSTOX)
                        
                        val keys = if (subscribedInstrumentKeys.isNotEmpty()) {
                            subscribedInstrumentKeys.toList()
                        } else {
                            listOf(
                                com.example.data.network.UpstoxSymbolMapper.KEY_NIFTY_50,
                                com.example.data.network.UpstoxSymbolMapper.KEY_BANK_NIFTY,
                                com.example.data.network.UpstoxSymbolMapper.KEY_FIN_NIFTY,
                                com.example.data.network.UpstoxSymbolMapper.KEY_MIDCP_NIFTY
                            )
                        }
                        subscribedInstrumentKeys.addAll(keys)
                        
                        scope.launch {
                            subscribeToMarketData(keys)
                        }
                    }"""

content = re.sub(
    r'                    override fun onOpen\(webSocket: okhttp3\.WebSocket, response: okhttp3\.Response\) \{.*?scope\.launch \{\s*subscribeToMarketData\(keys\)\s*\}\s*\}',
    new_onopen,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
