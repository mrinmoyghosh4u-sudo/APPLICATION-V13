import os
import glob

# 1. Update CommonComponents.kt
filepath = "app/src/main/java/com/example/ui/components/CommonComponents.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("fun DhanLiveStatusBadge(\n    isDhanConnected: Boolean", "fun LiveStatusBadge(\n    isLive: Boolean")
content = content.replace("isDhanConnected", "isLive")

with open(filepath, "w") as f:
    f.write(content)

# 2. Update MainViewModel to expose isLive
filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

is_live = """    val isLiveFeedActive: StateFlow<Boolean> = _marketDataSource.map {
        it.startsWith("LIVE", ignoreCase = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
"""

if "val isLiveFeedActive" not in content:
    content = content.replace("val marketDataSource: StateFlow<String> = _marketDataSource.asStateFlow()", "val marketDataSource: StateFlow<String> = _marketDataSource.asStateFlow()\n" + is_live)
    with open(filepath, "w") as f:
        f.write(content)

