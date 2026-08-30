import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

lines[1793] = "// " + lines[1793]
lines[1796] = "// " + lines[1796]

content = '\n'.join(lines) + "\n}\n"

with open(filepath, "w") as f:
    f.write(content)
