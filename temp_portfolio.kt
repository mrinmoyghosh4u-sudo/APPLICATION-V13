package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.CrownLogo
import com.example.ui.components.DonutChart
import com.example.ui.components.GoldButton
import com.example.ui.components.GoldCard
import com.example.ui.components.SparklineChart
import com.example.ui.theme.*

@Composable
fun PortfolioScreen(
    userProfile: UserProfileEntity,
    holdings: List<PortfolioHoldingEntity>,
    onNavigateToProfile: () -> Unit,
    onOpenNotificationCenter: () -> Unit
) {
    val totalVal = if (holdings.isNotEmpty()) holdings.sumOf { it.currentValue } else userProfile.accountBalance
    val totalRealized = userProfile.realizedPnl
    val totalUnrealized = userProfile.unrealizedPnl
    val totalPnl = totalRealized + totalUnrealized
    val totalPnlPct = if (totalVal > 0) (totalPnl / totalVal) * 100 else 0.0

    var activeTab by remember { mutableStateOf("Holdings (${holdings.size})") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CrownLogo(size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("KING KHAN AI TRADE", fontSize = 15.sp, fontWeight = FontWeight.Black, color = TextWhite)
                    Text("Trade Like a King 👑", fontSize = 10.sp, color = SecondaryGold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(ProfitGreen, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        val brokerName = userProfile.connectedBroker.ifEmpty { "Live Broker" }
                        Text("Live Broker • $brokerName", fontSize = 10.sp, color = TextWhite)
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onOpenNotificationCenter) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = SecondaryGold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Portfolio",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Text(
                text = "View Profile >",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryGold,
                modifier = Modifier.clickable { onNavigateToProfile() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Total Portfolio Banner Card
        GoldCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            borderColor = PrimaryGold,
            borderWidth = 1.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Portfolio Value", fontSize = 11.sp, color = TextGray)
                    Text(String.format("₹%,.2f", totalVal), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Overall Profit / Loss", fontSize = 10.sp, color = TextGray)
                    val pnlColor = if (totalPnl >= 0) ProfitGreen else LossRed
                    Text(
                        String.format("%s₹%,.2f (%s%.2f%%)", if (totalPnl >= 0) "+" else "", totalPnl, if (totalPnlPct >= 0) "+" else "", totalPnlPct),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Today's Profit / Loss", fontSize = 10.sp, color = TextGray)
                    val todaysPnlColor = if (userProfile.todaysPnl >= 0) ProfitGreen else LossRed
                    Text(
                        String.format("%s₹%,.2f (%s%.2f%%)", if (userProfile.todaysPnl >= 0) "+" else "", userProfile.todaysPnl, if (0.0 >= 0) "+" else "", 0.0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = todaysPnlColor
                    )
                }

                DonutChart(modifier = Modifier.size(110.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Funds & Margin Breakdown Card (Requirement 3)
        GoldCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            borderColor = DarkCardBorder
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Broker & Funds Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    Text(
                        text = "${userProfile.connectedBroker.ifEmpty { "Paper Trading" }} • ID: ${userProfile.angelClientId.ifEmpty { userProfile.dhanClientId.ifEmpty { "KK-90124" } }}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Available Margin", fontSize = 9.sp, color = TextGray)
                        Text(String.format("₹%,.2f", userProfile.availableMargin), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                    Column {
                        Text("Used Margin", fontSize = 9.sp, color = TextGray)
                        val usedMargin = (userProfile.accountBalance - userProfile.availableMargin).coerceAtLeast(0.0)
                        Text(String.format("₹%,.2f", usedMargin), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Free Margin", fontSize = 9.sp, color = TextGray)
                        Text(String.format("₹%,.2f", userProfile.availableMargin), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Fund Balance", fontSize = 9.sp, color = TextGray)
                        Text(String.format("₹%,.2f", userProfile.accountBalance), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                    Column {
                        Text("Total Investment", fontSize = 9.sp, color = TextGray)
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

        Spacer(modifier = Modifier.height(12.dp))

        // Portfolio Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Holdings (${holdings.size})", "Positions", "Analytics").forEach { tab ->
                val isSelected = activeTab.startsWith(tab.take(8))
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { activeTab = tab },
                    color = if (isSelected) PrimaryGold else DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else TextGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Holdings List
        if (holdings.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(holdings) { item ->
                    HoldingItemCard(item)
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = SecondaryGold,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Holdings Available",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val activeB = userProfile.connectedBroker.ifEmpty { "Dhan" }
                    Text(
                        text = "You currently do not have any delivery holdings in your $activeB account.",
                        fontSize = 11.sp,
                        color = TextGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Bottom Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
            ) {
                Text("ANALYZE PORTFOLIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            }

            GoldButton(
                text = "REBALANCE",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HoldingItemCard(item: PortfolioHoldingEntity) {
    GoldCard(borderColor = DarkCardBorder) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(DarkCardSecondary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("${item.exchange} ${item.type}", fontSize = 8.sp, color = TextGray)
                    }
                }
                if (item.expiry.isNotEmpty()) {
                    Text(item.expiry, fontSize = 10.sp, color = TextGray)
                }
            }

            SparklineChart(isPositive = item.pnl >= 0)

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("+₹%,.2f", item.pnl),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.pnl >= 0) ProfitGreen else LossRed
                )
                Text(
                    text = String.format("(+%.2f%%)", item.pnlPercent),
                    fontSize = 10.sp,
                    color = if (item.pnl >= 0) ProfitGreen else LossRed
                )
            }
        }

        if (item.qty > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Qty: ${item.qty}", fontSize = 10.sp, color = TextGray)
                Text(String.format("Avg: ₹%,.2f", item.avgPrice), fontSize = 10.sp, color = TextGray)
                Text(String.format("LTP: ₹%,.2f", item.ltp), fontSize = 10.sp, color = TextGray)
                Text(String.format("Value: ₹%,.2f", item.currentValue), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}
