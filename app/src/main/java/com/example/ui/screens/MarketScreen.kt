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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketDataStore
import com.example.data.model.NotificationEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
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

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val marketDataMap by MarketDataStore.marketData.collectAsStateWithLifecycle()

    val exchangeStatus = remember(selectedExchange) { MarketStatusUtil.getDetailedMarketStatus(selectedExchange) }
    val isMarketOpen = exchangeStatus.isOpen

    // Search Database containing Comprehensive NSE, BSE, MCX Indices & Option Contracts
    val searchInstrumentPool = remember {
        generateSearchInstrumentPool()
    }

    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.trim().uppercase()
            val tokens = q.split(" ", "-", "_").filter { it.isNotBlank() }
            searchInstrumentPool.filter { item ->
                tokens.all { token ->
                    item.symbol.contains(token, ignoreCase = true) ||
                    item.exchange.contains(token, ignoreCase = true) ||
                    item.category.contains(token, ignoreCase = true) ||
                    item.expiry.contains(token, ignoreCase = true)
                }
            }.take(12)
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
            val isDhanConnected = userProfile.isDhanConnected

            // 1. TOP HEADER (Crown Logo, App Name, Dhan Live Badge, Notifications with badge)
            MarketHeaderSection(
                isDhanConnected = isDhanConnected,
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

            // 5. EXCHANGE INDICES SECTION (Exact Lot Sizes: NIFTY=25, BANKNIFTY=15, FINNIFTY=25, MIDCPNIFTY=50, SENSEX=10, BANKEX=15, CRUDEOIL=100, GOLD=100, SILVER=30)
            ExchangeIndicesSection(
                exchange = selectedExchange,
                marketDataMap = marketDataMap,
                onNavigateToIndexDetails = onNavigateToIndexDetails,
                onOpenOrderDialog = onOpenOrderDialog
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
                onAddRecentSearch = onAddRecentSearch
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
}

// ==========================================
// 1. TOP HEADER (Logo, App Title, Dhan Live Badge, Notification Icon)
// ==========================================
@Composable
private fun MarketHeaderSection(
    isDhanConnected: Boolean,
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
            com.example.ui.components.DhanLiveStatusBadge(
                isDhanConnected = isDhanConnected,
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
    onTradeClick: (SearchInstrumentItem, Double) -> Unit,
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
                        val ltp = tick?.ltp ?: item.basePrice
                        val changePct = tick?.changePercent ?: item.baseChangePct
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
                                        text = "₹${String.format("%,.2f", ltp)}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", changePct)}%",
                                        color = if (isPositive) ProfitGreen else LossRed,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onTradeClick(item, ltp) },
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
// 5. EXCHANGE INDICES SECTION (Exact Lot Sizes)
// ==========================================
@Composable
private fun ExchangeIndicesSection(
    exchange: String,
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    onNavigateToIndexDetails: (exchange: String, indexName: String) -> Unit,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit
) {
    // Precise Indian Exchange Lot Sizes
    val indexList = when (exchange.uppercase()) {
        "BSE" -> listOf(
            IndexCardData("SENSEX", 0.0, 0.0, 0.0, 20),
            IndexCardData("BANKEX", 0.0, 0.0, 0.0, 30)
        )
        "MCX" -> listOf(
            IndexCardData("CRUDEOIL", 0.0, 0.0, 0.0, 100),
            IndexCardData("NATURALGAS", 0.0, 0.0, 0.0, 1250),
            IndexCardData("GOLD", 0.0, 0.0, 0.0, 100),
            IndexCardData("SILVER", 0.0, 0.0, 0.0, 30)
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
                text = "⭐ $exchange INDICES",
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
                        val ltp = tick?.ltp ?: item.price
                        val change = tick?.change ?: item.change
                        val changePct = tick?.changePercent ?: item.changePct

                        IndexGridCard(
                            name = item.name,
                            exchange = exchange,
                            ltp = ltp,
                            change = change,
                            changePct = changePct,
                            lotSize = item.lotSize,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToIndexDetails(exchange, item.name) },
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
                fontSize = 9.sp
            )

            Button(
                onClick = onOptionsClick,
                modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGold,
                    contentColor = Color.Black
                )
            ) {
                Icon(imageVector = Icons.Outlined.List, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "OPTIONS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
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
    onAddRecentSearch: (String) -> Unit
) {
    val categories = listOf("Top Gainers", "Top Losers", "High Volume", "High OI Chg")

    // Dynamic instruments according to active exchange and category (including requested Option Contracts in exact `<INDEX> <EXPIRY> <STRIKE> <CE/PE>` format)
    val moverItems = remember(selectedExchange, selectedCategory) {
        when (selectedExchange.uppercase()) {
            "BSE" -> when (selectedCategory) {
                "Top Losers" -> listOf(
                    MarketMoverCardData("SENSEX 29AUG 80000 PE", "BSE", 245.50, -11.20, 20, "29AUG"),
                    MarketMoverCardData("BANKEX 29AUG 56500 PE", "BSE", 195.00, -8.60, 30, "29AUG"),
                    MarketMoverCardData("SENSEX 29AUG 79500 PE", "BSE", 160.00, -14.20, 20, "29AUG")
                )
                "High Volume" -> listOf(
                    MarketMoverCardData("SENSEX 29AUG 80500 CE", "BSE", 380.00, 14.80, 20, "29AUG"),
                    MarketMoverCardData("BANKEX 29AUG 57000 CE", "BSE", 310.00, 12.00, 30, "29AUG"),
                    MarketMoverCardData("SENSEX 29AUG 81000 CE", "BSE", 215.00, 18.50, 20, "29AUG")
                )
                "High OI Chg" -> listOf(
                    MarketMoverCardData("SENSEX 29AUG 81000 CE", "BSE", 215.00, 18.50, 20, "29AUG"),
                    MarketMoverCardData("SENSEX 29AUG 79500 PE", "BSE", 160.00, -14.20, 20, "29AUG"),
                    MarketMoverCardData("BANKEX 29AUG 57500 CE", "BSE", 180.00, 16.20, 30, "29AUG")
                )
                else -> listOf(
                    MarketMoverCardData("SENSEX 29AUG 80500 CE", "BSE", 380.00, 15.20, 20, "29AUG"),
                    MarketMoverCardData("BANKEX 29AUG 57000 CE", "BSE", 310.00, 12.00, 30, "29AUG"),
                    MarketMoverCardData("SENSEX 29AUG 81000 CE", "BSE", 215.00, 18.50, 20, "29AUG")
                )
            }
            "MCX" -> when (selectedCategory) {
                "Top Losers" -> listOf(
                    MarketMoverCardData("CRUDEOIL 19SEP 6300 PE", "MCX", 142.00, -12.40, 100, "19SEP"),
                    MarketMoverCardData("SILVER 28NOV 83000 PE", "MCX", 780.00, -9.10, 30, "28NOV"),
                    MarketMoverCardData("NATURALGAS 26SEP 180 PE", "MCX", 6.20, -14.50, 1250, "26SEP")
                )
                "High Volume" -> listOf(
                    MarketMoverCardData("CRUDEOIL 19SEP 6400 CE", "MCX", 165.00, 8.20, 100, "19SEP"),
                    MarketMoverCardData("GOLD 05OCT 75000 CE", "MCX", 580.00, 4.80, 100, "05OCT"),
                    MarketMoverCardData("SILVER 28NOV 85000 CE", "MCX", 1240.00, 6.50, 30, "28NOV")
                )
                "High OI Chg" -> listOf(
                    MarketMoverCardData("NATURALGAS 26SEP 190 CE", "MCX", 9.80, 14.00, 1250, "26SEP"),
                    MarketMoverCardData("COPPER 30SEP 810 CE", "MCX", 12.50, 5.20, 2500, "30SEP"),
                    MarketMoverCardData("CRUDEOIL 19SEP 6500 CE", "MCX", 110.00, 16.50, 100, "19SEP")
                )
                else -> listOf(
                    MarketMoverCardData("CRUDEOIL 19SEP 6400 CE", "MCX", 165.00, 8.20, 100, "19SEP"),
                    MarketMoverCardData("GOLD 05OCT 75000 CE", "MCX", 580.00, 4.80, 100, "05OCT"),
                    MarketMoverCardData("NATURALGAS 26SEP 190 CE", "MCX", 9.80, 14.00, 1250, "26SEP")
                )
            }
            else -> when (selectedCategory) {
                "Top Losers" -> listOf(
                    MarketMoverCardData("NIFTY 25AUG 24200 PE", "NSE", 122.10, -12.80, 65, "25AUG"),
                    MarketMoverCardData("BANKNIFTY 25AUG 50800 PE", "NSE", 210.80, -8.60, 30, "25AUG"),
                    MarketMoverCardData("FINNIFTY 25AUG 23200 PE", "NSE", 64.20, -10.50, 60, "25AUG")
                )
                "High Volume" -> listOf(
                    MarketMoverCardData("NIFTY 25AUG 24400 CE", "NSE", 184.50, 18.40, 65, "25AUG"),
                    MarketMoverCardData("BANKNIFTY 25AUG 51200 CE", "NSE", 340.20, 14.20, 30, "25AUG"),
                    MarketMoverCardData("NIFTY 25AUG 24200 PE", "NSE", 122.10, -12.80, 65, "25AUG")
                )
                "High OI Chg" -> listOf(
                    MarketMoverCardData("NIFTY 25AUG 24500 CE", "NSE", 115.00, 22.50, 65, "25AUG"),
                    MarketMoverCardData("BANKNIFTY 25AUG 51500 CE", "NSE", 195.40, 19.10, 30, "25AUG"),
                    MarketMoverCardData("MIDCPNIFTY 25AUG 12800 CE", "NSE", 85.20, 16.80, 120, "25AUG")
                )
                else -> listOf(
                    MarketMoverCardData("NIFTY 25AUG 24400 CE", "NSE", 184.50, 18.40, 65, "25AUG"),
                    MarketMoverCardData("BANKNIFTY 25AUG 51200 CE", "NSE", 340.20, 14.20, 30, "25AUG"),
                    MarketMoverCardData("FINNIFTY 25AUG 23400 CE", "NSE", 95.40, 11.50, 60, "25AUG"),
                    MarketMoverCardData("MIDCPNIFTY 25AUG 12800 CE", "NSE", 85.20, 16.80, 120, "25AUG")
                )
            }
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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            moverItems.forEach { item ->
                val tick = marketDataMap[item.symbol]
                val ltp = tick?.ltp ?: item.price
                val pct = tick?.changePercent ?: item.changePct
                val isPositive = pct >= 0
                val isFav = watchlist.any { it.symbol.equals(item.symbol, ignoreCase = true) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13161C), RoundedCornerShape(8.dp))
                        .border(0.6.dp, Color(0xFF23272F), RoundedCornerShape(8.dp))
                        .clickable { onAddRecentSearch(item.symbol) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val isExpiryInSymbol = item.symbol.contains(item.expiry, ignoreCase = true) || 
                                                       item.symbol.contains(item.expiry.replace(" ", ""), ignoreCase = true)
                                if (!isExpiryInSymbol && item.expiry.isNotBlank() && item.expiry != "EQUITY") {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(PrimaryGold.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(item.expiry, fontSize = 8.sp, color = PrimaryGold, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "${item.exchange} • Lot: ${item.lotSize}",
                                color = TextGray,
                                fontSize = 9.sp
                            )
                        }
                    }

                    SparklineChart(
                        isPositive = isPositive,
                        modifier = Modifier.size(width = 46.dp, height = 20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${String.format("%,.2f", ltp)}",
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

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                onAddRecentSearch(item.symbol)
                                onOpenOrderDialog(item.symbol, "BUY", ltp, item.lotSize)
                            },
                            modifier = Modifier.height(28.dp),
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
        }
    }
}

private data class MarketMoverCardData(
    val symbol: String,
    val exchange: String,
    val price: Double,
    val changePct: Double,
    val lotSize: Int,
    val expiry: String = ""
)

data class SearchInstrumentItem(
    val symbol: String,
    val exchange: String,
    val category: String,
    val expiry: String,
    val lotSize: Int,
    val basePrice: Double,
    val baseChangePct: Double
)

private fun generateSearchInstrumentPool(): List<SearchInstrumentItem> {
    return listOf(
        // NSE Index Options & Futures
        SearchInstrumentItem("NIFTY 25AUG 24200 PE", "NSE", "OPTIONS", "25 AUG", 65, 122.10, -12.8),
        SearchInstrumentItem("NIFTY 25AUG 24400 CE", "NSE", "OPTIONS", "25 AUG", 65, 184.50, 18.4),
        SearchInstrumentItem("NIFTY 25AUG 24300 CE", "NSE", "OPTIONS", "25 AUG", 65, 230.00, 14.5),
        SearchInstrumentItem("NIFTY 25AUG 24500 CE", "NSE", "OPTIONS", "25 AUG", 65, 115.00, 22.5),
        SearchInstrumentItem("NIFTY 25AUG 24100 PE", "NSE", "OPTIONS", "25 AUG", 65, 88.50, -16.2),
        SearchInstrumentItem("NIFTY 25AUG 24600 CE", "NSE", "OPTIONS", "25 AUG", 65, 68.40, 28.0),
        SearchInstrumentItem("BANKNIFTY 25AUG 51200 CE", "NSE", "OPTIONS", "25 AUG", 30, 340.20, 14.2),
        SearchInstrumentItem("BANKNIFTY 25AUG 50800 PE", "NSE", "OPTIONS", "25 AUG", 30, 210.80, -8.6),
        SearchInstrumentItem("BANKNIFTY 25AUG 51500 CE", "NSE", "OPTIONS", "25 AUG", 30, 195.40, 19.1),
        SearchInstrumentItem("BANKNIFTY 25AUG 50500 PE", "NSE", "OPTIONS", "25 AUG", 30, 130.00, -14.5),
        SearchInstrumentItem("FINNIFTY 25AUG 23400 CE", "NSE", "OPTIONS", "25 AUG", 60, 95.40, 11.5),
        SearchInstrumentItem("FINNIFTY 25AUG 23200 PE", "NSE", "OPTIONS", "25 AUG", 60, 64.20, -10.5),
        SearchInstrumentItem("MIDCPNIFTY 25AUG 12800 CE", "NSE", "OPTIONS", "25 AUG", 120, 85.20, 16.8),
        SearchInstrumentItem("MIDCPNIFTY 25AUG 12600 PE", "NSE", "OPTIONS", "25 AUG", 120, 52.10, -12.4),
        
        // BSE Index Options & Stocks
        SearchInstrumentItem("SENSEX 29AUG 80500 CE", "BSE", "OPTIONS", "29 AUG", 20, 380.00, 15.2),
        SearchInstrumentItem("SENSEX 29AUG 80000 PE", "BSE", "OPTIONS", "29 AUG", 20, 245.50, -11.2),
        SearchInstrumentItem("SENSEX 29AUG 81000 CE", "BSE", "OPTIONS", "29 AUG", 20, 215.00, 18.5),
        SearchInstrumentItem("SENSEX 29AUG 79500 PE", "BSE", "OPTIONS", "29 AUG", 20, 160.00, -14.2),
        SearchInstrumentItem("BANKEX 29AUG 57000 CE", "BSE", "OPTIONS", "29 AUG", 30, 310.00, 12.0),
        SearchInstrumentItem("BANKEX 29AUG 56500 PE", "BSE", "OPTIONS", "29 AUG", 30, 195.00, -8.6),
        SearchInstrumentItem("BANKEX 29AUG 57500 CE", "BSE", "OPTIONS", "29 AUG", 30, 180.00, 16.2),
        
        // MCX Commodity Contracts & Options
        SearchInstrumentItem("CRUDEOIL 19SEP 6400 CE", "MCX", "OPTIONS", "19 SEP", 100, 165.00, 8.2),
        SearchInstrumentItem("CRUDEOIL 19SEP 6300 PE", "MCX", "OPTIONS", "19 SEP", 100, 142.00, -12.4),
        SearchInstrumentItem("CRUDEOIL 19SEP 6500 CE", "MCX", "OPTIONS", "19 SEP", 100, 110.00, 16.5),
        SearchInstrumentItem("CRUDEOIL", "MCX", "FUTURES", "19 SEP", 100, 6380.00, 1.45),
        SearchInstrumentItem("CRUDEOIL M", "MCX", "FUTURES", "19 SEP", 10, 6382.00, 1.42),
        SearchInstrumentItem("GOLD 05OCT 75000 CE", "MCX", "OPTIONS", "05 OCT", 100, 580.00, 4.8),
        SearchInstrumentItem("GOLD 05OCT 74500 PE", "MCX", "OPTIONS", "05 OCT", 100, 420.00, -6.2),
        SearchInstrumentItem("GOLD", "MCX", "FUTURES", "05 OCT", 100, 74650.00, 0.65),
        SearchInstrumentItem("GOLD M", "MCX", "FUTURES", "05 OCT", 10, 74680.00, 0.62),
        SearchInstrumentItem("SILVER 28NOV 85000 CE", "MCX", "OPTIONS", "28 NOV", 30, 1240.00, 6.5),
        SearchInstrumentItem("SILVER 28NOV 83000 PE", "MCX", "OPTIONS", "28 NOV", 30, 780.00, -9.10),
        SearchInstrumentItem("SILVER", "MCX", "FUTURES", "28 NOV", 30, 84200.00, 1.15),
        SearchInstrumentItem("SILVER M", "MCX", "FUTURES", "28 NOV", 5, 84250.00, 1.12),
        SearchInstrumentItem("NATURALGAS 26SEP 190 CE", "MCX", "OPTIONS", "26 SEP", 1250, 9.80, 14.0),
        SearchInstrumentItem("NATURALGAS 26SEP 180 PE", "MCX", "OPTIONS", "26 SEP", 1250, 6.20, -14.5),
        SearchInstrumentItem("NATURALGAS", "MCX", "FUTURES", "26 SEP", 1250, 188.50, 2.20),
        SearchInstrumentItem("COPPER 30SEP 810 CE", "MCX", "OPTIONS", "30 SEP", 2500, 12.50, 5.2),
        SearchInstrumentItem("COPPER", "MCX", "FUTURES", "30 SEP", 2500, 804.50, 0.85),
        
        // Equities
        SearchInstrumentItem("RELIANCE", "NSE", "EQUITY", "CASH", 1, 2950.45, 2.35),
        SearchInstrumentItem("TATASTEEL", "NSE", "EQUITY", "CASH", 1, 142.60, 1.89),
        SearchInstrumentItem("HDFCBANK", "NSE", "EQUITY", "CASH", 1, 1678.40, 1.45),
        SearchInstrumentItem("INFY", "NSE", "EQUITY", "CASH", 1, 1884.20, 1.10),
        SearchInstrumentItem("ICICIBANK", "NSE", "EQUITY", "CASH", 1, 1225.50, 0.95),
        SearchInstrumentItem("TCS", "NSE", "EQUITY", "CASH", 1, 4188.00, -0.45),
        SearchInstrumentItem("SBIN", "NSE", "EQUITY", "CASH", 1, 815.00, 1.25)
    )
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
