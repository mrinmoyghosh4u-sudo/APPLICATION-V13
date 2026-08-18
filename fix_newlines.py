import re

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'r') as f:
    content = f.read()

# I will just replace the specific broken lines.
# Wait, let's just do a string replace for the broken ones.
content = content.replace(
'''"Direct end-to-end token encryption • No password logging
Your data is 100% secure with bank-grade protection."''',
'''"Direct end-to-end token encryption • No password logging\\nYour data is 100% secure with bank-grade protection."'''
)

content = content.replace('''"256-BIT
ENCRYPTED"''', '''"256-BIT\\nENCRYPTED"''')
content = content.replace('''"REAL TIME
QUOTES"''', '''"REAL TIME\\nQUOTES"''')
content = content.replace('''"ALGO
EXECUTION"''', '''"ALGO\\nEXECUTION"''')
content = content.replace('''"24/7
SUPPORT"''', '''"24/7\\nSUPPORT"''')

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'w') as f:
    f.write(content)
