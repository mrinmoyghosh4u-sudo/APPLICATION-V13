package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AISignalEntity
import com.example.data.model.OrderEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CrownLogo
import com.example.ui.components.GoldCard
import com.example.ui.components.MarketDataStatusIndicator
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.theme.*
import com.example.util.MarketStatusUtil

@Composable
fun IndexSparkline(
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isPositive) ProfitGreen else LossRed
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()
        val points = if (isPositive) {
            listOf(0.75f, 0.6f, 0.65f, 0.4f, 0.5f, 0.3f, 0.15f)
        } else {
            listOf(0.15f, 0.3f, 0.25f, 0.5f, 0.45f, 0.7f, 0.85f)
        }
        val stepX = width / (points.size - 1)
        path.moveTo(0f, points[0] * height)
        for (i in 1 until points.size) {
            val x = i * stepX
            val y = points[i] * height
            val prevX = (i - 1) * stepX
            val prevY = points[i - 1] * height
            path.cubicTo((prevX + x) / 2f, prevY, (prevX + x) / 2f, y, x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun HomeScreen(
    userProfile: UserProfileEntity,
    orders: List<OrderEntity> = emptyList(),
    aiSignals: List<AISignalEntity> = emptyList(),
    apiError: String? = null,
    watchlist: List<WatchlistItem> = emptyList(),
    marketDataSource: String = "Angel One",
    marketDataLastUpdated: String = "",
    onNavigateToOrders: () -> Unit,
    onNavigateToMarket: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToIndexDetails: (String, String) -> Unit,
    onOpenNotificationCenter: () -> Unit,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit,
    onRefresh: () -> Unit = {}
) {
    var selectedExchange by remember { mutableStateOf("NSE") }

    val detailedStatus = remember(selectedExchange) { MarketStatusUtil.getDetailedMarketStatus(selectedExchange) }
    val isMarketOpen = detailedStatus.isOpen

    val exchangeSignals = remember(aiSignals, selectedExchange, isMarketOpen) {
        if (!isMarketOpen) emptyList()
        else aiSignals.filter { it.exchange.equals(selectedExchange, ignoreCase = true) }
    }
    val topSignal = exchangeSignals.firstOrNull()

    PullToRefreshLayout(onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            
            if (!apiError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(LossRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, LossRed, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("API Error: $apiError", color = LossRed, fontSize = 12.sp)
                }
            }
            
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownLogo(size = 36.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("KING KHAN AI TRADE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextWhite)
                        Text("Trade Like a King 👑", fontSize = 10.sp, color = SecondaryGold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenNotificationCenter) {
                        Box {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = SecondaryGold)
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ProfitGreen, CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Broker Live Status
            MarketDataStatusIndicator(
                source = marketDataSource,
                lastUpdatedTime = marketDataLastUpdated,
                isMarketOpen = isMarketOpen,
                onRefresh = onRefresh,
                onReconnect = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Exchange Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .border(1.dp, PrimaryGold, RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("NSE", "BSE", "MCX").forEach { exchange ->
                    val isSelected = selectedExchange == exchange
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SecondaryGold else Color.Transparent)
                            .clickable { selectedExchange = exchange },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = exchange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Index Cards
            val indexesToShow = when (selectedExchange) {
                "NSE" -> listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY")
                "BSE" -> listOf("SENSEX", "BANKEX")
                "MCX" -> listOf("CRUDEOIL", "CRUDEOIL M")
                else -> emptyList()
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(indexesToShow) { indexName ->
                    val indexItem = watchlist.find { 
                        it.symbol.equals(indexName, ignoreCase = true) || 
                        (indexName == "MIDCPNIFTY" && it.symbol.contains("MID", ignoreCase = true)) 
                    }
                    val ltp = indexItem?.ltp ?: 0.0
                    val change = indexItem?.change ?: 0.0
                    val changePercent = indexItem?.changePercent ?: 0.0
                    val isPositive = indexItem?.isPositive ?: true
                    val color = if (isPositive) ProfitGreen else LossRed
                    val hasData = ltp > 0.0

                    Surface(
                        modifier = Modifier
                            .width(160.dp)
                            .clickable { onNavigateToIndexDetails(selectedExchange, indexName) },
                        color = DarkCard,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasData) DarkGold else LossRed.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = indexName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = if (hasData) color else TextGray,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .rotate(if (isPositive) -45f else 45f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = if (hasData) String.format("%,.2f", ltp) else "DATA UNAVAILABLE",
                                fontSize = if (hasData) 15.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasData) TextWhite else LossRed
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (hasData) {
                                Text(
                                    text = String.format("%s%.2f (%s%.2f%%)", if (isPositive) "+" else "", change, if (isPositive) "+" else "", changePercent),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = color,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isMarketOpen) "LIVE" else "CLOSED",
                                    fontSize = 9.sp,
                                    color = if (isMarketOpen) ProfitGreen else Color(0xFFFFB300),
                                    fontWeight = FontWeight.Bold
                                )
                                if (marketDataLastUpdated.isNotBlank() && marketDataLastUpdated != "Not Updated") {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = marketDataLastUpdated,
                                        fontSize = 8.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "DISCONNECTED",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Check connection", fontSize = 9.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Account Summary Cards
            val isBrokerConnected = userProfile.isAngelConnected || userProfile.isDhanConnected || userProfile.connectedBroker.isNotBlank()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Available Margin
                    GoldCard(modifier = Modifier.weight(1f)) {
                        Text("Available Margin", fontSize = 11.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isBrokerConnected) {
                            Text(String.format("₹%,.2f", userProfile.availableMargin), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        } else {
                            Text("DISCONNECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(DarkCardSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(12.dp))
                        }
                    }
                    // Today's P&L
                    GoldCard(modifier = Modifier.weight(1f)) {
                        val pnlColor = if (userProfile.todaysPnl >= 0) ProfitGreen else LossRed
                        Text("Today's P&L", fontSize = 11.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isBrokerConnected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    String.format("%s₹%,.2f", if (userProfile.todaysPnl >= 0) "+" else "", userProfile.todaysPnl),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pnlColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(String.format("(%s%.2f%%)", if (userProfile.todaysPnlPercent >= 0) "+" else "", userProfile.todaysPnlPercent), fontSize = 10.sp, color = pnlColor)
                            }
                        } else {
                            Text("DISCONNECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                        }
                    }
                }
                
                // Account Balance
                GoldCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Account Balance", fontSize = 11.sp, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isBrokerConnected) {
                                Text(String.format("₹%,.2f", userProfile.accountBalance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            } else {
                                Text("DISCONNECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(DarkCardSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIVE AI SIGNAL CARD or MARKET CLOSED BANNER
            val indexItemForSignal = watchlist.find { it.symbol.contains(selectedExchange, ignoreCase = true) || it.exchange.equals(selectedExchange, ignoreCase = true) }
            val underlyingLtp = indexItemForSignal?.ltp ?: 0.0

            GoldCard(
                borderColor = if (isMarketOpen) PrimaryGold else DarkCardBorder,
                borderWidth = 1.dp
            ) {
                if (!isMarketOpen) {
                    // Market Closed Display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("★ AI OPTION SIGNALS (OPTION BUYER ONLY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                            Box(
                                modifier = Modifier
                                    .background(LossRedBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("MARKET CLOSED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LossRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("$selectedExchange Market is Currently Closed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(detailedStatus.nextOpeningTimeText, fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Live BUY CE/PE signals will automatically resume when exchange trading begins.", fontSize = 10.sp, color = TextGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                } else if (topSignal != null) {
                    // Active Signal Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("★ LIVE AI SIGNAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(ProfitGreenBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Confidence ", fontSize = 11.sp, color = TextGray)
                            Text("${topSignal.confidence}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val isPe = topSignal.actionType.contains("PE") || topSignal.symbol.contains("PE")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCardSecondary, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (isPe) LossRedBg else ProfitGreenBg, CircleShape)
                                .border(1.dp, if (isPe) LossRed else ProfitGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPe) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isPe) LossRed else ProfitGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(topSignal.symbol, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(
                                text = if (underlyingLtp > 0.0) String.format("Underlying LTP: ₹%,.2f", underlyingLtp) else String.format("Signal LTP ₹%.2f", topSignal.ltp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextWhite
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(topSignal.actionType, fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isPe) LossRed else ProfitGreen)
                            Text("Trend: ${topSignal.trend}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricColumn("Entry Zone", topSignal.entryZone)
                        MetricColumn("Stop Loss", "₹${String.format("%.2f", topSignal.stopLoss)}")
                        MetricColumn("Target 1", "₹${String.format("%.2f", topSignal.target1)}")
                        MetricColumn("Target 2", "₹${String.format("%.2f", topSignal.target2)}")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricColumn("Target 3", "₹${String.format("%.2f", topSignal.target3)}")
                        MetricColumn("Target 4", "₹${String.format("%.2f", topSignal.trailingSl * 1.5)}") // Assuming target4 based on SL logic if not in model
                        MetricColumn("Trailing SL", "₹${String.format("%.2f", topSignal.trailingSl)}")
                        MetricColumn("Time", topSignal.timestamp.substringAfter(" ").substringBeforeLast(":"))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onOpenOrderDialog(topSignal.symbol, if (isPe) "BUY" else "BUY", topSignal.ltp, topSignal.lotSize)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPe) LossRed else ProfitGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "EXECUTE SIGNAL (${topSignal.actionType})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // No Active Signal
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("★ AI OPTION SIGNALS (OPTION BUYER ONLY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Analyzing real-time market feed for High-Probability Option Buyer Setups...", fontSize = 11.sp, color = TextGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recent Orders Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("☷ Recent Orders", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                TextButton(onClick = onNavigateToOrders) {
                    Text("View All >", fontSize = 12.sp, color = SecondaryGold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (orders.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No recent orders found.", fontSize = 12.sp, color = TextGray)
                    }
                }
            } else {
                orders.take(3).forEach { order ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = DarkCard,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(order.symbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text("${order.side} • ${order.qty} Qty @ ₹${order.price}", fontSize = 11.sp, color = TextGray)
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (order.status) {
                                            "EXECUTED", "COMPLETE" -> ProfitGreenBg
                                            "PENDING", "OPEN" -> SecondaryGold.copy(alpha = 0.2f)
                                            else -> LossRedBg
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    order.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (order.status) {
                                        "EXECUTED", "COMPLETE" -> ProfitGreen
                                        "PENDING", "OPEN" -> SecondaryGold
                                        else -> LossRed
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = TextGray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
    }
}
