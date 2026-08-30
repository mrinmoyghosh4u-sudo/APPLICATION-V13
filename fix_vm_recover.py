import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Syntax error at line 59: `e: file:///app/applet/app/src/main/java/com/example/viewmodel/MainViewModel.kt:59:5 Expecting member declaration`
# Syntax error at 197: `e: file:///app/applet/app/src/main/java/com/example/viewmodel/MainViewModel.kt:197:55 Unexpected tokens (use ';' to separate expressions on the same line)`

lines = content.split('\n')
for i in [58, 196, 820, 827, 892, 903, 909, 989, 998, 1010, 1034, 1106, 1209, 1228]:
    print(f"Line {i+1}: {lines[i]}")

