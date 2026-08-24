import os
import re

filepath = "/app/applet/app/src/main/java/com/example/data/network/ProviderHealthManager.kt"

with open(filepath, "r") as f:
    content = f.read()

content = content.replace("const val PROVIDER_ANGEL_ONE", "const val PROVIDER_FYERS = \"Fyers\"\n        const val PROVIDER_ANGEL_ONE")
content = content.replace("listOf(PROVIDER_ANGEL_ONE, PROVIDER_MSTOCK, PROVIDER_NSE, PROVIDER_YAHOO, PROVIDER_TRADESMART)", "listOf(PROVIDER_FYERS, PROVIDER_ANGEL_ONE, PROVIDER_MSTOCK)")

with open(filepath, "w") as f:
    f.write(content)
