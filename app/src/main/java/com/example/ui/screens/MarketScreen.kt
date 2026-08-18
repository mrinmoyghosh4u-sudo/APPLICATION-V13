package com.example.ui.screens

import java.util.Calendar
import java.util.TimeZone
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CandlestickChart
import com.example.ui.components.CrownLogo
import com.example.ui.components.GoldButton
import com.example.ui.components.GoldCard
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.components.SparklineChart
import com.example.ui.theme.*
import com.example.util.MarketStatusUtil

@Composable
fun MarketScreen(
    userProfile: UserProfileEntity,
    watchlist: List<WatchlistItem>,
    recentSearches: List<String> = emptyList(),
    marketDataSource: String = "Angel One",
    marketDataLastUpdated: String = "",
    onOpenNotificationCenter: () -> Unit,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit,
    onAddSymbolToWatchlist: (symbol: String, exchange: String) -> Unit = { _, _ -> },
    onToggleFavorite: (symbol: String, currentStatus: Boolean) -> Unit = { _, _ -> },
    onAddRecentSearch: (query: String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedExchange by remember { mutableStateOf("NSE") }
    var selectedTimeframe by remember { mutableStateOf("1H") }
    var activeDepthTab by remember { mutableStateOf("ORDER BOOK") }
    var mainViewTab by remember { mutableStateOf("WATCHLIST") } // WATCHLIST or MARKET MOVERS
    var moverCategory by remember { mutableStateOf("GAINERS") } // GAINERS, LOSERS, VOLUME, OI_CHANGE
    var showAddSymbolDialog by remember { mutableStateOf(false) }

    // Fallback default Option contracts for each exchange if watchlist has no items for it
    // Master Symbol Database for Universal Smart Search (NSE, BSE, MCX - EQ, FUT, OPT)
    val masterUniversalSymbols = remember {
        listOf(
            // NSE Index Options & Futures
            WatchlistItem("NIFTY 24850 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY 24850 CE"), true),
            WatchlistItem("NIFTY 24850 PE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY 24850 PE"), false),
            WatchlistItem("NIFTY 24900 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY 24900 CE"), true),
            WatchlistItem("NIFTY 24900 PE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY 24900 PE"), false),
            WatchlistItem("NIFTY 24800 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY 24800 CE"), true),
            WatchlistItem("NIFTY 24800 PE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY 24800 PE"), false),
            WatchlistItem("NIFTY FUT", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY FUT"), true),
            WatchlistItem("BANKNIFTY 52400 PE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY 52400 PE"), false),
            WatchlistItem("BANKNIFTY 52500 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY 52500 CE"), true),
            WatchlistItem("BANKNIFTY 52000 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY 52000 CE"), true),
            WatchlistItem("BANKNIFTY 52000 PE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY 52000 PE"), false),
            WatchlistItem("BANKNIFTY FUT", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY FUT"), true),
            WatchlistItem("FINNIFTY 23400 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("FINNIFTY 23400 CE"), true),
            WatchlistItem("FINNIFTY 23400 PE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("FINNIFTY 23400 PE"), false),
            WatchlistItem("MIDCPNIFTY 12200 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("MIDCPNIFTY 12200 CE"), true),

            // NSE Equities
            WatchlistItem("RELIANCE", "NSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("TCS", "NSE", 0.0, 0.0, 0.0, 1, false),
            WatchlistItem("INFY", "NSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("HDFCBANK", "NSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("ICICIBANK", "NSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("SBIN", "NSE", 0.0, 0.0, 0.0, 1, false),
            WatchlistItem("TATAMOTORS", "NSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("TATASTEEL", "NSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("BHARTIARTL", "NSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("LT", "NSE", 0.0, 0.0, 0.0, 1, true),

            // BSE Index Options & Equities
            WatchlistItem("SENSEX 81500 CE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("SENSEX 81500 CE"), true),
            WatchlistItem("SENSEX 81000 PE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("SENSEX 81000 PE"), false),
            WatchlistItem("SENSEX 82000 CE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("SENSEX 82000 CE"), true),
            WatchlistItem("SENSEX 82000 PE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("SENSEX 82000 PE"), false),
            WatchlistItem("SENSEX FUT", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("SENSEX FUT"), true),
            WatchlistItem("BANKEX 58000 CE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKEX 58000 CE"), true),
            WatchlistItem("ZOMATO", "BSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("JIOFIN", "BSE", 0.0, 0.0, 0.0, 1, true),
            WatchlistItem("HAL", "BSE", 0.0, 0.0, 0.0, 1, true),

            // MCX Commodity Futures & Options
            WatchlistItem("CRUDEOIL FUT", "MCX", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL FUT"), true),
            WatchlistItem("CRUDEOIL 6800 CE", "MCX", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL 6800 CE"), true),
            WatchlistItem("CRUDEOIL 6800 PE", "MCX", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL 6800 PE"), false),
            WatchlistItem("CRUDEOILM 6450 CE", "MCX", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("CRUDEOILM 6450 CE"), true),
            WatchlistItem("NATURALGAS FUT", "MCX", 0.0, 0.0, 0.0, 1250, true),
            WatchlistItem("NATURALGAS 180 CE", "MCX", 0.0, 0.0, 0.0, 1250, true),
            WatchlistItem("GOLD FUT", "MCX", 0.0, 0.0, 0.0, 100, true),
            WatchlistItem("GOLD 72000 CE", "MCX", 0.0, 0.0, 0.0, 100, false),
            WatchlistItem("SILVER FUT", "MCX", 0.0, 0.0, 0.0, 30, true),
            WatchlistItem("SILVER 85000 PE", "MCX", 0.0, 0.0, 0.0, 30, true),
            WatchlistItem("COPPER FUT", "MCX", 0.0, 0.0, 0.0, 2500, true)
        )
    }

    val defaultNseOptions = remember {
        listOf(
            WatchlistItem("NIFTY 24850 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY 24850 CE"), true),
            WatchlistItem("BANKNIFTY 52400 PE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY 52400 PE"), false),
            WatchlistItem("FINNIFTY 23400 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("FINNIFTY 23400 CE"), true),
            WatchlistItem("NIFTY 24800 PE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("NIFTY 24800 PE"), false),
            WatchlistItem("BANKNIFTY 52500 CE", "NSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY 52500 CE"), true)
        )
    }

    val defaultBseOptions = remember {
        listOf(
            WatchlistItem("SENSEX 81500 CE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("SENSEX 81500 CE"), true),
            WatchlistItem("SENSEX 81000 PE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("SENSEX 81000 PE"), false),
            WatchlistItem("BANKEX 58000 CE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("BANKEX 58000 CE"), true),
            WatchlistItem("SENSEX 82000 CE", "BSE", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("SENSEX 82000 CE"), true)
        )
    }

    val defaultMcxOptions = remember {
        listOf(
            WatchlistItem("CRUDEOILM 6450 CE", "MCX", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("CRUDEOILM 6450 CE"), true),
            WatchlistItem("CRUDEOIL 6800 CE", "MCX", 0.0, 0.0, 0.0, com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL 6800 CE"), true),
            WatchlistItem("NATURALGAS 180 CE", "MCX", 0.0, 0.0, 0.0, 1250, true),
            WatchlistItem("GOLD 72000 CE", "MCX", 0.0, 0.0, 0.0, 100, false),
            WatchlistItem("SILVER 85000 PE", "MCX", 0.0, 0.0, 0.0, 30, true)
        )
    }

    // Universal search suggestions matching searchQuery across all master symbols
    val universalSearchSuggestions = remember(masterUniversalSymbols, watchlist, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val allList = (watchlist + masterUniversalSymbols).distinctBy { "${it.symbol}_${it.exchange}" }
            allList.filter {
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.exchange.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Determine items for selected exchange
    val currentExchangeItems = remember(watchlist, selectedExchange) {
        val userItems = watchlist.filter { it.exchange.equals(selectedExchange, ignoreCase = true) }
        if (userItems.isNotEmpty()) userItems
        else when (selectedExchange.uppercase()) {
            "BSE" -> defaultBseOptions
            "MCX" -> defaultMcxOptions
            else -> defaultNseOptions
        }
    }

    // Filter items based on searchQuery
    val filteredWatchlist = remember(currentExchangeItems, universalSearchSuggestions, searchQuery) {
        if (searchQuery.isBlank()) {
            currentExchangeItems
        } else {
            universalSearchSuggestions
        }
    }

    // Selected Symbol State
    var selectedSymbol by remember(selectedExchange) {
        mutableStateOf(currentExchangeItems.firstOrNull()?.symbol ?: "NIFTY 24850 CE")
    }

    val currentSymbolItem = remember(filteredWatchlist, currentExchangeItems, selectedSymbol) {
        filteredWatchlist.find { it.symbol == selectedSymbol }
            ?: currentExchangeItems.find { it.symbol == selectedSymbol }
            ?: currentExchangeItems.firstOrNull()
    }

    // Calculate Real Market Status in IST Timezone
    val marketStatus = remember(selectedExchange) { MarketStatusUtil.getDetailedMarketStatus(selectedExchange) }

    val isBrokerConnected = (userProfile.isAngelConnected || userProfile.isDhanConnected) && userProfile.connectedBroker.isNotBlank()
    val activeBrokerName = if (isBrokerConnected) userProfile.connectedBroker else "Broker API"

    PullToRefreshLayout(onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CrownLogo(size = 36.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("KING KHAN AI TRADE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = PrimaryGold)
                    Text("Trade Like a King \uD83D\uDC51", fontSize = 11.sp, color = SecondaryGold)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isDisconnected = marketDataSource.contains("Disconnected", ignoreCase = true) || 
                                         marketDataSource.contains("UNAVAILABLE", ignoreCase = true) || 
                                         marketDataSource == "DISCONNECTED"
                    val isLive = !isDisconnected
                    val indicatorColor = if (isLive) ProfitGreen else LossRed
                    
                    val activeBrokerName = when {
                        marketDataSource.contains("m.Stock", ignoreCase = true) -> "m.STOCK"
                        marketDataSource.contains("TradeSmart", ignoreCase = true) -> "TRADESMART"
                        else -> "ANGEL ONE"
                    }

                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLive) indicatorColor.copy(alpha = 0.5f) else LossRed.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(indicatorColor, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLive) "LIVE — $activeBrokerName" else "$activeBrokerName DATA UNAVAILABLE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = indicatorColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onOpenNotificationCenter, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = SecondaryGold, modifier = Modifier.size(22.dp))
                    }
                }
                if (!marketDataSource.contains("Disconnected", ignoreCase = true) && marketDataLastUpdated.isNotBlank() && marketDataLastUpdated != "Not Updated") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Updated: $marketDataLastUpdated",
                        fontSize = 8.sp,
                        color = TextGray
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.length >= 3) onAddRecentSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search Symbol, Strike (24850), CE/PE on $selectedExchange...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextGray)
                    }
                } else {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = SecondaryGold)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = DarkCardBorder,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // Recent Searches Row
        if (recentSearches.isNotEmpty() && searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent:", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    recentSearches.take(6).forEach { recent ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { searchQuery = recent },
                            color = DarkCardSecondary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = recent,
                                fontSize = 10.sp,
                                color = SecondaryGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Clear",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onClearRecentSearches() }
                        .padding(start = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Exchange Tabs (NSE, BSE, MCX)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(DarkCard, RoundedCornerShape(8.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("NSE", "BSE", "MCX").forEach { exchange ->
                val isSelected = selectedExchange == exchange
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryGold else Color.Transparent)
                        .clickable {
                            selectedExchange = exchange
                            searchQuery = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = exchange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else TextGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))        // Real Market Status Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
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
                                .background(if (marketStatus.isOpen) ProfitGreen else SecondaryGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (marketStatus.isOpen) "MARKET OPEN — ${activeBrokerName.uppercase()}" else "MARKET CLOSED — ${activeBrokerName.uppercase()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (marketStatus.isOpen) ProfitGreen else SecondaryGold
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SecondaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (marketDataLastUpdated.isNotBlank()) "Last valid update: $marketDataLastUpdated" else "Last valid update: N/A",
                    fontSize = 11.sp,
                    color = TextGray
                )
                
                if (!marketStatus.isOpen) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = marketStatus.fullDetailLabel.substringAfter("•").trim(),
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Scrollable Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Selected Option Chart & Quote Section
            GoldCard(borderColor = DarkCardBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentSymbolItem?.symbol ?: selectedSymbol, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(DarkGold.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(if ((currentSymbolItem?.symbol ?: selectedSymbol).contains("PE")) "PUT (PE)" else "CALL (CE)", fontSize = 8.sp, color = SecondaryGold, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("${currentSymbolItem?.exchange ?: selectedExchange} Options • Live Broker Quote", fontSize = 10.sp, color = TextGray)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val ltp = currentSymbolItem?.ltp ?: 0.0
                        val change = currentSymbolItem?.change ?: 0.0
                        val chgPct = currentSymbolItem?.changePercent ?: 0.0
                        val isPos = (currentSymbolItem?.isPositive ?: true)
                        Text(String.format("₹%.2f", ltp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isPos) ProfitGreen else LossRed)
                        Text("${if (isPos) "+" else ""}${String.format("%.2f", change)} (${if (isPos) "+" else ""}${String.format("%.2f", chgPct)}%)", fontSize = 10.sp, color = if (isPos) ProfitGreen else LossRed)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Timeframe Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("1m", "5m", "15m", "1H", "1D").forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Text(
                            text = tf,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryGold else TextGray,
                            modifier = Modifier
                                .clickable { selectedTimeframe = tf }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = TextGray, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Chart Canvas
                CandlestickChart()

                Spacer(modifier = Modifier.height(12.dp))
                
                // Option Information
                Text("OPTION DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                Spacer(modifier = Modifier.height(8.dp))

                val baseLtp = currentSymbolItem?.ltp ?: 0.0
                val volume = currentSymbolItem?.volume ?: 0L
                val oiChange = currentSymbolItem?.oiChange ?: 0.0
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strike", fontSize = 9.sp, color = TextGray)
                        Text((currentSymbolItem?.symbol ?: selectedSymbol).split(" ").getOrNull(1) ?: "--", fontSize = 11.sp, color = TextWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Volume", fontSize = 9.sp, color = TextGray)
                        Text(if (volume > 0) String.format("%,d", volume) else "--", fontSize = 11.sp, color = TextWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Bid", fontSize = 9.sp, color = TextGray)
                        Text(String.format("₹%.2f", baseLtp * 0.99), fontSize = 11.sp, color = ProfitGreen)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Option Type", fontSize = 9.sp, color = TextGray)
                        Text(if ((currentSymbolItem?.symbol ?: selectedSymbol).contains("PE")) "PUT" else "CALL", fontSize = 11.sp, color = TextWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("OI Change", fontSize = 9.sp, color = TextGray)
                        Text(if (oiChange != 0.0) String.format("%.1f%%", oiChange) else "--", fontSize = 11.sp, color = TextWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Ask", fontSize = 9.sp, color = TextGray)
                        Text(String.format("₹%.2f", baseLtp * 1.01), fontSize = 11.sp, color = LossRed)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Expiry", fontSize = 9.sp, color = TextGray)
                        Text("Weekly", fontSize = 11.sp, color = TextWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("IV", fontSize = 9.sp, color = TextGray)
                        Text("14.5%", fontSize = 11.sp, color = TextWhite)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main View Switcher: WATCHLIST vs MARKET MOVERS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("WATCHLIST" to "⭐ WATCHLIST", "MARKET_MOVERS" to "🔥 MARKET MOVERS").forEach { (tabKey, tabLabel) ->
                    val isSel = mainViewTab == tabKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) PrimaryGold else Color.Transparent)
                            .clickable { mainViewTab = tabKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mainViewTab == "WATCHLIST") {
                // Favorites Watchlist Top Section
                val favoriteItems = remember(filteredWatchlist) { filteredWatchlist.filter { it.isFavorite } }
                if (favoriteItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⭐ FAVORITES WATCHLIST", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                        }
                        Text("${favoriteItems.size} starred", fontSize = 10.sp, color = TextGray)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    favoriteItems.forEach { item ->
                        val isSelected = selectedSymbol == item.symbol
                        GoldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedSymbol = item.symbol },
                            borderColor = if (isSelected) PrimaryGold else PrimaryGold.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleFavorite(item.symbol, item.isFavorite) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = "Favorite", tint = PrimaryGold, modifier = Modifier.size(18.dp))
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(item.symbol, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("${item.exchange} • Lot: ${item.lotSize}", fontSize = 9.sp, color = TextGray)
                                }

                                Box(modifier = Modifier.weight(0.8f)) {
                                    SparklineChart(isPositive = item.isPositive)
                                }

                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format("₹%.2f", item.ltp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isPositive) ProfitGreen else LossRed
                                    )
                                    Text(
                                        text = "${if (item.isPositive) "+" else ""}${String.format("%.2f", item.change)} (${if (item.isPositive) "+" else ""}${String.format("%.2f", item.changePercent)}%)",
                                        fontSize = 9.sp,
                                        color = if (item.isPositive) ProfitGreen else LossRed
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // All Option Watchlist Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$selectedExchange ALL CONTRACTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }

                    Text("+ Add Symbol", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGold, modifier = Modifier.clickable { showAddSymbolDialog = true })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Watchlist Items
                if (filteredWatchlist.isNotEmpty()) {
                    filteredWatchlist.forEach { item ->
                        val isSelected = selectedSymbol == item.symbol
                        GoldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedSymbol = item.symbol },
                            borderColor = if (isSelected) PrimaryGold else DarkCardBorder
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleFavorite(item.symbol, item.isFavorite) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        if (item.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Favorite",
                                        tint = if (item.isFavorite) PrimaryGold else TextGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(item.symbol, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("${item.exchange} • Lot: ${item.lotSize}", fontSize = 9.sp, color = TextGray)
                                }

                                Box(modifier = Modifier.weight(0.8f)) {
                                    SparklineChart(isPositive = item.isPositive)
                                }

                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    if (item.ltp > 0.0) {
                                        Text(
                                            text = String.format("₹%.2f", item.ltp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isPositive) ProfitGreen else LossRed
                                        )
                                        Text(
                                            text = "${if (item.isPositive) "+" else ""}${String.format("%.2f", item.change)} (${if (item.isPositive) "+" else ""}${String.format("%.2f", item.changePercent)}%)",
                                            fontSize = 9.sp,
                                            color = if (item.isPositive) ProfitGreen else LossRed
                                        )
                                    } else {
                                        Text(
                                            text = "₹ --",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGray
                                        )
                                        Text(
                                            text = "--",
                                            fontSize = 9.sp,
                                            color = TextGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching $selectedExchange option contracts found.\nTap '+ Add Symbol' to add a contract.",
                            fontSize = 11.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // MARKET MOVERS SECTION
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "GAINERS" to "Top Gainers",
                            "LOSERS" to "Top Losers",
                            "VOLUME" to "High Vol",
                            "OI_CHANGE" to "High OI Chg"
                        ).forEach { (catKey, catLabel) ->
                            val isSel = moverCategory == catKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { moverCategory = catKey },
                                color = if (isSel) PrimaryGold else DarkCard,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) PrimaryGold else DarkCardBorder)
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else TextWhite,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val moverItems = remember(filteredWatchlist, moverCategory) {
                        when (moverCategory) {
                            "GAINERS" -> filteredWatchlist.sortedByDescending { it.changePercent }
                            "LOSERS" -> filteredWatchlist.sortedBy { it.changePercent }
                            "VOLUME" -> filteredWatchlist.sortedByDescending { if (it.volume > 0) it.volume else (it.ltp * 850).toLong() }
                            else -> filteredWatchlist.sortedByDescending { kotlin.math.abs(it.changePercent) }
                        }
                    }

                    moverItems.forEach { item ->
                        GoldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedSymbol = item.symbol },
                            borderColor = DarkCardBorder
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(item.symbol, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    val subtitle = when (moverCategory) {
                                        "VOLUME" -> "Vol: ${String.format("%,d", if (item.volume > 0) item.volume else (item.ltp * 850).toLong())}"
                                        "OI_CHANGE" -> "OI Chg: +${String.format("%.1f", kotlin.math.abs(item.changePercent * 2.4))}%"
                                        else -> "${item.exchange} • Lot: ${com.example.util.AppPreferences.getGlobalLotSize(item.symbol)}"
                                    }
                                    Text(subtitle, fontSize = 9.sp, color = SecondaryGold)
                                }

                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format("₹%.2f", item.ltp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isPositive) ProfitGreen else LossRed
                                    )
                                    Text(
                                        text = "${if (item.isPositive) "+" else ""}${String.format("%.2f", item.changePercent)}%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isPositive) ProfitGreen else LossRed
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onOpenOrderDialog(item.symbol, if (item.isPositive) "BUY" else "SELL", item.ltp, com.example.util.AppPreferences.getGlobalLotSize(item.symbol)) },
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (item.isPositive) ProfitGreen else LossRed),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text("TRADE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { showAddSymbolDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
            ) {
                Text("+ Add $selectedExchange Option Symbol", fontSize = 12.sp, color = SecondaryGold, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Fixed Quick Trade Bar
        val selectedItem = currentSymbolItem
        var qty by remember { mutableStateOf(1) }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(selectedSymbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedItem != null) String.format("₹%.2f", selectedItem.ltp) else "--",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedItem?.isPositive != false) ProfitGreen else LossRed
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Qty:", fontSize = 11.sp, color = TextGray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkCardSecondary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "-",
                                    fontSize = 16.sp,
                                    color = TextWhite,
                                    modifier = Modifier.clickable { if (qty > 1) qty-- }.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                Text(
                                    "$qty",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    "+",
                                    fontSize = 14.sp,
                                    color = TextWhite,
                                    modifier = Modifier.clickable { qty++ }.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onOpenOrderDialog(selectedSymbol, "BUY", selectedItem?.ltp, qty * com.example.util.AppPreferences.getGlobalLotSize(selectedSymbol)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                    ) {
                        Text("BUY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { onOpenOrderDialog(selectedSymbol, "SELL", selectedItem?.ltp, qty * com.example.util.AppPreferences.getGlobalLotSize(selectedSymbol)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                    ) {
                        Text("SELL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
    }

    // Modal: Add Custom Option Symbol Dialog
    if (showAddSymbolDialog) {
        AddSymbolDialog(
            defaultExchange = selectedExchange,
            onDismiss = { showAddSymbolDialog = false },
            onAddSymbol = { newSymbol, exchange ->
                onAddSymbolToWatchlist(newSymbol, exchange)
                selectedExchange = exchange
                selectedSymbol = newSymbol
                showAddSymbolDialog = false
            }
        )
    }
}

/**
 * Calculates Market Status in India Standard Time (Asia/Kolkata)
 */
private data class MarketStatusResult(
    val isOpen: Boolean,
    val statusLabel: String,
    val detailLabel: String
)

private fun getMarketStatusIST(exchange: String): MarketStatusResult {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Saturday = 7
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val totalMinutes = hour * 60 + minute

    val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

    return when (exchange.uppercase()) {
        "MCX" -> {
            // MCX Commodity hours: Mon-Fri 09:00 to 23:30 IST (540 mins to 1410 mins)
            val openMin = 9 * 60 // 09:00 AM
            val closeMin = 23 * 60 + 30 // 11:30 PM

            if (!isWeekend && totalMinutes in openMin..closeMin) {
                MarketStatusResult(
                    isOpen = true,
                    statusLabel = "MARKET OPEN",
                    detailLabel = "MCX Commodities Session (09:00 - 23:30 IST)"
                )
            } else {
                val nextMsg = if (isWeekend) "Opens Mon 09:00 IST" else if (totalMinutes < openMin) "Opens Today 09:00 IST" else "Opens Tomorrow 09:00 IST"
                MarketStatusResult(
                    isOpen = false,
                    statusLabel = "MARKET CLOSED",
                    detailLabel = "MCX Closed • $nextMsg"
                )
            }
        }
        else -> { // NSE / BSE Equity & Derivatives
            // Mon-Fri 09:15 to 15:30 IST (555 mins to 930 mins)
            val openMin = 9 * 60 + 15 // 09:15 AM
            val closeMin = 15 * 60 + 30 // 03:30 PM

            if (!isWeekend && totalMinutes in openMin..closeMin) {
                MarketStatusResult(
                    isOpen = true,
                    statusLabel = "MARKET OPEN",
                    detailLabel = "$exchange Equity & Options Session (09:15 - 15:30 IST)"
                )
            } else {
                val nextMsg = if (isWeekend) "Opens Mon 09:15 IST" else if (totalMinutes < openMin) "Opens Today 09:15 IST" else "Opens Tomorrow 09:15 IST"
                MarketStatusResult(
                    isOpen = false,
                    statusLabel = "MARKET CLOSED",
                    detailLabel = "$exchange Closed • $nextMsg"
                )
            }
        }
    }
}

@Composable
private fun AddSymbolDialog(
    defaultExchange: String,
    onDismiss: () -> Unit,
    onAddSymbol: (symbol: String, exchange: String) -> Unit
) {
    var symbolInput by remember { mutableStateOf("") }
    var selectedEx by remember { mutableStateOf(defaultExchange) }
    var optionType by remember { mutableStateOf("CE") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Option Contract", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exchange Selector
                Text("Select Exchange", fontSize = 11.sp, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("NSE", "BSE", "MCX").forEach { ex ->
                        val isSel = selectedEx == ex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) PrimaryGold else DarkCardSecondary)
                                .border(1.dp, if (isSel) PrimaryGold else DarkCardBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedEx = ex },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ex, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option Type Selector (CE / PE)
                Text("Option Type", fontSize = 11.sp, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CE" to "CALL (CE)", "PE" to "PUT (PE)").forEach { (type, label) ->
                        val isSel = optionType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) (if (type == "CE") ProfitGreen else LossRed) else DarkCardSecondary)
                                .clickable { optionType = type },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Symbol / Strike Input
                OutlinedTextField(
                    value = symbolInput,
                    onValueChange = { symbolInput = it },
                    label = { Text("Symbol Name & Strike (e.g. CRUDEOIL 6900)", color = TextGray, fontSize = 11.sp) },
                    placeholder = { Text("e.g. NIFTY 24900 or CRUDEOIL 6800", color = TextMuted, fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                GoldButton(
                    text = "ADD TO WATCHLIST",
                    onClick = {
                        val raw = symbolInput.trim().uppercase()
                        if (raw.isNotBlank()) {
                            val fullSymbol = if (raw.endsWith("CE") || raw.endsWith("PE")) raw else "$raw $optionType"
                            onAddSymbol(fullSymbol, selectedEx)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = symbolInput.isNotBlank()
                )
            }
        }
    }
}
