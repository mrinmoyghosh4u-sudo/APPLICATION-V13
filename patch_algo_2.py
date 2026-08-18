import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# 1. Remove isAutoTrading state collection
content = re.sub(r'\s*val isAutoTrading by AlgoEngine\.isAutoTradingEnabled\.collectAsState\(\)', '', content)

# 2. Update the Text to not use isAutoTrading
content = re.sub(r'if \(isAutoTrading\) "AUTO" else "PAPER"', '"LIVE SIGNALS"', content)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
