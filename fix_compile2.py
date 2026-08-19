import re

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

# Fix netAmount
content = content.replace("res.body()?.data?.netAmount?.toDoubleOrNull()", "res.body()?.data?.net?.toString()?.toDoubleOrNull()")

# Fix AngelModifyOrderRequest quantity being passed Int (newQty.toString() was failing because it was replaced previously, but let's check)
content = re.sub(r'quantity\s*=\s*newQty\s*\n', 'quantity = newQty.toString()\n', content)
content = re.sub(r'quantity\s*=\s*newQty\s*,', 'quantity = newQty.toString(),', content)

# Fix avgPrice
content = content.replace("avgPrice = item.averagePrice?.toString()?.toDoubleOrNull() ?: 0.0", "avgPrice = item.avgPrice?.toString()?.toDoubleOrNull() ?: item.averagePrice?.toString()?.toDoubleOrNull() ?: 0.0")
content = content.replace("item.averageprice", "item.averagePrice") # Just in case

open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)
