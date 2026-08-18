with open('app/src/main/java/com/example/ui/screens/IndexDetailsScreen.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'source = marketDataSource,\n                    lastUpdatedTime = marketDataLastUpdated,',
    'source = marketDataSource,\n                    lastUpdatedTime = marketDataLastUpdated,\n                    isMarketOpen = isMarketOpen,'
)

with open('app/src/main/java/com/example/ui/screens/IndexDetailsScreen.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'source = marketDataSource,\n                        lastUpdatedTime = marketDataLastUpdated,',
    'source = marketDataSource,\n                        lastUpdatedTime = marketDataLastUpdated,\n                        isMarketOpen = com.example.util.MarketStatusUtil.getDetailedMarketStatus(indexSymbol).isOpen,'
)

with open('app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'w') as f:
    f.write(text)
