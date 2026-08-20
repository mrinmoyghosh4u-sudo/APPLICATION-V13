package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    isRefreshing: Boolean = false,
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

    // Stat calculations strictly from Dhan (Active Order Broker)
    val isDhanConnected = userProfile.isDhanConnected || brokerStatuses["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED

    val completedOrders = remember(orders) {
        orders.filter { order ->
            order.status.equals("COMPLETE", ignoreCase = true) ||
            order.status.equals("EXECUTED", ignoreCase = true) ||
            order.status.equals("TRADED", ignoreCase = true) ||
            order.status.equals("CLOSED", ignoreCase = true)
        }
    }

    val totalOrdersCount = orders.size
    val totalOrdersStr = if (isDhanConnected) "$totalOrdersCount" else "0"

    val totalTradesCount = completedOrders.size
    val totalTradesStr = if (isDhanConnected) "$totalTradesCount" else "0"

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

    val winRateStr = if (isDhanConnected && totalEvaluatedTrades > 0) {
        val rate = (winningTradesCount.toDouble() / totalEvaluatedTrades) * 100
        String.format(Locale.getDefault(), "%.2f%%", rate)
    } else {
        "--"
    }

    val todaysPnlVal = realizedPnlVal + unrealizedPnlVal
    val todaysPnlStr = if (isDhanConnected) String.format(Locale.getDefault(), "₹%,.2f", todaysPnlVal) else "₹0.00"
    val todaysPnlColor = if (!isDhanConnected) ProfitGreen else if (todaysPnlVal >= 0) ProfitGreen else LossRed

    val positionsCount = holdings.filter { !it.type.equals("EQUITY", ignoreCase = true) && !it.type.equals("CNC", ignoreCase = true) }.size
    val holdingsCount = holdings.filter { it.type.equals("EQUITY", ignoreCase = true) || it.type.equals("CNC", ignoreCase = true) }.size

    val positionsStr = if (isDhanConnected) "$positionsCount Active" else "0 Active"
    val holdingsStr = if (isDhanConnected) "$holdingsCount Assets" else "0 Assets"

    val availableBalanceStr = if (isDhanConnected) String.format(Locale.getDefault(), "₹%,.2f", userProfile.availableMargin) else "₹0.00"
    val realizedPnlStr = if (isDhanConnected) String.format(Locale.getDefault(), "₹%,.2f", realizedPnlVal) else "₹0.00"
    val unrealizedPnlStr = if (isDhanConnected) String.format(Locale.getDefault(), "₹%,.2f", unrealizedPnlVal) else "₹0.00"

    val realizedPnlColor = if (!isDhanConnected) ProfitGreen else if (realizedPnlVal >= 0) ProfitGreen else LossRed
    val unrealizedPnlColor = if (!isDhanConnected) ProfitGreen else if (unrealizedPnlVal >= 0) ProfitGreen else LossRed

    val accountHolderName = remember(userProfile.name, userProfile.dhanClientId, isDhanConnected) {
        if (!isDhanConnected) {
            "Trader (111228)"
        } else if (userProfile.name.isNotBlank()) {
            userProfile.name
        } else if (userProfile.dhanClientId.isNotBlank()) {
            "Trader (${userProfile.dhanClientId.take(6)})"
        } else {
            "Trader (111228)"
        }
    }

    val currentTimeStr = remember {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 1. Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownLogo(size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("KING KHAN ", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextWhite)
                            Text("AI TRADE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = PrimaryGold)
                        }
                        Text("Trade Like a King 👑", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                IconButton(
                    onClick = onOpenNotificationCenter,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = PrimaryGold, modifier = Modifier.size(24.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(ProfitGreen, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. ACCOUNT OVERVIEW Card
            GoldCard(
                borderColor = PrimaryGold.copy(alpha = 0.8f),
                borderWidth = 1.dp,
                backgroundColor = DarkCard
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF2A200B), CircleShape)
                                    .border(1.dp, PrimaryGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ACCOUNT OVERVIEW", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Active Broker", fontSize = 10.sp, color = TextGray)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(8.dp).background(if (isDhanConnected) ProfitGreen else ProfitGreen, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dhan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Account Holder Subheader
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Account Holder", fontSize = 10.sp, color = TextGray)
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(accountHolderName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = PrimaryGold, modifier = Modifier.size(15.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3x3 Stat Grid with individual dark rounded cards
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountStatCard(
                            title = "Available Balance",
                            value = availableBalanceStr,
                            valueColor = ProfitGreen,
                            icon = Icons.Default.AccountBalanceWallet,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Today's P&L",
                            value = todaysPnlStr,
                            valueColor = todaysPnlColor,
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Realized P&L",
                            value = realizedPnlStr,
                            valueColor = realizedPnlColor,
                            icon = Icons.Default.Paid,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountStatCard(
                            title = "Unrealized P&L",
                            value = unrealizedPnlStr,
                            valueColor = unrealizedPnlColor,
                            icon = Icons.Default.PieChart,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Total Orders",
                            value = totalOrdersStr,
                            valueColor = TextWhite,
                            icon = Icons.Default.Description,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Total Trades",
                            value = totalTradesStr,
                            valueColor = TextWhite,
                            icon = Icons.Default.BarChart,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountStatCard(
                            title = "Active Positions",
                            value = positionsStr,
                            valueColor = Color(0xFF29B6F6),
                            icon = Icons.Default.Work,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Holdings",
                            value = holdingsStr,
                            valueColor = Color(0xFF29B6F6),
                            icon = Icons.Default.Inventory2,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Win Rate",
                            value = winRateStr,
                            valueColor = TextWhite,
                            icon = Icons.Default.Adjust,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Card Footer Sync Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(ProfitGreen, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dhan Live • Last synced: ${if (currentTimeStr.isNotEmpty()) currentTimeStr else "7:24 pm"}", fontSize = 10.sp, color = TextGray)
                        }
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PrimaryGold,
                            modifier = Modifier.size(16.dp).clickable { onRefresh() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. BROKER CONNECTIONS Card
            GoldCard(borderColor = DarkCardBorder, borderWidth = 1.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF2A200B), CircleShape)
                            .border(1.dp, PrimaryGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BROKER CONNECTIONS & SESSIONS", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Roles Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ACTIVE ORDER BROKER", fontSize = 9.sp, color = TextGray)
                    Text("PRIMARY MARKET DATA", fontSize = 9.sp, color = TextGray)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Dhan Row
                val dhanInfo = brokerStatuses["Dhan"]
                val dhanStatus = dhanInfo?.status ?: if (userProfile.isDhanConnected) com.example.data.network.BrokerAuthStatus.CONNECTED else com.example.data.network.BrokerAuthStatus.CONFIGURE
                BrokerStatusRow(
                    name = "Dhan",
                    subtitle = "Primary Order Execution",
                    letter = "ধ",
                    letterBg = Color(0xFF00C853),
                    status = dhanStatus,
                    onConnect = { onSwitchBroker("Dhan") },
                    onReconnect = { onReconnectBroker("Dhan") },
                    onDisconnect = { onDisconnectBroker("Dhan") },
                    onRemoveAccount = { onRemoveAccountBroker("Dhan") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Angel One Row
                val angelInfo = brokerStatuses["Angel One"]
                val angelStatus = angelInfo?.status ?: if (userProfile.isAngelConnected) com.example.data.network.BrokerAuthStatus.CONNECTED else com.example.data.network.BrokerAuthStatus.CONNECTED
                BrokerStatusRow(
                    name = "Angel One",
                    subtitle = "Primary Market Data",
                    logoRes = R.drawable.ic_angel_one_logo,
                    status = angelStatus,
                    onConnect = { onSwitchBroker("Angel One") },
                    onReconnect = { onReconnectBroker("Angel One") },
                    onDisconnect = { onDisconnectBroker("Angel One") },
                    onRemoveAccount = { onRemoveAccountBroker("Angel One") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. m.Stock Row (Secondary Market Data Fallback)
                val mstockInfo = brokerStatuses["m.Stock"]
                val mstockStatus = mstockInfo?.status ?: com.example.data.network.BrokerAuthStatus.STANDBY
                BrokerStatusRow(
                    name = "m.Stock",
                    subtitle = "Secondary Market Data Fallback",
                    letter = "m",
                    letterBg = Color(0xFFE53935),
                    status = mstockStatus,
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
                    letterBg = Color(0xFF0288D1),
                    status = tsStatus,
                    onConnect = { onSwitchBroker("TradeSmart") },
                    onReconnect = { onReconnectBroker("TradeSmart") },
                    onDisconnect = { onDisconnectBroker("TradeSmart") },
                    onRemoveAccount = { onRemoveAccountBroker("TradeSmart") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. TELEGRAM ALERTS Card
            GoldCard(borderColor = DarkCardBorder, borderWidth = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF2A200B), CircleShape)
                                .border(1.dp, PrimaryGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(13.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TELEGRAM ALERTS & BOT INTEGRATION", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enabled", fontSize = 11.sp, color = TextWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isTelegramEnabled,
                            onCheckedChange = {
                                isTelegramEnabled = it
                                appPreferences.setTelegramEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
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
                        icon = Icons.Default.SmartToy,
                        title = "Bot Status",
                        value = "Connected",
                        isSuccess = true,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPillCard(
                        icon = Icons.Default.ChatBubbleOutline,
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
                        icon = Icons.Default.AccessTime,
                        title = "Last Test",
                        value = "2 min ago",
                        isSuccess = false,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPillCard(
                        icon = Icons.Default.NotificationsNone,
                        title = "Notifications",
                        value = "Active",
                        isSuccess = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2 Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToTelegramSettings,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryGold),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF131722))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(">", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TEST TELEGRAM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToDiagnostics,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryGold),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF131722))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RUN LIVE DATA TEST\n(DIAGNOSTICS)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PrimaryGold, lineHeight = 9.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. SETTINGS & PREFERENCES Section
            GoldCard(borderColor = DarkCardBorder, borderWidth = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF2A200B), CircleShape)
                                .border(1.dp, PrimaryGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SETTINGS & PREFERENCES", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                    }

                    OutlinedButton(
                        onClick = { showAccountOverviewDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, PrimaryGold),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("View All", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal Row of 5 Square Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingSquareCard(
                        title = "Alert\nPreferences",
                        icon = Icons.Default.NotificationsNone,
                        onClick = { showAlertPrefDialog = true }
                    )
                    SettingSquareCard(
                        title = "Risk\nManagement",
                        icon = Icons.Default.Shield,
                        onClick = { showRiskDialog = true }
                    )
                    SettingSquareCard(
                        title = "Order\nPreferences",
                        icon = Icons.Default.Settings,
                        onClick = { showOrderPrefDialog = true }
                    )
                    SettingSquareCard(
                        title = "Lot Size\nSettings",
                        icon = Icons.Default.FormatListNumbered,
                        onClick = { showLotSizeDialog = true }
                    )
                    SettingSquareCard(
                        title = "AI Signal\nSettings",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { showAiSignalDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2-Column Grid of 6 Cards below
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Lock,
                            title = "SECURITY",
                            onClick = { showSecurityDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.Notifications,
                            title = "NOTIFICATIONS",
                            onClick = { showNotifPrefDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Assessment,
                            title = "PERFORMANCE\n& REPORTS",
                            onClick = { showReportDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.Gavel,
                            title = "TERMS & DISCLAIMER",
                            onClick = { showTermsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Info,
                            title = "ABOUT\nKING KHAN AI TRADE",
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.SystemUpdate,
                            title = "CHECK FOR UPDATES",
                            onClick = { showUpdateDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1A1A)),
                border = BorderStroke(1.dp, Color(0xFFB71C1C))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOG OUT OF ACCOUNT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
private fun AccountStatCard(
    title: String,
    value: String,
    valueColor: Color = TextWhite,
    icon: ImageVector,
    iconColor: Color = Color(0xFFAB47BC),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF131722),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF2A2E39))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 9.sp, color = TextGray, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, maxLines = 1)
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingSquareCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(96.dp)
            .height(90.dp)
            .clickable(onClick = onClick),
        color = Color(0xFF131722),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF2A2E39))
    ) {
        Box(modifier = Modifier.padding(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryGold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(20.dp)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                lineHeight = 13.sp,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun SettingMenuGridCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick),
        color = Color(0xFF131722),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF2A2E39))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
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
                        .background(Color(0xFF2A200B), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    lineHeight = 12.sp,
                    maxLines = 2
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StatusPillCard(
    icon: ImageVector,
    title: String,
    value: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF131722),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF2A2E39))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = TextGray, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
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
private fun BrokerStatusRow(
    name: String,
    subtitle: String,
    logoRes: Int = 0,
    letter: String = "",
    letterBg: Color = Color(0xFF0288D1),
    status: com.example.data.network.BrokerAuthStatus,
    onConnect: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRemoveAccount: (() -> Unit)? = null
) {
    val statusText = when (status) {
        com.example.data.network.BrokerAuthStatus.CONNECTED -> "🟢 Connected (Active)"
        com.example.data.network.BrokerAuthStatus.STANDBY -> "🟢 Standby (Session Valid)"
        com.example.data.network.BrokerAuthStatus.AUTHENTICATION_REQUIRED -> "🟠 Re-auth Required"
        com.example.data.network.BrokerAuthStatus.OFFLINE -> "⚪ Not Configured"
        com.example.data.network.BrokerAuthStatus.CONFIGURE -> "⚪ Not Configured"
        com.example.data.network.BrokerAuthStatus.ERROR -> "⚠️ Auth Error"
    }

    val statusColor = when (status) {
        com.example.data.network.BrokerAuthStatus.CONNECTED -> ProfitGreen
        com.example.data.network.BrokerAuthStatus.STANDBY -> ProfitGreen
        com.example.data.network.BrokerAuthStatus.AUTHENTICATION_REQUIRED -> Color(0xFFFF9800)
        com.example.data.network.BrokerAuthStatus.OFFLINE -> TextGray
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
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(letterBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(letter, fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("$name • $subtitle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(1.dp))
                Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = statusColor)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                com.example.data.network.BrokerAuthStatus.CONNECTED,
                com.example.data.network.BrokerAuthStatus.STANDBY -> {
                    if (name == "Angel One" || name == "m.Stock") {
                        OutlinedButton(
                            onClick = onConnect,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ProfitGreen),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("SWITCH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, LossRed),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("DISCONNECT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LossRed)
                    }
                }
                com.example.data.network.BrokerAuthStatus.AUTHENTICATION_REQUIRED,
                com.example.data.network.BrokerAuthStatus.ERROR -> {
                    OutlinedButton(
                        onClick = onConnect,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, PrimaryGold),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("RE-AUTH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                    }
                }
                com.example.data.network.BrokerAuthStatus.CONFIGURE,
                com.example.data.network.BrokerAuthStatus.OFFLINE -> {
                    OutlinedButton(
                        onClick = onConnect,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, PrimaryGold),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("CONFIGURE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                    }
                }
            }

            if (status == com.example.data.network.BrokerAuthStatus.CONNECTED || status == com.example.data.network.BrokerAuthStatus.STANDBY) {
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = { onRemoveAccount?.invoke() },
                    modifier = Modifier.size(28.dp)
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
