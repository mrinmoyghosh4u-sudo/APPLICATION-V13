sed -i '/val spotPrice = when (symbol.uppercase()) {/,/^        }/d' app/src/main/java/com/example/data/repository/TradingRepository.kt
sed -i 's/return com.example.data.network.OptionChainGenerator.generateOptionChain(symbol, spotPrice, selectedExpiry = expiry)/return emptyList()/g' app/src/main/java/com/example/data/repository/TradingRepository.kt
