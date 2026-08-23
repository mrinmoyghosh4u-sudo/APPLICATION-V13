import os

filepath = "app/src/main/java/com/example/ui/components/CommonComponents.kt"
with open(filepath, 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "isDhanConnected" in line and "ProfitGreen else TextGray" in line:
        if i != 127 and i != 134: # 127 is background, 134 is text color in DhanLiveStatusBadge
            lines[i] = line.replace("if (isDhanConnected) ProfitGreen else TextGray", "ProfitGreen")

with open(filepath, 'w') as f:
    f.writelines(lines)
