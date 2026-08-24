import os
import re

# Fix DiagnosticsScreen.kt
filepath = "/app/applet/app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace('SectionHeader("REFERENCE / BACKUP: NSE INDIA & YAHOO FINANCE")', '')

with open(filepath, "w") as f:
    f.write(content)

# Fix BrokerAuthManager.kt
filepath = "/app/applet/app/src/main/java/com/example/data/network/BrokerAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace('"Yahoo" to BrokerConnectionState("Yahoo", "Reference Only", BrokerAuthStatus.STANDBY)', '')
content = content.replace(' "Yahoo" -> "Reference Only"', '')

with open(filepath, "w") as f:
    f.write(content)

