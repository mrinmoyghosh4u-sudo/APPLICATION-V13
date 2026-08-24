import os

filepath = "app/src/main/java/com/example/data/network/BrokerAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Add validateFyersSession
fyers_validate = """
    private suspend fun validateFyersSession() {
        val hasSession = sessionManager.isFyersConnected && !sessionManager.fyersAccessToken.isNullOrBlank()
        if (!hasSession) {
            updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }
        
        // Fyers session doesn't expire quickly or we just assume it's valid if we have it, 
        // until a data request fails. But we can set to CONNECTED.
        updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Active for Market Data")
    }
"""

content = content.replace("private suspend fun validateAngelOneSession() {", fyers_validate + "\n    private suspend fun validateAngelOneSession() {")
content = content.replace("validateAngelOneSession()", "validateFyersSession()\n            validateAngelOneSession()")

# Update removeAccount
remove_fyers = """
            "Fyers" -> {
                sessionManager.clearFyersSession()
                updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.OFFLINE, "Disconnected")
            }
"""
content = content.replace("\"Angel One\" -> {", remove_fyers + "            \"Angel One\" -> {")

# Update disconnectBroker
content = content.replace("\"Angel One\" -> {", remove_fyers + "            \"Angel One\" -> {")

with open(filepath, "w") as f:
    f.write(content)
