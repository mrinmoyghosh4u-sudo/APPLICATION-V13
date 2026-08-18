import re

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'r') as f:
    content = f.read()

new_fun = """
    fun subscribeToTokens(exchangeType: Int, tokens: List<String>) {
        val ws = webSocket ?: return
        if (tokens.isEmpty()) return
        
        val req = JSONObject().apply {
            put("correlationID", "dynamic_sub")
            put("action", 1)
            put("params", JSONObject().apply {
                put("mode", 1)
                put("tokenList", JSONArray().apply {
                    put(JSONObject().apply {
                        put("exchangeType", exchangeType)
                        put("tokens", JSONArray(tokens))
                    })
                })
            })
        }
        ws.send(req.toString())
    }
"""

content = content.replace("private fun handleBinaryTick(bytes: ByteArray) {", new_fun + "\n    private fun handleBinaryTick(bytes: ByteArray) {")

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'w') as f:
    f.write(content)
