import os

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, 'r') as f:
    content = f.read()

target = """        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(repository.watchlistAll, com.example.data.model.MarketDataStore.marketData) {"""

replacement = """        viewModelScope.launch {
            brokerManager.brokerAuthManager.statuses.collectLatest { statuses ->
                val actualDhanStatus = statuses["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                val currentProfile = _userProfile.value
                if (currentProfile.isDhanConnected != actualDhanStatus) {
                    _userProfile.value = currentProfile.copy(isDhanConnected = actualDhanStatus)
                }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(repository.watchlistAll, com.example.data.model.MarketDataStore.marketData) {"""

if target in content:
    with open(filepath, 'w') as f:
        f.write(content.replace(target, replacement))
    print("Replaced brokerStatuses block")
else:
    print("Could not find brokerStatuses block")

