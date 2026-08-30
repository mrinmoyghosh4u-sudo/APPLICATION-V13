import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    lines = f.read().split('\n')

new_block = """    private suspend fun syncMarketDataPipeline() {
        runCatching {
            val isValidSession = sessionManager.hasValidSession()
            if (isValidSession) {
                repository.syncWithBroker()
            }

//             1. Fetch live quotes across all tracking symbols
            val symbols = getAllLiveTrackingSymbols()
            val quotesRes = brokerManager.marketDataEngine.getMarketQuotes(symbols)
//             quotesRes.getOrNull()?.let { quotes ->
//                 if (quotes.isNotEmpty()) {
//                     repository.updateWatchlistQuotes(quotes)
//                 }
//             }

//             2. Ensure historical candles are populated in CandleStore for active Algo Index
            val activeAlgoIndex = com.example.util.AlgoEngine.selectedIndex.value
            val strategyTimeframe = com.example.util.AlgoEngine.currentStrategy.value.timeframe
            if (!com.example.util.indicators.CandleStore.hasSufficientCandles(activeAlgoIndex, strategyTimeframe, 15)) {
                val candleInterval = when (com.example.util.indicators.CandleStore.normalizeTimeframe(strategyTimeframe)) {
                    "1 MIN" -> "1m"
                    "15 MIN" -> "15m"
                    "30 MIN" -> "30m"
                    "1 DAY" -> "1d"
                    else -> "5m"
                }
                val histRes = brokerManager.getHistoricalCandles(activeAlgoIndex, candleInterval)
//                 histRes.getOrNull()?.let { candles ->
//                     if (candles != null) {
//                         com.example.util.indicators.CandleStore.setHistoricalCandleData(activeAlgoIndex, strategyTimeframe, candles)
//                     }
//                 }
            }

//             3. Synchronously fetch option chain for active option index / algo index
            val targetOptionIndex = if (_selectedOptionIndex.value.isNotBlank()) _selectedOptionIndex.value else activeAlgoIndex
            val expiry = _selectedOptionExpiry.value
            val indexTick = com.example.data.model.MarketDataStore.getTick(targetOptionIndex)
            val indexLtp = indexTick?.price ?: _watchlist.value.find { it.symbol.equals(targetOptionIndex, ignoreCase = true) }?.ltp ?: 0.0

            val optChainRes = brokerManager.getOptionChain(targetOptionIndex, expiry)
            val strikes = emptyList<com.example.data.model.OptionStrikeItem>()
            val isValidForIndex = true && strikes.any { strike ->
                if (indexLtp > 0) {
                    true
                } else true
            }

            if (isValidForIndex && strikes != null) {
                _optionStrikes.value = strikes
            } else {
                _optionStrikes.value = emptyList()
            }

            _marketDataLastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            com.example.util.AlgoEngine.processMarketFeed(
                quotes = _watchlist.value,
                isLiveFeedActive = isLiveFeedActive.value,
                optionChain = _optionStrikes.value
            )
        }.onFailure { e ->
            Log.e("MainViewModel", "[MARKET_DATA_PIPELINE_ERROR] ${e.message}", e)
        }
    }"""

lines[1193:1260] = new_block.split('\n')

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
