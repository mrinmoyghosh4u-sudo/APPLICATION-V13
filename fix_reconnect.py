import os

filepath = "app/src/main/java/com/example/data/network/BrokerAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Remove the broken lines from reconnectBroker
content = content.replace("""
            "Fyers" -> {
                sessionManager.clearFyersSession()
                updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.OFFLINE, "Disconnected")
            }
            
            "Fyers" -> {
                sessionManager.clearFyersSession()
                updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.OFFLINE, "Disconnected")
            }""",
"""
            "Fyers" -> Result.failure(Exception("Fyers auto-reconnect not supported. Please re-login."))""")

with open(filepath, "w") as f:
    f.write(content)
