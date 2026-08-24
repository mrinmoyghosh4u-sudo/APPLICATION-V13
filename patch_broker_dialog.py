import os

filepath = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Add Fyers state vars
content = content.replace(
    "var tradeSmartToken by remember { mutableStateOf(sessionManager.tradesmartAccessToken ?: \"\") }",
    "var tradeSmartToken by remember { mutableStateOf(sessionManager.tradesmartAccessToken ?: \"\") }\n    var fyersAppId by remember { mutableStateOf(sessionManager.fyersAppId ?: \"\") }\n    var fyersSecretId by remember { mutableStateOf(sessionManager.fyersSecretId ?: \"\") }\n    var fyersAuthCode by remember { mutableStateOf(\"\") }"
)

# Add onFyersLogin to signature
content = content.replace(
    "onTradeSmartLogin: ((String, String, String) -> Unit)? = null",
    "onTradeSmartLogin: ((String, String, String) -> Unit)? = null,\n    onFyersLogin: ((String, String, String) -> Unit)? = null"
)

with open(filepath, "w") as f:
    f.write(content)
