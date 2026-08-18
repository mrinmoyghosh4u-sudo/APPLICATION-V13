with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'source = marketDataSource,\n                lastUpdatedTime = marketDataLastUpdated,\n                onRefresh = onRefresh,\n                onReconnect = onNavigateToProfile',
    'source = marketDataSource,\n                lastUpdatedTime = marketDataLastUpdated,\n                isMarketOpen = isMarketOpen,\n                onRefresh = onRefresh,\n                onReconnect = onNavigateToProfile'
)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(text)
