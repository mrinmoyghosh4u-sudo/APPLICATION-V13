with open('app/src/main/java/com/example/ui/screens/MarketScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("val change = currentSymbolItem?.change ?: 12.50", "val change = currentSymbolItem?.change ?: 0.0")
content = content.replace("val chgPct = currentSymbolItem?.changePercent ?: 6.32", "val chgPct = currentSymbolItem?.changePercent ?: 0.0")

# And wrap the Text to show DATA UNAVAILABLE if ltp is 0
text_replace = """                        val hasData = ltp > 0.0
                        if (hasData) {
                            Text(String.format("₹%.2f", ltp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isPos) ProfitGreen else LossRed)
                            Text("${if (isPos) "+" else ""}${String.format("%.2f", change)} (${if (isPos) "+" else ""}${String.format("%.2f", chgPct)}%)", fontSize = 10.sp, color = if (isPos) ProfitGreen else LossRed)
                        } else {
                            Text("DATA UNAVAILABLE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LossRed)
                        }"""
content = content.replace("Text(String.format(\"₹%.2f\", ltp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isPos) ProfitGreen else LossRed)\n                        Text(\"\\${if (isPos) \"+\" else \"\"}\\${String.format(\"%.2f\", change)} (\\${if (isPos) \"+\" else \"\"}\\${String.format(\"%.2f\", chgPct)}%)\", fontSize = 10.sp, color = if (isPos) ProfitGreen else LossRed)", text_replace)

with open('app/src/main/java/com/example/ui/screens/MarketScreen.kt', 'w') as f:
    f.write(content)
