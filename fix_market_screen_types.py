import re

filepath = "app/src/main/java/com/example/ui/screens/MarketScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("val combinedItems = remember(marketDataMap, selectedExchange) {", "val combinedItems: List<MarketMoverCardData> = remember(marketDataMap, selectedExchange) {")
content = content.replace("val moverItems = remember(combinedItems, selectedCategory) {", "val moverItems: List<MarketMoverCardData> = remember(combinedItems, selectedCategory) {")

with open(filepath, "w") as f:
    f.write(content)
