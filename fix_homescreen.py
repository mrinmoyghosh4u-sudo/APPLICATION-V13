import re

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
try:
    with open(filepath, "r") as f:
        content = f.read()

    content = content.replace("tick?.ltp", "tick?.price")
    content = content.replace("tick!!.ltp", "tick!!.price")
    content = content.replace("tick!!.changePercent", "0.0")
    content = content.replace("tick!!.change", "0.0")

    with open(filepath, "w") as f:
        f.write(content)
except Exception as e:
    print(e)
