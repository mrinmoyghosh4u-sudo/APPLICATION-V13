import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace('provider = "FYERS",\n            ,\n            createdAt = System.currentTimeMillis(),\n            consumed = false', 'provider = "FYERS",\n            state = randomState,\n            createdAt = System.currentTimeMillis(),\n            consumed = false')
content = content.replace('provider = "DHAN",\n            ,\n            createdAt = System.currentTimeMillis(),\n            consumed = false', 'provider = "DHAN",\n            state = randomState,\n            createdAt = System.currentTimeMillis(),\n            consumed = false')
content = content.replace('provider = "UPSTOX",\n            createdAt = System.currentTimeMillis(),\n            consumed = false', 'provider = "UPSTOX",\n            state = randomState,\n            createdAt = System.currentTimeMillis(),\n            consumed = false')

with open(filepath, "w") as f:
    f.write(content)
