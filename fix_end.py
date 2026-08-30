import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# remove all the commented braces at the end
content = re.sub(r'(// \}\n?)+', '', content)
content = re.sub(r'\}\s*$', '}\n', content) # Ensure newline

if content.strip().endswith("}"):
    # it has a closing brace at the end. Wait, let's just rewrite the end cleanly.
    pass

lines = content.split('\n')
# remove any trailing empty lines or commented braces
while len(lines) > 0 and (lines[-1].strip() == "" or lines[-1].strip().startswith("//")):
    lines.pop()

# lines[-1] is now the last non-empty line
# Add the class closing brace
lines.append("}")

with open(filepath, "w") as f:
    f.write('\n'.join(lines) + '\n')
