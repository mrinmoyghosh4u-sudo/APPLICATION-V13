import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

import re
content = re.sub(r'(\s+)fun onOpen\(', r'\1override fun onOpen(', content)
content = re.sub(r'(\s+)fun onMessage\(', r'\1override fun onMessage(', content)
content = re.sub(r'(\s+)fun onClosed\(', r'\1override fun onClosed(', content)
content = re.sub(r'(\s+)fun onFailure\(', r'\1override fun onFailure(', content)

with open(filepath, "w") as f:
    f.write(content)
