import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

lines_to_uncomment = [1256, 1257, 1258, 1259]
for i in lines_to_uncomment:
    if i < len(lines):
        lines[i] = lines[i].replace("// ", "")

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
