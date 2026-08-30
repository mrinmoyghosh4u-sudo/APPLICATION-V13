import re

filepath = "app/src/main/java/com/example/data/model/TradingEntities.kt"
with open(filepath, "r") as f:
    content = f.read()

old_profile = """    val totalOrders: Int = 0,
    val successRate: Double = 0.0
)"""

new_profile = """    val totalOrders: Int = 0,
    val successRate: Double = 0.0,
    val isDhanConnected: Boolean = false,
    val isAngelConnected: Boolean = false,
    val connectedBroker: String = ""
)"""

content = content.replace(old_profile, new_profile)
with open(filepath, "w") as f:
    f.write(content)
