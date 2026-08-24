import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

new_binary_parser = """    private val topicToSymbolMap = mutableMapOf<Int, String>()
    private val topicToMultiplierMap = mutableMapOf<Int, Int>()

    private fun handleBinaryMessage(bytes: ByteArray) {
        try {
            val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
            if (buffer.remaining() < 3) return
            val length = buffer.short.toInt()
            val respType = buffer.get().toInt()

            when (respType) {
                6 -> parseTopicInit(buffer)
                85 -> parseFullMode(buffer)
                76 -> parseLiteMode(buffer)
                else -> {
                    // Try JSON fallback for other types
                    val str = String(bytes)
                    if (str.startsWith("{")) {
                        val json = JSONObject(str)
                        parseJsonTick(json)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing FYERS binary message: ${e.message}")
        }
    }

    private fun parseTopicInit(buffer: java.nio.ByteBuffer) {
        if (buffer.remaining() < 6) return
        val messageNum = buffer.int
        val scripCount = buffer.short.toInt()
        
        for (i in 0 until scripCount) {
            if (buffer.remaining() < 1) break
            val dataType = buffer.get().toInt()
            if (dataType == 83) { // Snapshot
                if (buffer.remaining() < 3) break
                val topicId = buffer.short.toInt()
                val topicNameLen = buffer.get().toInt()
                if (buffer.remaining() < topicNameLen) break
                val topicNameBytes = ByteArray(topicNameLen)
                buffer.get(topicNameBytes)
                
                if (buffer.remaining() < 1) break
                val fieldCount = buffer.get().toInt()
                
                // Fields
                for (j in 0 until fieldCount) {
                    if (buffer.remaining() < 4) break
                    buffer.int
                }
                
                if (buffer.remaining() < 3) break
                val multiplier = buffer.short.toInt()
                topicToMultiplierMap[topicId] = multiplier
                buffer.get() // precision
                
                // exchange
                if (buffer.remaining() < 1) break
                val exLen = buffer.get().toInt()
                if (buffer.remaining() < exLen) break
                buffer.position(buffer.position() + exLen)
                
                // exchange_token
                if (buffer.remaining() < 1) break
                val extLen = buffer.get().toInt()
                if (buffer.remaining() < extLen) break
                buffer.position(buffer.position() + extLen)
                
                // symbol
                if (buffer.remaining() < 1) break
                val symLen = buffer.get().toInt()
                if (buffer.remaining() < symLen) break
                val symBytes = ByteArray(symLen)
                buffer.get(symBytes)
                val symbol = String(symBytes)
                
                topicToSymbolMap[topicId] = symbol
                Log.d(TAG, "FYERS Mapping: Topic $topicId -> $symbol (Multiplier $multiplier)")
            }
        }
    }

    private fun parseFullMode(buffer: java.nio.ByteBuffer) {
        if (buffer.remaining() < 3) return
        val topicId = buffer.short.toInt()
        val fieldCount = buffer.get().toInt()
        
        val symbol = topicToSymbolMap[topicId] ?: return
        val multiplier = topicToMultiplierMap[topicId]?.toDouble() ?: 100.0
        
        var ltp = Double.NaN
        var vol = 0L
        var open = Double.NaN
        var high = Double.NaN
        var low = Double.NaN
        var close = Double.NaN
        
        for (i in 0 until fieldCount) {
            if (buffer.remaining() < 4) break
            val value = buffer.int
            if (value != -2147483648) {
                val realValue = value / multiplier
                when (i) {
                    0 -> ltp = realValue
                    1 -> vol = realValue.toLong()
                    12 -> low = realValue
                    13 -> high = realValue
                    16 -> open = realValue
                    17 -> close = realValue
                }
            }
        }
        
        if (!ltp.isNaN()) {
            val tick = MarketTick(
                symbol = symbol,
                ltp = ltp,
                open = if (open.isNaN()) ltp else open,
                high = if (high.isNaN()) ltp else high,
                low = if (low.isNaN()) ltp else low,
                close = if (close.isNaN()) ltp else close,
                volume = vol,
                timestamp = System.currentTimeMillis(),
                exchange = "NSE"
            )
            scope.launch { marketDataEngine.updateFyersTick(tick) }
        }
    }

    private fun parseLiteMode(buffer: java.nio.ByteBuffer) {
        if (buffer.remaining() < 6) return
        val topicId = buffer.short.toInt()
        val ltpVal = buffer.int
        
        val symbol = topicToSymbolMap[topicId] ?: return
        val multiplier = topicToMultiplierMap[topicId]?.toDouble() ?: 100.0
        
        if (ltpVal != -2147483648) {
            val tick = MarketTick(
                symbol = symbol,
                ltp = ltpVal / multiplier,
                open = ltpVal / multiplier,
                high = ltpVal / multiplier,
                low = ltpVal / multiplier,
                close = ltpVal / multiplier,
                volume = 0L,
                timestamp = System.currentTimeMillis(),
                exchange = "NSE"
            )
            scope.launch { marketDataEngine.updateFyersTick(tick) }
        }
    }

    private fun parseJsonTick(json: JSONObject) {"""

# Replace handleBinaryMessage to parseJsonTick inclusive
pattern = re.compile(r'    private fun handleBinaryMessage\(bytes: ByteArray\).*?    private fun parseJsonTick\(json: JSONObject\) \{', re.DOTALL)
content = pattern.sub(new_binary_parser, content)

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)

