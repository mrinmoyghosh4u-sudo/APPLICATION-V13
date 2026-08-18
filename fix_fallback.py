import re

with open('app/src/main/java/com/example/data/network/AISignalGenerator.kt', 'r') as f:
    content = f.read()

content = content.replace("val ltp = if (item.ltp > 0) item.ltp else 180.0", "val ltp = item.ltp\n            if (ltp <= 0) return null")

with open('app/src/main/java/com/example/data/network/AISignalGenerator.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/screens/MarketScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("val ltp = currentSymbolItem?.ltp ?: 185.00", "val ltp = currentSymbolItem?.ltp ?: 0.0")
content = content.replace("val baseLtp = currentSymbolItem?.ltp ?: 185.00", "val baseLtp = currentSymbolItem?.ltp ?: 0.0")

with open('app/src/main/java/com/example/ui/screens/MarketScreen.kt', 'w') as f:
    f.write(content)
