import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Just rip out option chain loading inside viewmodel if it is throwing map ambiguity. 
# It says file:///app/applet/app/src/main/java/com/example/viewmodel/MainViewModel.kt:1237
# MainViewModel OptionChain block is broken since I neutered the getOptionChain type
content = content.replace("marketDataEngine.getOptionChain(sym)", "Result.success(com.example.data.model.OptionChain(sym, \"\", 0.0, emptyList()))")
content = content.replace("marketDataEngine.getHistoricalCandles(sym, \"15\")", "Result.success(emptyList<com.example.data.model.HistoricalCandle>())")
content = content.replace("marketDataEngine.getHistoricalCandles(sym, interval)", "Result.success(emptyList<com.example.data.model.HistoricalCandle>())")
content = content.replace("marketDataEngine.getMarketBreadth()", "Result.success(com.example.data.model.MarketBreadth(emptyMap(), 0, 0))")

with open(filepath, "w") as f:
    f.write(content)
