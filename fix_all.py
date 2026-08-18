with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    text = f.read()

import re
# Remove duplicate onNavigateToDiagnostics
text = re.sub(r'onNavigateToDiagnostics:\s*\(\)\s*->\s*Unit\s*=\s*\{\},\s*onNavigateToDiagnostics:\s*\(\)\s*->\s*Unit\s*=\s*\{\},', 'onNavigateToDiagnostics: () -> Unit = {},', text)

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'r') as f:
    text2 = f.read()

text2 = text2.replace('indexSymbol', 'selectedOptionIndex')

with open('app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'w') as f:
    f.write(text2)
