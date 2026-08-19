sed -i 's|if (reconnectAttempt > 5) {|if (reconnectAttempt > 5) {\n            _connectionState.value = "DISCONNECTED"|g' app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt
sed -i '/reconnectAttempt++/a \        _connectionState.value = "RECONNECTING"' app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt
