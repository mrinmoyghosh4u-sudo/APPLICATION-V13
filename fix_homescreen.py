import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("val isLiveFeedActive by viewModel.isLiveFeedActive.collectAsStateWithLifecycle()\n            HomeHeaderSection(", "")
content = content.replace("HomeHeaderSection(\n                isLive = isLiveFeedActive, dataSource = marketDataSource,", "val isLiveFeedActive by viewModel.isLiveFeedActive.collectAsStateWithLifecycle()\n            HomeHeaderSection(\n                isLiveFeedActive = isLiveFeedActive,\n                marketDataSource = marketDataSource,")

with open(filepath, "w") as f:
    f.write(content)
