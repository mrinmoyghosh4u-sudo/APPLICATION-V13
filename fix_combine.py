import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(") { providerState, profile, statuses ->", ") { profile, statuses ->")
content = content.replace("providerState.live && !providerState.stale && isDhanConnected", "isDhanConnected")

with open(filepath, "w") as f:
    f.write(content)
