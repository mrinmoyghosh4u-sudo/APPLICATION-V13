import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# I need to find where the class closed.
# There must be an extra closing brace somewhere between 1200 and 1660.
# The most likely suspect is the aggressive replacements I did in `loadOptionChain`, `refreshOptionChain`, and `loadHistoricalData`.
# Let's see the definitions of these functions.
lines = content.split('\n')
for i, line in enumerate(lines):
    if "fun clearRecentSearches()" in line:
        print(f"clearRecentSearches at line {i+1}")

brace_count = 0
for i, line in enumerate(lines):
    if i == 0: continue
    
    # Very crude brace counter just to find the culprit
    # ignore commented braces
    l = re.sub(r'//.*', '', line)
    brace_count += l.count('{') - l.count('}')
    
    if brace_count == 0 and i > 50 and i < 1600:
         print(f"Class closed at line {i+1}: {lines[i]}")

