with open('app/src/main/java/com/example/ui/components/MarketDataStatusIndicator.kt', 'r') as f:
    text = f.read()

text = text.replace(
    "lastUpdatedTime: String,",
    "lastUpdatedTime: String,\n    isMarketOpen: Boolean = true,"
)

text = text.replace(
    'if (isLive) ProfitGreen else LossRed',
    'if (!isMarketOpen) Color(0xFFFFB300) else if (isLive) ProfitGreen else LossRed'
)

text = text.replace(
    'text = "LIVE — $activeBrokerName",',
    'text = if (!isMarketOpen) "MARKET CLOSED — $activeBrokerName" else "LIVE — $activeBrokerName",'
)

text = text.replace(
    'color = ProfitGreen',
    'color = if (!isMarketOpen) Color(0xFFFFB300) else ProfitGreen'
)

with open('app/src/main/java/com/example/ui/components/MarketDataStatusIndicator.kt', 'w') as f:
    f.write(text)
