import re
content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()
content = content.replace("avgPrice = item.avgPrice?.toString()?.toDoubleOrNull() ?: 0.0", "avgPrice = item.buyAvgPrice?.toString()?.toDoubleOrNull() ?: item.sellAvgPrice?.toString()?.toDoubleOrNull() ?: 0.0")
open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)
