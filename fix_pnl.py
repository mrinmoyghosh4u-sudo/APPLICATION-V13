import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

replacement = """package com.example.ui.screens

import com.example.data.model.AlgoTradeHistory

val AlgoTradeHistory.pnl: Double
    get() {
        if (this.status.contains("PROFIT")) {
            val amt = this.status.substringAfter("₹").replace(",", "").toDoubleOrNull() ?: 0.0
            return amt
        }
        if (this.status.contains("LOSS")) {
            val amt = this.status.substringAfter("₹").replace(",", "").toDoubleOrNull() ?: 0.0
            return -amt
        }
        return 0.0
    }
    
val AlgoTradeHistory.strategyId: String
    get() = this.strategyName
"""

content = content.replace('package com.example.ui.screens', replacement)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
