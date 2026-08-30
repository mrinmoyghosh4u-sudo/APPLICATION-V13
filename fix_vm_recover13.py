import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# I am seeing lines missing '// ' because I deleted them initially.
lines_to_fix = [415, 439, 730, 757, 834, 920, 938, 973, 981, 1007, 1041, 1048, 1054, 1064, 1075, 1200]
lines = content.split('\n')

for i in lines_to_fix:
    if i < len(lines):
        lines[i] = "// " + lines[i]
        
# also lines 1175, 1180, 1181, 1204, 1205, 1206, 1222, 1224, 1282, 1283, 1284, 1319, 1320, 1321, 1344, 1346, 1385, 1386, 1387, 1760, 1763 from before that I might have messed up due to brace count mismatch. 
# actually let's just comment out everything listed here manually.
for i in [1174, 1179, 1180, 1222, 1383, 1386, 1792]:
    if i < len(lines):
        lines[i] = "// " + lines[i]

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
