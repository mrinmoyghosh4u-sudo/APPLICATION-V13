import os
import re

filepath = "/app/applet/app/src/main/java/com/example/ui/screens/MarketScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Let's find out what function was mangled
start = content.find("val detectedBase = baseCandidate ?: when {")
end = content.find("private fun generateSearchInstrumentPool")

print(content[start-200:start+200])

