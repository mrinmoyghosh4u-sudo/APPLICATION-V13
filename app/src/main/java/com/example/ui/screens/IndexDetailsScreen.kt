package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OptionStrikeItem
import com.example.ui.components.CrownLogo
import com.example.ui.components.MarketDataStatusIndicator
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.theme.*
import com.example.util.MarketStatusUtil
import com.example.util.OptionExpiryUtil
import com.example.viewmodel.MainViewModel

@Composable
fun IndexDetailsScreen(
    exchange: String,
    indexName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit
) {
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val apiError by viewModel.apiError.collectAsStateWithLifecycle()
    val marketDataSource by viewModel.marketDataSource.collectAsStateWithLifecycle()
    val marketDataLastUpdated by viewModel.marketDataLastUpdated.collectAsStateWithLifecycle()

    val indexItem = watchlist.find { 
        it.symbol.equals(indexName, ignoreCase = true) || 
        (indexName == "MIDCPNIFTY" && it.symbol.contains("MID", ignoreCase = true)) 
    }
    val ltp = indexItem?.ltp ?: 0.0
    val change = indexItem?.change ?: 0.0
    val changePercent = indexItem?.changePercent ?: 0.0
    val isPositive = change >= 0
    val hasData = ltp > 0.0

    val detailedStatus = remember(exchange) { MarketStatusUtil.getDetailedMarketStatus(exchange) }
    val isMarketOpen = detailedStatus.isOpen

    var selectedTab by remember { mutableStateOf("MARKET") }

    LaunchedEffect(indexName) {
        viewModel.setSelectedOptionIndex(indexName)
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = { viewModel.refreshMarketData() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Top App Bar
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
                    val isLiveFeedActive by viewModel.isLiveFeedActive.collectAsStateWithLifecycle()
                    com.example.ui.components.LiveStatusBadge(
                        isLive = isLiveFeedActive,
                        dataSource = marketDataSource,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = { /* Favorite */ }) {
                        Icon(
                            imageVector = if (indexItem?.isFavorite == true) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = SecondaryGold
                        )
                    }
                }
            }

            // Details Section
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(indexName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(DarkCardSecondary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(exchange, fontSize = 10.sp, color = SecondaryGold, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = if (hasData) String.format("%,.2f", ltp) else "DATA UNAVAILABLE",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = if (!hasData) LossRed else if (isPositive) ProfitGreen else LossRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasData) {
                                Text(
                                    text = String.format("%s%.2f (%.2f%%)", if (isPositive) "+" else "", change, changePercent),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) ProfitGreen else LossRed
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isPositive) ProfitGreen else LossRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text("Disconnected from Broker Feed", fontSize = 12.sp, color = TextGray)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))

            // Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                listOf("Market", "Option Chain", "Historical Data").forEach { tab ->
                    val isSelected = selectedTab.equals(tab, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) SecondaryGold else Color.Transparent)
                            .clickable { selectedTab = tab.uppercase() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content
            when (selectedTab) {
                "MARKET" -> MarketTabContent(indexName = indexName, ltp = ltp, change = change, hasData = hasData, viewModel = viewModel)
                "OPTION CHAIN" -> OptionChainTabContent(viewModel = viewModel, exchange = exchange, indexName = indexName, underlyingLtp = ltp, onOpenOrderDialog = onOpenOrderDialog)
                else -> HistoricalDataTabContent(viewModel = viewModel, indexName = indexName, ltp = ltp)
            }
        }
    }
}

@Composable
fun MarketTabContent(
    indexName: String,
    ltp: Double,
    change: Double,
    hasData: Boolean,
    viewModel: MainViewModel
) {
    val indexTick = remember(indexName, ltp) {
        com.example.data.model.MarketDataStore.getTick(indexName)
    }

    val prevCloseVal = indexTick?.previousClose?.takeIf { it > 0.0 }
    val openVal = indexTick?.open?.takeIf { it > 0.0 }
    val highVal = indexTick?.high?.takeIf { it > 0.0 }
    val lowVal = indexTick?.low?.takeIf { it > 0.0 }

    val prevCloseStr = prevCloseVal?.let { String.format(java.util.Locale.getDefault(), "%,.2f", it) } ?: "N/A"
    val openStr = openVal?.let { String.format(java.util.Locale.getDefault(), "%,.2f", it) } ?: "N/A"
    val highStr = highVal?.let { String.format(java.util.Locale.getDefault(), "%,.2f", it) } ?: "N/A"
    val lowStr = lowVal?.let { String.format(java.util.Locale.getDefault(), "%,.2f", it) } ?: "N/A"

    // Dynamic Underlying Component Analysis
    val cleanIndex = indexName.trim().uppercase()
    val totalConstituents = when {
        cleanIndex.contains("SENSEX") -> 30
        cleanIndex.contains("BANKNIFTY") -> 12
        cleanIndex.contains("BANKEX") -> 10
        cleanIndex.contains("FINNIFTY") -> 20
        cleanIndex.contains("MID") -> 25
        cleanIndex.contains("CRUDE") -> 1
        else -> 50 // NIFTY 50
    }

    var candleList by remember { mutableStateOf<List<com.example.ui.components.CandleData>>(emptyList()) }
    LaunchedEffect(indexName) {
        viewModel.getHistoricalCandlesForIndex(indexName) { fetched ->
            candleList = fetched
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text("Underlying Performance", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            if (hasData) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Index Tracked Constituents", fontSize = 12.sp, color = TextWhite)
                        Text("$totalConstituents Instruments", fontSize = 12.sp, color = SecondaryGold, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Real-time constituent market breadth tracks actively over provider socket connection.", fontSize = 11.sp, color = TextGray)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DATA UNAVAILABLE — Provider Disconnected", fontSize = 12.sp, color = TextGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Markets Today", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            if (hasData) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Open", fontSize = 11.sp, color = TextGray)
                            Text(openStr, fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("High", fontSize = 11.sp, color = TextGray)
                            Text(highStr, fontSize = 13.sp, color = ProfitGreen, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Low", fontSize = 11.sp, color = TextGray)
                            Text(lowStr, fontSize = 13.sp, color = LossRed, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Prev. Close", fontSize = 11.sp, color = TextGray)
                            Text(prevCloseStr, fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Day range bar
                    if (highVal != null && lowVal != null && highVal > lowVal && ltp > 0.0) {
                        val range = highVal - lowVal
                        val currentRatio = ((ltp - lowVal) / range).coerceIn(0.0, 1.0).toFloat()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(String.format(java.util.Locale.getDefault(), "Low: %,.2f", lowVal), fontSize = 10.sp, color = LossRed)
                            Text(String.format(java.util.Locale.getDefault(), "High: %,.2f", highVal), fontSize = 10.sp, color = ProfitGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(DarkCardSecondary)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(currentRatio)
                                    .background(if (change >= 0) ProfitGreen else LossRed)
                            )
                        }
                    } else {
                        Text("Day range: N/A", fontSize = 11.sp, color = TextGray)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DATA UNAVAILABLE — Real provider data required", fontSize = 12.sp, color = TextGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Historical Performance", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (candleList.isNotEmpty()) {
                    com.example.ui.components.CandlestickChart(
                        candles = candleList,
                        currentPrice = ltp.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "CHART DATA UNAVAILABLE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun HistoricalRow(label: String, low: Double, high: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("$label High", fontSize = 11.sp, color = TextGray)
            Text(String.format("%,.2f", high), fontSize = 12.sp, color = ProfitGreen, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$label Low", fontSize = 11.sp, color = TextGray)
            Text(String.format("%,.2f", low), fontSize = 12.sp, color = LossRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OptionChainTabContent(
    viewModel: MainViewModel,
    exchange: String,
    indexName: String,
    underlyingLtp: Double = 0.0,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit
) {
    val availableExpiries by viewModel.availableOptionExpiries.collectAsStateWithLifecycle()
    val selectedExpiry by viewModel.selectedOptionExpiry.collectAsStateWithLifecycle()
    val strikes by viewModel.optionStrikes.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val expiries = remember(indexName, availableExpiries) {
        if (availableExpiries.isNotEmpty()) availableExpiries
        else emptyList()
    }

    LaunchedEffect(expiries, selectedExpiry) {
        if (expiries.isNotEmpty() && !expiries.contains(selectedExpiry)) {
            viewModel.setSelectedOptionExpiry(expiries.first())
        }
    }

    var isScalpMode by remember { mutableStateOf(true) }
    var scalpLotMultiplier by remember { mutableStateOf(1) }
    val lotSize = com.example.util.AppPreferences.getGlobalLotSize(indexName)
    val closestAtmStrikePrice = remember(strikes, underlyingLtp) {
        if (strikes.isEmpty()) 0.0
        else strikes.minByOrNull { kotlin.math.abs(it.strikePrice - underlyingLtp) }?.strikePrice ?: 0.0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Expiry Selector
        if (expiries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "EXPIRY DATA UNAVAILABLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                expiries.forEach { expiry ->
                    val isSelected = expiry == selectedExpiry
                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { viewModel.setSelectedOptionExpiry(expiry) },
                        color = if (isSelected) PrimaryGold else DarkCard,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryGold else DarkCardBorder)
                    ) {
                        Text(
                            text = expiry,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Scalp Mode Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = "Scalp Mode",
                    tint = if (isScalpMode) ProfitGreen else TextGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("SCALP MODE (One-Tap MKT Order)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isScalpMode) ProfitGreen else TextGray)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isScalpMode,
                    onCheckedChange = { isScalpMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = ProfitGreen,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = DarkCardSecondary
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
            if (isScalpMode) {
                Row(
                    modifier = Modifier.background(DarkCardSecondary, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Qty:", fontSize = 10.sp, color = TextGray)
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(1, 2, 5).forEach { mult ->
                        val selected = scalpLotMultiplier == mult
                        Text(
                            "${mult * lotSize}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.Black else TextWhite,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .background(if (selected) PrimaryGold else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { scalpLotMultiplier = mult }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        var selectedStrikeType by remember { mutableStateOf("ALL") }
        
        // Strike Type Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("STRIKE TYPE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray)
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier.background(DarkCardSecondary, RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ALL", "ATM", "ITM", "OTM").forEach { type ->
                    val isSelected = selectedStrikeType == type
                    Box(
                        modifier = Modifier
                            .clickable { selectedStrikeType = type }
                            .background(
                                if (isSelected) PrimaryGold else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = type,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCardSecondary)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CALLS", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
            Text("STRIKE", modifier = Modifier.weight(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryGold, textAlign = TextAlign.Center)
            Text("PUTS", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray, textAlign = TextAlign.End)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("OI", fontSize = 9.sp, color = TextGray)
                Text("LTP", fontSize = 9.sp, color = TextGray)
            }
            Text("PCR", modifier = Modifier.weight(0.5f), fontSize = 9.sp, color = TextGray, textAlign = TextAlign.Center)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LTP", fontSize = 9.sp, color = TextGray)
                Text("OI", fontSize = 9.sp, color = TextGray)
            }
        }

        // Table Content
        val displayStrikes = remember(strikes, selectedStrikeType, closestAtmStrikePrice) {
            when (selectedStrikeType) {
                "ATM" -> strikes.sortedBy { kotlin.math.abs(it.strikePrice - closestAtmStrikePrice) }.take(10).sortedBy { it.strikePrice }
                else -> strikes
            }
        }
        
        if (displayStrikes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "CONNECT BROKER TO VIEW",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252) // Red color for unavailable
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Please configure Upstox, Fyers or Angel One API Key in Profile Settings to unlock live option chain.",
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(displayStrikes) { strike ->
                    val isAtm = strike.isAtm || (closestAtmStrikePrice > 0.0 && strike.strikePrice == closestAtmStrikePrice)
                    val callItm = strike.strikePrice < closestAtmStrikePrice
                    val putItm = strike.strikePrice > closestAtmStrikePrice
                    val itmBgColor = Color(0xFF2B2A26) // Faint yellow-tinted dark gray for ITM
                    
                    val showCall = when (selectedStrikeType) {
                        "ITM" -> callItm
                        "OTM" -> !callItm && !isAtm
                        else -> true
                    }
                    val showPut = when (selectedStrikeType) {
                        "ITM" -> putItm
                        "OTM" -> !putItm && !isAtm
                        else -> true
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isAtm) PrimaryGold.copy(alpha = 0.15f) else Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // CALLS
                        if (showCall) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                .background(if (callItm && !isAtm) itmBgColor else Color.Transparent)
                                .clickable {
                                    val symbol = strike.callSymbol.ifBlank { "$indexName ${strike.strikePrice.toInt()} CE" }
                                    val optExchange = when {
                                        indexName.contains("SENSEX", ignoreCase = true) || indexName.contains("BANKEX", ignoreCase = true) -> "BFO"
                                        indexName.contains("CRUDE", ignoreCase = true) -> "MCX"
                                        else -> "NFO"
                                    }
                                    if (isScalpMode) {
                                        viewModel.placeNewOrder(
                                            symbol = symbol,
                                            exchange = optExchange,
                                            side = "BUY",
                                            orderType = "MARKET",
                                            qty = lotSize * scalpLotMultiplier,
                                            price = strike.callLtp
                                        )
                                    } else {
                                        onOpenOrderDialog(symbol, "BUY", strike.callLtp, lotSize)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strike.callOi, fontSize = 11.sp, color = TextWhite)
                                Text(strike.callChgOi, fontSize = 9.sp, color = ProfitGreen)
                                // Minimal OI Bar
                                val oiVal = strike.callOi.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: 0f
                                if (oiVal > 0) {
                                    Box(modifier = Modifier.padding(top = 2.dp).height(2.dp).fillMaxWidth((oiVal / 1000000f).coerceIn(0f, 1f)).background(ProfitGreen.copy(alpha = 0.5f)))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isScalpMode) {
                                        Box(modifier = Modifier.background(ProfitGreen, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                            Text("BUY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(if (strike.callLtp > 0.0) String.format("%,.2f", strike.callLtp) else "--", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                }
                                Text(if (strike.callIv != null) "IV: ${String.format("%.1f", strike.callIv)}" else "IV: --", fontSize = 9.sp, color = TextGray)
                            }
                        }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // STRIKE
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                String.format("%,.0f", strike.strikePrice),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isAtm) PrimaryGold else TextWhite
                            )
                            val pcr = if ((strike.callOi.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 1.0) > 0) 
                                        (strike.putOi.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0) / (strike.callOi.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 1.0) 
                                      else 0.0
                            Text("PCR: ${String.format("%.2f", pcr)}", fontSize = 8.sp, color = TextGray)
                        }

                        // PUTS
                        if (showPut) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (putItm && !isAtm) itmBgColor else Color.Transparent)
                                    .clickable {
                                        val symbol = strike.putSymbol.ifBlank { "$indexName ${strike.strikePrice.toInt()} PE" }
                                        val optExchange = when {
                                            indexName.contains("SENSEX", ignoreCase = true) || indexName.contains("BANKEX", ignoreCase = true) -> "BFO"
                                            indexName.contains("CRUDE", ignoreCase = true) -> "MCX"
                                            else -> "NFO"
                                        }
                                        if (isScalpMode) {
                                            viewModel.placeNewOrder(
                                                symbol = symbol,
                                                exchange = optExchange,
                                                side = "BUY",
                                                orderType = "MARKET",
                                                qty = lotSize * scalpLotMultiplier,
                                                price = strike.putLtp
                                            )
                                        } else {
                                            onOpenOrderDialog(symbol, "BUY", strike.putLtp, lotSize)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (strike.putLtp > 0.0) String.format("%,.2f", strike.putLtp) else "--", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                        if (isScalpMode) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(modifier = Modifier.background(ProfitGreen, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                                Text("BUY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                    Text(if (strike.putIv != null) "IV: ${String.format("%.1f", strike.putIv)}" else "IV: --", fontSize = 9.sp, color = TextGray)
                                }
                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                    Text(strike.putOi, fontSize = 11.sp, color = TextWhite)
                                    Text(strike.putChgOi, fontSize = 9.sp, color = ProfitGreen)
                                    // Minimal OI Bar
                                    val oiVal = strike.putOi.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: 0f
                                    if (oiVal > 0) {
                                        Box(modifier = Modifier.padding(top = 2.dp).height(2.dp).fillMaxWidth((oiVal / 1000000f).coerceIn(0f, 1f)).background(LossRed.copy(alpha = 0.5f)).align(Alignment.End))
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun HistoricalDataTabContent(
    viewModel: MainViewModel,
    indexName: String,
    ltp: Double
) {
    var selectedInterval by remember { mutableStateOf("15m") }
    var candleList by remember { mutableStateOf<List<com.example.ui.components.CandleData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(indexName, selectedInterval) {
        isLoading = true
        viewModel.getHistoricalCandlesForIndex(indexName, selectedInterval) { fetched ->
            candleList = fetched
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Interval Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1m", "5m", "15m", "1h", "1d").forEach { interval ->
                val isSelected = interval == selectedInterval
                Surface(
                    modifier = Modifier.clickable { selectedInterval = interval },
                    color = if (isSelected) PrimaryGold else DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryGold else DarkCardBorder)
                ) {
                    Text(
                        text = interval.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else TextWhite,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chart Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$indexName - $selectedInterval Candlestick Chart", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryGold, strokeWidth = 2.dp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (candleList.isNotEmpty()) {
                    com.example.ui.components.TradingViewChart(
                        symbol = indexName,
                        modifier = Modifier.fillMaxWidth().height(350.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isLoading) "Loading historical candles..." else "CONNECT BROKER TO VIEW CHART",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLoading) TextGray else Color(0xFFFF5252)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Candle History Table
        Text("Historical Candle Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Time", fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1f))
                    Text("Open", fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1f))
                    Text("High", fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1f))
                    Text("Low", fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1f))
                    Text("Close", fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                if (candleList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No historical records", fontSize = 11.sp, color = TextGray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(candleList.reversed()) { idx, candle ->
                            val isGreen = candle.close >= candle.open
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("#${idx + 1}", fontSize = 10.sp, color = TextWhite, modifier = Modifier.weight(1f))
                                Text(String.format("%.1f", candle.open), fontSize = 10.sp, color = TextGray, modifier = Modifier.weight(1f))
                                Text(String.format("%.1f", candle.high), fontSize = 10.sp, color = ProfitGreen, modifier = Modifier.weight(1f))
                                Text(String.format("%.1f", candle.low), fontSize = 10.sp, color = LossRed, modifier = Modifier.weight(1f))
                                Text(String.format("%.1f", candle.close), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isGreen) ProfitGreen else LossRed, modifier = Modifier.weight(1f))
                            }
                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
