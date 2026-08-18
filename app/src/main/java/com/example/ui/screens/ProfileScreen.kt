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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.AppPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    userProfile: UserProfileEntity,
    orders: List<OrderEntity> = emptyList(),
    holdings: List<PortfolioHoldingEntity> = emptyList(),
    appPreferences: AppPreferences,
    onSwitchBroker: (String) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onNavigateToTelegramSettings: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onOpenNotificationCenter: () -> Unit = {},
    onLogout: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    var showAccountOverviewDialog by remember { mutableStateOf(false) }
    var showAlertPrefDialog by remember { mutableStateOf(false) }
    var showRiskDialog by remember { mutableStateOf(false) }
    var showOrderPrefDialog by remember { mutableStateOf(false) }
    var showLotSizeDialog by remember { mutableStateOf(false) }
    var showAiSignalDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showNotifPrefDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    var isTelegramEnabled by remember { mutableStateOf(appPreferences.isTelegramEnabled()) }

    // Stat calculations from real broker data
    val isBrokerConnected = (userProfile.connectedBroker == "Dhan" && userProfile.isDhanConnected) ||
                            (userProfile.connectedBroker == "Angel One" && userProfile.isAngelConnected) ||
                            (userProfile.connectedBroker == "m.Stock")

    val completedOrders = remember(orders) {
        orders.filter { order ->
            order.status.equals("COMPLETE", ignoreCase = true) ||
            order.status.equals("EXECUTED", ignoreCase = true) ||
            order.status.equals("TRADED", ignoreCase = true) ||
            order.status.equals("CLOSED", ignoreCase = true)
        }
    }

    val totalOrdersCount = orders.size
    val totalOrdersStr = if (isBrokerConnected) "$totalOrdersCount" else "--"

    val totalTradesCount = completedOrders.size
    val totalTradesStr = if (isBrokerConnected) "$totalTradesCount" else "--"

    val realizedPnlVal: Double = if (orders.isNotEmpty()) {
        orders.sumOf { it.realizedPnl }
    } else {
        userProfile.realizedPnl
    }

    val unrealizedPnlVal: Double = if (holdings.isNotEmpty()) {
        holdings.sumOf { it.unrealizedPnl }
    } else {
        userProfile.unrealizedPnl
    }

    val winningTradesCount = completedOrders.count { it.realizedPnl > 0 || (it.exitPrice > it.price && it.side == "BUY") }
    val losingTradesCount = completedOrders.count { it.realizedPnl < 0 || (it.exitPrice < it.price && it.side == "BUY" && it.exitPrice > 0) }
    val totalEvaluatedTrades = winningTradesCount + losingTradesCount

    val winRateStr = if (isBrokerConnected && totalEvaluatedTrades > 0) {
        val rate = (winningTradesCount.toDouble() / totalEvaluatedTrades) * 100
        String.format(Locale.getDefault(), "%.2f%%", rate)
    } else {
        "--"
    }

    val todaysPnlVal = realizedPnlVal + unrealizedPnlVal
    val todaysPnlStr = if (isBrokerConnected) String.format(Locale.getDefault(), "₹%,.2f", todaysPnlVal) else "Account data unavailable"
    val todaysPnlColor = if (!isBrokerConnected) TextWhite else if (todaysPnlVal >= 0) ProfitGreen else LossRed

    val positionsCount = holdings.filter { !it.type.equals("EQUITY", ignoreCase = true) && !it.type.equals("CNC", ignoreCase = true) }.size
    val holdingsCount = holdings.filter { it.type.equals("EQUITY", ignoreCase = true) || it.type.equals("CNC", ignoreCase = true) }.size

    val positionsStr = if (isBrokerConnected) "$positionsCount Active" else "--"
    val holdingsStr = if (isBrokerConnected) "$holdingsCount Assets" else "--"

    val availableBalanceStr = if (isBrokerConnected) String.format(Locale.getDefault(), "₹%,.2f", userProfile.availableMargin) else "Account data unavailable"
    val realizedPnlStr = if (isBrokerConnected) String.format(Locale.getDefault(), "₹%,.2f", realizedPnlVal) else "Account data unavailable"
    val unrealizedPnlStr = if (isBrokerConnected) String.format(Locale.getDefault(), "₹%,.2f", unrealizedPnlVal) else "Account data unavailable"

    val realizedPnlColor = if (!isBrokerConnected) TextWhite else if (realizedPnlVal >= 0) ProfitGreen else LossRed
    val unrealizedPnlColor = if (!isBrokerConnected) TextWhite else if (unrealizedPnlVal >= 0) ProfitGreen else LossRed

    val accountHolderName = remember(userProfile.name, userProfile.dhanClientId, userProfile.angelClientId, userProfile.connectedBroker, isBrokerConnected) {
        if (!isBrokerConnected) {
            "Not Connected"
        } else if (userProfile.name.isNotBlank()) {
            userProfile.name
        } else {
            "Trader"
        }
    }

    val activeBrokerName = if (isBrokerConnected) userProfile.connectedBroker else "--"

    val currentTimeStr = remember {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    PullToRefreshLayout(onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownLogo(size = 32.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("KING KHAN AI TRADE", fontSize = 15.sp, fontWeight = FontWeight.Black, color = TextWhite)
                        Text("Trade Like a King 👑", fontSize = 10.sp, color = SecondaryGold)
                    }
                }

                IconButton(onClick = onOpenNotificationCenter) {
                    Box {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = SecondaryGold)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(ProfitGreen, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Profile & Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)

            Spacer(modifier = Modifier.height(12.dp))

            // 2. ACCOUNT OVERVIEW Card
            GoldCard(
                borderColor = PrimaryGold,
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAccountOverviewDialog = true }
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
                                    .background(DarkCardSecondary, CircleShape)
                                    .border(1.dp, PrimaryGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ACCOUNT OVERVIEW", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryGold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Account Holder", fontSize = 10.sp, color = TextGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(accountHolderName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Active Broker", fontSize = 10.sp, color = TextGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(if (isBrokerConnected) ProfitGreen else TextGray, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(activeBrokerName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3x3 Stat Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Available Balance", fontSize = 10.sp, color = TextGray)
                            Text(availableBalanceStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (isBrokerConnected) ProfitGreen else TextWhite)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Realized P&L", fontSize = 10.sp, color = TextGray)
                            Text(realizedPnlStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = realizedPnlColor)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unrealized P&L", fontSize = 10.sp, color = TextGray)
                            Text(unrealizedPnlStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = unrealizedPnlColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Today's P&L", fontSize = 10.sp, color = TextGray)
                            Text(todaysPnlStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = todaysPnlColor)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Orders", fontSize = 10.sp, color = TextGray)
                            Text(totalOrdersStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Trades", fontSize = 10.sp, color = TextGray)
                            Text(totalTradesStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Win Rate", fontSize = 10.sp, color = TextGray)
                            Text(winRateStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (winRateStr != "--") ProfitGreen else TextWhite)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Positions", fontSize = 10.sp, color = TextGray)
                            Text(positionsStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Holdings", fontSize = 10.sp, color = TextGray)
                            Text(holdingsStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isBrokerConnected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(ProfitGreen, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live data • Last updated: $currentTimeStr", fontSize = 10.sp, color = TextGray)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(TextGray, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disconnected • Connect a broker to sync real data", fontSize = 10.sp, color = TextGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. BROKER CONNECTIONS Card
            GoldCard(borderColor = DarkCardBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BROKER CONNECTIONS", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Roles Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ACTIVE ORDER BROKER", fontSize = 9.sp, color = TextGray)
                        Text("🟢 Dhan (Primary Execution)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ACTIVE MARKET DATA", fontSize = 9.sp, color = TextGray)
                        Text("🟢 Angel One (Primary Data)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Dhan Row
                val isDhanConnected = userProfile.isDhanConnected || userProfile.connectedBroker == "Dhan"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dhan_logo),
                            contentDescription = "Dhan Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Dhan • Primary Execution", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(if (isDhanConnected) "🟢 Connected" else "🔴 Disconnected", fontSize = 11.sp, color = if (isDhanConnected) ProfitGreen else LossRed)
                        }
                    }

                    if (isDhanConnected) {
                        OutlinedButton(
                            onClick = { onSwitchBroker("Dhan") },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("DISCONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                    } else {
                        Button(
                            onClick = { onSwitchBroker("Dhan") },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text("CONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Angel One Row
                val isAngelConnected = userProfile.isAngelConnected || (userProfile.connectedBroker == "Angel One" && isBrokerConnected)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_angel_one_logo),
                            contentDescription = "Angel One Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Angel One • Primary Data Provider", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(if (isAngelConnected) "🟢 Connected" else "🔴 Disconnected", fontSize = 11.sp, color = if (isAngelConnected) ProfitGreen else LossRed)
                        }
                    }

                    if (isAngelConnected) {
                        OutlinedButton(
                            onClick = { onSwitchBroker("Angel One") },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("DISCONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                    } else {
                        Button(
                            onClick = { onSwitchBroker("Angel One") },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text("CONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                // 3. m.Stock Row (Secondary Market Data Fallback)
                val isMStockConnected = userProfile.connectedBroker == "m.Stock" || isBrokerConnected // Just proxy for now if any is connected since it's hard to fetch specific mstock pref sync here, we will just use the connectedBroker status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).background(DarkCardSecondary, CircleShape).border(1.dp, PrimaryGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("M", fontWeight = FontWeight.Black, color = PrimaryGold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("m.Stock • Secondary Data Fallback", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(if (userProfile.connectedBroker == "m.Stock" || userProfile.name == "m.Stock User") "🟢 Connected" else "🔴 Disconnected", fontSize = 11.sp, color = if (userProfile.connectedBroker == "m.Stock" || userProfile.name == "m.Stock User") ProfitGreen else LossRed)
                        }
                    }
                    if (userProfile.connectedBroker == "m.Stock" || userProfile.name == "m.Stock User") {
                        TextButton(
                            onClick = { onSwitchBroker("m.Stock") },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text("DISCONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LossRed)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSwitchBroker("m.Stock") },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("CONFIGURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. TradeSmart Row (Tertiary Market Data Fallback)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).background(DarkCardSecondary, CircleShape).border(1.dp, PrimaryGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", fontWeight = FontWeight.Black, color = PrimaryGold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("TradeSmart • Tertiary Data Fallback", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("🔴 Disconnected", fontSize = 11.sp, color = LossRed)
                        }
                    }

                    OutlinedButton(
                        onClick = { onSwitchBroker("TradeSmart") },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("CONFIGURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Warning Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Angel One is primary market data source. Dhan handles all order routing.", fontSize = 10.sp, color = TextGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. TELEGRAM ALERTS Card
            GoldCard(borderColor = DarkCardBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TELEGRAM ALERTS", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enabled", fontSize = 11.sp, color = TextGray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isTelegramEnabled,
                            onCheckedChange = {
                                isTelegramEnabled = it
                                appPreferences.setTelegramEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ProfitGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2x2 Status Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPillCard(
                        iconEmoji = "🤖",
                        title = "Bot Status",
                        value = "Connected",
                        isSuccess = true,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPillCard(
                        iconEmoji = "💬",
                        title = "Chat Status",
                        value = "Connected",
                        isSuccess = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPillCard(
                        iconEmoji = "⏱️",
                        title = "Last Test",
                        value = "2 min ago",
                        isSuccess = false,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPillCard(
                        iconEmoji = "🔔",
                        title = "Notifications",
                        value = "Active",
                        isSuccess = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Telegram Settings Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTelegramSettings() }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Telegram Alerts & Bot Integration", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Configure Bot Token, Chat ID & Test", fontSize = 10.sp, color = TextGray)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryGold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Telegram Button
                OutlinedButton(
                    onClick = onNavigateToTelegramSettings,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TEST TELEGRAM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onNavigateToDiagnostics,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RUN LIVE DATA TEST (DIAGNOSTICS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. App Settings & Preferences Menu
            Text("SETTINGS & PREFERENCES", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
            Spacer(modifier = Modifier.height(8.dp))

            GoldCard(borderColor = DarkCardBorder) {
                ProfileMenuItemRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "ALERT PREFERENCES",
                    onClick = { showAlertPrefDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.Shield,
                    title = "RISK MANAGEMENT",
                    onClick = { showRiskDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.Settings,
                    title = "ORDER PREFERENCES",
                    onClick = { showOrderPrefDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.FormatListNumbered,
                    title = "LOT SIZE SETTINGS",
                    onClick = { showLotSizeDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI SIGNAL SETTINGS",
                    onClick = { showAiSignalDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.Lock,
                    title = "SECURITY",
                    onClick = { showSecurityDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.Notifications,
                    title = "NOTIFICATIONS",
                    onClick = { showNotifPrefDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.Assessment,
                    title = "PERFORMANCE & REPORTS",
                    onClick = { showReportDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.Gavel,
                    title = "TERMS & DISCLAIMER",
                    onClick = { showTermsDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.Info,
                    title = "ABOUT KING KHAN AI TRADE",
                    onClick = { showAboutDialog = true }
                )
                HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)

                ProfileMenuItemRow(
                    icon = Icons.Default.SystemUpdate,
                    title = "CHECK FOR UPDATES",
                    onClick = { showUpdateDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LossRed)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOG OUT OF ACCOUNT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Dialog Triggers
        if (showAccountOverviewDialog || showReportDialog) {
            PerformanceReportDialog(
                orders = orders,
                holdings = holdings,
                onDismiss = {
                    showAccountOverviewDialog = false
                    showReportDialog = false
                }
            )
        }

        if (showAlertPrefDialog) {
            AlertPreferencesDialog(
                appPreferences = appPreferences,
                onDismiss = { showAlertPrefDialog = false }
            )
        }

        if (showRiskDialog) {
            RiskManagementDialog(
                appPreferences = appPreferences,
                onDismiss = { showRiskDialog = false },
                onSave = { showRiskDialog = false }
            )
        }

        if (showOrderPrefDialog) {
            OrderPreferencesDialog(
                appPreferences = appPreferences,
                onDismiss = { showOrderPrefDialog = false },
                onSave = { showOrderPrefDialog = false }
            )
        }

        if (showLotSizeDialog) {
            LotSizeSettingsDialog(
                appPreferences = appPreferences,
                onDismiss = { showLotSizeDialog = false },
                onSave = { showLotSizeDialog = false }
            )
        }

        if (showAiSignalDialog) {
            AiSignalSettingsDialog(
                appPreferences = appPreferences,
                onDismiss = { showAiSignalDialog = false }
            )
        }

        if (showSecurityDialog) {
            SecuritySettingsDialog(
                appPreferences = appPreferences,
                onDismiss = { showSecurityDialog = false },
                onSecurityUpdated = { showSecurityDialog = false }
            )
        }

        if (showNotifPrefDialog) {
            NotificationSettingsDialog(
                appPreferences = appPreferences,
                onDismiss = { showNotifPrefDialog = false },
                onSave = { showNotifPrefDialog = false }
            )
        }

        if (showTermsDialog) {
            TermsAndPolicyDialog(
                onDismiss = { showTermsDialog = false }
            )
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }

        if (showUpdateDialog) {
            AppUpdateDialog(
                appPreferences = appPreferences,
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}

@Composable
private fun StatusPillCard(
    iconEmoji: String,
    title: String,
    value: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DarkCardSecondary,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(iconEmoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(title, fontSize = 9.sp, color = TextGray)
                Text(
                    value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) ProfitGreen else TextWhite
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(20.dp))
    }
}
