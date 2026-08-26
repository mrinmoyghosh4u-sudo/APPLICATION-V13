import re

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "r") as f:
    content = f.read()

new_parser = """    fun parseBinaryPacket(bytes: ByteArray) {
        try {
            if (bytes.size < 32) return
            val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            val length = buffer.short.toInt() and 0xFFFF
            if (length > bytes.size || length < 4) return
            
            val mode = buffer.get().toInt()
            val exchangeCode = buffer.get().toInt()
            val token = buffer.int
            
            // Map exchange
            val exchange = when (exchangeCode) {
                1 -> "NSE"
                2 -> "NFO"
                3 -> "BSE"
                4 -> "BFO"
                5 -> "CDS"
                6 -> "MCX"
                else -> "NSE"
            }
            
            var ltp = 0.0
            var open = 0.0
            var high = 0.0
            var low = 0.0
            var close = 0.0
            var volume = 0L
            
            // LTP is at offset 8, 4 bytes
            if (buffer.remaining() >= 4) {
                ltp = buffer.int / 100.0
            }
            if (buffer.remaining() >= 16) {
                open = buffer.int / 100.0
                high = buffer.int / 100.0
                low = buffer.int / 100.0
                close = buffer.int / 100.0
            }
            if (buffer.remaining() >= 8) {
                volume = buffer.long
            }
            
            if (ltp > 0.0) {
                val tokenStr = token.toString()
                val symbol = resolveSymbol(exchange, tokenStr)
                
                val now = System.currentTimeMillis()
                if (!hasFirstTick) {
                    try { android.util.Log.i(TAG, "[MSTOCK_FIRST_REAL_TICK] First valid m.Stock binary real tick received!") } catch (_: Throwable) {}
                }
                hasFirstTick = true
                lastTickReceivedTime = now
                _connectionState.value = "LIVE"
                healthManager?.reportTickReceived(ProviderHealthManager.PROVIDER_MSTOCK, now)
                
                MarketDataStore.updateTick(
                    source = MarketDataSourceNames.MSTOCK,
                    symbol = symbol,
                    token = tokenStr,
                    exchange = exchange,
                    ltp = ltp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    exchangeTimestamp = now,
                    receivedTimestamp = now,
                    state = "LIVE"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse m.Stock binary frame: ${e.localizedMessage}")
        }
    }"""

content = re.sub(
    r'    fun parseBinaryPacket\(bytes: ByteArray\) \{.*?    \}',
    new_parser,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "w") as f:
    f.write(content)
