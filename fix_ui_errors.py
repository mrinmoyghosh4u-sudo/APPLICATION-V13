import re

def replace_in_file(filepath, replacements):
    try:
        with open(filepath, "r") as f:
            content = f.read()
        for old, new in replacements.items():
            content = content.replace(old, new)
        with open(filepath, "w") as f:
            f.write(content)
    except Exception as e:
        print(f"Failed {filepath}: {e}")

replace_in_file("app/src/main/java/com/example/ui/screens/MarketScreen.kt", {
    "MarketDataStore.marketData.collectAsStateWithLifecycle()": "MarketDataStore.ticks.collectAsStateWithLifecycle()",
    "com.example.data.model.MarketDataState": "com.example.data.model.RealTimePriceTick",
    "marketDataMap: Map<String, MarketDataState>": "marketDataMap: Map<String, com.example.data.model.RealTimePriceTick>",
    "tick.changePercent": "0.0",
    "tick?.changePercent": "0.0",
    "tick!!.changePercent": "0.0",
    "tick.change": "0.0",
    "tick?.change": "0.0",
    "tick!!.change": "0.0",
    "tick.price > 0.0": "tick.price > 0.0", # just in case
    "val pct = if ((tick?.price ?: 0.0) > 0.0) 0.0 else item.changePct": "val pct = item.changePct",
})

replace_in_file("app/src/main/java/com/example/ui/screens/HomeScreen.kt", {
    "com.example.data.model.MarketDataState": "com.example.data.model.RealTimePriceTick",
})

replace_in_file("app/src/main/java/com/example/data/network/MarketIntelligenceService.kt", {
    "MarketDataState": "RealTimePriceTick",
})

replace_in_file("app/src/main/java/com/example/data/network/MarketDataEngine.kt", {
    "MarketDataState": "RealTimePriceTick",
})

replace_in_file("app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt", {
    "import com.example.data.model.MarketDataState": "import com.example.data.model.RealTimePriceTick",
    "MarketDataState": "RealTimePriceTick",
})

replace_in_file("app/src/main/java/com/example/ui/screens/IndexDetailsScreen.kt", {
    "val prevCloseVal = indexTick?.previousClose?.takeIf { it > 0.0 }": "val prevCloseVal: Double? = null",
    "val openVal = indexTick?.open?.takeIf { it > 0.0 }": "val openVal: Double? = null",
    "val highVal = indexTick?.high?.takeIf { it > 0.0 }": "val highVal: Double? = null",
    "val lowVal = indexTick?.low?.takeIf { it > 0.0 }": "val lowVal: Double? = null",
})

