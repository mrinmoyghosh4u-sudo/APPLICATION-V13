import re
with open('app/src/main/java/com/example/data/network/AISignalGenerator.kt', 'r') as f:
    content = f.read()

content = content.replace("if (ltp <= 0) return null", "if (ltp <= 0) return@forEach")

with open('app/src/main/java/com/example/data/network/AISignalGenerator.kt', 'w') as f:
    f.write(content)
