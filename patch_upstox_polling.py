with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

# Add restPollingJob variable
if "private var restPollingJob: Job? = null" not in content:
    content = content.replace("private var reconnectJob: Job? = null", "private var reconnectJob: Job? = null\n    private var restPollingJob: Job? = null")

# Add startRestPolling method
polling_method = """    private fun startRestPolling() {
        if (restPollingJob?.isActive == true) return
        restPollingJob = scope.launch {
            while (isConfigured()) {
                if (!isConnectionLive()) {
                    try {
                        val symbolsToFetch = if (subscribedInstrumentKeys.isNotEmpty()) {
                            subscribedInstrumentKeys.map { com.example.data.network.UpstoxSymbolMapper.fromUpstoxInstrumentKey(it).first }
                        } else {
                            listOf("NSE_INDEX|Nifty 50", "NSE_INDEX|Nifty Bank")
                        }
                        val quotes = getMarketQuotes(symbolsToFetch).getOrNull()
                        if (quotes != null) {
                            var hasValidTick = false
                            val now = System.currentTimeMillis()
                            for (q in quotes) {
                                if (q.ltp > 0.0) {
                                    hasValidTick = true
                                    val tick = com.example.data.model.MarketTick(
                                        symbol = q.symbol,
                                        token = com.example.data.network.UpstoxSymbolMapper.toUpstoxInstrumentKey(q.symbol),
                                        exchange = q.exchange,
                                        ltp = q.ltp,
                                        open = q.ltp,
                                        high = q.ltp,
                                        low = q.ltp,
                                        close = q.ltp,
                                        volume = 0L,
                                        timestamp = now
                                    )
                                    marketDataEngine.updateUpstoxTick(tick)
                                }
                            }
                            if (hasValidTick) {
                                hasFirstTick = true
                                lastTickReceivedTime = now
                                _connectionState.value = "LIVE"
                                healthManager?.reportTickReceived(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX, now)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Upstox REST polling error: ${e.message}")
                    }
                }
                kotlinx.coroutines.delay(2000L)
            }
        }
    }
"""

if "private fun startRestPolling()" not in content:
    content = content.replace("    suspend fun connect() = withContext(Dispatchers.IO) {", polling_method + "\n    suspend fun connect() = withContext(Dispatchers.IO) {")

# Start polling in connect()
content = content.replace("connectWebSocket()", "startRestPolling()\n        connectWebSocket()")

# Cancel polling in disconnect()
content = content.replace("reconnectJob?.cancel()", "reconnectJob?.cancel()\n        restPollingJob?.cancel()")

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
