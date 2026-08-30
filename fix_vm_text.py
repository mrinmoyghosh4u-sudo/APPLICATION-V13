import re
filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("Try to fetch sequentially or concurrently", "// Try to fetch sequentially or concurrently")

with open(filepath, "w") as f:
    f.write(content)
