import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

lines = content.split('\n')

# Wait, `sourceExpiries` definition block has been commented out (1344 to 1347), leaving `}` at 1348 active.
# This causes an extra closing brace!
# Let's comment out 1348.
lines[1347] = "// " + lines[1347]

# I also need to provide `sourceExpiries` so the code compiles.
lines[1346] = "val sourceExpiries = emptyList<String>()"

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
