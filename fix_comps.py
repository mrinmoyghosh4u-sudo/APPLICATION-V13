import os
import glob

files = glob.glob("app/src/main/java/com/example/ui/screens/*.kt")

for filepath in files:
    with open(filepath, "r") as f:
        content = f.read()

    # Specifically fix MarketScreen
    if "MarketScreen.kt" in filepath:
        content = content.replace("MarketHeaderSection(\n                isLive = isDhanConnected, dataSource = \"\",", "MarketHeaderSection(\n                isDhanConnected = isDhanConnected,")
    
    # Fix PortfolioScreen
    if "PortfolioScreen.kt" in filepath:
        content = content.replace("PortfolioHeaderSection(\n                isLive = isDhanConnected, dataSource = \"\",", "PortfolioHeaderSection(\n                isDhanConnected = isDhanConnected,")

    with open(filepath, "w") as f:
        f.write(content)
