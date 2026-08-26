import re

with open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt", "r") as f:
    content = f.read()

is_configured_code = """
    fun isConfigured(): Boolean {
        return !sessionManager.angelJwtToken.isNullOrEmpty() && !sessionManager.angelClientId.isNullOrEmpty() && !sessionManager.angelFeedToken.isNullOrEmpty()
    }
"""

if "fun isConfigured" not in content:
    content = content.replace("    fun isConnectingOrLive(): Boolean {", is_configured_code + "\n    fun isConnectingOrLive(): Boolean {")
    with open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt", "w") as f:
        f.write(content)
