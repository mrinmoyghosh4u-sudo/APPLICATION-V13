import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# Replace "import " with "\nimport " if there's no newline before it
content = re.sub(r'(?<!\n)import ', '\nimport ', content)
content = re.sub(r'(?<!\n)enum class ', '\n\nenum class ', content)
content = re.sub(r'(?<!\n)@Composable', '\n\n@Composable', content)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
