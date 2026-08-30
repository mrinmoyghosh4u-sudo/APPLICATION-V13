import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    lines = f.read().split('\n')

brace_count = 0
for i, line in enumerate(lines):
    # extremely naive brace counting
    l = re.sub(r'".*?"', '', line)
    l = l.split('//')[0]
    brace_count += l.count('{') - l.count('}')
    
    if "fun " in line and not line.strip().startswith("//"):
        if brace_count != 2: # wait, `fun` has `{` on the same line, so if it's `fun foo() {`, it will be 2 after the line, 1 before.
             # let's just print brace_count BEFORE the line is processed
             pass

brace_count = 0
for i, line in enumerate(lines):
    l = re.sub(r'".*?"', '', line)
    l = l.split('//')[0]
    
    # check before adding this line's braces
    if (" fun " in line or line.strip().startswith("fun ")) and not line.strip().startswith("//") and " = " not in line:
        if brace_count != 1:
            print(f"Line {i+1}: Brace count is {brace_count} before function: {line.strip()}")
            
    brace_count += l.count('{') - l.count('}')

