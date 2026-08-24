import os

filepath = "app/src/main/java/com/example/data/network/BrokerAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

disconnect_fyers = """
            "Fyers" -> {
                sessionManager.clearFyersSession()
                updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.OFFLINE, "Disconnected")
            }
"""

remove_fyers = """
            "Fyers" -> {
                sessionManager.clearFyersSession()
                sessionManager.fyersAppId = ""
                sessionManager.fyersSecretId = ""
                updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Account Removed")
            }
"""

content = re.sub(r'            "Fyers" -> Result\.failure\(Exception\("Fyers auto-reconnect not supported\. Please re-login\."\)\)\n            "Angel One" -> {\n                angelMarketDataService\.disconnect\(\)', disconnect_fyers + '            "Angel One" -> {\n                angelMarketDataService.disconnect()', content, count=1)

content = re.sub(r'            "Fyers" -> Result\.failure\(Exception\("Fyers auto-reconnect not supported\. Please re-login\."\)\)\n            "Angel One" -> {\n                angelMarketDataService\.disconnect\(\)', remove_fyers + '            "Angel One" -> {\n                angelMarketDataService.disconnect()', content, count=1)


with open(filepath, "w") as f:
    f.write(content)
