import re

with open('app/src/main/java/com/example/util/AlgoEngine.kt', 'r') as f:
    content = f.read()

content = re.sub(r'\s*private val _isAutoTradingEnabled = MutableStateFlow\(false\)\n\s*val isAutoTradingEnabled: StateFlow<Boolean> = _isAutoTradingEnabled\.asStateFlow\(\)', '', content)
content = re.sub(r'\s*fun setAutoTradingEnabled\(enabled: Boolean\) \{.*?\n\s*\}', '', content, flags=re.DOTALL)
content = re.sub(r'\s*_isAutoTradingEnabled\.value = false', '', content)

# I should also replace the PAPER TRADING condition check with a general fallback if needed, but since it's just generating signals and paper trading is disabled by default for live users, wait...
# The prompt says: "Do not automatically place an order after a signal; always require USER CONFIRMATION before DHAN order execution."
# Actually, the user doesn't want dummy/mock/simulated market data or fake paper trading.

# Let's remove paper trading too?
# "Do not use dummy, fake, mock, hardcoded or simulated market data. Use only real data from the configured sources and actual broker responses."
# "Do not automatically place an order after a signal; always require USER CONFIRMATION before DHAN order execution."
# I will keep the signal generation but remove the mock execution / paper execution. Wait, if I remove paper execution, the ALGO just generates signals, which is exactly what the flow says:
# KING KHAN ENGINE -> BUY PE/CE SIGNAL -> USER CONFIRMATION -> RISK MANAGEMENT -> DHAN

with open('app/src/main/java/com/example/util/AlgoEngine.kt', 'w') as f:
    f.write(content)
