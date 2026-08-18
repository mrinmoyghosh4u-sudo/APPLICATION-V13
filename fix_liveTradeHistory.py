import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# AlgoDashboard
content = content.replace(
    "    val riskPerTrade by AlgoEngine.riskPerTrade.collectAsState()",
    "    val riskPerTrade by AlgoEngine.riskPerTrade.collectAsState()\n    val liveTradeHistory by AlgoEngine.liveTradeHistory.collectAsState()\n    val paperTradeHistory by AlgoEngine.paperTradeHistory.collectAsState()"
)

# AlgoPerformance
content = content.replace(
    "fun AlgoPerformance() {\n    var selectedTimeframe by remember { mutableStateOf(\"Today\") }",
    "fun AlgoPerformance() {\n    var selectedTimeframe by remember { mutableStateOf(\"Today\") }\n    val liveTradeHistory by AlgoEngine.liveTradeHistory.collectAsState()\n    val paperTradeHistory by AlgoEngine.paperTradeHistory.collectAsState()"
)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
