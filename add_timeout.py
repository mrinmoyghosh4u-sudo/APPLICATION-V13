import re

content = open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt").read()

if "var lastTickTimestamp: Long" not in content:
    content = content.replace("private var hasFirstTick = false", 
"""private var hasFirstTick = false
    private var lastTickTimestamp: Long = 0
    private var lastTickSymbol: String = ""
    private var lastTickLtp: Double = 0.0""")

if "private fun monitorConnection()" not in content:
    content = content.replace("init {",
"""init {
        scope.launch {
            monitorConnection()
        }""")
        
    func = """
    private suspend fun monitorConnection() {
        while (true) {
            kotlinx.coroutines.delay(5000)
            if (_connectionState.value == "LIVE" && lastTickTimestamp > 0) {
                if (System.currentTimeMillis() - lastTickTimestamp > 15000) {
                    _connectionState.value = "STALE"
                    Log.w("AngelOneMarketDataService", "No ticks received for 15s, marking connection STALE")
                }
            }
        }
    }
    """
    content = content.replace("fun connect() {", func + "\n    fun connect() {")

if "lastTickTimestamp = System.currentTimeMillis()" not in content:
    content = content.replace(
"""            if (!hasFirstTick) {
                hasFirstTick = true
                _connectionState.value = "LIVE"
            }
            Log.d("AngelOneMarketDataService", "LTP TICK: ${inst.symbol} = $ltp (Raw: $ltpInt)")""",
"""            if (!hasFirstTick || _connectionState.value == "STALE") {
                hasFirstTick = true
                _connectionState.value = "LIVE"
            }
            lastTickTimestamp = System.currentTimeMillis()
            lastTickSymbol = inst.symbol
            lastTickLtp = ltp
            Log.d("AngelOneMarketDataService", "LTP TICK: ${inst.symbol} = $ltp (Raw: $ltpInt)")""")

open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt", "w").write(content)

