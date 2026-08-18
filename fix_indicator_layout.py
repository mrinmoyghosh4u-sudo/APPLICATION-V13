with open('app/src/main/java/com/example/ui/components/MarketDataStatusIndicator.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'if (isLive && lastUpdatedTime.isNotBlank() && lastUpdatedTime != "Not Updated") {',
    '''if (!isMarketOpen) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Next Open: 09:15 AM",
                        fontSize = 10.sp,
                        color = Color(0xFFFFB300)
                    )
                }
                
                if (isLive && lastUpdatedTime.isNotBlank() && lastUpdatedTime != "Not Updated") {'''
)

with open('app/src/main/java/com/example/ui/components/MarketDataStatusIndicator.kt', 'w') as f:
    f.write(text)
