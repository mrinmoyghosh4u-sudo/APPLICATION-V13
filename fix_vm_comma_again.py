import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace('provider = "UPSTOX",\n            ,\n            createdAt = System.currentTimeMillis(),', 'provider = "UPSTOX",\n            state = randomState,\n            createdAt = System.currentTimeMillis(),')

with open(filepath, "w") as f:
    f.write(content)
