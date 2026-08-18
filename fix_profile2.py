with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'onNavigateToDiagnostics: () -> Unit = {},\n    onNavigateToDiagnostics: () -> Unit = {},',
    'onNavigateToDiagnostics: () -> Unit = {},'
)

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(text)
