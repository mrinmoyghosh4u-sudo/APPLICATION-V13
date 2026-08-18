import re

with open('app/src/main/java/com/example/data/repository/TradingRepository.kt', 'r') as f:
    content = f.read()

# We need to add market quote fetching to syncWithBroker
new_sync = """    suspend fun syncWithBroker() {
        brokerManager?.let { manager ->
            val profileRes = manager.getProfile()
            profileRes.getOrNull()?.let { prof ->
                val current = dao.getUserProfile().firstOrNull() ?: UserProfileEntity()
                val isAngelConn = if (prof.connectedBroker == "Angel One") true else (current.isAngelConnected || prof.isAngelConnected)
                val isDhanConn = if (prof.connectedBroker == "Dhan") true else (current.isDhanConnected || prof.isDhanConnected)

                dao.insertOrUpdateProfile(
                    current.copy(
                        name = prof.name,
                        email = if (prof.email.isNotBlank()) prof.email else current.email,
                        phone = if (prof.phone.isNotBlank()) prof.phone else current.phone,
                        connectedBroker = prof.connectedBroker,
                        isAngelConnected = isAngelConn,
                        angelClientId = if (prof.connectedBroker == "Angel One" && prof.angelClientId.isNotBlank()) prof.angelClientId else current.angelClientId,
                        isDhanConnected = isDhanConn,
                        dhanClientId = if (prof.connectedBroker == "Dhan" && prof.dhanClientId.isNotBlank()) prof.dhanClientId else current.dhanClientId,
                        availableMargin = prof.availableMargin,
                        accountBalance = prof.accountBalance,
                        realizedPnl = prof.realizedPnl,
                        unrealizedPnl = prof.unrealizedPnl,
                        todaysPnl = prof.todaysPnl
                    )
                )
            }
                
            val holdingsList = manager.getHoldings().getOrDefault(emptyList())
            val positionsList = manager.getPositions().getOrDefault(emptyList())
            val combined = holdingsList + positionsList
                
            dao.clearAllHoldings()
            if (combined.isNotEmpty()) {
                dao.insertHoldings(combined)
            }

            val ordersList = manager.getOrders().getOrDefault(emptyList())
            dao.clearAllOrders()
            if (ordersList.isNotEmpty()) {
                dao.insertOrders(ordersList)
            }

            // Sync Market Quotes
            val currentWatchlist = dao.getWatchlist("ALL").firstOrNull() ?: emptyList()
            if (currentWatchlist.isEmpty()) {
                // Seed default symbols
                val defaultSymbols = listOf(
                    WatchlistItem(symbol = "NIFTY 50", exchange = "NSE", ltp = 0.0),
                    WatchlistItem(symbol = "BANKNIFTY", exchange = "NSE", ltp = 0.0),
                    WatchlistItem(symbol = "FINNIFTY", exchange = "NSE", ltp = 0.0),
                    WatchlistItem(symbol = "SENSEX", exchange = "BSE", ltp = 0.0)
                )
                dao.insertWatchlist(defaultSymbols)
            }
            
            val symbolsToFetch = (dao.getWatchlist("ALL").firstOrNull() ?: emptyList()).map { it.symbol }
            if (symbolsToFetch.isNotEmpty()) {
                val quotesRes = manager.getMarketQuotes(symbolsToFetch)
                quotesRes.getOrNull()?.let { quotes ->
                    if (quotes.isNotEmpty()) {
                        dao.insertWatchlist(quotes)
                    }
                }
            }
        }
    }"""

content = re.sub(r'    suspend fun syncWithBroker\(\) \{.*?    \}\n\n    suspend fun checkAndSeedInitialData', new_sync + '\n\n    suspend fun checkAndSeedInitialData', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/TradingRepository.kt', 'w') as f:
    f.write(content)
