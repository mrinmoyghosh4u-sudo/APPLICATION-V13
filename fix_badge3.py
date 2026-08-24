import os
import glob

files = glob.glob("app/src/main/java/com/example/ui/screens/*.kt")

for filepath in files:
    with open(filepath, "r") as f:
        content = f.read()

    if "isDhanConnected =" in content and "LiveStatusBadge" in content:
        content = content.replace(
            "com.example.ui.components.LiveStatusBadge(\n                    isDhanConnected =", 
            "com.example.ui.components.LiveStatusBadge(\n                    isLive ="
        )
        content = content.replace(
            "com.example.ui.components.LiveStatusBadge(\n                isDhanConnected =", 
            "com.example.ui.components.LiveStatusBadge(\n                isLive ="
        )

    with open(filepath, "w") as f:
        f.write(content)
