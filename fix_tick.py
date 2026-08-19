import re

content = open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt").read()

content = content.replace("bytes.size < 47", "bytes.size < 51")
content = content.replace("val ltpInt = buffer.getInt()", "val ltpInt = buffer.getLong()")

content = content.replace(
"""            if (!hasFirstTick) {
                hasFirstTick = true
                _connectionState.value = "LIVE"
            }""",
"""            if (!hasFirstTick) {
                hasFirstTick = true
                _connectionState.value = "LIVE"
            }
            Log.d("AngelOneMarketDataService", "LTP TICK: ${inst.symbol} = $ltp (Raw: $ltpInt)")""")

open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt", "w").write(content)

