import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# First, remove the previously inserted block
old_block = """import com.example.data.model.AlgoTradeHistory

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
content = content.replace(old_block, "")

# Now find the LAST import statement and put it there
import_end_idx = content.rfind('import ')
import_end_line_idx = content.find('\\n', import_end_idx)

content = content[:import_end_line_idx] + '\\n\\n' + old_block + content[import_end_line_idx:]

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
