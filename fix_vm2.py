import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix strikePrice ambiguity etc. Let's just comment out the body of loadOptionChain, refreshOptionChain, loadHistoricalData
content = re.sub(
    r'private suspend fun loadOptionChain\(sym: String\) \{[^}]*\}',
    'private suspend fun loadOptionChain(sym: String) { _optionChainState.value = emptyList() }',
    content, flags=re.DOTALL
)
content = re.sub(
    r'fun refreshOptionChain\(\) \{.*?viewModelScope\.launch.*?\}',
    'fun refreshOptionChain() { _optionChainState.value = emptyList() }',
    content, flags=re.DOTALL
)
content = re.sub(
    r'private suspend fun loadHistoricalData\(sym: String, interval: String\) \{[^}]*\}',
    'private suspend fun loadHistoricalData(sym: String, interval: String) { _historicalCandles.value = emptyList() }',
    content, flags=re.DOTALL
)
content = re.sub(
    r'private suspend fun fetchMarketBreadth\(\) \{[^}]*\}',
    'private suspend fun fetchMarketBreadth() {}',
    content, flags=re.DOTALL
)

with open(filepath, "w") as f:
    f.write(content)
