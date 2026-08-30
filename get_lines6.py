import re
filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    lines = f.read().split('\n')
for i in range(1170, 1195):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}")
