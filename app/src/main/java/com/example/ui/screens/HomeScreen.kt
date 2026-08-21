package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AISignalEntity
import com.example.data.model.MarketDataStore
import com.example.data.model.OrderEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CrownLogo
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.components.SparklineChart
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
    onNavigateToAISignals: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToIndexDetails: (String, String) -> Unit = { _, _ -> },
    onOpenNotificationCenter: () -> Unit = {},
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit = { _, _, _, _ -> },
    onRefresh: () -> Unit = {}
) {
    var isPortfolioVisible by rememberSaveable { mutableStateOf(true) }
    var selectedMoverTab by rememberSaveable { mutableStateOf("Top Gainers") }

    val marketDataMap by MarketDataStore.marketData.collectAsStateWithLifecycle()

    val nseStatus = remember { MarketStatusUtil.getDetailedMarketStatus("NSE") }
    val isMarketOpen = nseStatus.isOpen

    val isRefreshingState = viewModel?.isRefreshing?.collectAsStateWithLifecycle()
    val isRefreshing = isRefreshingState?.value ?: false

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090A0C))
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

            // 1. TOP HEADER (Logo, King Khan AI Trade, Search, Notifications, Profile)
            HomeHeaderSection(
                onNavigateToMarket = onNavigateToMarket,
                onOpenNotificationCenter = onOpenNotificationCenter,
                onNavigateToProfile = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. MARKET STATUS BAR
            MarketStatusBarSection(
                isMarketOpen = isMarketOpen,
                nextOpeningText = nseStatus.nextOpeningTimeText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. PORTFOLIO OVERVIEW CARD
            PortfolioOverviewSection(
                userProfile = userProfile,
                orders = orders,
                isVisible = isPortfolioVisible,
                onToggleVisibility = { isPortfolioVisible = !isPortfolioVisible }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. QUICK ACTIONS
            QuickActionsSection(
                onNavigateToMarket = onNavigateToMarket,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToOrders = onNavigateToOrders,
                onOpenNotificationCenter = onOpenNotificationCenter
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. AI MARKET INSIGHTS
            AiMarketInsightsSection(
                marketDataMap = marketDataMap,
                onNavigateToAISignals = onNavigateToAISignals
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6. TOP MOVERS
            TopMoversSection(
                selectedTab = selectedMoverTab,
                onTabSelected = { selectedMoverTab = it },
                marketDataMap = marketDataMap,
                onOpenOrderDialog = onOpenOrderDialog,
                onNavigateToMarket = onNavigateToMarket
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 7. WATCHLIST
            WatchlistSection(
                marketDataMap = marketDataMap,
                fallbackWatchlist = watchlist,
                onNavigateToIndexDetails = onNavigateToIndexDetails,
                onNavigateToMarket = onNavigateToMarket
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 1. TOP HEADER
// ==========================================
@Composable
private fun HomeHeaderSection(
    onNavigateToMarket: () -> Unit,
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
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "KING KHAN AI TRADE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Trade Like a King 👑",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onNavigateToMarket,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = PrimaryGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier.clickable { onOpenNotificationCenter() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = PrimaryGold,
                    modifier = Modifier.size(24.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(15.dp)
                        .background(PrimaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "5",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onNavigateToProfile,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Profile",
                    tint = PrimaryGold,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. MARKET STATUS BAR
// ==========================================
@Composable
private fun MarketStatusBarSection(
    isMarketOpen: Boolean,
    nextOpeningText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF14171C), RoundedCornerShape(16.dp))
            .border(0.8.dp, Color(0xFF23272F), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
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
                color = if (isMarketOpen) ProfitGreen else LossRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Next Opening: $nextOpeningText",
                color = TextGray,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ==========================================
// 3. PORTFOLIO OVERVIEW CARD
// ==========================================
@Composable
private fun PortfolioOverviewSection(
    userProfile: UserProfileEntity,
    orders: List<OrderEntity>,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    val totalEquity = userProfile.totalBalance
    val todayPnl = userProfile.todaysPnl
    val todayPnlPct = userProfile.todaysPnlPercent
    val isProfit = todayPnl >= 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13161B), RoundedCornerShape(12.dp))
            .border(0.8.dp, Color(0xFF262A32), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Portfolio Overview",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = TextGray,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onToggleVisibility() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Equity",
                    color = TextGray,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isVisible) "₹${String.format("%,.2f", totalEquity)}" else "••••••••",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isVisible) {
                        "${if (isProfit) "+" else ""}₹${String.format("%,.2f", todayPnl)} (${if (isProfit) "+" else ""}${String.format("%.2f", todayPnlPct)}%)"
                    } else "••••••",
                    color = if (isProfit) ProfitGreen else LossRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            SparklineChart(
                isPositive = isProfit,
                modifier = Modifier.size(width = 90.dp, height = 36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Grid Stats
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PortfolioStatBox(
                    label = "Available Margin",
                    value = if (isVisible) "₹${String.format("%,.2f", userProfile.availableMargin)}" else "••••••",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconColor = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
                PortfolioStatBox(
                    label = "Used Margin",
                    value = if (isVisible) "₹${String.format("%,.2f", (userProfile.totalBalance - userProfile.availableMargin).coerceAtLeast(0.0))}" else "••••••",
                    icon = Icons.Outlined.WorkOutline,
                    iconColor = PrimaryGold,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PortfolioStatBox(
                    label = "Unrealized P&L",
                    value = if (isVisible) "₹1,150.25" else "••••••",
                    icon = Icons.Outlined.History,
                    iconColor = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
                PortfolioStatBox(
                    label = "Realized P&L",
                    value = if (isVisible) "₹1,200.50" else "••••••",
                    icon = Icons.Outlined.BarChart,
                    iconColor = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PortfolioStatBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF191C22), RoundedCornerShape(8.dp))
            .border(0.6.dp, Color(0xFF262B34), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = label, color = TextGray, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ==========================================
// 4. QUICK ACTIONS
// ==========================================
@Composable
private fun QuickActionsSection(
    onNavigateToMarket: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onOpenNotificationCenter: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Quick Actions",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionButton(
                label = "Trade",
                icon = Icons.Outlined.ShowChart,
                onClick = onNavigateToMarket
            )
            QuickActionButton(
                label = "Funds",
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onNavigateToProfile
            )
            QuickActionButton(
                label = "Positions",
                icon = Icons.Outlined.Layers,
                onClick = onNavigateToOrders
            )
            QuickActionButton(
                label = "Orders",
                icon = Icons.Outlined.Assignment,
                onClick = onNavigateToOrders
            )
            QuickActionButton(
                label = "Alerts",
                icon = Icons.Outlined.NotificationsNone,
                onClick = onOpenNotificationCenter
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFF14171C), CircleShape)
                .border(0.8.dp, Color(0xFF2D323C), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PrimaryGold,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// 5. AI MARKET INSIGHTS
// ==========================================
@Composable
private fun AiMarketInsightsSection(
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    onNavigateToAISignals: () -> Unit
) {
    val niftyState = marketDataMap["NIFTY 50"] ?: marketDataMap["NIFTY"]
    val ltp = niftyState?.ltp ?: 24231.85
    val change = niftyState?.change ?: (-55.85)
    val isBullish = change >= 0

    val supportPrice = (ltp * 0.99).toInt()
    val resistancePrice = (ltp * 1.01).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Market Insights",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All >",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToAISignals() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13161C), RoundedCornerShape(12.dp))
                .border(0.8.dp, Color(0xFF282C35), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NIFTY 50",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isBullish) Color(0xFF163824) else Color(0xFF381616),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isBullish) "↑ BULLISH" else "↓ BEARISH",
                                color = if (isBullish) ProfitGreen else LossRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isBullish) "Strong support at $supportPrice. Break above $resistancePrice can push towards ${resistancePrice + 300}."
                        else "Resistance seen at $resistancePrice. Break below $supportPrice can push towards ${supportPrice - 300}.",
                        color = TextGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Key Levels", color = TextGray, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(text = "Support", color = TextGray, fontSize = 9.sp)
                            Text(text = String.format("%,d", supportPrice), color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "Resistance", color = TextGray, fontSize = 9.sp)
                            Text(text = String.format("%,d", resistancePrice), color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(PrimaryGold.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, PrimaryGold.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CrownLogo(size = 36.dp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "AI BULL",
                            color = PrimaryGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. TOP MOVERS
// ==========================================
@Composable
private fun TopMoversSection(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit,
    onNavigateToMarket: () -> Unit
) {
    val moverTabs = listOf("Top Gainers", "Top Losers", "High Volume", "High OI Chg")

    val fallbackMovers = listOf(
        MoverItemData("RELIANCE", "NSE", 2950.45, 2.35, 1),
        MoverItemData("TATASTEEL", "NSE", 142.60, 1.89, 1),
        MoverItemData("HDFCBANK", "NSE", 1678.40, 1.45, 1)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top Movers",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All >",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToMarket() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            moverTabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) PrimaryGold else Color(0xFF14171C),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            0.6.dp,
                            if (isSelected) PrimaryGold else Color(0xFF282C35),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Movers List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fallbackMovers.forEach { item ->
                val tick = marketDataMap[item.symbol]
                val ltp = tick?.ltp ?: item.price
                val pct = tick?.changePercent ?: item.changePct

                MoverListItem(
                    symbol = item.symbol,
                    exchange = item.exchange,
                    price = ltp,
                    changePct = pct,
                    onTrade = { onOpenOrderDialog(item.symbol, "BUY", ltp, item.lotSize) }
                )
            }
        }
    }
}

private data class MoverItemData(
    val symbol: String,
    val exchange: String,
    val price: Double,
    val changePct: Double,
    val lotSize: Int
)

@Composable
private fun MoverListItem(
    symbol: String,
    exchange: String,
    price: Double,
    changePct: Double,
    onTrade: () -> Unit
) {
    val isPositive = changePct >= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13161C), RoundedCornerShape(8.dp))
            .border(0.6.dp, Color(0xFF23272F), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.StarBorder,
                contentDescription = "Favorite",
                tint = PrimaryGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = exchange, color = TextGray, fontSize = 9.sp)
            }
        }

        SparklineChart(
            isPositive = isPositive,
            modifier = Modifier.size(width = 50.dp, height = 20.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%,.2f", price)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", changePct)}%",
                    color = if (isPositive) ProfitGreen else LossRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onTrade,
                modifier = Modifier
                    .height(28.dp)
                    .widthIn(min = 60.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "TRADE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// 7. WATCHLIST
// ==========================================
@Composable
private fun WatchlistSection(
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    fallbackWatchlist: List<WatchlistItem>,
    onNavigateToIndexDetails: (String, String) -> Unit,
    onNavigateToMarket: () -> Unit
) {
    val items = listOf(
        WatchlistItem("NIFTY 50", "NSE", 24231.85, -55.85, -0.23, 65, true),
        WatchlistItem("BANKNIFTY", "NSE", 57239.75, -22.65, -0.04, 30, true),
        WatchlistItem("FINNIFTY", "NSE", 26019.40, -95.00, -0.36, 60, true)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Watchlist",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All >",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToMarket() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val tick = marketDataMap[item.symbol]
                val ltp = tick?.ltp ?: item.ltp
                val pct = tick?.changePercent ?: item.changePercent
                val isPositive = pct >= 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13161C), RoundedCornerShape(8.dp))
                        .border(0.6.dp, Color(0xFF23272F), RoundedCornerShape(8.dp))
                        .clickable { onNavigateToIndexDetails(item.exchange, item.symbol) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = PrimaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = item.symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = item.exchange, color = TextGray, fontSize = 9.sp)
                        }
                    }

                    SparklineChart(
                        isPositive = isPositive,
                        modifier = Modifier.size(width = 50.dp, height = 20.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("%,.2f", ltp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.2f", pct)}%",
                                color = if (isPositive) ProfitGreen else LossRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
