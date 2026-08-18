import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# Remove the previously inserted stuff
pattern = r'import com\.example\.data\.model\.AlgoTradeHistory.*?strategyName'
content = re.sub(pattern, '', content, flags=re.DOTALL)

# Insert the import after package
content = content.replace("package com.example.ui.screens", "package com.example.ui.screens\\nimport com.example.data.model.AlgoTradeHistory")

# Insert the val declaration before enum class
old_block = """
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
content = content.replace("enum class AlgoScreenState", old_block + "enum class AlgoScreenState")

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
