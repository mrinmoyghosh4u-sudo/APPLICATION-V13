import re

# 1. Add MarketDataProviderState
filepath = "app/src/main/java/com/example/data/model/UnifiedDataModels.kt"
with open(filepath, "r") as f:
    content = f.read()
if "MarketDataProviderState" not in content:
    content += "\n\ndata class MarketDataProviderState(\n    val provider: String = \"UNKNOWN\",\n    val status: String = \"DISCONNECTED\",\n    val live: Boolean = false,\n    val stale: Boolean = false,\n    val error: String? = null,\n    val lastUpdate: Long = 0L,\n    val ping: Long = 0L\n)\n"
    with open(filepath, "w") as f:
        f.write(content)

# 2. Add providerState to MarketDataEngine
filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
    private val _providerState = kotlinx.coroutines.flow.MutableStateFlow(com.example.data.model.MarketDataProviderState())
    val providerState: kotlinx.coroutines.flow.StateFlow<com.example.data.model.MarketDataProviderState> = _providerState.asStateFlow()
"""
if "val providerState" not in content:
    idx = content.find("class MarketDataEngine")
    brace_idx = content.find("{", idx)
    content = content[:brace_idx+1] + props + content[brace_idx+1:]
    with open(filepath, "w") as f:
        f.write(content)

