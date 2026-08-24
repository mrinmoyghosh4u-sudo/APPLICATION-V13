import os

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

new_fyers_update = """    suspend fun updateFyersTick(tick: MarketTick) {
        val current = MarketDataStore.getTick(tick.symbol)
        
        MarketDataStore.updateTick(
            source = "FYERS",
            symbol = tick.symbol,
            token = "",
            exchange = "NSE",
            ltp = tick.ltp,
            open = tick.open.takeIf { it > 0.0 } ?: current?.open ?: tick.ltp,
            high = tick.high.takeIf { it > 0.0 } ?: current?.high ?: tick.ltp,
            low = tick.low.takeIf { it > 0.0 } ?: current?.low ?: tick.ltp,
            close = tick.close.takeIf { it > 0.0 } ?: current?.previousClose ?: tick.ltp,
            volume = tick.volume.takeIf { it > 0L } ?: current?.volume ?: 0L,
            exchangeTimestamp = tick.timestamp,
            receivedTimestamp = System.currentTimeMillis(),
            state = "LIVE"
        )
        
        _unifiedFeedStatus.value = "LIVE"
        _internalActiveProvider.value = "FYERS"
        updateLastTickTime()
    }"""

content = re.sub(r'    suspend fun updateFyersTick\(tick: MarketTick\) \{[\s\S]*?updateLastTickTime\(\)\n    \}', new_fyers_update, content)

with open(filepath, "w") as f:
    f.write(content)
