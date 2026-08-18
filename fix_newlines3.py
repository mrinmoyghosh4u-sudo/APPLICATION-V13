import re
with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

content = re.sub(r'(package com\.example\.ui\.screens)(import)', r'\1\n\2', content)
content = re.sub(r'([A-Za-z0-9_*])(import androidx)', r'\1\n\2', content)
content = re.sub(r'([A-Za-z0-9_*])(import com)', r'\1\n\2', content)
content = re.sub(r'(MainViewModel)(val AlgoTradeHistory)', r'\1\n\n\2', content)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
