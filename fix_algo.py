import re

with open('app/src/main/java/com/example/util/AlgoEngine.kt', 'r') as f:
    content = f.read()

# Find the end of processAutoTrader
marker = '        _marketBias.value = "INDICATOR UNAVAILABLE"\n        return\n'
idx = content.find(marker)
if idx != -1:
    end_idx = idx + len(marker)
    # The rest of the file currently is broken updateLivePositions.
    # We will replace everything from end_idx to `fun exitPaperPosition` with the proper updateLivePositions.
    
    exit_paper_idx = content.find('    fun exitPaperPosition(posId: String)')
    
    if exit_paper_idx != -1:
        new_content = content[:end_idx] + "    }\n\n    private fun updateLivePositions(quotes: List<WatchlistItem>) {\n        if (_activePositions.value.isEmpty()) return\n        val updatedList = _activePositions.value.map { pos ->\n            val quote = quotes.find { it.symbol.equals(pos.symbol, ignoreCase = true) }\n            if (quote != null && quote.ltp > 0) {\n                val newLtp = quote.ltp\n                val pnl = if (pos.type == \"CE\") (newLtp - pos.entryPrice) * pos.qty else (pos.entryPrice - newLtp) * pos.qty\n                pos.copy(currentLtp = newLtp, pnl = pnl)\n            } else pos\n        }\n        _activePositions.value = updatedList\n        _todayPnl.value = updatedList.sumOf { it.pnl }\n    }\n\n" + content[exit_paper_idx:]
        
        with open('app/src/main/java/com/example/util/AlgoEngine.kt', 'w') as f:
            f.write(new_content)
        print("Fixed AlgoEngine.kt")
    else:
        print("Could not find exitPaperPosition")
else:
    print("Could not find marker")

