import os

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Remove the broken updateFyersTick and replace it with a valid one
pattern_to_remove = r"    suspend fun updateFyersTick\(tick: MarketTick\) \{.*?\n        updateLastTickTime\(\)\n    \}"

fyers_update = """    suspend fun updateFyersTick(tick: MarketTick) {
        val current = MarketDataStore.getTick(tick.symbol)
        val newState = if (current != null) {
            val change = tick.ltp - current.previousClose
            val changePercent = if (current.previousClose > 0) (change / current.previousClose) * 100 else 0.0
            current.copy(
                source = "FYERS",
                ltp = tick.ltp,
                change = change,
                changePercent = changePercent,
                receivedTimestamp = System.currentTimeMillis(),
                exchangeTimestamp = tick.timestamp,
                volume = tick.volume.takeIf { it > 0 } ?: current.volume,
                high = tick.high.takeIf { it > 0.0 } ?: current.high,
                low = tick.low.takeIf { it > 0.0 } ?: current.low,
                open = tick.open.takeIf { it > 0.0 } ?: current.open,
                state = "LIVE"
            )
        } else {
            MarketDataState(
                source = "FYERS",
                symbol = tick.symbol,
                exchange = "NSE",
                token = "",
                ltp = tick.ltp,
                open = tick.open.takeIf { it > 0.0 } ?: tick.ltp,
                high = tick.high.takeIf { it > 0.0 } ?: tick.ltp,
                low = tick.low.takeIf { it > 0.0 } ?: tick.ltp,
                previousClose = tick.close.takeIf { it > 0.0 } ?: tick.ltp,
                change = tick.ltp - (tick.close.takeIf { it > 0.0 } ?: tick.ltp),
                changePercent = 0.0,
                volume = tick.volume,
                exchangeTimestamp = tick.timestamp,
                receivedTimestamp = System.currentTimeMillis(),
                state = "LIVE"
            )
        }
        
        MarketDataStore.updateTick(tick.symbol, newState)
        
        _unifiedFeedStatus.value = "LIVE"
        _internalActiveProvider.value = "FYERS"
        updateLastTickTime()
    }"""

content = re.sub(pattern_to_remove, fyers_update, content, flags=re.DOTALL)

with open(filepath, "w") as f:
    f.write(content)

