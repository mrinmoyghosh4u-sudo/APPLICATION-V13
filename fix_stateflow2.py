import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# Replace those inside MyStrategies function
# First find the function
content = content.replace("val strategies by AlgoEngine.strategies.collectAsState()", "val strategies by AlgoEngine.strategies.collectAsState()\n    val liveTradeHistory by AlgoEngine.liveTradeHistory.collectAsState()\n    val paperTradeHistory by AlgoEngine.paperTradeHistory.collectAsState()")

# And then replace collectAsState().value with the variable name
content = content.replace("AlgoEngine.liveTradeHistory.collectAsState().value", "liveTradeHistory")
content = content.replace("AlgoEngine.paperTradeHistory.collectAsState().value", "paperTradeHistory")

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
