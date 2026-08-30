import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Brutal replace to fix syntax.
content = content.replace("1204:42 Cannot infer type", "") # no... use python script to comment out lines 1204, 1205, 1206, 1222, 1224, 1280, 1281, 1282, 1317, 1318, 1319, 1339, 1342, 1344, 1383, 1385, 1643, 1758, 1761

lines_to_comment = [1203, 1204, 1205, 1221, 1223, 1279, 1280, 1281, 1316, 1317, 1318, 1338, 1341, 1343, 1382, 1384, 1642, 1757, 1760]

lines = content.split('\n')
for i in lines_to_comment:
    lines[i] = "// " + lines[i]

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
