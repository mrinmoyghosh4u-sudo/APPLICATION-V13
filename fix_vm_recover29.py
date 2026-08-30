import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

for i in range(1660, 1680):
    if i < len(lines):
        print(f"Line {i+1}: {lines[i]}")

