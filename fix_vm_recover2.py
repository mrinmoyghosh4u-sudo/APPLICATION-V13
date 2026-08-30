import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# I removed all comments previously that did not start with // 1. etc... but I accidentally stripped the // from lines that did not have 1. 2. 3.
# Wait, I did `line.strip().startswith("// 1.")` but in my previous script I did `content = content.replace("// ", "")`
# Oh god, I removed `// ` globally in the first script!

# To fix this, I need to restore the whole MainViewModel.kt from a known good state, but I don't have git.
# Let's write a python script that will use my chat history to deduce how to fix the syntax errors.
# Actually I will just fix all the lines manually.

import fileinput

lines_to_comment = [58, 196, 820, 827, 892, 903, 909, 989, 998, 1010, 1034, 1106, 1209, 1228]

lines = content.split('\n')
for i in lines_to_comment:
    lines[i] = "// " + lines[i]

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
