with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    text = f.read()
text = text.replace('Text("₹0.00", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)', 'Text("--", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)')
text = text.replace('val stratPnlStr = if (stratPnl == 0.0) "₹0.00" else String.format("%+₹.2f", stratPnl)', 'val stratPnlStr = if (stratPnl == 0.0) "--" else String.format("%+₹.2f", stratPnl)')
text = text.replace('if (perfPnl == 0.0) "₹0.00" else String.format("%+₹.2f", perfPnl)', 'if (perfPnl == 0.0) "--" else String.format("%+₹.2f", perfPnl)')

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(text)
