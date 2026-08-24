import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Make HomeHeaderSection take isLiveFeedActive and marketDataSource
content = content.replace(
    "fun HomeHeaderSection(\n    isDhanConnected: Boolean,\n    unreadCount: Int",
    "fun HomeHeaderSection(\n    isLiveFeedActive: Boolean,\n    marketDataSource: String,\n    unreadCount: Int"
)
content = content.replace(
    "isLive = isDhanConnected, dataSource = \"\"",
    "isLive = isLiveFeedActive, dataSource = marketDataSource"
)

# Call site in HomeScreen
content = content.replace(
    "HomeHeaderSection(\n                isDhanConnected = isDhanConnected,\n                unreadCount = unreadCount,",
    "val isLiveFeedActive by viewModel.isLiveFeedActive.collectAsStateWithLifecycle()\n            HomeHeaderSection(\n                isLiveFeedActive = isLiveFeedActive,\n                marketDataSource = marketDataSource,\n                unreadCount = unreadCount,"
)


with open(filepath, "w") as f:
    f.write(content)
