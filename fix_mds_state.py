import re

filepath = "app/src/main/java/com/example/data/model/MarketDataStore.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
    private val _providerState = kotlinx.coroutines.flow.MutableStateFlow(MarketDataProviderState())
    val providerState: kotlinx.coroutines.flow.StateFlow<MarketDataProviderState> = _providerState.asStateFlow()
"""
if "val providerState" not in content:
    idx = content.find("object MarketDataStore {")
    brace_idx = content.find("{", idx)
    content = content[:brace_idx+1] + props + content[brace_idx+1:]
    with open(filepath, "w") as f:
        f.write(content)

