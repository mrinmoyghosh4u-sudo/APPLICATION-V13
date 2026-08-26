import re

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "r") as f:
    content = f.read()

subscribe_code = """
    suspend fun subscribeToTokens(exchange: String, symbols: List<String>) {
        if (symbols.isEmpty()) return
        
        if (upstoxMarketDataService?.isConfigured() == true) {
            upstoxMarketDataService?.subscribeToMarketData(symbols)
        }
        if (fyersMarketDataService?.isConfigured() == true) {
            fyersMarketDataService?.subscribeToMarketData(symbols)
        }
        if (angelMarketDataService.isConfigured()) {
            angelMarketDataService.subscribeToTokens(1, symbols) // Assuming NSE
        }
        if (mStockMarketDataService.isConfigured()) {
            mStockMarketDataService.subscribe(exchange, symbols)
        }
    }
"""

if "subscribeToTokens(" not in content:
    content = content.replace("    fun retryConnection() {", subscribe_code + "\n    fun retryConnection() {")
    with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "w") as f:
        f.write(content)
