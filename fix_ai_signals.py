import re

with open("app/src/main/java/com/example/data/network/AISignalGenerator.kt", "r") as f:
    content = f.read()

ai_logic = """    fun generateSignalsFromMarketData(quotes: List<WatchlistItem>): List<AISignalEntity> {
        val signals = mutableListOf<AISignalEntity>()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        
        quotes.forEach { quote ->
            if (quote.ltp > 0 && quote.changePercent != 0.0) {
                val isBullish = quote.changePercent > 0.5
                val isBearish = quote.changePercent < -0.5
                
                if (isBullish || isBearish) {
                    val optionSymbol = formatOptionSymbol(quote.symbol, isBullish, quote.ltp)
                    val sl = if (isBullish) quote.ltp * 0.99 else quote.ltp * 1.01
                    val tg1 = if (isBullish) quote.ltp * 1.01 else quote.ltp * 0.99
                    val tg2 = if (isBullish) quote.ltp * 1.02 else quote.ltp * 0.98
                    
                    signals.add(
                        AISignalEntity(
                            id = 0,
                            symbol = optionSymbol,
                            exchange = quote.exchange,
                            side = if (isBullish) "BUY" else "SELL",
                            actionType = if (isBullish) "BUY CE" else "BUY PE",
                            trend = if (isBullish) "BULLISH" else "BEARISH",
                            ltp = quote.ltp,
                            changePercent = quote.changePercent,
                            entryZone = String.format(Locale.US, "%.2f - %.2f", quote.ltp * 0.998, quote.ltp * 1.002),
                            target1 = tg1,
                            target2 = tg2,
                            stopLoss = sl,
                            confidence = if (kotlin.math.abs(quote.changePercent) > 1.5) 95 else 85,
                            riskReward = "1:2",
                            lotSize = 1,
                            timeframe = "Live Tick",
                            timestamp = currentTime,
                            status = "ACTIVE"
                        )
                    )
                }
            }
        }
        return signals.sortedByDescending { it.confidence }
    }"""

content = re.sub(
    r'fun generateSignalsFromMarketData.*?return signals\.sortedByDescending \{ it\.confidence \}\s*\}',
    ai_logic,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/data/network/AISignalGenerator.kt", "w") as f:
    f.write(content)

