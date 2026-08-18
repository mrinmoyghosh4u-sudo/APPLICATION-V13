with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("AlgoEngine.liveTradeHistory.value", "AlgoEngine.liveTradeHistory.collectAsState().value")
content = content.replace("AlgoEngine.paperTradeHistory.value", "AlgoEngine.paperTradeHistory.collectAsState().value")

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
