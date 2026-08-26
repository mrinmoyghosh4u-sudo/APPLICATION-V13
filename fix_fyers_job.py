import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

content = re.sub(r'restPollingJob\?\.cancel\(\)', '', content)
content = re.sub(r'restPollingJob = null', '', content)

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
