import re

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    'onConnect = { onSwitchBroker("Dhan") }',
    'onConnect = { onReconnectBroker("Dhan") }'
)

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(content)

