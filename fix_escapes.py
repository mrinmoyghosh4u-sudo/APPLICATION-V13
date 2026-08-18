with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('\\n', '\n')
text = text.replace('\\(', '(')
text = text.replace('\\)', ')')
text = text.replace('\\.', '.')
text = text.replace('\\{', '{')
text = text.replace('\\}', '}')

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(text)
