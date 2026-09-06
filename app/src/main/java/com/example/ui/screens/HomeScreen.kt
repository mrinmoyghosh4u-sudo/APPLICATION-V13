package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AISignalEntity
import com.example.data.model.MarketDataStore
import com.example.data.model.NotificationEntity
import com.example.data.model.OrderEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CrownLogo
import com.example.ui.components.MarketNewsDialog
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
    notifications: List<NotificationEntity> = emptyList(),
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
    var showMarketNewsDialog by remember { mutableStateOf(false) }

    val marketDataMap by MarketDataStore.ticks.collectAsStateWithLifecycle()

    val nseStatus = MarketStatusUtil.getDetailedMarketStatus("NSE")
    val bseStatus = MarketStatusUtil.getDetailedMarketStatus("BSE")
    val mcxStatus = MarketStatusUtil.getDetailedMarketStatus("MCX")
    val isMarketOpen = nseStatus.isOpen || bseStatus.isOpen || mcxStatus.isOpen

    val isRefreshingState = viewModel?.isRefreshing?.collectAsStateWithLifecycle()
    val isRefreshing = isRefreshingState?.value ?: false

    val unreadCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    if (showMarketNewsDialog) {
        MarketNewsDialog(
            viewModel = viewModel,
            onDismiss = { showMarketNewsDialog = false }
        )
    }

    BackHandler(enabled = showMarketNewsDialog) {
        showMarketNewsDialog = false
    }

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

            // 1. TOP HEADER (Logo, App Title, Live Provider Badge, Notifications Badge)
            val isLiveFeedActive by (viewModel?.isLiveFeedActive ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsStateWithLifecycle()
            HomeHeaderSection(
                isLiveFeedActive = isLiveFeedActive,
                marketDataSource = marketDataSource,
                unreadCount = unreadCount,
                onOpenNotificationCenter = onOpenNotificationCenter
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. MARKET STATUS BAR
            val nextOpeningText = when {
                isMarketOpen && mcxStatus.isOpen && !nseStatus.isOpen -> "MCX Active (09:00 - 23:30 IST)"
                isMarketOpen -> "Session Active (09:15 - 15:30 IST)"
                else -> nseStatus.nextOpeningTimeText
            }
            MarketStatusBarSection(
                isMarketOpen = isMarketOpen,
                nextOpeningText = nextOpeningText
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. MARKET OVERVIEW (All requested indices & MCX commodities)
            MarketOverviewSection(
                marketDataMap = marketDataMap,
                watchlist = watchlist,
                onNavigateToIndexDetails = onNavigateToIndexDetails,
                onNavigateToMarket = onNavigateToMarket
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. PORTFOLIO OVERVIEW CARD (Clean 2x2 Grid - Total Equity removed)
            PortfolioOverviewSection(
                userProfile = userProfile,
                isVisible = isPortfolioVisible,
                onToggleVisibility = { isPortfolioVisible = !isPortfolioVisible },
                onNavigateToProfile = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. QUICK ACTIONS (Trade, Funds, Positions, News, Options)
            QuickActionsSection(
                onNavigateToMarket = onNavigateToMarket,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToOrders = onNavigateToOrders,
                onOpenNews = { showMarketNewsDialog = true },
                onOpenOptionChain = { onNavigateToIndexDetails("NSE", "NIFTY 50") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6. AI MARKET INSIGHTS (Multi-Index Selector & Rectified AI Bull/Bear Card)
            AiMarketInsightsSection(
                marketDataMap = marketDataMap,
                watchlist = watchlist,
                viewModel = viewModel,
                onNavigateToAISignals = onNavigateToAISignals
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 7. RECENT ORDERS / POSITIONS SUMMARY
            RecentOrdersPositionsSection(
                orders = orders,
                onNavigateToOrders = onNavigateToOrders
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 1. TOP HEADER (Logo, App Title, Dhan Live Badge, Notification Icon)
// ==========================================
@Composable
private fun HomeHeaderSection(
    isLiveFeedActive: Boolean,
    marketDataSource: String,
    unreadCount: Int,
    onOpenNotificationCenter: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
                isLive = isLiveFeedActive, dataSource = marketDataSource,
                modifier = Modifier.padding(end = 6.dp)
            )

            // Notification Icon with dynamic badge ONLY when unreadCount > 0
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onOpenNotificationCenter() }
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = PrimaryGold,
                    modifier = Modifier.size(24.dp)
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-2).dp)
                            .size(16.dp)
                            .background(LossRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            color = Color.White,
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
                text = if (isMarketOpen) nextOpeningText else "Next Opening: $nextOpeningText",
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
// 3. MARKET OVERVIEW (All requested indices & commodities)
// ==========================================
@Composable
private fun MarketOverviewSection(
    marketDataMap: Map<String, com.example.data.model.RealTimePriceTick>,
    watchlist: List<WatchlistItem>,
    onNavigateToIndexDetails: (String, String) -> Unit,
    onNavigateToMarket: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }

    val allInstruments = remember {
        listOf(
            Triple("NSE", "NIFTY 50", "INDEX"),
            Triple("NSE", "BANKNIFTY", "INDEX"),
            Triple("NSE", "FINNIFTY", "INDEX"),
            Triple("NSE", "MIDCPNIFTY", "INDEX"),
            Triple("BSE", "SENSEX", "INDEX"),
            Triple("BSE", "BANKEX", "INDEX"),
            Triple("MCX", "CRUDEOIL", "COMMODITY"),
            Triple("MCX", "CRUDEOIL M", "COMMODITY")
        )
    }

    val filteredList = remember(selectedCategory) {
        when (selectedCategory) {
            "INDICES" -> allInstruments.filter { it.third == "INDEX" }
            "COMMODITIES" -> allInstruments.filter { it.third == "COMMODITY" }
            else -> allInstruments
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Market Overview",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live Terminal >",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToMarket() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All (8)",
                "INDICES" to "Indices (NSE/BSE)",
                "COMMODITIES" to "Commodities (MCX)"
            ).forEach { (catKey, catLabel) ->
                val isSelected = selectedCategory == catKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryGold else Color(0xFF161920))
                        .border(0.6.dp, if (isSelected) PrimaryGold else Color(0xFF282D38), RoundedCornerShape(12.dp))
                        .clickable { selectedCategory = catKey }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = catLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else TextWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filteredList.forEach { (exch, symbol, _) ->
                val tick = marketDataMap[symbol]
                    ?: marketDataMap.values.find { it.symbol.equals(symbol, ignoreCase = true) }
                    ?: marketDataMap.values.find { it.symbol.contains(symbol, ignoreCase = true) }
                
                val watchItem = watchlist.find { it.symbol.equals(symbol, ignoreCase = true) }

                val tickPrice = tick?.price ?: 0.0
                val watchLtp = watchItem?.ltp ?: 0.0
                val ltp = if (tickPrice > 0.0) tickPrice else watchLtp
                val pct = if (tickPrice > 0.0) 0.0 else (watchItem?.changePercent ?: 0.0)
                val change = if (tickPrice > 0.0) 0.0 else (watchItem?.change ?: 0.0)
                val isPositive = pct >= 0

                Column(
                    modifier = Modifier
                        .width(142.dp)
                        .background(Color(0xFF13161C), RoundedCornerShape(10.dp))
                        .border(0.6.dp, Color(0xFF23272F), RoundedCornerShape(10.dp))
                        .clickable { onNavigateToIndexDetails(exch, symbol) }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = symbol,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    when (exch) {
                                        "MCX" -> Color(0xFF382612)
                                        "BSE" -> Color(0xFF162538)
                                        else -> Color(0xFF163824)
                                    },
                                    RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = exch,
                                color = when (exch) {
                                    "MCX" -> PrimaryGold
                                    "BSE" -> Color(0xFF64B5F6)
                                    else -> ProfitGreen
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = if (ltp > 0.0) "₹${String.format("%,.2f", ltp)}" else "LIVE...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (ltp > 0.0) {
                            "${if (isPositive) "+" else ""}${String.format("%.2f", pct)}% (${if (isPositive) "+" else ""}${String.format("%.2f", change)})"
                        } else "Updating...",
                        color = if (ltp > 0.0) (if (isPositive) ProfitGreen else LossRed) else TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. PORTFOLIO OVERVIEW CARD (Clean 2x2 Grid - Total Equity Removed)
// ==========================================
@Composable
private fun PortfolioOverviewSection(
    userProfile: UserProfileEntity,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val usedMargin = (userProfile.totalBalance - userProfile.availableMargin).coerceAtLeast(0.0)

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

            val isDhanConnected = userProfile.isDhanConnected
            Text(
                text = if (isDhanConnected) "BROKER LIVE" else "READY",
                color = if (isDhanConnected) ProfitGreen else PrimaryGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1C2028))
                    .clickable { onNavigateToProfile() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2x2 Grid Stats
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val marginText = when {
                    !isVisible -> "••••••"
                    userProfile.isMarginAvailable -> "₹${String.format(java.util.Locale.US, "%,.2f", userProfile.availableMargin)}"
                    userProfile.isMarginStale -> "₹${String.format(java.util.Locale.US, "%,.2f", userProfile.availableMargin)} (Stale)"
                    userProfile.isBrokerConnected -> "Unavailable"
                    userProfile.availableMargin > 0 -> "₹${String.format(java.util.Locale.US, "%,.2f", userProfile.availableMargin)} (Paper)"
                    else -> "Unavailable"
                }
                PortfolioStatBox(
                    label = "Available Margin",
                    value = marginText,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconColor = if (userProfile.isMarginUnavailable && userProfile.isBrokerConnected) TextGray else ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
                val usedMargin = (userProfile.totalBalance - userProfile.availableMargin).coerceAtLeast(0.0)
                val usedMarginText = when {
                    !isVisible -> "••••••"
                    userProfile.isMarginAvailable -> "₹${String.format(java.util.Locale.US, "%,.2f", usedMargin)}"
                    userProfile.isMarginStale -> "₹${String.format(java.util.Locale.US, "%,.2f", usedMargin)} (Stale)"
                    userProfile.isBrokerConnected -> "Unavailable"
                    else -> "₹0.00"
                }
                PortfolioStatBox(
                    label = "Used Margin",
                    value = usedMarginText,
                    icon = Icons.Outlined.WorkOutline,
                    iconColor = if (userProfile.isMarginUnavailable && userProfile.isBrokerConnected) TextGray else PrimaryGold,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val unrealized = userProfile.unrealizedPnl
                val unrealizedText = when {
                    !isVisible -> "••••••"
                    userProfile.isPnlAvailable -> "${if (unrealized >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.2f", unrealized)}"
                    userProfile.isPnlStale -> "${if (unrealized >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.2f", unrealized)} (Stale)"
                    userProfile.isBrokerConnected -> "Unavailable"
                    else -> "₹0.00"
                }
                val unrealizedColor = when {
                    userProfile.isPnlUnavailable && userProfile.isBrokerConnected -> TextGray
                    unrealized >= 0 -> ProfitGreen
                    else -> LossRed
                }
                PortfolioStatBox(
                    label = "Unrealized P&L",
                    value = unrealizedText,
                    icon = Icons.Outlined.History,
                    iconColor = unrealizedColor,
                    modifier = Modifier.weight(1f)
                )

                val realized = userProfile.realizedPnl
                val realizedText = when {
                    !isVisible -> "••••••"
                    userProfile.isPnlAvailable -> "${if (realized >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.2f", realized)}"
                    userProfile.isPnlStale -> "${if (realized >= 0) "+" else ""}₹${String.format(java.util.Locale.US, "%,.2f", realized)} (Stale)"
                    userProfile.isBrokerConnected -> "Unavailable"
                    else -> "₹0.00"
                }
                val realizedColor = when {
                    userProfile.isPnlUnavailable && userProfile.isBrokerConnected -> TextGray
                    realized >= 0 -> ProfitGreen
                    else -> LossRed
                }
                PortfolioStatBox(
                    label = "Realized P&L",
                    value = realizedText,
                    icon = Icons.Outlined.BarChart,
                    iconColor = realizedColor,
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
// 5. QUICK ACTIONS (News replaces Order, Options Chain fixed)
// ==========================================
@Composable
private fun QuickActionsSection(
    onNavigateToMarket: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenOptionChain: () -> Unit
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
                icon = Icons.Outlined.TrendingUp,
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
                label = "News",
                icon = Icons.Outlined.Newspaper,
                onClick = onOpenNews
            )
            QuickActionButton(
                label = "Options",
                icon = Icons.Outlined.FormatListBulleted,
                onClick = onOpenOptionChain
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
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
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
// 6. AI MARKET INSIGHTS (All Indices & Commodities + Rectified AI Bull/Bear)
// ==========================================
@Composable
private fun AiMarketInsightsSection(
    marketDataMap: Map<String, com.example.data.model.RealTimePriceTick>,
    watchlist: List<WatchlistItem>,
    viewModel: MainViewModel? = null,
    onNavigateToAISignals: () -> Unit
) {
    val insightSymbols = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX", "CRUDEOIL", "CRUDEOIL M")
    var selectedSymbol by remember { mutableStateOf("NIFTY 50") }

    val tick = marketDataMap[selectedSymbol]
        ?: marketDataMap.values.find { it.symbol.equals(selectedSymbol, ignoreCase = true) }
        ?: marketDataMap.values.find { it.symbol.contains(selectedSymbol, ignoreCase = true) }
    val watchItem = watchlist.find { it.symbol.equals(selectedSymbol, ignoreCase = true) }

    var candleList by remember { mutableStateOf<List<com.example.ui.components.CandleData>>(emptyList()) }
    LaunchedEffect(selectedSymbol) {
        viewModel?.getHistoricalCandlesForIndex(selectedSymbol, "15m") { fetched ->
            candleList = fetched
        }
    }

    val tickPrice = tick?.price ?: 0.0
    val watchLtp = watchItem?.ltp ?: 0.0
    val ltp = if (tickPrice > 0.0) tickPrice else watchLtp
    val change = if (tickPrice > 0.0) 0.0 else (watchItem?.change ?: 0.0)
    val hasPrice = ltp > 0.0
    val isBullish = change >= 0

    val supportPrice = if (candleList.isNotEmpty()) {
        candleList.minOf { it.low }.toInt()
    } else {
        if (hasPrice) (ltp * 0.992).toInt() else 0
    }

    val resistancePrice = if (candleList.isNotEmpty()) {
        candleList.maxOf { it.high }.toInt()
    } else {
        if (hasPrice) (ltp * 1.008).toInt() else 0
    }

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
                text = "AI Signal Hub >",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToAISignals() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Index Selection Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            insightSymbols.forEach { sym ->
                val isSelected = selectedSymbol == sym
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryGold else Color(0xFF161920))
                        .border(0.6.dp, if (isSelected) PrimaryGold else Color(0xFF282D38), RoundedCornerShape(12.dp))
                        .clickable { selectedSymbol = sym }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = sym,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else TextWhite
                    )
                }
            }
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
                            text = selectedSymbol,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (!hasPrice) Color(0xFF202530) else if (isBullish) Color(0xFF163824) else Color(0xFF381616),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (!hasPrice) "NEUTRAL" else if (isBullish) "↑ BULLISH BIAS" else "↓ BEARISH BIAS",
                                color = if (!hasPrice) TextGray else if (isBullish) ProfitGreen else LossRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (!hasPrice) {
                            "Awaiting market tick data for $selectedSymbol. Active live session feed updates in real-time."
                        } else if (isBullish) {
                            "Strong support cushion at ₹${String.format("%,d", supportPrice)}. Breakout above ₹${String.format("%,d", resistancePrice)} signals long momentum continuation."
                        } else {
                            "Overhead resistance capped at ₹${String.format("%,d", resistancePrice)}. Breakdown below ₹${String.format("%,d", supportPrice)} tests downside levels."
                        },
                        color = TextGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Key Pivot Levels", color = TextGray, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(text = "Support", color = TextGray, fontSize = 9.sp)
                            Text(text = if (hasPrice) "₹${String.format("%,d", supportPrice)}" else "--", color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "Resistance", color = TextGray, fontSize = 9.sp)
                            Text(text = if (hasPrice) "₹${String.format("%,d", resistancePrice)}" else "--", color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "LTP", color = TextGray, fontSize = 9.sp)
                            Text(text = if (hasPrice) "₹${String.format("%,.2f", ltp)}" else "--", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Rectified AI Bull / Bear Visual Indicator
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    (if (isBullish) ProfitGreen else LossRed).copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            1.2.dp,
                            if (isBullish) ProfitGreen.copy(alpha = 0.5f) else LossRed.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(if (isBullish) R.drawable.ic_bull else R.drawable.ic_bear),
                            contentDescription = if (isBullish) "Bullish Trend" else "Bearish Trend",
                            tint = if (isBullish) ProfitGreen else LossRed,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBullish) "AI BULL" else "AI BEAR",
                            color = if (isBullish) ProfitGreen else LossRed,
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
// 7. RECENT ORDERS / POSITIONS SUMMARY
// ==========================================
@Composable
private fun RecentOrdersPositionsSection(
    orders: List<OrderEntity>,
    onNavigateToOrders: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Orders & Positions",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All Orders >",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToOrders() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF13161C),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF23272F))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (orders.isEmpty()) {
                    Text(
                        text = "No active orders placed today. Live executions appear here.",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                } else {
                    val recent = orders.first()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = recent.symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${recent.side} • ${recent.orderType} • Qty: ${recent.qty}", color = TextGray, fontSize = 10.sp)
                        }
                        Surface(
                            color = if (recent.status == "EXECUTED" || recent.status == "COMPLETE") ProfitGreen.copy(alpha = 0.15f) else SecondaryGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = recent.status,
                                color = if (recent.status == "EXECUTED" || recent.status == "COMPLETE") ProfitGreen else SecondaryGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
