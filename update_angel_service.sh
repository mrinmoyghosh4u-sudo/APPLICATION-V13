sed -i 's/generateFallbackQuotes(symbols)/emptyList()/g' app/src/main/java/com/example/data/network/AngelOneBrokerService.kt
sed -i '/private fun generateFallbackQuotes/,/^    }/d' app/src/main/java/com/example/data/network/AngelOneBrokerService.kt
sed -i 's/OptionChainGenerator.generateOptionChain(symbol, spotPrice, selectedExpiry = expiry)/emptyList()/g' app/src/main/java/com/example/data/network/AngelOneBrokerService.kt
