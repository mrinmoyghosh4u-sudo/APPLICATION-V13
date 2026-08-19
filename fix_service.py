import re

content = open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt').read()
lines = content.split('\n')

# I'll just write a script that counts indentation to insert braces!
out = []
for line in lines:
    out.append(line)

# Let's write the whole file content out as a string!
