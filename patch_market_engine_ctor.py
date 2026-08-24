import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/MarketDataEngine.kt"

with open(filepath, "r") as f:
    content = f.read()

content = content.replace("val nseFeedService: NseAuthorizedFeedService,", "")
content = content.replace("val nseFeedService: NseAuthorizedFeedService? = null,", "")

with open(filepath, "w") as f:
    f.write(content)
