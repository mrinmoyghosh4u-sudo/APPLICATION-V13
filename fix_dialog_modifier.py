import os

filepath = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Fix double modifier
content = content.replace("modifier = Modifier.fillMaxWidth(),\n                    horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())", "modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),\n                    horizontalArrangement = Arrangement.spacedBy(4.dp)")

with open(filepath, "w") as f:
    f.write(content)

