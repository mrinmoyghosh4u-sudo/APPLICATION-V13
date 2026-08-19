import re

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

# Fix UserProfileEntity instantiation
content = re.sub(r'clientId\s*=\s*data\.clientCode\?\.toString\(\)\s*\?\:\s*"",\n', '', content)
content = re.sub(r'mobile\s*=\s*data\.mobileNo\?\.toString\(\)\s*\?\:\s*"",\n', '', content)
content = re.sub(r'exchanges\s*=\s*data\.exchanges\s*\?\:\s*emptyList\(\),\n', '', content)

# Fix netAmount to net
content = content.replace("data.netAmount", "data.net?.toString()")

# Fix OrderEntity expects String but gets Any?
content = content.replace("orderId = item.orderId ?: \"\",", "orderId = item.orderId?.toString() ?: \"\",")
content = content.replace("orderType = item.orderType ?: \"\",", "orderType = item.orderType?.toString() ?: \"\",")

# Fix squareOff and stopLoss in placeOrder
content = re.sub(r'squareOff\s*=\s*"0",\n', '', content)
content = re.sub(r'stopLoss\s*=\s*"0",\n', '', content)

# Fix newQty
content = content.replace("quantity = newQty,", "quantity = newQty.toString(),")

# Fix averagePrice to avgPrice
content = content.replace("item.averagePrice", "item.avgPrice")

open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)
