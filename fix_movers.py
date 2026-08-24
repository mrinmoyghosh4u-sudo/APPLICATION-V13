import os

filepath = "/app/applet/app/src/main/java/com/example/ui/screens/MarketScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("when (selectedCategory) {\n            \"Top Gainers\" -> mapped.filter { it.changePct >= 0 }.sortedByDescending { it.changePct }\n            \"Top Losers\" -> mapped.filter { it.changePct < 0 }.sortedBy { it.changePct }\n            \"High Volume\" -> mapped.sortedByDescending { it.volume }\n            \"High OI Chg\" -> mapped.sortedByDescending { kotlin.math.abs(it.oiChangePct) }\n            else -> mapped\n        }", 
"when (selectedCategory) {\n            \"Top Gainers\" -> combinedItems.filter { it.changePct >= 0 }.sortedByDescending { it.changePct }\n            \"Top Losers\" -> combinedItems.filter { it.changePct < 0 }.sortedBy { it.changePct }\n            \"High Volume\" -> combinedItems.sortedByDescending { it.volume }\n            \"High OI Chg\" -> combinedItems.sortedByDescending { kotlin.math.abs(it.oiChangePct) }\n            else -> combinedItems\n        }")

with open(filepath, "w") as f:
    f.write(content)

