package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketDataStore
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
    var selectedContractFilter by rememberSaveable { mutableStateOf("All") }
    var selectedMoverCategory by rememberSaveable { mutableStateOf("Top Gainers") }
    var showAddSymbolDialog by rememberSaveable { mutableStateOf(false) }

    val marketDataMap by MarketDataStore.marketData.collectAsStateWithLifecycle()

    val exchangeStatus = remember(selectedExchange) { MarketStatusUtil.getDetailedMarketStatus(selectedExchange) }
    val isMarketOpen = exchangeStatus.isOpen

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090A0C))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 1. TOP HEADER (Logo, King Khan AI Trade, Search, Notifications, Profile)
            MarketHeaderSection(
                onOpenNotificationCenter = onOpenNotificationCenter
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. MARKET STATUS BAR
            MarketStatusBarSection(
                isMarketOpen = isMarketOpen,
                nextOpeningText = exchangeStatus.nextOpeningTimeText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. SEARCH BAR & RECENT SEARCH CHIPS
            SearchBarSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                recentSearches = if (recentSearches.isNotEmpty()) recentSearches else listOf("NIFTY 22000 CE", "BANKNIFTY 48000 PE", "RELIANCE", "TATASTEEL"),
                onRecentChipClick = { chip ->
                    searchQuery = chip
                    onAddRecentSearch(chip)
                },
                onClearRecent = onClearRecentSearches
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. EXCHANGE SEGMENT TABS (NSE, BSE, MCX)
            ExchangeSegmentTabs(
                selectedExchange = selectedExchange,
                onExchangeSelected = {
                    selectedExchange = it
                    searchQuery = ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. EXCHANGE INDICES SECTION (⭐ NSE INDICES / BSE INDICES / MCX INDICES)
            ExchangeIndicesSection(
                exchange = selectedExchange,
                marketDataMap = marketDataMap,
                onNavigateToIndexDetails = onNavigateToIndexDetails,
                onOpenOrderDialog = onOpenOrderDialog
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6. MARKET MOVERS SECTION (🔥 MARKET MOVERS)
            MarketMoversSection(
                selectedCategory = selectedMoverCategory,
                onCategorySelected = { selectedMoverCategory = it },
                marketDataMap = marketDataMap,
                onOpenOrderDialog = onOpenOrderDialog,
                onToggleFavorite = onToggleFavorite
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
// 1. TOP HEADER
// ==========================================
@Composable
private fun MarketHeaderSection(
    onOpenNotificationCenter: () -> Unit
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
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = PrimaryGold,
                modifier = Modifier.size(22.dp)
            )

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

            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Profile",
                tint = PrimaryGold,
                modifier = Modifier.size(26.dp)
            )
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
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filter",
                    tint = PrimaryGold,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "Recent:", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)

            recentSearches.take(3).forEach { chip ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFF181B21), RoundedCornerShape(4.dp))
                        .border(0.6.dp, Color(0xFF282D36), RoundedCornerShape(4.dp))
                        .clickable { onRecentChipClick(chip) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = chip, color = PrimaryGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Clear",
                color = TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onClearRecent() }
                    .padding(start = 4.dp)
            )
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
// 5. EXCHANGE INDICES SECTION (2x2 Grid)
// ==========================================
@Composable
private fun ExchangeIndicesSection(
    exchange: String,
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    onNavigateToIndexDetails: (exchange: String, indexName: String) -> Unit,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit
) {
    val indexList = when (exchange.uppercase()) {
        "BSE" -> listOf(
            IndexCardData("SENSEX", 0.0, 0.0, 0.0, 10),
            IndexCardData("BANKEX", 0.0, 0.0, 0.0, 15)
        )
        "MCX" -> listOf(
            IndexCardData("CRUDEOIL", 0.0, 0.0, 0.0, 100),
            IndexCardData("NATURALGAS", 0.0, 0.0, 0.0, 1250),
            IndexCardData("GOLD", 0.0, 0.0, 0.0, 100),
            IndexCardData("SILVER", 0.0, 0.0, 0.0, 30)
        )
        else -> listOf(
            IndexCardData("NIFTY 50", 0.0, 0.0, 0.0, 50),
            IndexCardData("BANKNIFTY", 0.0, 0.0, 0.0, 15),
            IndexCardData("FINNIFTY", 0.0, 0.0, 0.0, 25),
            IndexCardData("MIDCPNIFTY", 0.0, 0.0, 0.0, 50)
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
                            onTrade = { onOpenOrderDialog(item.name, "BUY", ltp, item.lotSize) }
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
    onTrade: () -> Unit
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
                onClick = onClick,
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

@Composable
private fun MarketMoversSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    marketDataMap: Map<String, com.example.data.model.MarketDataState>,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit,
    onToggleFavorite: (symbol: String, currentStatus: Boolean) -> Unit
) {
    val categories = listOf("Top Gainers", "Top Losers", "High Volume", "High OI Chg")

    val items = listOf(
        MarketMoverCardData("RELIANCE", "NSE", 2950.45, 2.35, 1),
        MarketMoverCardData("TATASTEEL", "NSE", 142.60, 1.89, 1),
        MarketMoverCardData("HDFCBANK", "NSE", 1678.40, 1.45, 1)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥 MARKET MOVERS",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All >",
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
            items.forEach { item ->
                val tick = marketDataMap[item.symbol]
                val ltp = tick?.ltp ?: item.price
                val pct = tick?.changePercent ?: item.changePct
                val isPositive = pct >= 0

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
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onToggleFavorite(item.symbol, false) }
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
                            onClick = { onOpenOrderDialog(item.symbol, "BUY", ltp, item.lotSize) },
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
    val lotSize: Int
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
                Text("Enter Symbol or Option Contract (e.g. RELIANCE, NIFTY 24900 CE):", color = TextGray, fontSize = 11.sp)
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
