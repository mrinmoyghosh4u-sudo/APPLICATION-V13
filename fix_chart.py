import re

with open("app/src/main/java/com/example/ui/screens/IndexDetailsScreen.kt", "r") as f:
    content = f.read()

# Instead of custom candlestick chart, use TradingViewChart
content = content.replace(
    'if (candleList.isNotEmpty()) {',
    'if (true) {'
)

content = re.sub(
    r'com\.example\.ui\.components\.CandlestickChart\(\s*candles = candleList,\s*currentPrice = ltp\.toFloat\(\),\s*modifier = Modifier\.fillMaxWidth\(\)\.height\(220\.dp\)\s*\)',
    'com.example.ui.components.TradingViewChart(\n                        symbol = indexName,\n                        modifier = Modifier.fillMaxWidth().height(350.dp)\n                    )',
    content
)

with open("app/src/main/java/com/example/ui/screens/IndexDetailsScreen.kt", "w") as f:
    f.write(content)

