import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

for i in range(1868, len(lines)):
    lines[i] = "// " + lines[i]

content = '\n'.join(lines) + "\n}\n"

with open(filepath, "w") as f:
    f.write(content)
