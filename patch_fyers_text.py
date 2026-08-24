import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

text_handler = """    private fun handleTextMessage(text: String) {
        try {
            if (text.startsWith("{")) {
                val json = JSONObject(text)
                parseJsonTick(json)
            }
        } catch (e: Exception) {
            // ignore
        }
    }"""

content = content.replace("    private fun handleTextMessage(text: String) {\n        try {\n            val json = JSONObject(text)\n            // parse JSON ticks if Fyers sends JSON\n            Log.d(TAG, \"Text message received: $text\")\n        } catch (e: Exception) {\n            // ignore\n        }\n    }", text_handler)

with open(filepath, "w") as f:
    f.write(content)
