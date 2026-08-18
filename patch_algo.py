import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# 1. Remove AUTO_TRADING from enum
content = re.sub(r',\n\s+AUTO_TRADING', '', content)

# 2. Remove the when clause
content = re.sub(r'\s+AlgoScreenState\.AUTO_TRADING -> AutoTrading\(\)', '', content)

# 3. Remove the ActionCard
card_regex = r'\s+ActionCard\(\n\s+title = "Auto Trading",\n\s+icon = Icons\.Default\.SmartToy,\n\s+subtitle = "Live Execution Settings",\n\s+onClick = \{\n\s+onNavigate\(AlgoScreenState\.AUTO_TRADING\)\n\s+\}\n\s+\)'
content = re.sub(card_regex, '', content, flags=re.MULTILINE)

# 4. Remove the AutoTrading composable function
bot_func_regex = r'@Composable\s*fun AutoTrading\(\)\s*\{.*?(?=@Composable|$)'
content = re.sub(bot_func_regex, '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
