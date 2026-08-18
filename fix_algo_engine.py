import re

with open('app/src/main/java/com/example/util/AlgoEngine.kt', 'r') as f:
    content = f.read()

# Let's fix the specific lines
old_text = '''    fun setTradingMode(mode: String) {
        _tradingMode.value = mode
    } else {
            _tradingMode.value = "PAPER TRADING"
        }
    }

    fun updateRiskSettings(riskPerTrade: Double, maxLossPct: Double, maxTrades: Int, onePos: Boolean) {'''

new_text = '''    fun setTradingMode(mode: String) {
        _tradingMode.value = mode
    }

    fun updateRiskSettings(riskPerTrade: Double, maxLossPct: Double, maxTrades: Int, onePos: Boolean) {'''

content = content.replace(old_text, new_text)

with open('app/src/main/java/com/example/util/AlgoEngine.kt', 'w') as f:
    f.write(content)
