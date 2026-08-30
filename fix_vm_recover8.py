import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# I see what happened. In `fix_vm2.py` I did:
# content = re.sub(
#    r'private suspend fun loadOptionChain\(sym: String\) \{[^}]*\}',
#    'private suspend fun loadOptionChain(sym: String) { _optionChainState.value = emptyList() }',
#    content, flags=re.DOTALL
# )
# `[^}]*` only matches until the FIRST `}`. But `loadOptionChain` had nested braces! So it left extra `}` hanging around, which closed the class!
# Let's count braces properly.

lines = content.split('\n')
brace_count = 0
for i, line in enumerate(lines):
    l = line.split('//')[0] # remove comments
    brace_count += l.count('{') - l.count('}')
    if brace_count == 0 and i > 50:
         print(f"Brace count hit 0 at {i+1}: {line}")
         
