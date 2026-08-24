import os
import re

# 1. Fix MarketDataStore.kt completely
filepath = "/app/applet/app/src/main/java/com/example/data/model/MarketDataStore.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("_yahooHealth", "_unusedHealth")
content = content.replace("yahooHealth", "unusedHealth")
with open(filepath, "w") as f:
    f.write(content)

# 2. Fix DiagnosticsScreen.kt
filepath = "/app/applet/app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Just remove all DiagnosticItem calls that have NSE or Yahoo in them.
lines = content.split('\n')
new_lines = []
skip = False
for line in lines:
    if "DiagnosticItem" in line and ("NSE" in line or "Yahoo" in line or "nseHealth" in line or "nseState" in line):
        continue
    new_lines.append(line)

with open(filepath, "w") as f:
    f.write('\n'.join(new_lines))

