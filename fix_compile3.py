import re

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

content = content.replace("item.avgPrice?.toString()?.toString()?.toDoubleOrNull() ?: 0.0", "item.averagePrice?.toString()?.toDoubleOrNull() ?: 0.0")

# For positions, there is no avgPrice, let's use buyAvgPrice or sellAvgPrice based on qty
content = content.replace("avgPrice = item.averagePrice?.toString()?.toDoubleOrNull() ?: 0.0", "avgPrice = item.buyAvgPrice?.toString()?.toDoubleOrNull() ?: 0.0")

# Re-read position avgPrice specifically
content = re.sub(r'avgPrice = item.avgPrice\?\.toString\(\)\?\.toString\(\)\?\.toDoubleOrNull\(\)\s*\?\:\s*0\.0,', 'avgPrice = 0.0,', content)

open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)
