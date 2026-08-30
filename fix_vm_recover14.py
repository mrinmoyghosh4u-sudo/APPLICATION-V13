import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')
for i in [1186, 1190, 1191, 1255, 1258, 1387, 1391, 1793, 1794, 1796, 1869]:
    if i < len(lines):
        print(f"Line {i+1}: {lines[i]}")

