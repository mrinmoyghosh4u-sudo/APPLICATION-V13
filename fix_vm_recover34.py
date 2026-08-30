import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

for i in range(1870, len(lines)):
    lines[i] = "// " + lines[i]

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
