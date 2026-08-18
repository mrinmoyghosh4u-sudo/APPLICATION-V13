import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# Clean up the end of the file
pattern = r'\\n\\nimport com\.example\.data\.model\.AlgoTradeHistory.*?get\(\) = this\.strategyName'
content = re.sub(pattern, '', content, flags=re.DOTALL)

# Insert after package com.example.ui.screens
# Need to put imports carefully
old_block = """
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

# Find the first class or function
import_block_end = content.find('\n\n@')
if import_block_end == -1:
    import_block_end = content.find('\n@')

content = content[:import_block_end] + old_block + content[import_block_end:]

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
