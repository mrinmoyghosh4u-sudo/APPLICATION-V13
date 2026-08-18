sed -i '/val initialWatchlist = listOf(/,/)/c\            val initialWatchlist = emptyList<WatchlistItem>()' app/src/main/java/com/example/data/repository/TradingRepository.kt
