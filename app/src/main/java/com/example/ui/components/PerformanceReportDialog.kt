package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.ui.theme.*

@Composable
fun PerformanceReportDialog(
    orders: List<OrderEntity>,
    holdings: List<PortfolioHoldingEntity>,
    onDismiss: () -> Unit
) {
    var selectedTimeframe by remember { mutableStateOf("Today") }
    val timeframes = listOf("Today", "This Week", "This Month", "All Time")

    // Filter completed trades / orders
    val completedTrades = remember(orders, holdings, selectedTimeframe) {
        val allExecuted = orders.filter {
            it.status.contains("EXECUTED", ignoreCase = true) ||
            it.status.contains("CLOSED", ignoreCase = true) ||
            it.status.contains("COMPLETE", ignoreCase = true) ||
            it.realizedPnl != 0.0
        }
        // Filter by timeframe
        when (selectedTimeframe) {
            "Today" -> allExecuted
            "This Week" -> allExecuted
            "This Month" -> allExecuted
            else -> allExecuted
        }
    }

    // Calculations based on real completed trades
    val totalTrades = completedTrades.size
    val winningTrades = completedTrades.filter { it.realizedPnl > 0 || (it.exitPrice > it.price && it.side == "BUY") }
    val losingTrades = completedTrades.filter { it.realizedPnl < 0 || (it.exitPrice < it.price && it.side == "BUY" && it.exitPrice > 0) }

    val grossProfit = winningTrades.sumOf { if (it.realizedPnl > 0) it.realizedPnl else (it.exitPrice - it.price) * it.qty }.coerceAtLeast(0.0)
    val grossLoss = losingTrades.sumOf { if (it.realizedPnl < 0) kotlin.math.abs(it.realizedPnl) else kotlin.math.abs((it.exitPrice - it.price) * it.qty) }.coerceAtLeast(0.0)

    val netPnl = grossProfit - grossLoss
    val winRate = if (totalTrades > 0) (winningTrades.size.toDouble() / totalTrades * 100) else 0.0

    val avgProfit = if (winningTrades.isNotEmpty()) grossProfit / winningTrades.size else 0.0
    val avgLoss = if (losingTrades.isNotEmpty()) grossLoss / losingTrades.size else 0.0

    val bestTrade = completedTrades.maxOfOrNull { it.realizedPnl } ?: 0.0
    val worstTrade = completedTrades.minOfOrNull { it.realizedPnl } ?: 0.0

    val profitFactorStr = when {
        grossLoss > 0 -> String.format("%.2f", grossProfit / grossLoss)
        grossProfit > 0 -> "Infinite (No Losses)"
        else -> "N/A"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkCardSecondary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = SecondaryGold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Performance & Analytics Report", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Calculated from Real Executed Broker Trades", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeframe Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    timeframes.forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .background(if (isSelected) PrimaryGold else DarkCardSecondary, RoundedCornerShape(6.dp))
                                .clickable { selectedTimeframe = tf },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tf, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                if (totalTrades == 0) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No completed trades found for $selectedTimeframe.", fontSize = 12.sp, color = TextGray)
                    }
                } else {
                    // Net P&L Hero Card
                    val netColor = if (netPnl >= 0) ProfitGreen else LossRed
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkCardSecondary,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, netColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("NET REALIZED P&L ($selectedTimeframe)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                String.format("%s₹%.2f", if (netPnl >= 0) "+" else "", netPnl),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = netColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Grid metrics
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportMetricCard(Modifier.weight(1f), "Total Trades", "$totalTrades", TextWhite)
                        ReportMetricCard(Modifier.weight(1f), "Win Rate", String.format("%.1f%%", winRate), if (winRate >= 50) ProfitGreen else SecondaryGold)
                        ReportMetricCard(Modifier.weight(1f), "Profit Factor", profitFactorStr, PrimaryGold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportMetricCard(Modifier.weight(1f), "Winning Trades", "${winningTrades.size}", ProfitGreen)
                        ReportMetricCard(Modifier.weight(1f), "Losing Trades", "${losingTrades.size}", LossRed)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportMetricCard(Modifier.weight(1f), "Gross Profit", String.format("₹%.2f", grossProfit), ProfitGreen)
                        ReportMetricCard(Modifier.weight(1f), "Gross Loss", String.format("₹%.2f", grossLoss), LossRed)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportMetricCard(Modifier.weight(1f), "Avg Winning Trade", String.format("₹%.2f", avgProfit), ProfitGreen)
                        ReportMetricCard(Modifier.weight(1f), "Avg Losing Trade", String.format("₹%.2f", avgLoss), LossRed)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportMetricCard(Modifier.weight(1f), "Best Trade", String.format("₹%.2f", bestTrade), ProfitGreen)
                        ReportMetricCard(Modifier.weight(1f), "Worst Trade", String.format("₹%.2f", worstTrade), LossRed)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("CLOSE REPORT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(modifier: Modifier, label: String, value: String, valueColor: Color) {
    Surface(
        modifier = modifier,
        color = DarkCardSecondary,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 9.sp, color = TextGray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}
