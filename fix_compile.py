import re

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

# Fix UserProfileEntity exchanges
content = re.sub(r'exchanges\s*=\s*data\.exchanges\s*\?\:\s*emptyList\(\)', '', content)

# Fix netAmount
content = content.replace("data.net?.toString()Amount", "data.net?.toString()")
content = content.replace("data.netAmount", "data.net?.toString()")

# quantity in modify order
content = content.replace("quantity = newQty,", "quantity = newQty.toString(),")

# avgPrice in portfolio holding
content = content.replace("item.averagePrice?.toString()price", "item.averagePrice?.toString()")
content = content.replace("item.averageprice", "item.averagePrice?.toString()")

open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)
