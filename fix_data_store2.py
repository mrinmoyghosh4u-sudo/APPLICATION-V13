import os

filepath = "/app/applet/app/src/main/java/com/example/data/model/MarketDataStore.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("false > 30000", "false")
content = content.replace("fyersHealth = _fyersHealth", "fyersHealth = _fyersHealth") # dummy
content = content.replace("_fyersHealth.value", "fyersHealth.value")
# Wait, let's just make sure `private val _fyersHealth` exists in MarketDataStore.kt
if "private val _fyersHealth" not in content:
    content = content.replace("val fyersHealth =", "private val _fyersHealth = kotlinx.coroutines.flow.MutableStateFlow(\"DISCONNECTED\")\n    val fyersHealth = _fyersHealth.asStateFlow()")
    content = content.replace("fyersHealth.value", "_fyersHealth.value")
    # also fix the other _fyersHealth reference inside checkHealth
    content = content.replace("val _fyersHealth", "val _fyersHealth")

with open(filepath, "w") as f:
    f.write(content)

