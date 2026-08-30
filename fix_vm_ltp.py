import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Replace live.ltp with live.price
content = content.replace("live.ltp", "live.price")

# Replace live.change with 0.0 (since it doesn't exist, or we can calculate it)
# We can just keep item.change
content = content.replace("live.changePercent", "item.changePercent")
content = content.replace("live.change", "item.change")

with open(filepath, "w") as f:
    f.write(content)
