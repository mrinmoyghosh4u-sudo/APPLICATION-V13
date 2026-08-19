import re
content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

# Replace both occurrences carefully
content = re.sub(r'avgPrice = item\.averagePrice\?\.toString\(\)\?\.toDoubleOrNull\(\)\s*\?\:\s*0\.0', 'avgPrice = 0.0', content)

open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)
