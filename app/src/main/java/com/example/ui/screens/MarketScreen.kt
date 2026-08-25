package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketDataStore
import com.example.data.model.NotificationEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CandleData
import com.example.ui.components.CandlestickChart
import com.example.ui.components.CrownLogo
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.components.SparklineChart
import com.example.ui.theme.*
import com.example.util.MarketStatusUtil

@Composable
fun MarketScreen(
    userProfile: UserProfileEntity,
    watchlist: List<WatchlistItem> = emptyList(),
    notifications: List<NotificationEntity> = emptyList(),
    recentSearches: List<String> = emptyList(),
    marketDataSource: String = "Angel One",
    marketDataLastUpdated: String = "",
    onToggleFavorite: (symbol: String, currentStatus: Boolean) -> Unit = { _, _ -> },
    onAddRecentSearch: (query: String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onOpenNotificationCenter: () -> Unit = {},
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit = { _, _, _, _ -> },
    onAddSymbolToWatchlist: (symbol: String, exchange: String) -> Unit = { _, _ -> },
    onNavigateToIndexDetails: (exchange: String, indexName: String) -> Unit = { _, _ -> },
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedExchange by rememberSaveable { mutableStateOf("NSE") }
    var selectedMoverCategory by rememberSaveable { mutableStateOf("Top Gainers") }
    var showAddSymbolDialog by rememberSaveable { mutableStateOf(false) }
    var chartDialogInstrument by remember { mutableStateOf<ChartDialogData?>(null) }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val marketDataMap by MarketDataStore.marketData.collectAsStateWithLifecycle()

    val exchangeStatus = remember(selectedExchange) { MarketStatusUtil.getDetailedMarketStatus(selectedExchange) }
    val isMarketOpen = exchangeStatus.isOpen

    // Search Database containing Comprehensive NSE, BSE, MCX Indices & Option Contracts
    val searchInstrumentPool = remember {
        generateSearchInstrumentPool()
    }

    val searchResults = remember(searchQuery, searchInstrumentPool) {
        searchInstruments(searchQuery, searchInstrumentPool)
    }

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090A0C))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val isLiveFeedActive = marketDataSource.startsWith("LIVE", ignoreCase = true)

            // 1. TOP HEADER (Crown Logo, App Name, Live Provider Badge, Notifications with badge)
            MarketHeaderSection(
                isLiveFeedActive = isLiveFeedActive,
                marketDataSource = marketDataSource,
                unreadCount = unreadCount,
                onOpenNotificationCenter = onOpenNotificationCenter
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. MARKET STATUS BAR
            MarketStatusBarSection(
                isMarketOpen = isMarketOpen,
                nextOpeningText = exchangeStatus.nextOpeningTimeText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. SEARCH BAR & RECENT SEARCHES
            SearchBarSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                recentSearches = if (recentSearches.isNotEmpty()) recentSearches else listOf("NIFTY 25400 CE", "BANKNIFTY 52000 PE", "CRUDEOIL 6400 CE", "RELIANCE"),
                onRecentChipClick = { chip ->
                    searchQuery = chip
                    onAddRecentSearch(chip)
                },
                onClearRecent = onClearRecentSearches
            )

            // LIVE SEARCH RESULTS DROPDOWN (Automatically appears right below search)
            if (searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                SearchResultsCard(
                    query = searchQuery,
                    results = searchResults,
                    marketDataMap = marketDataMap,
                    watchlist = watchlist,
                    onItemClick = { item ->
                        onAddRecentSearch(item.symbol)
                    },
                    onTradeClick = { item, ltp ->
                        onAddRecentSearch(item.symbol)
                        onOpenOrderDialog(item.symbol, "BUY", ltp, item.lotSize)
                    },
                    onToggleFavorite = onToggleFavorite
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. EXCHANGE SEGMENT TABS (NSE, BSE, MCX)
            ExchangeSegmentTabs(
                selectedExchange = selectedExchange,
                onExchangeSelected = {
                    selectedExchange = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. EXCHANGE INDICES SECTION (Exact Lot Sizes: NIFTY=65, BANKNIFTY=30, FINNIFTY=60, MIDCPNIFTY=120, SENSEX=20, BANKEX=30, CRUDEOIL=100, GOLD=100, SILVER=30, etc.)
            ExchangeIndicesSection(
                exchange = selectedExchange,
                marketDataMap = marketDataMap,
                watchlist = watchlist,
                onNavigateToIndexDetails = onNavigateToIndexDetails,
                onOpenOrderDialog = onOpenOrderDialog,
                onOpenChartDialog = { name, exch, ltp, change, changePct, lot ->
                    chartDialogInstrument = ChartDialogData(name, exch, ltp, change, changePct, lot)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6. MARKET MOVERS & OPTIONS SECTION (NSE, BSE, MCX Option Contracts & Gainers/Losers)
            MarketMoversSection(
                selectedExchange = selectedExchange,
                selectedCategory = selectedMoverCategory,
                onCategorySelected = { selectedMoverCategory = it },
                marketDataMap = marketDataMap,
                watchlist = watchlist,
                onOpenOrderDialog = onOpenOrderDialog,
                onToggleFavorite = onToggleFavorite,
                onAddRecentSearch = onAddRecentSearch,
                onOpenChart = { item ->
                    val tick = marketDataMap[item.symbol]
                    val ltp = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.ltp else item.price
                    val change = tick?.change ?: 0.0
                    val changePct = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.changePercent else item.changePct
                    chartDialogInstrument = ChartDialogData(item.symbol, item.exchange, ltp, change, changePct, item.lotSize)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddSymbolDialog) {
        AddSymbolDialog(
            exchange = selectedExchange,
            onDismiss = { showAddSymbolDialog = false },
            onAddSymbol = { symbol ->
                onAddSymbolToWatchlist(symbol, selectedExchange)
                showAddSymbolDialog = false
            }
        )
    }

    // INTERACTIVE CANDLESTICK CHART DIALOG
    chartDialogInstrument?.let { chartData ->
        MarketChartDialog(
            data = chartData,
            onDismiss = { chartDialogInstrument = null },
            onTrade = { side ->
                onOpenOrderDialog(chartData.symbol, side, if (chartData.ltp > 0) chartData.ltp else null, chartData.lotSize)
                chartDialogInstrument = null
            }
        )
    }
}

// ==========================================
// 1. TOP HEADER (Logo, App Title, Live Provider Badge, Notification Icon)
// ==========================================
@Composable
private fun MarketHeaderSection(
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
                isLive = isLiveFeedActive,
                dataSource = marketDataSource,
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
// 3. SEARCH BAR SECTION
// ==========================================
@Composable
private fun SearchBarSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    recentSearches: List<String>,
    onRecentChipClick: (String) -> Unit,
    onClearRecent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search Symbol, Strike, CE/PE, E.g. NIFTY, RELIANCE...",
                    color = TextGray,
                    fontSize = 11.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (searchQuery.isNotBlank()) PrimaryGold else TextGray,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = PrimaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = PrimaryGold.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF13161C), RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color(0xFF23272F),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Recent searches row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Recent:", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(6.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                recentSearches.take(3).forEach { chip ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF181B21), RoundedCornerShape(4.dp))
                            .border(0.6.dp, Color(0xFF282D36), RoundedCornerShape(4.dp))
                            .clickable { onRecentChipClick(chip) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = chip, color = PrimaryGold, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "Clear",
                color = TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onClearRecent() }
                    .padding(start = 6.dp, end = 2.dp)
            )
        }
    }
}

// ==========================================
// SEARCH RESULTS CARD (Instant Auto-Suggest)
// ==========================================
@Composable
private fun SearchResultsCard(
    query: String,
    results: List<SearchInstrumentItem>,
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    watchlist: List<WatchlistItem>,
    onItemClick: (SearchInstrumentItem) -> Unit,
    onTradeClick: (SearchInstrumentItem, Double?) -> Unit,
    onToggleFavorite: (symbol: String, currentStatus: Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF13161C),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SEARCH RESULTS FOR \"${query.uppercase()}\"",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${results.size} matches",
                    fontSize = 9.sp,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No instruments found matching \"$query\"",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    results.forEach { item ->
                        val tick = marketDataMap[item.symbol]
                            ?: marketDataMap.values.find { it.symbol.equals(item.symbol, ignoreCase = true) }
                        val watchItem = watchlist.find { it.symbol.equals(item.symbol, ignoreCase = true) }
                        val ltp = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.ltp else (watchItem?.ltp ?: 0.0)
                        val changePct = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.changePercent else (watchItem?.changePercent ?: 0.0)
                        val hasPrice = ltp > 0.0
                        val isPositive = changePct >= 0
                        val isFav = watchlist.any { it.symbol.equals(item.symbol, ignoreCase = true) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF181B22), RoundedCornerShape(6.dp))
                                .border(0.6.dp, Color(0xFF282D36), RoundedCornerShape(6.dp))
                                .clickable { onItemClick(item) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (isFav) PrimaryGold else TextGray,
                                    modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onToggleFavorite(item.symbol, isFav) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.symbol,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (item.symbol.endsWith("CE")) ProfitGreen.copy(alpha = 0.2f)
                                                    else if (item.symbol.endsWith("PE")) LossRed.copy(alpha = 0.2f)
                                                    else PrimaryGold.copy(alpha = 0.2f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (item.symbol.endsWith("CE")) "CE" else if (item.symbol.endsWith("PE")) "PE" else item.category,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.symbol.endsWith("CE")) ProfitGreen else if (item.symbol.endsWith("PE")) LossRed else PrimaryGold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${item.exchange} • ${item.expiry} • Lot: ${item.lotSize}",
                                        color = TextGray,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (hasPrice) "₹${String.format("%,.2f", ltp)}" else "--",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (hasPrice) "${if (isPositive) "+" else ""}${String.format("%.2f", changePct)}%" else "--",
                                        color = if (isPositive) ProfitGreen else LossRed,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onTradeClick(item, if (hasPrice) ltp else null) },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00C853),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("TRADE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. EXCHANGE SEGMENT TABS (NSE, BSE, MCX)
// ==========================================
@Composable
private fun ExchangeSegmentTabs(
    selectedExchange: String,
    onExchangeSelected: (String) -> Unit
) {
    val exchanges = listOf("NSE", "BSE", "MCX")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13161C), RoundedCornerShape(8.dp))
            .border(0.8.dp, Color(0xFF23272F), RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        exchanges.forEach { exch ->
            val isSelected = exch == selectedExchange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(
                        if (isSelected) PrimaryGold else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onExchangeSelected(exch) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exch,
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// 5. EXCHANGE INDICES SECTION (Exact Lot Sizes & Charts)
// ==========================================
@Composable
private fun ExchangeIndicesSection(
    exchange: String,
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    watchlist: List<com.example.data.model.WatchlistItem> = emptyList(),
    onNavigateToIndexDetails: (exchange: String, indexName: String) -> Unit,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit,
    onOpenChartDialog: (name: String, exchange: String, ltp: Double, change: Double, changePct: Double, lotSize: Int) -> Unit
) {
    // Precise Indian Exchange Lot Sizes
    val indexList = when (exchange.uppercase()) {
        "BSE" -> listOf(
            IndexCardData("SENSEX", 0.0, 0.0, 0.0, 20),
            IndexCardData("BANKEX", 0.0, 0.0, 0.0, 30)
        )
        "MCX" -> listOf(
            IndexCardData("CRUDEOIL", 0.0, 0.0, 0.0, 100),
            IndexCardData("CRUDEOIL M", 0.0, 0.0, 0.0, 10),
            IndexCardData("NATURALGAS", 0.0, 0.0, 0.0, 1250),
            IndexCardData("NATURALGAS M", 0.0, 0.0, 0.0, 250),
            IndexCardData("GOLD", 0.0, 0.0, 0.0, 100),
            IndexCardData("GOLD M", 0.0, 0.0, 0.0, 10),
            IndexCardData("GOLD GUINEA", 0.0, 0.0, 0.0, 1),
            IndexCardData("GOLD PETAL", 0.0, 0.0, 0.0, 1),
            IndexCardData("SILVER", 0.0, 0.0, 0.0, 30),
            IndexCardData("SILVER M", 0.0, 0.0, 0.0, 5),
            IndexCardData("SILVER MIC", 0.0, 0.0, 0.0, 1),
            IndexCardData("COPPER", 0.0, 0.0, 0.0, 2500),
            IndexCardData("COPPER M", 0.0, 0.0, 0.0, 250),
            IndexCardData("ZINC", 0.0, 0.0, 0.0, 5000),
            IndexCardData("ZINC M", 0.0, 0.0, 0.0, 1000),
            IndexCardData("ALUMINIUM", 0.0, 0.0, 0.0, 5000),
            IndexCardData("ALUMINIUM M", 0.0, 0.0, 0.0, 1000),
            IndexCardData("LEAD", 0.0, 0.0, 0.0, 5000),
            IndexCardData("LEAD M", 0.0, 0.0, 0.0, 1000),
            IndexCardData("NICKEL", 0.0, 0.0, 0.0, 1500),
            IndexCardData("MCXBULLDEX", 0.0, 0.0, 0.0, 50),
            IndexCardData("MCXMETLDEX", 0.0, 0.0, 0.0, 50),
            IndexCardData("MCXENRGDEX", 0.0, 0.0, 0.0, 125)
        )
        else -> listOf(
            IndexCardData("NIFTY 50", 0.0, 0.0, 0.0, 65),
            IndexCardData("BANKNIFTY", 0.0, 0.0, 0.0, 30),
            IndexCardData("FINNIFTY", 0.0, 0.0, 0.0, 60),
            IndexCardData("MIDCPNIFTY", 0.0, 0.0, 0.0, 120)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (exchange.uppercase() == "MCX") "⭐ MCX COMMODITIES & INDICES (${indexList.size})" else "⭐ $exchange INDICES (${indexList.size})",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All >",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    if (indexList.isNotEmpty()) {
                        onNavigateToIndexDetails(exchange, indexList.first().name)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Grid Layout
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val chunked = indexList.chunked(2)
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        val tick = marketDataMap[item.name]
                            ?: marketDataMap.values.find { it.symbol.equals(item.name, ignoreCase = true) }
                        val watchItem = watchlist.find { it.symbol.equals(item.name, ignoreCase = true) }
                        
                        val ltp = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.ltp else if ((watchItem?.ltp ?: 0.0) > 0.0) watchItem!!.ltp else item.price
                        val change = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.change else if ((watchItem?.ltp ?: 0.0) > 0.0) watchItem!!.change else item.change
                        val changePct = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.changePercent else if ((watchItem?.ltp ?: 0.0) > 0.0) watchItem!!.changePercent else item.changePct

                        IndexGridCard(
                            name = item.name,
                            exchange = exchange,
                            ltp = ltp,
                            change = change,
                            changePct = changePct,
                            lotSize = item.lotSize,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToIndexDetails(exchange, item.name) },
                            onChartClick = { onOpenChartDialog(item.name, exchange, ltp, change, changePct, item.lotSize) },
                            onOptionsClick = { onNavigateToIndexDetails(exchange, item.name) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class IndexCardData(
    val name: String,
    val price: Double,
    val change: Double,
    val changePct: Double,
    val lotSize: Int
)

@Composable
private fun IndexGridCard(
    name: String,
    exchange: String,
    ltp: Double,
    change: Double,
    changePct: Double,
    lotSize: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onChartClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val isPositive = changePct >= 0

    Column(
        modifier = modifier
            .background(Color(0xFF13161C), RoundedCornerShape(10.dp))
            .border(0.8.dp, Color(0xFF23272F), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (isPositive) ProfitGreen else LossRed,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (ltp > 0.0) String.format("%,.2f", ltp) else "--",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (ltp > 0.0) "${if (isPositive) "+" else ""}${String.format("%.2f", change)} (${if (isPositive) "+" else ""}${String.format("%.2f", changePct)}%)" else "WAITING FOR TICK",
            color = if (ltp > 0.0) (if (isPositive) ProfitGreen else LossRed) else TextGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        SparklineChart(
            isPositive = isPositive,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$exchange • Lot: $lotSize",
                color = TextGray,
                fontSize = 8.5.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // CHART BUTTON
                Button(
                    onClick = onChartClick,
                    modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E222B),
                        contentColor = PrimaryGold
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, PrimaryGold.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.BarChart, contentDescription = "Chart", modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "CHART",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // OPTIONS BUTTON
                Button(
                    onClick = onOptionsClick,
                    modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGold,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Outlined.List, contentDescription = "Options", modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "OPTIONS",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. MARKET MOVERS & OPTIONS SECTION (NSE, BSE, MCX Option Contracts)
// ==========================================
@Composable
private fun MarketMoversSection(
    selectedExchange: String,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    watchlist: List<WatchlistItem>,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit,
    onToggleFavorite: (symbol: String, currentStatus: Boolean) -> Unit,
    onAddRecentSearch: (String) -> Unit,
    onOpenChart: (MarketMoverCardData) -> Unit
) {
    val categories = listOf("Top Gainers", "Top Losers", "High Volume", "High OI Chg")

    // Pool of Comprehensive Exchange Option Contracts ONLY (CE and PE)
    
    // We only use REAL live market data from the map, no synthetic pools.
    val combinedItems = remember(marketDataMap, selectedExchange) {
        marketDataMap.values.filter { 
            it.exchange.equals(selectedExchange, ignoreCase = true) 
        }.map { tick ->
            MarketMoverCardData(
                symbol = tick.symbol,
                exchange = tick.exchange,
                price = tick.ltp,
                changePct = tick.changePercent,
                lotSize = 1, // Need real lot size mapping if available
                expiry = "",
                volume = tick.volume,
                oiChangePct = 0.0
            )
        }
    }

    val moverItems = remember(combinedItems, selectedCategory) {
        when (selectedCategory) {

            "Top Gainers" -> combinedItems.filter { it.changePct >= 0 }.sortedByDescending { it.changePct }
            "Top Losers" -> combinedItems.filter { it.changePct < 0 }.sortedBy { it.changePct }
            "High Volume" -> combinedItems.sortedByDescending { it.volume }
            "High OI Chg" -> combinedItems.sortedByDescending { kotlin.math.abs(it.oiChangePct) }
            else -> combinedItems
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥 $selectedExchange MARKET MOVERS & OPTIONS",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live Movers",
                color = PrimaryGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = cat == selectedCategory
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
                        .clickable { onCategorySelected(cat) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (moverItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF13161C), RoundedCornerShape(8.dp))
                    .border(0.6.dp, Color(0xFF23272F), RoundedCornerShape(8.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No $selectedExchange option instruments active in this category",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                moverItems.forEach { item ->
                    val tick = marketDataMap[item.symbol]
                    val ltp = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.ltp else item.price
                    val pct = if ((tick?.ltp ?: 0.0) > 0.0) tick!!.changePercent else item.changePct
                    val hasPrice = ltp > 0.0
                    val isPositive = pct >= 0
                    val isFav = watchlist.any { it.symbol.equals(item.symbol, ignoreCase = true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF13161C), RoundedCornerShape(8.dp))
                            .border(0.6.dp, Color(0xFF23272F), RoundedCornerShape(8.dp))
                            .clickable { onAddRecentSearch(item.symbol) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) PrimaryGold else TextGray,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onToggleFavorite(item.symbol, isFav) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.symbol,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${item.exchange} • Lot: ${item.lotSize}" + if (selectedCategory == "High Volume") " • Vol: ${String.format("%,d", item.volume)}" else if (selectedCategory == "High OI Chg") " • OI Chg: ${if (item.oiChangePct >= 0) "+" else ""}${String.format("%.1f", item.oiChangePct)}%" else "",
                                    color = TextGray,
                                    fontSize = 8.5.sp
                                )
                            }
                        }

                        SparklineChart(
                            isPositive = isPositive,
                            modifier = Modifier.size(width = 40.dp, height = 18.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (hasPrice) "₹${String.format("%,.2f", ltp)}" else "--",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasPrice) "${if (isPositive) "+" else ""}${String.format("%.2f", pct)}%" else "--",
                                    color = if (isPositive) ProfitGreen else LossRed,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // CHART BUTTON
                            IconButton(
                                onClick = { onOpenChart(item) },
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color(0xFF1E222B), RoundedCornerShape(4.dp))
                                    .border(0.6.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Chart",
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // TRADE BUTTON
                            Button(
                                onClick = {
                                    onAddRecentSearch(item.symbol)
                                    onOpenOrderDialog(item.symbol, "BUY", if (hasPrice) ltp else null, item.lotSize)
                                },
                                modifier = Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00C853),
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = "TRADE",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ChartDialogData(
    val symbol: String,
    val exchange: String,
    val ltp: Double,
    val change: Double,
    val changePct: Double,
    val lotSize: Int
)

@Composable
private fun MarketChartDialog(
    data: ChartDialogData,
    onDismiss: () -> Unit,
    onTrade: (side: String) -> Unit
) {
    var selectedTimeframe by remember { mutableStateOf("5M") }
    val timeframes = listOf("1M", "5M", "15M", "1H", "1D")
    val isPositive = data.changePct >= 0

    val basePrice = if (data.ltp > 0.0) data.ltp.toFloat() else 100f
    val candles = remember(data.symbol, selectedTimeframe, basePrice) {
        emptyList<com.example.ui.components.CandleData>() // Replaced fake candles with empty list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11141A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D38))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = data.symbol,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(data.exchange, fontSize = 9.sp, color = PrimaryGold, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lot Size: ${data.lotSize} • Real-time Interactive Candlestick",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Price & Stats Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF171B22), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (data.ltp > 0.0) "₹${String.format("%,.2f", data.ltp)}" else "--",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.2f", data.change)} (${if (isPositive) "+" else ""}${String.format("%.2f", data.changePct)}%)",
                                color = if (isPositive) ProfitGreen else LossRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Timeframe Pills
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            timeframes.forEach { tf ->
                                val isSelected = tf == selectedTimeframe
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) PrimaryGold else Color(0xFF222732))
                                        .clickable { selectedTimeframe = tf }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tf,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Candlestick Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color(0xFF0B0D11), RoundedCornerShape(8.dp))
                            .border(0.6.dp, Color(0xFF1E232D), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        if (candles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("CHART DATA UNAVAILABLE", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            CandlestickChart(
                                modifier = Modifier.fillMaxSize(),
                                candles = candles,
                                currentPrice = basePrice
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Key Stats Grid
                    val high = candles.maxOfOrNull { it.high } ?: basePrice
                    val low = candles.minOfOrNull { it.low } ?: basePrice
                    val open = candles.firstOrNull()?.open ?: basePrice
                    val close = candles.lastOrNull()?.close ?: basePrice

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("OPEN", color = TextGray, fontSize = 9.sp)
                            Text("₹${String.format("%.2f", open)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("HIGH", color = TextGray, fontSize = 9.sp)
                            Text("₹${String.format("%.2f", high)}", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("LOW", color = TextGray, fontSize = 9.sp)
                            Text("₹${String.format("%.2f", low)}", color = LossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("CLOSE", color = TextGray, fontSize = 9.sp)
                            Text("₹${String.format("%.2f", close)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Action Buy / Sell Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onTrade("BUY") },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen, contentColor = Color.Black)
                        ) {
                            Text("BUY / CALL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { onTrade("SELL") },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed, contentColor = Color.White)
                        ) {
                            Text("SELL / PUT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class MarketMoverCardData(
    val symbol: String,
    val exchange: String,
    val price: Double,
    val changePct: Double,
    val lotSize: Int,
    val expiry: String = "",
    val volume: Long = 50000L,
    val oiChangePct: Double = 0.0
)







data class SearchInstrumentItem(
    val symbol: String,
    val exchange: String,
    val category: String,
    val expiry: String,
    val lotSize: Int
)

private fun searchInstruments(query: String, pool: List<SearchInstrumentItem>): List<SearchInstrumentItem> {
    val q = query.trim().uppercase()
    if (q.isBlank()) return emptyList()
    val tokens = q.split(" ").filter { it.isNotBlank() }
    val results = mutableListOf<SearchInstrumentItem>()

    // 1. DYNAMIC ON-THE-FLY STRIKE PARSER (For any typed index + strike + CE/PE)
    val numberMatch = Regex("""\b(\d{2,6})\b""").find(q)?.groupValues?.get(1)?.toIntOrNull()
    val isCeExplicit = tokens.any { it == "CE" || it == "CALL" }
    val isPeExplicit = tokens.any { it == "PE" || it == "PUT" }

    val baseCandidate = when {
        tokens.any { it.contains("BANKNIFTY") || it == "BN" } -> "BANKNIFTY"
        tokens.any { it.contains("FINNIFTY") || it == "FN" } -> "FINNIFTY"
        tokens.any { it.contains("MIDCP") || it.contains("MIDCAP") } -> "MIDCPNIFTY"
        tokens.any { it.contains("NIFTY") } -> "NIFTY"
        tokens.any { it.contains("SENSEX") } -> "SENSEX"
        tokens.any { it.contains("BANKEX") } -> "BANKEX"
        tokens.any { it.contains("CRUDEOIL") || it.contains("CRUDE") } -> "CRUDEOIL"
        tokens.any { it.contains("NATURALGAS") || it.contains("NATGAS") || it == "NG" } -> "NATURALGAS"
        tokens.any { it.contains("GOLD") } -> "GOLD"
        tokens.any { it.contains("SILVER") } -> "SILVER"
        tokens.any { it.contains("COPPER") } -> "COPPER"
        tokens.any { it.contains("ZINC") } -> "ZINC"
        tokens.any { it.contains("ALUMINIUM") || it.contains("ALUM") } -> "ALUMINIUM"
        tokens.any { it.contains("LEAD") } -> "LEAD"
        else -> null
    }

    if (numberMatch != null) {
        val detectedBase = baseCandidate ?: when {
            numberMatch in 20000..26500 -> "NIFTY"
            numberMatch in 46000..56000 -> "BANKNIFTY"
            numberMatch in 74000..88000 -> "SENSEX"
            numberMatch in 11000..15000 -> "MIDCPNIFTY"
            numberMatch in 5000..9000 -> "CRUDEOIL"
            numberMatch in 100..400 -> "NATURALGAS"
            numberMatch in 600..1200 -> "COPPER"
            else -> "NIFTY"
        }

        val (exch, lot, exp) = when (detectedBase) {
            "BANKNIFTY" -> Triple("NSE", 30, "25 AUG")
            "FINNIFTY" -> Triple("NSE", 60, "25 AUG")
            "MIDCPNIFTY" -> Triple("NSE", 120, "25 AUG")
            "NIFTY" -> Triple("NSE", 65, "25 AUG")
            "SENSEX" -> Triple("BSE", 20, "29 AUG")
            "BANKEX" -> Triple("BSE", 30, "29 AUG")
            "CRUDEOIL" -> Triple("MCX", 100, "19 SEP")
            "NATURALGAS" -> Triple("MCX", 1250, "26 SEP")
            "GOLD" -> Triple("MCX", 100, "05 OCT")
            "SILVER" -> Triple("MCX", 30, "28 NOV")
            "COPPER" -> Triple("MCX", 2500, "30 SEP")
            "ZINC" -> Triple("MCX", 5000, "30 SEP")
            else -> Triple("NSE", 65, "25 AUG")
        }

        if (isCeExplicit && !isPeExplicit) {
            results.add(SearchInstrumentItem("$detectedBase $exp $numberMatch CE", exch, "OPTIONS", exp, lot))
        } else if (isPeExplicit && !isCeExplicit) {
            results.add(SearchInstrumentItem("$detectedBase $exp $numberMatch PE", exch, "OPTIONS", exp, lot))
        } else {
            results.add(SearchInstrumentItem("$detectedBase $exp $numberMatch CE", exch, "OPTIONS", exp, lot))
            results.add(SearchInstrumentItem("$detectedBase $exp $numberMatch PE", exch, "OPTIONS", exp, lot))
        }
    }

    // 2. Comprehensive Token Search against Pre-computed Pool
    val filteredPool = pool.filter { item ->
        tokens.all { token ->
            item.symbol.contains(token, ignoreCase = true) ||
            item.exchange.contains(token, ignoreCase = true) ||
            item.category.contains(token, ignoreCase = true) ||
            item.expiry.contains(token, ignoreCase = true)
        }
    }

    // 3. Merge & Deduplicate
    val merged = (results + filteredPool).distinctBy { it.symbol }
    return merged.take(20)
}

private fun generateSearchInstrumentPool(): List<SearchInstrumentItem> {
    val items = mutableListOf<SearchInstrumentItem>()

    // NIFTY STRIKES (23000 to 26000, step 100)
    for (strike in 23000..26000 step 100) {
        items.add(SearchInstrumentItem("NIFTY 25AUG $strike CE", "NSE", "OPTIONS", "25 AUG", 65))
        items.add(SearchInstrumentItem("NIFTY 25AUG $strike PE", "NSE", "OPTIONS", "25 AUG", 65))
    }

    // BANKNIFTY STRIKES (48000 to 54000, step 200)
    for (strike in 48000..54000 step 200) {
        items.add(SearchInstrumentItem("BANKNIFTY 25AUG $strike CE", "NSE", "OPTIONS", "25 AUG", 30))
        items.add(SearchInstrumentItem("BANKNIFTY 25AUG $strike PE", "NSE", "OPTIONS", "25 AUG", 30))
    }

    // FINNIFTY STRIKES (22000 to 25000, step 100)
    for (strike in 22000..25000 step 100) {
        items.add(SearchInstrumentItem("FINNIFTY 25AUG $strike CE", "NSE", "OPTIONS", "25 AUG", 60))
        items.add(SearchInstrumentItem("FINNIFTY 25AUG $strike PE", "NSE", "OPTIONS", "25 AUG", 60))
    }

    // MIDCPNIFTY STRIKES (11500 to 14000, step 100)
    for (strike in 11500..14000 step 100) {
        items.add(SearchInstrumentItem("MIDCPNIFTY 25AUG $strike CE", "NSE", "OPTIONS", "25 AUG", 120))
        items.add(SearchInstrumentItem("MIDCPNIFTY 25AUG $strike PE", "NSE", "OPTIONS", "25 AUG", 120))
    }

    // SENSEX STRIKES (77000 to 85000, step 500)
    for (strike in 77000..85000 step 500) {
        items.add(SearchInstrumentItem("SENSEX 29AUG $strike CE", "BSE", "OPTIONS", "29 AUG", 20))
        items.add(SearchInstrumentItem("SENSEX 29AUG $strike PE", "BSE", "OPTIONS", "29 AUG", 20))
    }

    // BANKEX STRIKES (53000 to 60000, step 500)
    for (strike in 53000..60000 step 500) {
        items.add(SearchInstrumentItem("BANKEX 29AUG $strike CE", "BSE", "OPTIONS", "29 AUG", 30))
        items.add(SearchInstrumentItem("BANKEX 29AUG $strike PE", "BSE", "OPTIONS", "29 AUG", 30))
    }

    // CRUDEOIL STRIKES (5800 to 7200, step 100)
    for (strike in 5800..7200 step 100) {
        items.add(SearchInstrumentItem("CRUDEOIL 19SEP $strike CE", "MCX", "OPTIONS", "19 SEP", 100))
        items.add(SearchInstrumentItem("CRUDEOIL 19SEP $strike PE", "MCX", "OPTIONS", "19 SEP", 100))
    }

    // NATURALGAS STRIKES (160 to 260, step 10)
    for (strike in 160..260 step 10) {
        items.add(SearchInstrumentItem("NATURALGAS 26SEP $strike CE", "MCX", "OPTIONS", "26 SEP", 1250))
        items.add(SearchInstrumentItem("NATURALGAS 26SEP $strike PE", "MCX", "OPTIONS", "26 SEP", 1250))
    }

    // GOLD STRIKES (72000 to 78000, step 500)
    for (strike in 72000..78000 step 500) {
        items.add(SearchInstrumentItem("GOLD 05OCT $strike CE", "MCX", "OPTIONS", "05 OCT", 100))
        items.add(SearchInstrumentItem("GOLD 05OCT $strike PE", "MCX", "OPTIONS", "05 OCT", 100))
    }

    // SILVER STRIKES (80000 to 90000, step 1000)
    for (strike in 80000..90000 step 1000) {
        items.add(SearchInstrumentItem("SILVER 28NOV $strike CE", "MCX", "OPTIONS", "28 NOV", 30))
        items.add(SearchInstrumentItem("SILVER 28NOV $strike PE", "MCX", "OPTIONS", "28 NOV", 30))
    }

    // COPPER STRIKES (780 to 860, step 10)
    for (strike in 780..860 step 10) {
        items.add(SearchInstrumentItem("COPPER 30SEP $strike CE", "MCX", "OPTIONS", "30 SEP", 2500))
        items.add(SearchInstrumentItem("COPPER 30SEP $strike PE", "MCX", "OPTIONS", "30 SEP", 2500))
    }

    // ALL MCX COMMODITIES & MINIS
    items.add(SearchInstrumentItem("CRUDEOIL", "MCX", "FUTURES", "19 SEP", 100))
    items.add(SearchInstrumentItem("CRUDEOIL M", "MCX", "FUTURES", "19 SEP", 10))
    items.add(SearchInstrumentItem("NATURALGAS", "MCX", "FUTURES", "26 SEP", 1250))
    items.add(SearchInstrumentItem("NATURALGAS M", "MCX", "FUTURES", "26 SEP", 250))
    items.add(SearchInstrumentItem("GOLD", "MCX", "FUTURES", "05 OCT", 100))
    items.add(SearchInstrumentItem("GOLD M", "MCX", "FUTURES", "05 OCT", 10))
    items.add(SearchInstrumentItem("GOLD GUINEA", "MCX", "FUTURES", "05 OCT", 1))
    items.add(SearchInstrumentItem("GOLD PETAL", "MCX", "FUTURES", "05 OCT", 1))
    items.add(SearchInstrumentItem("SILVER", "MCX", "FUTURES", "28 NOV", 30))
    items.add(SearchInstrumentItem("SILVER M", "MCX", "FUTURES", "28 NOV", 5))
    items.add(SearchInstrumentItem("SILVER MIC", "MCX", "FUTURES", "28 NOV", 1))
    items.add(SearchInstrumentItem("COPPER", "MCX", "FUTURES", "30 SEP", 2500))
    items.add(SearchInstrumentItem("COPPER M", "MCX", "FUTURES", "30 SEP", 250))
    items.add(SearchInstrumentItem("ZINC", "MCX", "FUTURES", "30 SEP", 5000))
    items.add(SearchInstrumentItem("ZINC M", "MCX", "FUTURES", "30 SEP", 1000))
    items.add(SearchInstrumentItem("ALUMINIUM", "MCX", "FUTURES", "30 SEP", 5000))
    items.add(SearchInstrumentItem("ALUMINIUM M", "MCX", "FUTURES", "30 SEP", 1000))
    items.add(SearchInstrumentItem("LEAD", "MCX", "FUTURES", "30 SEP", 5000))
    items.add(SearchInstrumentItem("LEAD M", "MCX", "FUTURES", "30 SEP", 1000))
    items.add(SearchInstrumentItem("NICKEL", "MCX", "FUTURES", "30 SEP", 1500))
    items.add(SearchInstrumentItem("MCXBULLDEX", "MCX", "INDEX", "30 SEP", 50))
    items.add(SearchInstrumentItem("MCXMETLDEX", "MCX", "INDEX", "30 SEP", 50))
    items.add(SearchInstrumentItem("MCXENRGDEX", "MCX", "INDEX", "30 SEP", 125))

    // ALL MAJOR NSE & BSE EQUITIES & INDICES
    items.add(SearchInstrumentItem("NIFTY 50", "NSE", "INDEX", "SPOT", 65))
    items.add(SearchInstrumentItem("BANKNIFTY", "NSE", "INDEX", "SPOT", 30))
    items.add(SearchInstrumentItem("FINNIFTY", "NSE", "INDEX", "SPOT", 60))
    items.add(SearchInstrumentItem("MIDCPNIFTY", "NSE", "INDEX", "SPOT", 120))
    items.add(SearchInstrumentItem("SENSEX", "BSE", "INDEX", "SPOT", 20))
    items.add(SearchInstrumentItem("BANKEX", "BSE", "INDEX", "SPOT", 30))
    items.add(SearchInstrumentItem("RELIANCE", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("HDFCBANK", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("ICICIBANK", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("INFY", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("TCS", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("SBIN", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("TATASTEEL", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("TATAMOTORS", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("BHARTIARTL", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("ITC", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("LT", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("AXISBANK", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("KOTAKBANK", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("BAJFINANCE", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("MARUTI", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("SUNPHARMA", "NSE", "EQUITY", "CASH", 1))
    items.add(SearchInstrumentItem("TITAN", "NSE", "EQUITY", "CASH", 1))

    return items
}

// ==========================================
// ADD SYMBOL DIALOG
// ==========================================
@Composable
private fun AddSymbolDialog(
    exchange: String,
    onDismiss: () -> Unit,
    onAddSymbol: (String) -> Unit
) {
    var symbolInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Symbol to $exchange Watchlist", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter Symbol or Option Contract (e.g. RELIANCE, NIFTY 25400 CE):", color = TextGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = symbolInput,
                    onValueChange = { symbolInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color(0xFF23272F),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (symbolInput.isNotBlank()) {
                        onAddSymbol(symbolInput.trim().uppercase())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
            ) {
                Text("ADD", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextGray)
            }
        },
        containerColor = Color(0xFF13161C)
    )
}




