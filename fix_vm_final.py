import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Let's brutally comment out these lines
lines_to_kill = [1175, 1180, 1181, 1204, 1205, 1206, 1222, 1224, 1282, 1283, 1284, 1319, 1320, 1321, 1344, 1346, 1385, 1386, 1387, 1760, 1763]

lines = content.split('\n')
for num in lines_to_kill:
    lines[num-1] = "// " + lines[num-1]

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
