import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    lines = f.read().split('\n')

# Let's replace the block from 1172 to 1192 with a clean version
new_block = """    private fun startLiveMarketFeed() {
        viewModelScope.launch {
            launch {
                // brokerManager.marketDataEngine.unifiedFeedStatus.collect { status ->
                //     _marketDataSource.value = status
                // }
            }
            launch {
                // brokerManager.marketDataEngine.lastTickTimeMs.collect { time ->
                //     if (time > 0) {
                //         _marketDataLastUpdated.value = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(time))
                //     }
                // }
            }

            while (true) {
                syncMarketDataPipeline()
                kotlinx.coroutines.delay(5000L)
            }
        }
    }"""

# Actually, replacing by line index is safer
lines[1171:1192] = new_block.split('\n')

with open(filepath, "w") as f:
    f.write('\n'.join(lines))
