sed -i 's/override suspend fun placeOrder(/override suspend fun placeOrder(/g' app/src/main/java/com/example/data/network/DhanBrokerService.kt
sed -i 's/id = "1",/id = 1,/g' app/src/main/java/com/example/data/network/DhanBrokerService.kt
sed -i 's/id = item.isin.ifEmpty/symbol = item.isin.ifEmpty/g' app/src/main/java/com/example/data/network/DhanBrokerService.kt
sed -i 's/id = "${item.tradingSymbol}_${item.productType}",/symbol = "${item.tradingSymbol}_${item.productType}",/g' app/src/main/java/com/example/data/network/DhanBrokerService.kt
