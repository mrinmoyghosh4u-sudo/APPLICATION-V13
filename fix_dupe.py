import os
import re

filepath = "/app/applet/app/src/main/java/com/example/ui/screens/MarketScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Let's count them
count = content.count("data class MarketMoverCardData")
print(f"Found {count} MarketMoverCardData")

if count > 1:
    # Just keep the first one
    parts = content.split("data class MarketMoverCardData")
    # Actually wait, they might have different bodies. 
    # Let's use regex to remove the last one safely.
    pattern = r"data class MarketMoverCardData\s*\([^)]*\)"
    matches = list(re.finditer(pattern, content))
    if len(matches) > 1:
        last_match = matches[-1]
        content = content[:last_match.start()] + content[last_match.end():]
        with open(filepath, "w") as f:
            f.write(content)
        print("Removed last duplicate")
