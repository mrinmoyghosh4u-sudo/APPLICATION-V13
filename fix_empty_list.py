import os

filepath = "/app/applet/app/src/main/java/com/example/ui/screens/MarketScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
    "emptyList() // Replaced fake candles with empty list",
    "emptyList<com.example.ui.components.CandleData>() // Replaced fake candles with empty list"
)

with open(filepath, "w") as f:
    f.write(content)

