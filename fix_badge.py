import os

filepath = "app/src/main/java/com/example/ui/components/CommonComponents.kt"
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace("fun DhanLiveStatusBadge(\n    isDhanConnected: Boolean,", "fun DhanLiveStatusBadge(\n    isDhanConnected: Boolean, // Kept for signature compatibility but ignored")
content = content.replace("color = if (isDhanConnected) ProfitGreen.copy(alpha = 0.15f) else DarkCardSecondary", "color = ProfitGreen.copy(alpha = 0.15f)")
content = content.replace("color = if (isDhanConnected) ProfitGreen.copy(alpha = 0.6f) else DarkCardBorder", "color = ProfitGreen.copy(alpha = 0.6f)")
content = content.replace("color = if (isDhanConnected) ProfitGreen else TextGray", "color = ProfitGreen")
content = content.replace("text = if (isDhanConnected) \"LIVE\" else \"OFFLINE\"", "text = \"LIVE\"")

with open(filepath, 'w') as f:
    f.write(content)
