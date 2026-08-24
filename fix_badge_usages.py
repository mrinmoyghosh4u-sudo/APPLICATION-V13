import os
import glob

files = glob.glob("app/src/main/java/com/example/ui/screens/*.kt")

for filepath in files:
    with open(filepath, "r") as f:
        content = f.read()

    # Replace the component call
    content = content.replace("com.example.ui.components.DhanLiveStatusBadge", "com.example.ui.components.LiveStatusBadge")
    content = content.replace("isDhanConnected = isDhanConnected", "isLive = isDhanConnected, dataSource = \"\"")

    with open(filepath, "w") as f:
        f.write(content)
