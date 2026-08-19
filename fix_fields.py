import re

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

content = content.replace("item.clientcode", "item.clientCode?.toString()")
content = content.replace("data.clientcode", "data.clientCode?.toString()")
content = content.replace("data.name", "data.name?.toString()")
content = content.replace("data.email", "data.email?.toString()")
content = content.replace("data.mobileno", "data.mobileNo?.toString()")

content = content.replace("item.updatetime", "item.orderUpdateTime?.toString()")
content = content.replace("item.exchange", "item.exchange?.toString()")
content = content.replace("item.ordertype", "item.orderType?.toString()")
content = content.replace("item.status", "item.status?.toString()")
content = content.replace("item.tradingsymbol", "item.tradingSymbol?.toString()")
content = content.replace("item.transactiontype", "item.transactionType?.toString()")
content = content.replace("item.quantity?.toIntOrNull()", "item.quantity?.toString()?.toDoubleOrNull()?.toInt()")
content = content.replace("item.price?.toDoubleOrNull()", "item.price?.toString()?.toDoubleOrNull()")
content = content.replace("item.orderid", "item.orderId?.toString()")

content = content.replace("item.averageprice", "item.averagePrice?.toString()")
content = content.replace("item.ltp", "item.ltp?.toString()")
content = content.replace("item.netqty", "item.netQty?.toString()")

open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)

