import os

filepath = "app/src/main/java/com/example/data/network/BrokerAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
    "    private suspend fun validateFyersSession()\n            validateAngelOneSession() {",
    "    private suspend fun validateAngelOneSession() {"
)

with open(filepath, "w") as f:
    f.write(content)
