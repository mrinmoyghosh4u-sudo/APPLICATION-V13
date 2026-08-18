import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

pattern = r'Text\("P&L", color = TextGray, fontSize = 10\.sp\)\s*Text\("\+ ₹1,250\.00", color = ProfitGreen, fontSize = 11\.sp, fontWeight = FontWeight\.Bold\)'
replacement = """Text("P&L", color = TextGray, fontSize = 10.sp)
                                val stratPnl = AlgoEngine.liveTradeHistory.value.filter { it.strategyId == strat.id }.sumOf { it.pnl } + AlgoEngine.paperTradeHistory.value.filter { it.strategyId == strat.id }.sumOf { it.pnl }
                                val stratPnlStr = if (stratPnl == 0.0) "₹0.00" else String.format("%+₹.2f", stratPnl)
                                Text(stratPnlStr, color = if(stratPnl > 0) ProfitGreen else if(stratPnl < 0) LossRed else TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)"""

content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)
