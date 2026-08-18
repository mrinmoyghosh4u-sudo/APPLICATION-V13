sed -i '201,209c\
    override suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {\
        return runCatching {\
            emptyList()\
        }\
    }' app/src/main/java/com/example/data/network/DhanBrokerService.kt
