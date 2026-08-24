package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.CrownLogo
import com.example.ui.components.GoldCard
import com.example.ui.theme.*

@Composable
fun PortfolioScreen(
    holdings: List<PortfolioHoldingEntity>,
    userProfile: UserProfileEntity,
    onNavigateToPositions: () -> Unit,
    onNavigateToOrders: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val totalVal = holdings.sumOf { it.currentValue }
    val totalRealized = userProfile.realizedPnl
    val totalUnrealized = userProfile.unrealizedPnl
    val totalPnl = totalRealized + totalUnrealized
    val totalPnlPct = if (totalVal > 0) (totalPnl / totalVal) * 100 else 0.0

    var activeTab by remember { mutableStateOf("Holdings (${holdings.size})") }

    com.example.ui.components.PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CrownLogo(size = 36.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "KING KHAN AI TRADE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    com.example.ui.components.KingKhanTagline(fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.ui.components.LiveStatusBadge(
                    isLive = userProfile.isDhanConnected,
                    modifier = Modifier.padding(end = 6.dp)
                )
                if (userProfile.connectedBroker.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(ProfitGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(userProfile.connectedBroker, color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PNL Summary Card
        GoldCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            borderColor = SecondaryGold
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Overall Profit / Loss", fontSize = 10.sp, color = TextGray)
                    val pnlColor = if (totalPnl >= 0) ProfitGreen else LossRed
                    Text(
                        String.format("%s₹%,.2f (%s%.2f%%)", if (totalPnl >= 0) "+" else "", totalPnl, if (true) "+" else "", totalPnlPct),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Realized P&L", fontSize = 10.sp, color = TextGray)
                    val realizedColor = if (totalRealized >= 0) ProfitGreen else LossRed
                    Text(
                        String.format("%s₹%,.2f", if (totalRealized >= 0) "+" else "", totalRealized),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = realizedColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Unrealized MTM", fontSize = 10.sp, color = TextGray)
                    val unrealizedColor = if (totalUnrealized >= 0) ProfitGreen else LossRed
                    Text(
                        String.format("%s₹%,.2f", if (totalUnrealized >= 0) "+" else "", totalUnrealized),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = unrealizedColor
                    )
                }

                DonutChart(modifier = Modifier.size(110.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Funds & Margin Breakdown Card
        GoldCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            borderColor = DarkCardBorder
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Margin & Funds", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Available Margin", fontSize = 10.sp, color = TextGray)
                        Text(String.format("₹%,.2f", userProfile.availableMargin), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Account Balance", fontSize = 10.sp, color = TextGray)
                        Text(String.format("₹%,.2f", userProfile.accountBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Invested", fontSize = 9.sp, color = TextGray)
                        val totalInvested = (totalVal - totalPnl).coerceAtLeast(0.0)
                        Text(String.format("₹%,.2f", totalInvested), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Current Value", fontSize = 9.sp, color = TextGray)
                        Text(String.format("₹%,.2f", totalVal), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        val tabs = listOf("Holdings (${holdings.size})")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) PrimaryGold.copy(alpha = 0.2f) else DarkCard,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { activeTab = tab }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) PrimaryGold else TextGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter / Sort Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Symbol", fontSize = 11.sp, color = TextGray)
            Row {
                Text("Invested / LTP", fontSize = 11.sp, color = TextGray)
                Spacer(modifier = Modifier.width(32.dp))
                Text("P&L", fontSize = 11.sp, color = TextGray)
            }
        }

        HorizontalDivider(color = DarkCardBorder)

        // Holdings List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (holdings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No Holdings Found", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your connected broker account has no open holdings.", color = TextGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(holdings) { holding ->
                    HoldingItemRow(item = holding)
                    HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
                }
            }
        }
    }
    }
}

@Composable
fun HoldingItemRow(item: PortfolioHoldingEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Symbol Info
            Column(modifier = Modifier.weight(1.5f)) {
                Text(item.symbol, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(DarkCardBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(item.exchange, fontSize = 9.sp, color = TextGray)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${item.qty} Qty", fontSize = 11.sp, color = TextGray)
                }
            }

            // Prices
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(String.format("₹%,.2f", item.avgPrice), fontSize = 12.sp, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(String.format("₹%,.2f", item.ltp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextWhite)
            }

            // PNL & Sparkline
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format("%s₹%,.2f", if (item.pnl >= 0) "+" else "", item.pnl),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.pnl >= 0) ProfitGreen else LossRed
                )
                Text(
                    text = String.format("(%s%.2f%%)", if (item.pnlPercent >= 0) "+" else "", item.pnlPercent),
                    fontSize = 10.sp,
                    color = if (item.pnl >= 0) ProfitGreen else LossRed
                )
            }
        }
    }
}

@Composable
fun DonutChart(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Background Track
        drawArc(
            color = DarkCardBorder,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )

        // Equity Segment (Gold)
        drawArc(
            color = PrimaryGold,
            startAngle = -90f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Options Segment (Secondary Gold)
        drawArc(
            color = SecondaryGold,
            startAngle = 60f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Commodities Segment (Dark Gold)
        drawArc(
            color = Color(0xFF8C7335), // Dark Gold
            startAngle = 170f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
