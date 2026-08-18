cat << 'INNER_EOF' >> app/src/main/java/com/example/viewmodel/MainViewModel_patch.kt
    // Background polling job
    private var marketDataJob: kotlinx.coroutines.Job? = null

    private fun startMarketDataPolling() {
        if (marketDataJob?.isActive == true) return
        marketDataJob = viewModelScope.launch {
            while (true) {
                runCatching {
                    if (sessionManager.hasValidSession()) {
                        repository.syncWithBroker()
                        _optionStrikes.value = repository.getOptionChainStrikes(_selectedOptionIndex.value)
                    }
                }.onFailure { err ->
                    _apiError.value = err.localizedMessage ?: "Failed to refresh market data."
                }
                kotlinx.coroutines.delay(5000L)
            }
        }
    }

    private fun stopMarketDataPolling() {
        marketDataJob?.cancel()
        marketDataJob = null
    }
INNER_EOF
