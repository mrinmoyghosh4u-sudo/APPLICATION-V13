import re

filepath = "app/src/main/java/com/example/util/diagnostic/SelfDiagnosticEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("lastTickTimestamp", "lastUpdate")

with open(filepath, "w") as f:
    f.write(content)
