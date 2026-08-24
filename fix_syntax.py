import os

filepath = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
with open(filepath, "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if line.strip() == "}}":
        new_lines.append("}\n")
    else:
        new_lines.append(line)

with open(filepath, "w") as f:
    f.writelines(new_lines)
