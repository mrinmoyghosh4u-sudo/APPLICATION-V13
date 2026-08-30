import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# I messed up braces. I commented out the `quotesRes.getOrNull()?.let { quotes ->` (1204) and `if (quotes.isNotEmpty()) {` (1205) but left their closing braces `}` on 1207 and 1208.
# Same for `histRes.getOrNull()?.let { candles ->` (1222) and its closing brace on 1226.

lines = content.split('\n')
for i in [1206, 1207, 1225]:
    lines[i] = "// " + lines[i]

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
