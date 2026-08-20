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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AISignalEntity
import com.example.data.model.OrderEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CrownLogo
import com.example.ui.components.GoldCard
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.theme.*
import com.example.util.MarketStatusUtil
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    userProfile: UserProfileEntity,
    orders: List<OrderEntity> = emptyList(),
    aiSignals: List<AISignalEntity> = emptyList(),
    apiError: String? = null,
    watchlist: List<WatchlistItem> = emptyList(),
    marketDataSource: String = "Angel One",
    marketDataLastUpdated: String = "",
    viewModel: MainViewModel? = null,
    onNavigateToOrders: () -> Unit = {},
    onNavigateToMarket: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToIndexDetails: (String, String) -> Unit = { _, _ -> },
    onOpenNotificationCenter: () -> Unit = {},
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit = { _, _, _, _ -> },
    onRefresh: () -> Unit = {}
) {
    var selectedExchange by rememberSaveable { mutableStateOf("NSE") }

    val detailedStatus = remember(selectedExchange) { MarketStatusUtil.getDetailedMarketStatus(selectedExchange) }
    val isMarketOpen = detailedStatus.isOpen

    val isRefreshingState = viewModel?.isRefreshing?.collectAsStateWithLifecycle()
    val isRefreshing = isRefreshingState?.value ?: false

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0F12))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            
            if (!apiError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .background(LossRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, LossRed, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text("API Alert: $apiError", color = LossRed, fontSize = 11.sp)
                }
            }

            // 1. Top Bar (Logo, Brand Name, Search, Notifications, Profile)
            HomeTopHeader(
                onOpenNotificationCenter = onOpenNotificationCenter,
                onNavigateToProfile = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Market Status Bar (Market Closed / Open status)
            MarketStatusBar(
                isMarketOpen = isMarketOpen,
                nextOpeningText = detailedStatus.nextOpeningTimeText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Exchange Selector Bar (NSE, BSE, MCX)
            ExchangeSelectorBar(
                selectedExchange = selectedExchange,
                onExchangeSelected = { selectedExchange = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Index Cards Row (NIFTY 50, BANKNIFTY, FINNIFTY, MIDCPNIFTY, etc.)
            IndexCardsSection(
                selectedExchange = selectedExchange,
                viewModel = viewModel,
                fallbackWatchlist = watchlist,
                isMarketOpen = isMarketOpen,
                onNavigateToIndexDetails = onNavigateToIndexDetails
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Account Summary Section
            AccountSummarySection(
                userProfile = userProfile,
                orders = orders
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6. KING KHAN AI SIGNAL (PRIMARY)
            PrimaryAISignalSection(
                selectedExchange = selectedExchange,
                aiSignals = aiSignals,
                viewModel = viewModel,
                fallbackWatchlist = watchlist,
                onOpenOrderDialog = onOpenOrderDialog
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 7. AI OPTION BUYER SIGNALS
            OptionBuyerSignalsSection(
                aiSignals = aiSignals,
                onOpenOrderDialog = onOpenOrderDialog
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 8. OPEN POSITIONS
            OpenPositionsSection(
                orders = orders,
                onNavigateToOrders = onNavigateToOrders
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 9. RECENT ORDERS
            RecentOrdersSection(
                orders = orders,
                onNavigateToOrders = onNavigateToOrders
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 10. QUICK SHORTCUTS GRID
            QuickShortcutsGrid(
                onNavigateToOptionChain = onNavigateToMarket,
                onNavigateToScanner = onNavigateToMarket,
                onNavigateToChart = onNavigateToMarket,
                onNavigateToOrders = onNavigateToOrders,
                onNavigateToAISignals = onNavigateToMarket
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 1. TOP BAR
// ==========================================
@Composable
private fun HomeTopHeader(
    onOpenNotificationCenter: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CrownLogo(size = 38.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "KING KHAN AI TRADE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Trade Like a King 👑",
                    fontSize = 11.sp,
                    color = SecondaryGold,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextWhite, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onOpenNotificationCenter) {
                Box {
                    Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = SecondaryGold, modifier = Modifier.size(22.dp))
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(LossRed, CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("5", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
            }
            IconButton(onClick = onNavigateToProfile) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = SecondaryGold, modifier = Modifier.size(26.dp))
            }
        }
    }
}

// ==========================================
// 2. MARKET STATUS BAR
// ==========================================
@Composable
private fun MarketStatusBar(
    isMarketOpen: Boolean,
    nextOpeningText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF14171C))
            .border(0.8.dp, Color(0xFF2A2E35), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isMarketOpen) ProfitGreen else LossRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMarketOpen) "MARKET LIVE" else "MARKET CLOSED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMarketOpen) ProfitGreen else LossRed
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isMarketOpen) "Live Feed Active" else nextOpeningText,
                    fontSize = 10.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==========================================
// 3. EXCHANGE SELECTOR BAR
// ==========================================
@Composable
private fun ExchangeSelectorBar(
    selectedExchange: String,
    onExchangeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF14171C), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF2B2F38), RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("NSE" to Icons.Default.AccountBalance, "BSE" to Icons.Default.Domain, "MCX" to Icons.Default.CellTower).forEach { (exchange, icon) ->
            val isSelected = selectedExchange == exchange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) SecondaryGold else Color.Transparent)
                    .clickable { onExchangeSelected(exchange) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.Black else TextWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = exchange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else TextWhite
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. INDEX CARDS SECTION
// ==========================================
@Composable
private fun IndexCardsSection(
    selectedExchange: String,
    viewModel: MainViewModel?,
    fallbackWatchlist: List<WatchlistItem>,
    isMarketOpen: Boolean,
    onNavigateToIndexDetails: (String, String) -> Unit
) {
    val indexesToShow = remember(selectedExchange) {
        when (selectedExchange) {
            "NSE" -> listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY")
            "BSE" -> listOf("SENSEX", "BANKEX")
            "MCX" -> listOf("CRUDEOIL", "CRUDEOIL M")
            else -> emptyList()
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = indexesToShow,
            key = { indexName -> indexName }
        ) { indexName ->
            IndexCardItem(
                indexName = indexName,
                selectedExchange = selectedExchange,
                viewModel = viewModel,
                fallbackWatchlist = fallbackWatchlist,
                isMarketOpen = isMarketOpen,
                onNavigateToIndexDetails = onNavigateToIndexDetails
            )
        }
    }
}

@Composable
private fun IndexCardItem(
    indexName: String,
    selectedExchange: String,
    viewModel: MainViewModel?,
    fallbackWatchlist: List<WatchlistItem>,
    isMarketOpen: Boolean,
    onNavigateToIndexDetails: (String, String) -> Unit
) {
    val itemFlow = remember(indexName, viewModel) {
        viewModel?.getWatchlistItemFlow(indexName)
    }
    val liveItem = itemFlow?.collectAsStateWithLifecycle(initialValue = null)?.value

    var cachedItem by remember(indexName) { mutableStateOf<WatchlistItem?>(null) }
    if (liveItem != null) {
        cachedItem = liveItem
    }

    val indexItem = liveItem ?: cachedItem ?: fallbackWatchlist.find {
        it.symbol.equals(indexName, ignoreCase = true) ||
        (indexName == "MIDCPNIFTY" && it.symbol.contains("MID", ignoreCase = true))
    }

    val defaultLtp = when (indexName) {
        "NIFTY 50" -> 24231.85
        "BANKNIFTY" -> 57239.75
        "FINNIFTY" -> 26019.40
        "MIDCPNIFTY" -> 14877.15
        "SENSEX" -> 79627.50
        "BANKEX" -> 65110.20
        else -> 8120.00
    }

    val ltp = if (indexItem != null && indexItem.ltp > 0.0) indexItem.ltp else defaultLtp
    val change = if (indexItem != null) indexItem.change else -55.85
    val changePercent = if (indexItem != null) indexItem.changePercent else -0.23
    val isPositive = change >= 0
    val color = if (isPositive) ProfitGreen else LossRed

    Surface(
        modifier = Modifier
            .width(155.dp)
            .clickable { onNavigateToIndexDetails(selectedExchange, indexName) },
        color = Color(0xFF14171C),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E35))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = indexName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = String.format("%,.2f", ltp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = String.format("%s%.2f (%s%.2f%%)", if (isPositive) "+" else "", change, if (isPositive) "+" else "", changePercent),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sparkline Graph
            IndexSparkline(
                isPositive = isPositive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .background(
                        if (isMarketOpen) ProfitGreen.copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                        RoundedCornerShape(3.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isMarketOpen) "LIVE" else "CLOSED",
                    fontSize = 8.sp,
                    color = if (isMarketOpen) ProfitGreen else Color(0xFFFFB300),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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

// ==========================================
// 5. ACCOUNT SUMMARY SECTION
// ==========================================
@Composable
private fun AccountSummarySection(
    userProfile: UserProfileEntity,
    orders: List<OrderEntity>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ACCOUNT SUMMARY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            }
            Text("View All >", fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 1: Available Margin & Account Balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMiniCard(
                title = "Available Margin",
                value = String.format("₹%,.2f", if (userProfile.availableMargin > 0) userProfile.availableMargin else 50000.0),
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
            SummaryMiniCard(
                title = "Account Balance",
                value = String.format("₹%,.2f", if (userProfile.accountBalance > 0) userProfile.accountBalance else 125000.0),
                icon = Icons.Default.Savings,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Today's P&L & Unrealized P&L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMiniCard(
                title = "Today's P&L",
                value = "+₹2,350.75",
                subText = "(+1.92%)",
                isPositive = true,
                icon = Icons.Default.ShowChart,
                modifier = Modifier.weight(1f)
            )
            SummaryMiniCard(
                title = "Unrealized P&L",
                value = "+₹1,150.25",
                subText = "(+0.94%)",
                isPositive = true,
                icon = Icons.Default.PieChart,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Active Positions Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF14171C),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2A2E35))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Active Positions", fontSize = 10.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("3", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                Icon(Icons.Default.Work, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SummaryMiniCard(
    title: String,
    value: String,
    subText: String? = null,
    isPositive: Boolean? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF14171C),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2A2E35))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = TextGray)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (isPositive) {
                        true -> ProfitGreen
                        false -> LossRed
                        else -> TextWhite
                    }
                )
                if (subText != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive == true) ProfitGreen else LossRed
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(icon, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ==========================================
// 6. PRIMARY AI SIGNAL CARD
// ==========================================
@Composable
private fun PrimaryAISignalSection(
    selectedExchange: String,
    aiSignals: List<AISignalEntity>,
    viewModel: MainViewModel?,
    fallbackWatchlist: List<WatchlistItem>,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("KING KHAN AI SIGNAL (PRIMARY)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            }
            Text("View All Signals >", fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Signal Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF14171C),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                
                // Top Title & Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NIFTY 50", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(ProfitGreenBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("BUY CE ↗", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                    }

                    // Confidence Gauge
                    CircularConfidenceGauge(confidence = 82)
                }

                Text("25,000 CE • 30 MAY 2024", fontSize = 11.sp, color = TextGray)

                Spacer(modifier = Modifier.height(12.dp))

                // Price Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SignalMetric("Entry", "₹125.40", TextWhite)
                    SignalMetric("Stop Loss", "₹110.00", LossRed)
                    SignalMetric("Target 1", "₹138.00", TextWhite)
                    SignalMetric("Target 2", "₹150.00", TextWhite)
                    SignalMetric("Target 3", "₹165.00", TextWhite)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("Time: 07:45 PM", fontSize = 10.sp, color = TextGray)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryGold)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VIEW DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { onOpenOrderDialog("NIFTY 25000 CE", "BUY", 125.40, 75) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CONFIRM TRADE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 10.sp, color = TextGray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CircularConfidenceGauge(confidence: Int) {
    Box(
        modifier = Modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            drawArc(
                color = Color(0xFF22262E),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = ProfitGreen,
                startAngle = 135f,
                sweepAngle = 270f * (confidence / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${confidence}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextWhite)
            Text("Confidence", fontSize = 7.sp, color = TextGray)
        }
    }
}

// ==========================================
// 7. AI OPTION BUYER SIGNALS
// ==========================================
@Composable
private fun OptionBuyerSignalsSection(
    aiSignals: List<AISignalEntity>,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI OPTION BUYER SIGNALS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            }
            Text("View All >", fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        val staticSignals = listOf(
            Triple("NIFTY CE", "• Strong BUY", "125.40" to "110.00"),
            Triple("NIFTY PE", "• BUY", "118.75" to "105.00"),
            Triple("BANKNIFTY CE", "• Strong BUY", "254.30" to "225.00"),
            Triple("BANKNIFTY PE", "• WAIT", "--" to "--")
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(staticSignals) { (symbol, status, prices) ->
                val isWait = status.contains("WAIT")
                val isStrong = status.contains("Strong")

                Surface(
                    modifier = Modifier.width(140.dp),
                    color = Color(0xFF14171C),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2A2E35))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(symbol, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWait) Color(0xFFFF9800) else ProfitGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Entry: ${prices.first}", fontSize = 10.sp, color = TextGray)
                        Text("SL: ${prices.second}", fontSize = 10.sp, color = TextGray)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(DarkCardSecondary, CircleShape)
                                    .border(1.dp, SecondaryGold, CircleShape)
                                    .clickable {
                                        if (!isWait) {
                                            onOpenOrderDialog(symbol, "BUY", prices.first.toDoubleOrNull(), 50)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = SecondaryGold,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .rotate(-45f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. OPEN POSITIONS SECTION
// ==========================================
@Composable
private fun OpenPositionsSection(
    orders: List<OrderEntity>,
    onNavigateToOrders: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Work, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("OPEN POSITIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            }
            Text("View All >", fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF14171C),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2A2E35))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Symbol", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(1.3f))
                    Text("Type", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.6f))
                    Text("Qty", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.5f))
                    Text("Avg", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.8f))
                    Text("LTP", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.8f))
                    Text("P&L", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.9f))
                }

                Divider(color = Color(0xFF2A2E35), thickness = 0.5.dp)

                val staticPositions = listOf(
                    PositionRowData("NIFTY 25,000 CE", "BUY", "75", "125.40", "128.95", "+266.25", true),
                    PositionRowData("BANKNIFTY 57,500 PE", "BUY", "25", "254.30", "251.10", "-80.00", false),
                    PositionRowData("FINNIFTY 26,000 CE", "BUY", "50", "116.75", "118.60", "+92.50", true)
                )

                staticPositions.forEach { pos ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pos.symbol, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.weight(1.3f))
                        Text(pos.type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfitGreen, modifier = Modifier.weight(0.6f))
                        Text(pos.qty, fontSize = 10.sp, color = TextWhite, modifier = Modifier.weight(0.5f))
                        Text(pos.avg, fontSize = 10.sp, color = TextWhite, modifier = Modifier.weight(0.8f))
                        Text(pos.ltp, fontSize = 10.sp, color = TextWhite, modifier = Modifier.weight(0.8f))
                        Text(pos.pnl, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (pos.isPositive) ProfitGreen else LossRed, modifier = Modifier.weight(0.9f))
                    }
                }
            }
        }
    }
}

private data class PositionRowData(
    val symbol: String,
    val type: String,
    val qty: String,
    val avg: String,
    val ltp: String,
    val pnl: String,
    val isPositive: Boolean
)

// ==========================================
// 9. RECENT ORDERS SECTION
// ==========================================
@Composable
private fun RecentOrdersSection(
    orders: List<OrderEntity>,
    onNavigateToOrders: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RECENT ORDERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            }
            Text("View All >", fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF14171C),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2A2E35))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Time", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.8f))
                    Text("Symbol", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(1.4f))
                    Text("Type", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.6f))
                    Text("Qty", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.5f))
                    Text("Price", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.8f))
                    Text("Status", fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.9f))
                }

                Divider(color = Color(0xFF2A2E35), thickness = 0.5.dp)

                val staticOrders = listOf(
                    OrderRowData("07:45 PM", "NIFTY 25,000 CE", "BUY", "75", "125.40", "COMPLETE"),
                    OrderRowData("07:30 PM", "BANKNIFTY 57,500 PE", "BUY", "25", "254.30", "COMPLETE"),
                    OrderRowData("07:15 PM", "FINNIFTY 26,000 CE", "BUY", "50", "116.75", "COMPLETE")
                )

                staticOrders.forEach { ord ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ord.time, fontSize = 9.sp, color = TextGray, modifier = Modifier.weight(0.8f))
                        Text(ord.symbol, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.weight(1.4f))
                        Text(ord.type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfitGreen, modifier = Modifier.weight(0.6f))
                        Text(ord.qty, fontSize = 10.sp, color = TextWhite, modifier = Modifier.weight(0.5f))
                        Text(ord.price, fontSize = 10.sp, color = TextWhite, modifier = Modifier.weight(0.8f))
                        Text(ord.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfitGreen, modifier = Modifier.weight(0.9f))
                    }
                }
            }
        }
    }
}

private data class OrderRowData(
    val time: String,
    val symbol: String,
    val type: String,
    val qty: String,
    val price: String,
    val status: String
)

// ==========================================
// 10. QUICK SHORTCUTS GRID
// ==========================================
@Composable
private fun QuickShortcutsGrid(
    onNavigateToOptionChain: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToChart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToAISignals: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ShortcutItem(title = "OPTION CHAIN", icon = Icons.Default.Link, modifier = Modifier.weight(1f), onClick = onNavigateToOptionChain)
        ShortcutItem(title = "SCANNER", icon = Icons.Default.FilterList, modifier = Modifier.weight(1f), onClick = onNavigateToScanner)
        ShortcutItem(title = "CHART", icon = Icons.Default.ShowChart, modifier = Modifier.weight(1f), onClick = onNavigateToChart)
        ShortcutItem(title = "ORDERS", icon = Icons.Default.ReceiptLong, modifier = Modifier.weight(1f), onClick = onNavigateToOrders)
        ShortcutItem(title = "AI SIGNALS", icon = Icons.Default.Psychology, modifier = Modifier.weight(1f), onClick = onNavigateToAISignals)
    }
}

@Composable
private fun ShortcutItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
        color = Color(0xFF14171C),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, SecondaryGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
