import os

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

fyers_method = """
    suspend fun updateFyersTick(tick: MarketTick) {
        val current = _marketData.value[tick.symbol]
        val newState = if (current != null) {
            val change = tick.ltp - current.closePrice
            val changePercent = if (current.closePrice > 0) (change / current.closePrice) * 100 else 0.0
            current.copy(
                ltp = tick.ltp,
                change = change,
                changePercent = changePercent,
                lastUpdatedTime = System.currentTimeMillis(),
                volume = tick.volume.takeIf { it > 0 } ?: current.volume,
                highPrice = tick.high.takeIf { it > 0.0 } ?: current.highPrice,
                lowPrice = tick.low.takeIf { it > 0.0 } ?: current.lowPrice,
                openPrice = tick.open.takeIf { it > 0.0 } ?: current.openPrice,
                source = "FYERS"
            )
        } else {
            MarketDataState(
                symbol = tick.symbol,
                ltp = tick.ltp,
                closePrice = tick.close.takeIf { it > 0.0 } ?: tick.ltp,
                openPrice = tick.open.takeIf { it > 0.0 } ?: tick.ltp,
                highPrice = tick.high.takeIf { it > 0.0 } ?: tick.ltp,
                lowPrice = tick.low.takeIf { it > 0.0 } ?: tick.ltp,
                lastUpdatedTime = tick.timestamp,
                volume = tick.volume,
                source = "FYERS"
            )
        }
        
        val updatedMap = _marketData.value.toMutableMap()
        updatedMap[tick.symbol] = newState
        _marketData.value = updatedMap
        MarketDataStore.updateTick(tick.symbol, newState)
        
        _unifiedFeedStatus.value = "LIVE"
        _internalActiveProvider.value = "FYERS"
        updateLastTickTime()
    }
"""

if "updateFyersTick" not in content:
    content = content.replace("fun retryConnection() {", fyers_method + "\n    fun retryConnection() {")
    with open(filepath, "w") as f:
        f.write(content)
    print("Added updateFyersTick")
