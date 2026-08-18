import re

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'r') as f:
    content = f.read()

sub_indices = """
    private fun subscribeToIndices(ws: WebSocket) {
        if (!instrumentMaster.isLoaded) return
        
        val tokensToSubscribe = mutableListOf<String>()
        val indices = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY")
        
        for (index in indices) {
            val inst = instrumentMaster.resolveIndexToken(index)
            if (inst != null) {
                tokensToSubscribe.add(inst.token)
                
                // Let's also subscribe to ATM options if we know the LTP
                // But we don't know the LTP yet!
            }
        }
        
        if (tokensToSubscribe.isNotEmpty()) {
"""

content = re.sub(r'private fun subscribeToIndices.*?if \(tokensToSubscribe\.isNotEmpty\(\)\) \{', sub_indices.strip() + " {", content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'w') as f:
    f.write(content)
