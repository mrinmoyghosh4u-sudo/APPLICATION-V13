import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

start_idx = content.find('// Metrics Grid (2x2)')
end_idx = content.find('// P&L OVER TIME CANVAS CHART')

replacement = """// Metrics Grid (2x2)
        val perfHistory = AlgoEngine.liveTradeHistory.value + AlgoEngine.paperTradeHistory.value
        val perfPnl = perfHistory.sumOf { it.pnl }
        val perfTrades = perfHistory.size
        val perfWins = perfHistory.count { it.pnl > 0 }
        val perfLosses = perfHistory.count { it.pnl < 0 }
        val perfWinRateStr = if (perfTrades == 0) "--" else String.format("%.2f%%", (perfWins.toDouble() / perfTrades) * 100)
        
        val perfTotalProfit = perfHistory.filter { it.pnl > 0 }.sumOf { it.pnl }
        val perfTotalLoss = kotlin.math.abs(perfHistory.filter { it.pnl < 0 }.sumOf { it.pnl })
        val perfProfitFactor = if (perfTotalLoss == 0.0) "--" else String.format("%.2f", perfTotalProfit / perfTotalLoss)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("P&L", if (perfPnl == 0.0) "₹0.00" else String.format("%+₹.2f", perfPnl), if (perfPnl > 0) ProfitGreen else if (perfPnl < 0) LossRed else TextGray, Modifier.weight(1f))
                PerformanceMetricBox("Net P&L %", "--", TextGray, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Trades", "$perfTrades", TextWhite, Modifier.weight(1f))
                PerformanceMetricBox("Win Rate", perfWinRateStr, if (perfWinRateStr == "--") TextGray else ProfitGreen, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Winning Trades", "$perfWins", ProfitGreen, Modifier.weight(1f))
                PerformanceMetricBox("Losing Trades", "$perfLosses", LossRed, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Profit Factor", perfProfitFactor, PrimaryGold, Modifier.weight(1f))
                PerformanceMetricBox("Max Drawdown", "--", TextGray, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        """

content = content[:start_idx] + replacement + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)

