with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Balance brackets correctly
open_brackets = content.count('{')
close_brackets = content.count('}')

if open_brackets > close_brackets:
    content += '}' * (open_brackets - close_brackets)
elif close_brackets > open_brackets:
    for _ in range(close_brackets - open_brackets):
        content = content[:content.rfind('}')]
        
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
