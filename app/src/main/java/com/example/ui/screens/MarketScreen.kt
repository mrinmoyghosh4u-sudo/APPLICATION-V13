package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
    onGetHistoricalCandles: (symbol: String, interval: String, onResult: (List<com.example.ui.components.CandleData>) -> Unit) -> Unit = { _, _, _ -> },
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedExchange by rememberSaveable { mutableStateOf("NSE") }
    var selectedMoverCategory by rememberSaveable { mutableStateOf("Top Gainers") }
    var showAddSymbolDialog by rememberSaveable { mutableStateOf(false) }
    var chartDialogInstrument by remember { mutableStateOf<ChartDialogData?>(null) }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val marketDataMap by MarketDataStore.ticks.collectAsStateWithLifecycle()

    val exchangeStatus = MarketStatusUtil.getDetailedMarketStatus(selectedExchange)
    val nseStatus = MarketStatusUtil.getDetailedMarketStatus("NSE")
    val bseStatus = MarketStatusUtil.getDetailedMarketStatus("BSE")
    val mcxStatus = MarketStatusUtil.getDetailedMarketStatus("MCX")
    val isMarketOpen = nseStatus.isOpen || bseStatus.isOpen || mcxStatus.isOpen

    BackHandler(enabled = chartDialogInstrument != null || showAddSymbolDialog || searchQuery.isNotBlank()) {
        when {
            chartDialogInstrument != null -> chartDialogInstrument = null
            showAddSymbolDialog -> showAddSymbolDialog = false
            searchQuery.isNotBlank() -> searchQuery = ""
        }
    }

    // Search Database containing Comprehensive NSE, BSE, MCX Indices & Option Contracts
    

    
    val searchResults = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            emptyList<SearchInstrumentItem>()
        } else {
            val master = com.example.data.network.InstrumentMasterService.instance
            if (master != null) {
                master.searchInstruments(q).map { inst ->
                    SearchInstrumentItem(
                        symbol = inst.symbol,
                        exchange = com.example.data.network.InstrumentMasterService.normalizeExchange(inst.exch_seg),
                        category = inst.instrumenttype,
                        expiry = inst.expiry,
                        lotSize = inst.lotsize.toIntOrNull() ?: 1,
                        token = inst.token
                    )
                }
            } else {
                emptyList<SearchInstrumentItem>()
            }
        }
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
            val nextOpeningText = when {
                isMarketOpen && mcxStatus.isOpen && !nseStatus.isOpen -> "MCX Active (09:00 - 23:30 IST)"
                isMarketOpen -> "Session Active (09:15 - 15:30 IST)"
                else -> nseStatus.nextOpeningTimeText
            }
            MarketStatusBarSection(
                isMarketOpen = isMarketOpen,
                nextOpeningText = nextOpeningText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. SEARCH BAR & RECENT SEARCHES
            SearchBarSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                recentSearches = recentSearches,
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
                    val tickPrice = tick?.price ?: 0.0
                    val ltp = if (tickPrice > 0.0) tickPrice else item.price
                    val change = 0.0
                    val changePct = if (tickPrice > 0.0) 0.0 else item.changePct
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
            onGetHistoricalCandles = onGetHistoricalCandles,
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
    marketDataMap: Map<String, com.example.data.model.RealTimePriceTick>,
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
                        val tickPrice = tick?.price ?: 0.0
                        val watchLtp = watchItem?.ltp ?: 0.0
                        val ltp = if (tickPrice > 0.0) tickPrice else watchLtp
                        val changePct = if (tickPrice > 0.0) 0.0 else (watchItem?.changePercent ?: 0.0)
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
    marketDataMap: Map<String, com.example.data.model.RealTimePriceTick>,
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
            IndexCardData("CRUDEOIL M", 0.0, 0.0, 0.0, 10)
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
                        
                        val tickPrice = tick?.price ?: 0.0
                        val watchLtp = watchItem?.ltp ?: 0.0
                        val ltp = if (tickPrice > 0.0) tickPrice else if (watchLtp > 0.0) watchLtp else item.price
                        val change = if (tickPrice > 0.0) 0.0 else if (watchLtp > 0.0) (watchItem?.change ?: 0.0) else item.change
                        val changePct = if (tickPrice > 0.0) 0.0 else if (watchLtp > 0.0) (watchItem?.changePercent ?: 0.0) else item.changePct

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
    val lotSize: Int,
    val token: String = ""
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
    marketDataMap: Map<String, com.example.data.model.RealTimePriceTick>,
    watchlist: List<WatchlistItem>,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit,
    onToggleFavorite: (symbol: String, currentStatus: Boolean) -> Unit,
    onAddRecentSearch: (String) -> Unit,
    onOpenChart: (MarketMoverCardData) -> Unit
) {
    val categories = listOf("Top Gainers", "Top Losers", "High Volume", "High OI Chg")

    // Pool of Comprehensive Exchange Option Contracts ONLY (CE and PE)
    
    // We only use REAL live market data from the map, no synthetic pools.
    val combinedItems: List<MarketMoverCardData> = remember(marketDataMap, selectedExchange) {
        marketDataMap.values.filter { 
            it.exchange.equals(selectedExchange, ignoreCase = true) 
        }.map { tick ->
            MarketMoverCardData(
                symbol = tick.symbol,
                exchange = tick.exchange,
                price = tick.price,
                changePct = 0.0,
                lotSize = 1, // Need real lot size mapping if available
                expiry = "",
                volume = tick.volume,
                oiChangePct = 0.0
            )
        }
    }

    val moverItems: List<MarketMoverCardData> = remember(combinedItems, selectedCategory) {
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
                    val ltp = if ((tick?.price ?: 0.0) > 0.0) tick!!.price else item.price
                    val pct = item.changePct
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
    val lotSize: Int,
    val token: String = ""
)

@Composable
private fun MarketChartDialog(
    data: ChartDialogData,
    onGetHistoricalCandles: (symbol: String, interval: String, onResult: (List<com.example.ui.components.CandleData>) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onTrade: (side: String) -> Unit
) {
    var selectedTimeframe by remember { mutableStateOf("5M") }
    val timeframes = listOf("1M", "5M", "15M", "1H", "1D")
    val isPositive = data.changePct >= 0

    val basePrice = if (data.ltp > 0.0) data.ltp.toFloat() else 100f
    
    var candles by remember { mutableStateOf<List<com.example.ui.components.CandleData>>(emptyList()) }
    
    LaunchedEffect(data.symbol, selectedTimeframe) {
        val intervalStr = when (selectedTimeframe) {
            "1M" -> "1m"
            "5M" -> "5m"
            "15M" -> "15m"
            "1H" -> "1h"
            "1D" -> "1d"
            else -> "5m"
        }
        onGetHistoricalCandles(data.symbol, intervalStr) { fetched ->
            candles = fetched
        }
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
    val token: String = "",
    val expiry: String = "",
    val volume: Long = 50000L,
    val oiChangePct: Double = 0.0
)







data class SearchInstrumentItem(
    val symbol: String,
    val exchange: String,
    val category: String,
    val expiry: String,
    val lotSize: Int,
    val token: String = ""
)





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




