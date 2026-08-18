with open('app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'source = marketDataSource,\n                        lastUpdatedTime = marketDataLastUpdated\n                    )',
    'source = marketDataSource,\n                        lastUpdatedTime = marketDataLastUpdated,\n                        isMarketOpen = com.example.util.MarketStatusUtil.getDetailedMarketStatus(indexSymbol).isOpen\n                    )'
)

with open('app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'w') as f:
    f.write(text)
