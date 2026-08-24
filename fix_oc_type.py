import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("callVolume = (contract.volume ?: 0.0).toLong(),", 'callVolume = (contract.volume ?: 0.0).toLong().toString(),')
content = content.replace("putVolume = (contract.volume ?: 0.0).toLong(),", 'putVolume = (contract.volume ?: 0.0).toLong().toString(),')

content = content.replace("callChangePct = contract.chp ?: 0.0,", "")
content = content.replace("putChangePct = contract.chp ?: 0.0,", "")

with open(filepath, "w") as f:
    f.write(content)

