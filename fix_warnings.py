import os

files = [
    'app/src/main/java/com/example/ui/components/TermsAndPolicyDialog.kt',
    'app/src/main/java/com/example/ui/screens/AISignalsScreen.kt',
    'app/src/main/java/com/example/ui/screens/AlgoScreen.kt',
    'app/src/main/java/com/example/ui/screens/DisclaimerScreen.kt',
    'app/src/main/java/com/example/ui/screens/HomeScreen.kt',
    'app/src/main/java/com/example/ui/screens/OrdersScreen.kt',
    'app/src/main/java/com/example/ui/screens/TelegramSettingsScreen.kt'
]

for file in files:
    if os.path.exists(file):
        with open(file, 'r') as f:
            content = f.read()
        
        content = content.replace('Icons.Filled.TrendingUp', 'Icons.AutoMirrored.Filled.TrendingUp')
        content = content.replace('Icons.Filled.TrendingDown', 'Icons.AutoMirrored.Filled.TrendingDown')
        content = content.replace('Icons.Outlined.ListAlt', 'Icons.AutoMirrored.Outlined.ListAlt')
        content = content.replace('Icons.Outlined.ShowChart', 'Icons.AutoMirrored.Outlined.ShowChart')
        content = content.replace('Icons.Filled.ArrowForward', 'Icons.AutoMirrored.Filled.ArrowForward')
        content = content.replace('Icons.Filled.ReceiptLong', 'Icons.AutoMirrored.Filled.ReceiptLong')
        content = content.replace('Icons.Filled.ArrowBack', 'Icons.AutoMirrored.Filled.ArrowBack')
        content = content.replace('Icons.Filled.Send', 'Icons.AutoMirrored.Filled.Send')
        content = content.replace('Divider(', 'HorizontalDivider(')
        
        with open(file, 'w') as f:
            f.write(content)
