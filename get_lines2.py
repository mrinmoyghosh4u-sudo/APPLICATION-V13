import re
filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    lines = f.read().split('\n')
for i in range(1270, 1360):
    print(f"{i+1}: {lines[i]}")
