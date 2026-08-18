import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

pattern = r'val activeHistoryForRate = if \(tradingMode == "AUTO TRADING"\) liveTradeHistory else paperTradeHistory'
replacement = """val liveHistoryVal = AlgoEngine.liveTradeHistory.collectAsState().value
                            val paperHistoryVal = AlgoEngine.paperTradeHistory.collectAsState().value
                            val activeHistoryForRate = if (isAutoTrading) liveHistoryVal else paperHistoryVal"""

content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
