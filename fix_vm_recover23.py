import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content + "\n}\n"

with open(filepath, "w") as f:
    f.write(content)
