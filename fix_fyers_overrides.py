import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Remove override from methods in FyersMarketDataService
content = re.sub(r'override\s+(suspend\s+)?fun\s+', r'\1fun ', content)
content = re.sub(r'override\s+val\s+', r'val ', content)

with open(filepath, "w") as f:
    f.write(content)

