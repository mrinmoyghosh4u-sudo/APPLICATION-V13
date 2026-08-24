import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersAuthManager.kt"
with open(filepath, "r") as f:
    lines = f.readlines()

# The class closing brace is at line 67
# We want to move it to line 108
lines[66] = "" # remove closing brace
# wait, the last line is `}` which is line 108

with open(filepath, "w") as f:
    f.writelines(lines)
