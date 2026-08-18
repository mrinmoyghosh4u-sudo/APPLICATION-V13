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
        
        content = content.replace('Icons.AutoMirrored.Filled.', 'Icons.Filled.')
        content = content.replace('Icons.AutoMirrored.Outlined.', 'Icons.Outlined.')
        content = content.replace('HorizontalHorizontalDivider(', 'Divider(')
        
        # for those that were already HorizontalDivider before my replace?
        # If I replaced Divider( with HorizontalDivider( 
        # Then HorizontalDivider( became HorizontalHorizontalDivider(
        # So I revert it to Divider( but some were legit HorizontalDivider( maybe?
        
        with open(file, 'w') as f:
            f.write(content)
