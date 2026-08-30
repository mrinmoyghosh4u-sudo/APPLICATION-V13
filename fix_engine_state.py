import re

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
package com.example.data.model
data class MarketDataProviderState(
    val provider: String = "UNKNOWN",
    val status: String = "DISCONNECTED",
    val live: Boolean = false,
    val stale: Boolean = false,
    val error: String? = null,
    val lastUpdate: Long = 0L,
    val ping: Long = 0L
)
"""

# Let's add it to MarketDataEngine.kt at the bottom. But wait, it's package com.example.data.network. 
# Better to put it in UnifiedDataModels.kt !
