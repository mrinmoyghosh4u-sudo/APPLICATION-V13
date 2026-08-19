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
    brokerStatuses: Map<String, com.example.data.network.BrokerConnectionState> = emptyMap(),
    onSwitchBroker: (String) -> Unit,
    onReconnectBroker: (String) -> Unit = {},
    onDisconnectBroker: (String) -> Unit = {},
    onRemoveAccountBroker: (String) -> Unit = {},
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
                    Text("BROKER CONNECTIONS & SESSIONS", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Roles Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ACTIVE ORDER BROKER", fontSize = 9.sp, color = TextGray)
                        val dhanSt = brokerStatuses["Dhan"]?.status?.name ?: if (userProfile.isDhanConnected) "CONNECTED" else "OFFLINE"
                        val dhanColor = when (dhanSt) {
                            "CONNECTED" -> ProfitGreen
                            "STANDBY" -> Color(0xFFFFD54F)
                            "AUTHENTICATION_REQUIRED" -> Color(0xFFFF9800)
                            else -> LossRed
                        }
                        Text("Dhan ($dhanSt)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = dhanColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PRIMARY MARKET DATA", fontSize = 9.sp, color = TextGray)
                        val angelSt = brokerStatuses["Angel One"]?.status?.name ?: if (userProfile.isAngelConnected) "CONNECTED" else "OFFLINE"
                        val angelColor = when (angelSt) {
                            "CONNECTED" -> ProfitGreen
                            "STANDBY" -> Color(0xFFFFD54F)
                            "AUTHENTICATION_REQUIRED" -> Color(0xFFFF9800)
                            else -> LossRed
                        }
                        Text("Angel One ($angelSt)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = angelColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Dhan Row
                val dhanInfo = brokerStatuses["Dhan"]
                val dhanStatus = dhanInfo?.status ?: if (userProfile.isDhanConnected) com.example.data.network.BrokerAuthStatus.CONNECTED else com.example.data.network.BrokerAuthStatus.OFFLINE
                BrokerStatusRow(
                    name = "Dhan",
                    subtitle = "Primary Order Execution",
                    logoRes = R.drawable.ic_dhan_logo,
                    status = dhanStatus,
                    lastRefreshTime = dhanInfo?.lastSyncTimestamp ?: 0L,
                    onConnect = { onSwitchBroker("Dhan") },
                    onReconnect = { onReconnectBroker("Dhan") },
                    onDisconnect = { onDisconnectBroker("Dhan") },
                    onRemoveAccount = { onRemoveAccountBroker("Dhan") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Angel One Row
                val angelInfo = brokerStatuses["Angel One"]
                val angelStatus = angelInfo?.status ?: if (userProfile.isAngelConnected) com.example.data.network.BrokerAuthStatus.CONNECTED else com.example.data.network.BrokerAuthStatus.OFFLINE
                BrokerStatusRow(
                    name = "Angel One",
                    subtitle = "Primary Market Data",
                    logoRes = R.drawable.ic_angel_one_logo,
                    status = angelStatus,
                    lastRefreshTime = angelInfo?.lastSyncTimestamp ?: 0L,
                    onConnect = { onSwitchBroker("Angel One") },
                    onReconnect = { onReconnectBroker("Angel One") },
                    onDisconnect = { onDisconnectBroker("Angel One") },
                    onRemoveAccount = { onRemoveAccountBroker("Angel One") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. m.Stock Row (Secondary Market Data Fallback)
                val mstockInfo = brokerStatuses["m.Stock"]
                val mstockStatus = mstockInfo?.status ?: com.example.data.network.BrokerAuthStatus.CONFIGURE
                BrokerStatusRow(
                    name = "m.Stock",
                    subtitle = "Secondary Market Data Fallback",
                    letter = "m",
                    status = mstockStatus,
                    lastRefreshTime = mstockInfo?.lastSyncTimestamp ?: 0L,
                    onConnect = { onSwitchBroker("m.Stock") },
                    onReconnect = { onReconnectBroker("m.Stock") },
                    onDisconnect = { onDisconnectBroker("m.Stock") },
                    onRemoveAccount = { onRemoveAccountBroker("m.Stock") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. TradeSmart Row (Tertiary Market Data Fallback)
                val tsInfo = brokerStatuses["TradeSmart"]
                val tsStatus = tsInfo?.status ?: com.example.data.network.BrokerAuthStatus.CONFIGURE
                BrokerStatusRow(
                    name = "TradeSmart",
                    subtitle = "Tertiary Market Data Fallback",
                    letter = "T",
                    status = tsStatus,
                    lastRefreshTime = tsInfo?.lastSyncTimestamp ?: 0L,
                    onConnect = { onSwitchBroker("TradeSmart") },
                    onReconnect = { onReconnectBroker("TradeSmart") },
                    onDisconnect = { onDisconnectBroker("TradeSmart") },
                    onRemoveAccount = { onRemoveAccountBroker("TradeSmart") }
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                        Text("Auto-Auth Manager maintains sessions across all 4 brokers. Sessions are automatically refreshed on startup.", fontSize = 10.sp, color = TextGray)
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

@Composable
private fun BrokerStatusRow(
    name: String,
    subtitle: String,
    logoRes: Int = 0,
    letter: String = "",
    status: com.example.data.network.BrokerAuthStatus,
    lastRefreshTime: Long = 0L,
    onConnect: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRemoveAccount: (() -> Unit)? = null
) {
    val statusText = when (status) {
        com.example.data.network.BrokerAuthStatus.CONNECTED -> "🟢 Connected (Active)"
        com.example.data.network.BrokerAuthStatus.STANDBY -> "🟡 Standby (Session Valid)"
        com.example.data.network.BrokerAuthStatus.AUTHENTICATION_REQUIRED -> "🟠 Re-auth Required"
        com.example.data.network.BrokerAuthStatus.OFFLINE -> "🔴 Disconnected"
        com.example.data.network.BrokerAuthStatus.CONFIGURE -> "⚪ Not Configured"
        com.example.data.network.BrokerAuthStatus.ERROR -> "⚠️ Auth Error"
    }

    val statusColor = when (status) {
        com.example.data.network.BrokerAuthStatus.CONNECTED -> ProfitGreen
        com.example.data.network.BrokerAuthStatus.STANDBY -> Color(0xFFFFD54F)
        com.example.data.network.BrokerAuthStatus.AUTHENTICATION_REQUIRED -> Color(0xFFFF9800)
        com.example.data.network.BrokerAuthStatus.OFFLINE -> LossRed
        com.example.data.network.BrokerAuthStatus.CONFIGURE -> TextGray
        com.example.data.network.BrokerAuthStatus.ERROR -> LossRed
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (logoRes != 0) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = name,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                val boxBg = if (letter == "m") Color(0xFFE53935) else Color(0xFF0288D1)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(boxBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(letter, fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("$name • $subtitle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = statusColor)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                com.example.data.network.BrokerAuthStatus.CONNECTED -> {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LossRed),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("DISCONNECT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LossRed)
                    }
                }
                com.example.data.network.BrokerAuthStatus.STANDBY -> {
                    OutlinedButton(
                        onClick = onConnect,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("SWITCH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                }
                com.example.data.network.BrokerAuthStatus.AUTHENTICATION_REQUIRED,
                com.example.data.network.BrokerAuthStatus.ERROR -> {
                    Button(
                        onClick = onConnect,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("RE-AUTH", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                com.example.data.network.BrokerAuthStatus.CONFIGURE,
                com.example.data.network.BrokerAuthStatus.OFFLINE -> {
                    Button(
                        onClick = onConnect,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(if (status == com.example.data.network.BrokerAuthStatus.CONFIGURE) "CONFIGURE" else "CONNECT", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (onRemoveAccount != null && status != com.example.data.network.BrokerAuthStatus.CONFIGURE) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRemoveAccount,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove Account",
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

