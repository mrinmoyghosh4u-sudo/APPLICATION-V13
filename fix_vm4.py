import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    lines = f.read().split('\n')

new_block = """    fun sendSignalToTelegram(signal: com.example.data.model.AISignalEntity) {
        viewModelScope.launch {
            val isBullish = signal.trend.equals("BULLISH", ignoreCase = true) || signal.actionType.contains("CE", ignoreCase = true)
            if (isBullish) {
                alertService.notifyAiBuyCeSignal(
                    symbol = signal.symbol,
                    contract = "${signal.symbol} ${signal.actionType}",
                    entry = String.format(Locale.US, "%.2f", signal.ltp),
                    sl = String.format(Locale.US, "%.2f", signal.stopLoss),
                    t1 = String.format(Locale.US, "%.2f", signal.target1),
                    t2 = String.format(Locale.US, "%.2f", signal.target2),
                    t3 = if (signal.target3 > 0.0) String.format(Locale.US, "%.2f", signal.target3) else "",
                    t4 = if (signal.target4 > 0.0) String.format(Locale.US, "%.2f", signal.target4) else "",
                    confidence = signal.confidence
                )
            } else {
                alertService.notifyAiBuyPeSignal(
                    symbol = signal.symbol,
                    contract = "${signal.symbol} ${signal.actionType}",
                    entry = String.format(Locale.US, "%.2f", signal.ltp),
                    sl = String.format(Locale.US, "%.2f", signal.stopLoss),
                    t1 = String.format(Locale.US, "%.2f", signal.target1),
                    t2 = String.format(Locale.US, "%.2f", signal.target2),
                    t3 = if (signal.target3 > 0.0) String.format(Locale.US, "%.2f", signal.target3) else "",
                    t4 = if (signal.target4 > 0.0) String.format(Locale.US, "%.2f", signal.target4) else "",
                    confidence = signal.confidence
                )
            }
        }
    }"""

# I need to find the exact start line of sendSignalToTelegram
start_idx = -1
for i, line in enumerate(lines):
    if "fun sendSignalToTelegram(" in line:
        start_idx = i
        break

if start_idx != -1:
    lines[start_idx:1798] = new_block.split('\n')
    with open(filepath, "w") as f:
        f.write('\n'.join(lines))
else:
    print("Function not found!")

