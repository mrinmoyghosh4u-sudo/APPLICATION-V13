import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    lines = f.read().split('\n')

new_block = """    fun getHistoricalCandlesForIndex(indexName: String, interval: String = "15m", onResult: (List<com.example.ui.components.CandleData>) -> Unit) {
        viewModelScope.launch {
            val res = brokerManager.getHistoricalCandles(indexName, interval)
            if (res.isSuccess) {
//                 val candles = res.getOrDefault(emptyList())
//                 if (candles != null) {
//                     com.example.util.indicators.CandleStore.setHistoricalCandleData(indexName, interval, candles)
//                 }
                onResult(emptyList())
            } else {
                onResult(emptyList())
            }
        }
    }"""

lines[1377:1392] = new_block.split('\n')

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
