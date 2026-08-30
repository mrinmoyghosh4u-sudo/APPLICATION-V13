import re

filepath = "app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# I will just comment out these lines for now, or fix them if they're easy
content = content.replace(
    "val upstoxConnectionState by viewModel.brokerManager.upstox.connectionState.collectAsStateWithLifecycle()",
    "val upstoxConnectionState by viewModel.brokerManager.upstoxMarketDataService.connectionState.collectAsStateWithLifecycle()"
)

with open(filepath, "w") as f:
    f.write(content)
