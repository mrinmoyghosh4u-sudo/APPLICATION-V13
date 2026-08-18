import re

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    content = f.read()

text_to_replace = """        viewModelScope.launch {
            repository.watchlistAll.collectLatest { list ->
                _watchlist.value = list
            }
        }"""

new_text = """        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(repository.watchlistAll, com.example.data.model.MarketDataStore.marketData) { dbList, liveData ->
                dbList.map { item ->
                    // For index we use the name to map
                    val symbolToMatch = item.symbol.replace(" 50", "") // "NIFTY 50" -> "NIFTY"
                    val live = liveData[symbolToMatch]
                    if (live != null && live.ltp > 0) {
                        item.copy(
                            ltp = live.ltp,
                            change = live.change,
                            changePercent = live.changePercent,
                            isPositive = live.change >= 0
                        )
                    } else {
                        item
                    }
                }
            }.collectLatest { combinedList ->
                _watchlist.value = combinedList
            }
        }"""

content = content.replace(text_to_replace, new_text)

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'w') as f:
    f.write(content)
