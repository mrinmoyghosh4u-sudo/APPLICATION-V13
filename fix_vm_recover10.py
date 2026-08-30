import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

for i in range(1340, 1370):
    print(f"Line {i+1}: {lines[i]}")

