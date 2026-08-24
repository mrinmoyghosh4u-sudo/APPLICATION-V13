import os
import re

filepath = "/app/applet/app/src/main/java/com/example/data/model/MarketDataStore.kt"

with open(filepath, "r") as f:
    content = f.read()

# Remove YAHOO from constants
content = re.sub(r'const val YAHOO = "YAHOO"\n', '', content)
# Remove NSE from constants if we want, but let's keep it to avoid too many breakages unless requested. The prompt says "Remove Yahoo Finance from the production market-data path."
# We'll just patch MarketDataStore to not mention YAHOO.

content = content.replace("MarketDataSourceNames.YAHOO", '"REFERENCE"')
content = content.replace("val _yahooHealth", "val _unusedHealth")
content = content.replace("val yahooHealth", "val unusedHealth")

with open(filepath, "w") as f:
    f.write(content)

