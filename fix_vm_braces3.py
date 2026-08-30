import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

lines_to_comment = [1798, 1867]
for i in lines_to_comment:
    if i < len(lines):
        lines[i] = "// " + lines[i]

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
