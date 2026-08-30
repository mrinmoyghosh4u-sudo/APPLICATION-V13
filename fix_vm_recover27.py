import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

lines_to_uncomment = [1175, 1180, 1181, 1183, 1184, 1185]
for i in lines_to_uncomment:
    if i < len(lines):
        lines[i] = lines[i].replace("// ", "")

# I also need to uncomment fetchOptionChain and markAllNotificationsAsRead where they are declared
for i, line in enumerate(lines):
    if "private fun fetchOptionChain()" in line:
        print(f"fetchOptionChain is at {i+1}")
    if "fun markAllNotificationsAsRead()" in line:
        print(f"markAllNotificationsAsRead is at {i+1}")

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
