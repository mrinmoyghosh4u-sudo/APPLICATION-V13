import os

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

init_logic = """
            brokerAuthManager.initialize()
            if (sessionManager.isFyersConnected && !sessionManager.fyersAccessToken.isNullOrBlank()) {
                brokerManager.fyersMarketDataService.connect()
            }
"""

content = content.replace("            brokerAuthManager.initialize()", init_logic)

with open(filepath, "w") as f:
    f.write(content)
