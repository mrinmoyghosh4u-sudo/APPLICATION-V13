with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(
    r'\s*Spacer\(modifier = Modifier\.height\(10\.dp\)\)\s*// --- M\.STOCK LOGIN BUTTON \(SECONDARY DATA\) ---[\s\S]*?onClick = \{ onConnectBroker\("m\.Stock"\) \}\s*\)',
    '',
    content
)

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'w') as f:
    f.write(content)

