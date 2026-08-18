import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'\s*QuickActionCard\("AUTO TRADING", Icons\.Outlined\.Bolt, Modifier\.weight\(1f\)\) \{\s*onNavigate\(AlgoScreenState\.AUTO_TRADING\)\s*\}',
    r'',
    content
)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
