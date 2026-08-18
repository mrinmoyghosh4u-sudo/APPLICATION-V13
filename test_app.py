import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'r') as f:
    content = f.read()

print("OptionChainTabContent" in content)
