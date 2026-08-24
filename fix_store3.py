import os
import re

filepath = "/app/applet/app/src/main/java/com/example/data/model/MarketDataStore.kt"
with open(filepath, "r") as f:
    content = f.read()

# I will use regex to remove everything from `// TradeSmart Health` down to `_unusedHealth.value = "DELAYED"\n                }`
pattern = re.compile(r'// TradeSmart Health.*_unusedHealth\.value = "DELAYED"\n\s*\}', re.DOTALL)
content = pattern.sub('', content)

# I will also fix _fyersHealth if it's missing
if "val _fyersHealth" not in content and "var _fyersHealth" not in content:
    content = content.replace("val fyersHealth =", 'private val _fyersHealth = kotlinx.coroutines.flow.MutableStateFlow("DISCONNECTED")\n    val fyersHealth =')
    content = content.replace("fyersHealth.value = health", "_fyersHealth.value = health")

with open(filepath, "w") as f:
    f.write(content)
