import re

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "r") as f:
    content = f.read()

# Replace parseBinaryPacket function completely
binary_regex = r"fun parseBinaryPacket.*?catch\s*\(e:\s*Exception\)\s*\{\s*Log\.e\(TAG,\s*\"Failed to parse m\.Stock binary packet: \$\{e\.localizedMessage\}\"\)\s*\}\s*\}"

def get_decimal_divisor(token):
    segment = token & 0xff
    if segment == 3: return 10000000.0
    if segment == 6: return 10000.0
    return 100.0

replacement = """fun parseBinaryPacket(bytes: ByteArray) {
        try {
            if (bytes.size < 2) return
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN) // DataView false is BigEndian
            val count = buffer.short.toInt() and 0xFFFF
            
            for (i in 0 until count) {
                if (buffer.remaining() < 2) break
                val length = buffer.short.toInt() and 0xFFFF
                if (buffer.remaining() < length || length < 4) {
                    // Skip if not enough bytes or invalid length
                    if (buffer.remaining() >= length) buffer.position(buffer.position() + length)
                    break
                }
                
                val startPos = buffer.position()
                val instrumentToken = buffer.int
                val segment = instrumentToken and 0xFF
                val divisor = when (segment) {
                    3 -> 10000000.0
                    6 -> 10000.0
                    else -> 100.0
                }
                
                var ltp = 0.0
                var open = 0.0
                var high = 0.0
                var low = 0.0
                var close = 0.0
                var volume = 0L
                
                when (length) {
                    8 -> {
                        // LTP mode
                        ltp = buffer.int / divisor
                    }
                    44, 184, 200 -> {
                        // Quote / Full mode
                        ltp = buffer.int / divisor
                        val lastQty = buffer.int
                        val avgPrice = buffer.int / divisor
                        volume = buffer.int.toLong() and 0xFFFFFFFFL
                        val buyQty = buffer.int
                        val sellQty = buffer.int
                        open = buffer.int / divisor
                        high = buffer.int / divisor
                        low = buffer.int / divisor
                        close = buffer.int / divisor
                    }
                    else -> {
                        // Unknown length, skip it
                    }
                }
                
                if (ltp > 0.0) {
                    val tokenStr = instrumentToken.toString()
                    val exchange = subscribedTokens[tokenStr] ?: "NSE"
                    val symbol = resolveSymbol(exchange, tokenStr)
                    
                    val now = System.currentTimeMillis()
                    if (!hasFirstTick) {
                        try { Log.i(TAG, "[MSTOCK_FIRST_REAL_TICK] First valid m.Stock binary real tick received!") } catch (_: Throwable) {}
                    }
                    hasFirstTick = true
                    lastTickReceivedTime = now
                    _connectionState.value = "LIVE"
                    healthManager?.reportTickReceived(ProviderHealthManager.PROVIDER_MSTOCK, now)
                    MarketDataStore.setSourceHealth(MarketDataSourceNames.MSTOCK, "LIVE")
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
                
                buffer.position(startPos + length)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse m.Stock binary packet: ${e.localizedMessage}")
        }
    }"""

content = re.sub(binary_regex, replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "w") as f:
    f.write(content)
