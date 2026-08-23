package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CrownLogo
import com.example.ui.components.GoldButton
import com.example.ui.components.GoldCard
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.theme.*

@Composable
fun OrdersScreen(
    userProfile: UserProfileEntity = UserProfileEntity(),
    orders: List<OrderEntity>,
    positions: List<PortfolioHoldingEntity> = emptyList(),
    watchlist: List<WatchlistItem> = emptyList(),
    notifications: List<com.example.data.model.NotificationEntity> = emptyList(),
    availableMargin: Double,
    onOpenNotificationCenter: () -> Unit = {},
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit,
    onCancelOrder: ((orderId: String) -> Unit)? = null,
    onModifyOrder: ((orderId: String, newPrice: Double, newQty: Int, orderType: String, stopLoss: Double, target: Double) -> Unit)? = null,
    onExitPosition: ((orderId: String, exitPrice: Double, realizedPnl: Double) -> Unit)? = null,
    onPartialExitPosition: ((orderId: String, exitLots: Int, exitPrice: Double, partialPnl: Double) -> Unit)? = null,
    onUpdateStopLossTarget: ((orderId: String, newSl: Double, newTarget: Double) -> Unit)? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val isBrokerConnected = (userProfile.isAngelConnected || userProfile.isDhanConnected) && userProfile.connectedBroker.isNotBlank()
    val activeBrokerName = if (isBrokerConnected) userProfile.connectedBroker else "Local State"

    // Top Mode Switcher: "ORDERS" or "POSITIONS"
    var selectedMainMode by remember { mutableStateOf("POSITIONS") }

    // Orders Filter States
    var selectedOrderStatusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, OPEN, EXECUTED, CANCELLED, REJECTED
    var searchQuery by remember { mutableStateOf("") }
    var selectedExchangeFilter by remember { mutableStateOf("ALL") } // ALL, NSE, BSE, MCX
    var selectedSideFilter by remember { mutableStateOf("ALL") } // ALL, BUY, SELL
    var selectedSortOrder by remember { mutableStateOf("NEWEST") } // NEWEST, OLDEST, PRICE_HIGH, VALUE_HIGH
    var showFilterDialog by remember { mutableStateOf(false) }

    // Positions Sub-Tab: "OPEN" or "CLOSED"
    var selectedPositionsSubTab by remember { mutableStateOf("OPEN") }

    // Dialog state for full order modification
    var selectedOrderForModify by remember { mutableStateOf<OrderEntity?>(null) }
    // Dialog state for Exit / Square Off confirmation
    var selectedOrderForSquareOff by remember { mutableStateOf<Pair<OrderEntity, Double>?>(null) }
    // Dialog state for Partial Exit
    var selectedOrderForPartialExit by remember { mutableStateOf<Pair<OrderEntity, Double>?>(null) }

    // Derived Orders list with status, search, filter, and sort applied
    val filteredOrders = remember(orders, selectedOrderStatusFilter, searchQuery, selectedExchangeFilter, selectedSideFilter, selectedSortOrder) {
        val uniqueOrders = orders.distinctBy { if (it.orderId.isNotBlank()) it.orderId else "${it.symbol}_${it.time}_${it.id}" }
        var result = uniqueOrders.filter { order ->
            when (selectedOrderStatusFilter) {
                "PENDING" -> order.status.equals("PENDING", ignoreCase = true)
                "OPEN" -> order.status.equals("OPEN", ignoreCase = true)
                "EXECUTED" -> order.status.equals("EXECUTED", ignoreCase = true) || order.status.equals("TRADED", ignoreCase = true)
                "CANCELLED" -> order.status.equals("CANCELLED", ignoreCase = true)
                "REJECTED" -> order.status.equals("REJECTED", ignoreCase = true)
                else -> true
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            result = result.filter {
                it.symbol.contains(q, ignoreCase = true) ||
                it.orderId.contains(q, ignoreCase = true) ||
                it.brokerOrderId.contains(q, ignoreCase = true) ||
                it.exchange.contains(q, ignoreCase = true)
            }
        }

        if (selectedExchangeFilter != "ALL") {
            result = result.filter { it.exchange.equals(selectedExchangeFilter, ignoreCase = true) }
        }

        if (selectedSideFilter != "ALL") {
            result = result.filter { it.side.equals(selectedSideFilter, ignoreCase = true) }
        }

        when (selectedSortOrder) {
            "OLDEST" -> result.sortedBy { it.id }
            "PRICE_HIGH" -> result.sortedByDescending { it.price }
            "VALUE_HIGH" -> result.sortedByDescending { it.value }
            else -> result.sortedByDescending { it.id }
        }
    }

    // Derived Positions lists from positions (PortfolioHoldingEntity) or fallback to orders
    val openHoldingPositions = remember(positions) {
        positions.filter { it.qty != 0 && !it.positionStatus.equals("CLOSED", ignoreCase = true) }
            .distinctBy { "${it.symbol}_${it.exchange}" }
    }
    val closedHoldingPositions = remember(positions) {
        positions.filter { it.qty == 0 || it.positionStatus.equals("CLOSED", ignoreCase = true) }
            .distinctBy { "${it.symbol}_${it.exchange}" }
    }

    val openPositions = remember(orders) {
        orders.distinctBy { if (it.orderId.isNotBlank()) it.orderId else "${it.symbol}_${it.time}_${it.id}" }
            .filter {
                (it.status.equals("EXECUTED", ignoreCase = true) || it.status.equals("OPEN", ignoreCase = true) || it.status.equals("TRADED", ignoreCase = true)) &&
                !it.status.equals("CLOSED", ignoreCase = true)
            }
    }
    val closedPositions = remember(orders) {
        orders.distinctBy { if (it.orderId.isNotBlank()) it.orderId else "${it.symbol}_${it.time}_${it.id}" }
            .filter { it.status.equals("CLOSED", ignoreCase = true) }
    }

    val useHoldings = positions.isNotEmpty()

    val unrealizedPnl = if (useHoldings) {
        openHoldingPositions.fold(0.0) { acc, item -> acc + item.unrealizedPnl }
    } else {
        openPositions.fold(0.0) { acc, order ->
            val liveLtp = calculateLiveLtp(order, watchlist)
            val isBuy = order.side.equals("BUY", ignoreCase = true)
            val diff = if (isBuy) (liveLtp - order.price) else (order.price - liveLtp)
            acc + (diff * order.qty * order.lotSize)
        }
    }

    val realizedPnl = if (useHoldings) {
        closedHoldingPositions.fold(0.0) { acc, item -> acc + item.realizedPnl } + openHoldingPositions.fold(0.0) { acc, item -> acc + item.realizedPnl }
    } else {
        closedPositions.fold(0.0) { acc, order -> acc + order.realizedPnl } + openPositions.fold(0.0) { acc, order -> acc + order.realizedPnl }
    }

    val todayPnl = unrealizedPnl + realizedPnl
    val overallPnl = todayPnl
    val mtmVal = unrealizedPnl

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
                Box(
                    modifier = Modifier
                        .background(if (isBrokerConnected) ProfitGreenBg else SecondaryGold.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(if (isBrokerConnected) ProfitGreen else SecondaryGold, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBrokerConnected) "LIVE API" else "LOCAL STATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isBrokerConnected) ProfitGreen else SecondaryGold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onOpenNotificationCenter) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = SecondaryGold)
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

        // Active Broker Banner / Disconnected Warning Screen
        if (!isBrokerConnected) {
            GoldCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                borderColor = LossRed.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(LossRedBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Broker Disconnected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                            Text("Operating in Local State. Connect Dhan for real orders.", fontSize = 9.sp, color = TextGray)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Available Margin", fontSize = 8.sp, color = TextGray)
                        Text(String.format("₹%,.2f", availableMargin), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }
                }
            }
        } else {
            GoldCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                borderColor = DarkCardBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(ProfitGreenBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Connected • $activeBrokerName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            Text("NSE • BSE • MCX Live Orders & Positions", fontSize = 9.sp, color = TextGray)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Available Margin", fontSize = 8.sp, color = TextGray)
                        Text(String.format("₹%,.2f", availableMargin), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Mode Switcher: "📋 ORDERS" vs "💼 POSITIONS"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { selectedMainMode = "POSITIONS" },
                color = if (selectedMainMode == "POSITIONS") PrimaryGold else DarkCard,
                border = if (selectedMainMode == "POSITIONS") null else androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        tint = if (selectedMainMode == "POSITIONS") Color.Black else SecondaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "POSITIONS (${openPositions.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMainMode == "POSITIONS") Color.Black else TextWhite
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { selectedMainMode = "ORDERS" },
                color = if (selectedMainMode == "ORDERS") PrimaryGold else DarkCard,
                border = if (selectedMainMode == "ORDERS") null else androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = if (selectedMainMode == "ORDERS") Color.Black else SecondaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ORDERS (${orders.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMainMode == "ORDERS") Color.Black else TextWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // CONTENT BLOCK 1: POSITIONS VIEW
        // ==========================================
        if (selectedMainMode == "POSITIONS") {
            // Summary Banner Metrics
            GoldCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                borderColor = DarkCardBorder
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Today's P&L", fontSize = 10.sp, color = TextGray)
                            val isPos = todayPnl >= 0
                            Text(
                                text = String.format("%s₹%,.2f", if (isPos) "+" else "", todayPnl),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isPos) ProfitGreen else LossRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Overall P&L", fontSize = 10.sp, color = TextGray)
                            val isOvPos = overallPnl >= 0
                            Text(
                                text = String.format("%s₹%,.2f", if (isOvPos) "+" else "", overallPnl),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOvPos) ProfitGreen else LossRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = DarkCardBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Unrealized MTM", fontSize = 9.sp, color = TextGray)
                            Text(
                                String.format("%s₹%,.2f", if (mtmVal >= 0) "+" else "", mtmVal),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mtmVal >= 0) ProfitGreen else LossRed
                            )
                        }

                        Column {
                            Text("Realized P&L", fontSize = 9.sp, color = TextGray)
                            Text(
                                String.format("%s₹%,.2f", if (realizedPnl >= 0) "+" else "", realizedPnl),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (realizedPnl >= 0) ProfitGreen else LossRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Open Positions", fontSize = 9.sp, color = TextGray)
                            Text("${openPositions.size} Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-tabs: Open Positions vs Closed Positions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val openCount = if (useHoldings) openHoldingPositions.size else openPositions.size
                val closedCount = if (useHoldings) closedHoldingPositions.size else closedPositions.size

                listOf("OPEN" to openCount, "CLOSED" to closedCount).forEach { (tab, count) ->
                    val isSel = selectedPositionsSubTab == tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedPositionsSubTab = tab },
                        color = if (isSel) DarkCardSecondary else DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) PrimaryGold else DarkCardBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (tab == "OPEN") "Open Positions ($count)" else "Closed Positions ($count)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) SecondaryGold else TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (useHoldings) {
                val holdingList = if (selectedPositionsSubTab == "OPEN") openHoldingPositions else closedHoldingPositions
                if (holdingList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(holdingList) { holding ->
                            HoldingPositionCardItem(
                                holding = holding,
                                liveLtp = holding.ltp,
                                isOpen = selectedPositionsSubTab == "OPEN",
                                onSquareOff = { },
                                onPartialExit = { }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = SecondaryGold.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (selectedPositionsSubTab == "OPEN") "No open positions currently active." else "No closed positions history today.",
                                fontSize = 13.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Real position data synchronized with broker.", fontSize = 10.sp, color = TextGray)
                        }
                    }
                }
            } else {
                val currentPositionsList = if (selectedPositionsSubTab == "OPEN") openPositions else closedPositions

                if (currentPositionsList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentPositionsList) { order ->
                            val liveLtp = calculateLiveLtp(order, watchlist)
                            PositionCardItem(
                                order = order,
                                liveLtp = liveLtp,
                                isOpen = selectedPositionsSubTab == "OPEN",
                                onSquareOff = { selectedOrderForSquareOff = order to liveLtp },
                                onPartialExit = { selectedOrderForPartialExit = order to liveLtp },
                                onOpenModifySlTarget = { selectedOrderForModify = order }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = SecondaryGold.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (selectedPositionsSubTab == "OPEN") "No open positions currently active." else "No closed positions history today.",
                                fontSize = 13.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Use BUY / SELL in Market or Option Chain to enter trades.", fontSize = 10.sp, color = TextGray)
                        }
                    }
                }
            }
        }

        // ==========================================
        // CONTENT BLOCK 2: ORDERS VIEW
        // ==========================================
        else {
            // Status Filter Chips (ALL, PENDING, OPEN, EXECUTED, CANCELLED, REJECTED)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statusList = listOf("ALL", "PENDING", "OPEN", "EXECUTED", "CANCELLED", "REJECTED")
                statusList.forEach { status ->
                    val isSelected = selectedOrderStatusFilter == status
                    val count = when (status) {
                        "ALL" -> orders.size
                        "PENDING" -> orders.count { it.status.equals("PENDING", ignoreCase = true) }
                        "OPEN" -> orders.count { it.status.equals("OPEN", ignoreCase = true) }
                        "EXECUTED" -> orders.count { it.status.equals("EXECUTED", ignoreCase = true) || it.status.equals("TRADED", ignoreCase = true) }
                        "CANCELLED" -> orders.count { it.status.equals("CANCELLED", ignoreCase = true) }
                        "REJECTED" -> orders.count { it.status.equals("REJECTED", ignoreCase = true) }
                        else -> 0
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedOrderStatusFilter = status },
                        label = {
                            Text(
                                "$status ($count)",
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGold,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkCard,
                            labelColor = TextGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = PrimaryGold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar & Filter Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Symbol, Order ID...", fontSize = 11.sp, color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp)) } }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(DarkCard, RoundedCornerShape(8.dp))
                        .border(1.dp, if (selectedExchangeFilter != "ALL" || selectedSideFilter != "ALL") PrimaryGold else DarkCardBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Filter & Sort",
                        tint = if (selectedExchangeFilter != "ALL" || selectedSideFilter != "ALL") SecondaryGold else TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredOrders.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOrders) { order ->
                        val liveLtp = calculateLiveLtp(order, watchlist)
                        DetailedOrderCardItem(
                            order = order,
                            liveLtp = liveLtp,
                            onCancelOrder = onCancelOrder,
                            onOpenModify = { selectedOrderForModify = it }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = SecondaryGold.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        if (orders.isEmpty()) {
                            Text(
                                text = "No Orders Found",
                                fontSize = 15.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No orders are available in your connected broker account.",
                                fontSize = 11.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "No Matching Orders",
                                fontSize = 14.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No orders match your filter criteria. Try clearing search or status filter.",
                                fontSize = 10.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }


    }
    }

    // Modal Dialog 1: Full Order Modify Dialog
    selectedOrderForModify?.let { order ->
        FullModifyOrderDialog(
            order = order,
            onDismiss = { selectedOrderForModify = null },
            onConfirm = { newPrice, newQty, newOrderType, newSl, newTarget ->
                onModifyOrder?.invoke(order.orderId, newPrice, newQty, newOrderType, newSl, newTarget)
                selectedOrderForModify = null
            }
        )
    }

    // Modal Dialog 2: Square Off Confirmation Dialog
    selectedOrderForSquareOff?.let { (order, currentLtp) ->
        val isBuy = order.side.equals("BUY", ignoreCase = true)
        val diff = if (isBuy) (currentLtp - order.price) else (order.price - currentLtp)
        val pnl = diff * order.qty * order.lotSize

        SquareOffConfirmDialog(
            order = order,
            currentLtp = currentLtp,
            pnl = pnl,
            onDismiss = { selectedOrderForSquareOff = null },
            onConfirmExit = {
                onExitPosition?.invoke(order.orderId, currentLtp, pnl)
                selectedOrderForSquareOff = null
            }
        )
    }

    // Modal Dialog 3: Partial Exit Dialog
    selectedOrderForPartialExit?.let { (order, currentLtp) ->
        PartialExitDialog(
            order = order,
            currentLtp = currentLtp,
            onDismiss = { selectedOrderForPartialExit = null },
            onConfirmPartialExit = { exitLots, partialPnl ->
                onPartialExitPosition?.invoke(order.orderId, exitLots, currentLtp, partialPnl)
                selectedOrderForPartialExit = null
            }
        )
    }

    // Modal Dialog 4: Filter & Sort Dialog
    if (showFilterDialog) {
        FilterAndSortDialog(
            currentExchange = selectedExchangeFilter,
            currentSide = selectedSideFilter,
            currentSort = selectedSortOrder,
            onDismiss = { showFilterDialog = false },
            onApply = { ex, side, sort ->
                selectedExchangeFilter = ex
                selectedSideFilter = side
                selectedSortOrder = sort
                showFilterDialog = false
            }
        )
    }
}

/**
 * Calculates live LTP for an order matching watchlist items
 */
private fun calculateLiveLtp(order: OrderEntity, watchlist: List<WatchlistItem>): Double {
    val match = watchlist.find {
        order.symbol.contains(it.symbol, ignoreCase = true) || it.symbol.contains(order.symbol, ignoreCase = true)
    }
    if (match != null && match.ltp > 0) return match.ltp
    return if (order.ltp > 0) order.ltp else order.price
}

/**
 * Position Item Card (Requirement 2)
 */
@Composable
private fun PositionCardItem(
    order: OrderEntity,
    liveLtp: Double,
    isOpen: Boolean,
    onSquareOff: () -> Unit,
    onPartialExit: () -> Unit,
    onOpenModifySlTarget: () -> Unit
) {
    val isBuy = order.side.equals("BUY", ignoreCase = true)
    val diff = if (isBuy) (liveLtp - order.price) else (order.price - liveLtp)
    val pnlAmount = if (isOpen) (diff * order.qty * order.lotSize) else order.realizedPnl
    val pnlPct = if (order.price > 0) (diff / order.price) * 100.0 else 0.0

    // Extract option type (CE/PE) and strike if available
    val optType = if (order.optionType.isNotBlank()) order.optionType else if (order.symbol.contains("PE")) "PE" else "CE"
    val strikeText = if (order.strike > 0) String.format("%.0f", order.strike) else ""

    GoldCard(borderColor = DarkCardBorder) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (optType == "CE") ProfitGreenBg else LossRedBg, CircleShape)
                        .border(1.dp, if (optType == "CE") ProfitGreen else LossRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (optType == "CE") ProfitGreen else LossRed
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(order.symbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(DarkCardSecondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(order.exchange, fontSize = 8.sp, color = TextGray)
                        }
                    }
                    val displayExpiry = order.expiry.ifEmpty { com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(order.symbol).firstOrNull() ?: "" }
                    Text("Expiry: $displayExpiry • Strike: $strikeText", fontSize = 9.sp, color = TextGray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(if (isBuy) ProfitGreenBg else LossRedBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(order.side, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isBuy) ProfitGreen else LossRed)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("Broker ID: ${order.brokerOrderId.ifEmpty { order.orderId }}", fontSize = 8.sp, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid Metrics: Net Qty, Day Qty, Avg Price, LTP, P&L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Net Quantity", fontSize = 8.sp, color = TextGray)
                Text("${order.qty} Lots (${order.qty * order.lotSize})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            Column {
                Text("Avg Price", fontSize = 8.sp, color = TextGray)
                Text(String.format("₹%,.2f", order.price), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            Column {
                Text(if (isOpen) "Live LTP" else "Exit Price", fontSize = 8.sp, color = TextGray)
                val displayPrice = if (isOpen) liveLtp else if (order.exitPrice > 0) order.exitPrice else order.price
                Text(String.format("₹%,.2f", displayPrice), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (isOpen) "Unrealized P&L" else "Realized P&L", fontSize = 8.sp, color = TextGray)
                val isPnlPos = pnlAmount >= 0
                Text(
                    text = String.format("%s₹%,.2f", if (isPnlPos) "+" else "", pnlAmount),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPnlPos) ProfitGreen else LossRed
                )
                if (isOpen) {
                    Text(
                        text = String.format("%s%.2f%%", if (pnlPct >= 0) "+" else "", pnlPct),
                        fontSize = 8.sp,
                        color = if (pnlPct >= 0) ProfitGreen else LossRed
                    )
                }
            }
        }

        // Action Buttons Bar for OPEN POSITIONS (Square Off & Partial Exit)
        if (isOpen) {
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = DarkCardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenModifySlTarget,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold)
                ) {
                    Text("MODIFY SL/TG", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                }

                if (order.qty > 1) {
                    OutlinedButton(
                        onClick = onPartialExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LossRed)
                    ) {
                        Text("PARTIAL EXIT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = LossRed)
                    }
                }

                Button(
                    onClick = onSquareOff,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("SQUARE OFF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Detailed Order Card Item (Requirement 1)
 */
@Composable
private fun DetailedOrderCardItem(
    order: OrderEntity,
    liveLtp: Double,
    onCancelOrder: ((String) -> Unit)?,
    onOpenModify: (OrderEntity) -> Unit
) {
    val isBuy = order.side.equals("BUY", ignoreCase = true)
    val isPendingOrOpen = order.status.equals("PENDING", ignoreCase = true) || order.status.equals("OPEN", ignoreCase = true)

    GoldCard(borderColor = DarkCardBorder) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(DarkCardSecondary, CircleShape)
                        .border(1.dp, PrimaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(order.symbol.take(1), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(order.symbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(DarkCardSecondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(order.exchange, fontSize = 8.sp, color = TextGray)
                        }
                    }
                    Text("ID: ${order.orderId} • Broker ID: ${order.brokerOrderId.ifEmpty { "N/A" }}", fontSize = 8.sp, color = TextGray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(if (isBuy) ProfitGreenBg else LossRedBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(order.side, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isBuy) ProfitGreen else LossRed)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                when (order.status.uppercase()) {
                                    "PENDING" -> SecondaryGold.copy(alpha = 0.2f)
                                    "EXECUTED", "TRADED" -> ProfitGreenBg
                                    "CLOSED" -> DarkCardSecondary
                                    else -> LossRedBg
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            order.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (order.status.uppercase()) {
                                "PENDING" -> SecondaryGold
                                "EXECUTED", "TRADED" -> ProfitGreen
                                "CLOSED" -> TextGray
                                else -> LossRed
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(order.time, fontSize = 8.sp, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid 1: Qty, Order Type, Product, Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Quantity (Lots)", fontSize = 8.sp, color = TextGray)
                Text("${order.qty} (${order.qty * order.lotSize})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            Column {
                Text("Order / Product", fontSize = 8.sp, color = TextGray)
                Text("${order.orderType} • ${order.productType}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            Column {
                Text("Limit Price", fontSize = 8.sp, color = TextGray)
                Text(String.format("₹%,.2f", order.price), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Avg Executed Price", fontSize = 8.sp, color = TextGray)
                val avgP = if (order.avgPrice > 0) order.avgPrice else order.price
                Text(String.format("₹%,.2f", avgP), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grid 2: Filled Qty, Remaining Qty, Stop Loss, Target
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Filled / Remaining", fontSize = 8.sp, color = TextGray)
                Text("${order.filledQty} / ${order.remainingQty}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextGray)
            }
            Column {
                Text("Stop Loss", fontSize = 8.sp, color = TextGray)
                Text(String.format("₹%,.2f", order.stopLoss), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LossRed)
            }
            Column {
                Text("Target", fontSize = 8.sp, color = TextGray)
                Text(String.format("₹%,.2f", order.target), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Order Value", fontSize = 8.sp, color = TextGray)
                Text(String.format("₹%,.2f", order.value), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }

        // Action Buttons Bar for Pending / Open Orders
        if (isPendingOrOpen) {
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = DarkCardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenModify(order) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold)
                ) {
                    Text("MODIFY ORDER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                }

                Button(
                    onClick = { onCancelOrder?.invoke(order.orderId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("CANCEL ORDER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Full Order Modify Dialog
 */
@Composable
private fun FullModifyOrderDialog(
    order: OrderEntity,
    onDismiss: () -> Unit,
    onConfirm: (newPrice: Double, newQty: Int, newOrderType: String, newSl: Double, newTarget: Double) -> Unit
) {
    var priceText by remember { mutableStateOf(order.price.toString()) }
    var qtyText by remember { mutableStateOf(order.qty.toString()) }
    var orderType by remember { mutableStateOf(order.orderType) }
    var slText by remember { mutableStateOf(order.stopLoss.toString()) }
    var targetText by remember { mutableStateOf(order.target.toString()) }

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
                    Text("Modify Live Order", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                    }
                }

                Text("Symbol: ${order.symbol} (${order.side}) • ID: ${order.orderId}", fontSize = 11.sp, color = SecondaryGold)
                Spacer(modifier = Modifier.height(14.dp))

                // Price Input
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Order Price (₹)", color = SecondaryGold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        cursorColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity Input
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantity (Lots)", color = SecondaryGold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        cursorColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stop Loss & Target
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = slText,
                        onValueChange = { slText = it },
                        label = { Text("Stop Loss (₹)", color = LossRed) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LossRed,
                            unfocusedBorderColor = DarkCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Target (₹)", color = ProfitGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfitGreen,
                            unfocusedBorderColor = DarkCardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                GoldButton(
                    text = "Confirm Order Modification",
                    onClick = {
                        val p = priceText.toDoubleOrNull() ?: order.price
                        val q = qtyText.toIntOrNull() ?: order.qty
                        val sl = slText.toDoubleOrNull() ?: order.stopLoss
                        val tg = targetText.toDoubleOrNull() ?: order.target
                        onConfirm(p, q, orderType, sl, tg)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Square Off Confirmation Dialog
 */
@Composable
private fun SquareOffConfirmDialog(
    order: OrderEntity,
    currentLtp: Double,
    pnl: Double,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
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
                Text("Square Off Position", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Exit entire position at market LTP?", fontSize = 11.sp, color = TextGray)

                Spacer(modifier = Modifier.height(12.dp))

                GoldCard(borderColor = DarkCardBorder) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(order.symbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(order.side, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (order.side == "BUY") ProfitGreen else LossRed)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Lots:", fontSize = 10.sp, color = TextGray)
                            Text("${order.qty} Lots (${order.qty * order.lotSize} Qty)", fontSize = 11.sp, color = TextWhite)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entry Price:", fontSize = 10.sp, color = TextGray)
                            Text("₹${order.price}", fontSize = 11.sp, color = TextWhite)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Market Exit LTP:", fontSize = 10.sp, color = TextGray)
                            Text("₹$currentLtp", fontSize = 11.sp, color = SecondaryGold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Realized P&L:", fontSize = 10.sp, color = TextGray)
                            Text(
                                String.format("%s₹%,.2f", if (pnl >= 0) "+" else "", pnl),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pnl >= 0) ProfitGreen else LossRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text("CANCEL", fontSize = 11.sp, color = TextGray)
                    }

                    Button(
                        onClick = onConfirmExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                    ) {
                        Text("SQUARE OFF NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Partial Exit Dialog (Requirement 2)
 */
@Composable
private fun PartialExitDialog(
    order: OrderEntity,
    currentLtp: Double,
    onDismiss: () -> Unit,
    onConfirmPartialExit: (exitLots: Int, partialPnl: Double) -> Unit
) {
    var exitLotsText by remember { mutableStateOf("1") }
    val exitLots = exitLotsText.toIntOrNull() ?: 1
    val isBuy = order.side.equals("BUY", ignoreCase = true)
    val diff = if (isBuy) (currentLtp - order.price) else (order.price - currentLtp)
    val partialPnl = diff * exitLots * order.lotSize

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
                Text("Partial Exit Position", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Select number of lots to exit out of ${order.qty} lots.", fontSize = 11.sp, color = TextGray)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = exitLotsText,
                    onValueChange = { input ->
                        val n = input.toIntOrNull() ?: 1
                        if (n in 1 until order.qty) {
                            exitLotsText = input
                        }
                    },
                    label = { Text("Exit Lots (Max ${order.qty - 1})", color = SecondaryGold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        cursorColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                GoldCard(borderColor = DarkCardBorder) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Exiting Quantity:", fontSize = 10.sp, color = TextGray)
                            Text("$exitLots Lots (${exitLots * order.lotSize} Qty)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Quantity:", fontSize = 10.sp, color = TextGray)
                            Text("${order.qty - exitLots} Lots", fontSize = 11.sp, color = SecondaryGold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Partial Realized P&L:", fontSize = 10.sp, color = TextGray)
                            Text(
                                String.format("%s₹%,.2f", if (partialPnl >= 0) "+" else "", partialPnl),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (partialPnl >= 0) ProfitGreen else LossRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text("CANCEL", fontSize = 11.sp, color = TextGray)
                    }

                    Button(
                        onClick = { onConfirmPartialExit(exitLots, partialPnl) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                    ) {
                        Text("EXIT $exitLots LOTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Filter and Sort Dialog
 */
@Composable
private fun FilterAndSortDialog(
    currentExchange: String,
    currentSide: String,
    currentSort: String,
    onDismiss: () -> Unit,
    onApply: (exchange: String, side: String, sort: String) -> Unit
) {
    var selectedEx by remember { mutableStateOf(currentExchange) }
    var selectedSide by remember { mutableStateOf(currentSide) }
    var selectedSort by remember { mutableStateOf(currentSort) }

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
                    Text("Filter & Sort Orders", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exchange Filter
                Text("Exchange", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "NSE", "BSE", "MCX").forEach { ex ->
                        val isSel = selectedEx == ex
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedEx = ex },
                            color = if (isSel) PrimaryGold else DarkCardSecondary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(ex, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Side Filter
                Text("Order Side", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "BUY", "SELL").forEach { side ->
                        val isSel = selectedSide == side
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedSide = side },
                            color = if (isSel) PrimaryGold else DarkCardSecondary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(side, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sorting Option
                Text("Sort By", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "NEWEST" to "Newest Order Time First",
                        "OLDEST" to "Oldest Order Time First",
                        "PRICE_HIGH" to "Highest Price First",
                        "VALUE_HIGH" to "Highest Value First"
                    ).forEach { (sortKey, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSort = sortKey }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSort == sortKey,
                                onClick = { selectedSort = sortKey },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontSize = 11.sp, color = TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                GoldButton(
                    text = "Apply Filters",
                    onClick = { onApply(selectedEx, selectedSide, selectedSort) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HoldingPositionCardItem(
    holding: PortfolioHoldingEntity,
    liveLtp: Double,
    isOpen: Boolean,
    onSquareOff: () -> Unit,
    onPartialExit: () -> Unit
) {
    val optType = if (holding.optionType.isNotBlank()) holding.optionType else if (holding.symbol.contains("PE")) "PE" else if (holding.symbol.contains("CE")) "CE" else "EQ"
    val strikeText = if (holding.strikePrice > 0) String.format("%.0f", holding.strikePrice) else ""
    val isClosed = holding.qty == 0 || holding.positionStatus.equals("CLOSED", ignoreCase = true)
    val pnlAmount = if (isClosed) holding.realizedPnl else holding.pnl

    GoldCard(borderColor = DarkCardBorder) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (optType == "CE") ProfitGreenBg else LossRedBg, CircleShape)
                        .border(1.dp, if (optType == "CE") ProfitGreen else LossRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (optType == "CE") ProfitGreen else LossRed
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(holding.symbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(DarkCardSecondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(holding.exchange, fontSize = 8.sp, color = TextGray)
                        }
                    }
                    val displayExpiry = holding.expiry.ifEmpty { com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(holding.symbol).firstOrNull() ?: "" }
                    Text("Expiry: $displayExpiry" + (if (strikeText.isNotBlank()) " • Strike: $strikeText" else ""), fontSize = 9.sp, color = TextGray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(if (isClosed) DarkCardSecondary else ProfitGreenBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (isClosed) "CLOSED" else "OPEN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isClosed) TextGray else ProfitGreen
                    )
                }
                if (holding.productType.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(holding.productType, fontSize = 8.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isClosed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("BUY", fontSize = 8.sp, color = TextGray)
                    Text("${holding.buyQty} @ ₹${String.format("%.2f", holding.buyAvg)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                Column {
                    Text("SELL", fontSize = 8.sp, color = TextGray)
                    Text("${holding.sellQty} @ ₹${String.format("%.2f", holding.sellAvg)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                Column {
                    Text("Net Qty", fontSize = 8.sp, color = TextGray)
                    Text("0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Realized P&L", fontSize = 8.sp, color = TextGray)
                    val isPos = pnlAmount >= 0
                    Text(
                        text = String.format("%s₹%,.2f", if (isPos) "+" else "", pnlAmount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPos) ProfitGreen else LossRed
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Net Quantity", fontSize = 8.sp, color = TextGray)
                    Text("${holding.qty}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                Column {
                    Text("Avg Price", fontSize = 8.sp, color = TextGray)
                    Text(String.format("₹%,.2f", holding.avgPrice), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                Column {
                    Text("Live LTP", fontSize = 8.sp, color = TextGray)
                    val ltpToDisplay = if (liveLtp > 0) liveLtp else holding.ltp
                    Text(String.format("₹%,.2f", ltpToDisplay), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("P&L", fontSize = 8.sp, color = TextGray)
                    val isPos = pnlAmount >= 0
                    Text(
                        text = String.format("%s₹%,.2f", if (isPos) "+" else "", pnlAmount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPos) ProfitGreen else LossRed
                    )
                }
            }
        }
    }
}
