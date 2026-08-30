import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')
brace_count = 0
for i, line in enumerate(lines):
    # ignore string literals for brace counting to be safe, very simple approximation
    l = re.sub(r'".*?"', '', line)
    l = l.split('//')[0]
    brace_count += l.count('{') - l.count('}')
    if brace_count == 0 and i > 50:
         print(f"Brace count hit 0 at {i+1}: {line}")
         break

print(f"Final brace count: {brace_count}")
