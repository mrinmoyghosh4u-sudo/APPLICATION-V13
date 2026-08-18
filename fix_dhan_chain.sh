sed -i 's/val spotPrice = getMarketQuotes(listOf(symbol)).getOrNull()?.firstOrNull()?.ltp ?: 24850.40/emptyList<OptionStrikeItem>()/g' app/src/main/java/com/example/data/network/DhanBrokerService.kt
sed -i 's/OptionChainGenerator.generateOptionChain(symbol, spotPrice, selectedExpiry = expiry)//g' app/src/main/java/com/example/data/network/DhanBrokerService.kt
