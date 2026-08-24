import os
import re

# Fix MarketScreen.kt
filepath = "/app/applet/app/src/main/java/com/example/ui/screens/MarketScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# We will remove generateExchangeOptionMovers and generateRealisticCandles
start1 = content.find("private fun generateRealisticCandles")
if start1 != -1:
    end1 = content.find("private data class MarketMoverCardData", start1)
    if end1 != -1:
        content = content[:start1] + content[end1:]

start2 = content.find("private fun generateExchangeOptionMovers")
if start2 != -1:
    end2 = content.find("}", content.find("}", content.find("}", start2)+1)+1)+1
    content = content[:start2] + "\n\n" + content[end2:]

# Also remove the actual call to generateRealisticCandles
content = content.replace("generateRealisticCandles(basePrice, selectedTimeframe)", "emptyList() // Replaced fake candles with empty list")

with open(filepath, "w") as f:
    f.write(content)

