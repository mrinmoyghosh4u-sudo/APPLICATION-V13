import os

# Fix HomeScreen viewModel?.
filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("val isLiveFeedActive by viewModel.isLiveFeedActive.collectAsStateWithLifecycle()", "val isLiveFeedActive by (viewModel?.isLiveFeedActive ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsStateWithLifecycle()")

with open(filepath, "w") as f:
    f.write(content)

# Fix PortfolioScreen
filepath = "app/src/main/java/com/example/ui/screens/PortfolioScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("PortfolioHeaderSection(\n                isDhanConnected = isDhanConnected,", "PortfolioHeaderSection(\n                isLive = isDhanConnected, dataSource = \"\",")

with open(filepath, "w") as f:
    f.write(content)

