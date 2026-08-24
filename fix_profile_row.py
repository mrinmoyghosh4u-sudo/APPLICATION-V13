import os

filepath = "app/src/main/java/com/example/ui/screens/ProfileScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

fyers_row_fix = """                BrokerStatusRow(
                    name = "Fyers",
                    subtitle = "Primary Market Data Feed (API V3)",
                    logoRes = R.drawable.ic_dhan_logo, // Fallback icon
                    status = fyersStatus,
                    onConnect = { onSwitchBroker("Fyers") },
                    onReconnect = { onReconnectBroker("Fyers") },
                    onDisconnect = { onDisconnectBroker("Fyers") },
                    onRemoveAccount = { onRemoveAccountBroker("Fyers") }
                )"""

pattern = r"                BrokerConnectionRow\([\s\S]*?onRemoveAccountBroker\(\"Fyers\"\)\s*\}\n                \)"

content = re.sub(pattern, fyers_row_fix, content)

with open(filepath, "w") as f:
    f.write(content)

