package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    userProfile: UserProfileEntity,
    orders: List<OrderEntity> = emptyList(),
    holdings: List<PortfolioHoldingEntity> = emptyList(),
    notifications: List<com.example.data.model.NotificationEntity> = emptyList(),
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    // Dialog States
    var showAccountOverviewDialog by remember { mutableStateOf(false) }
    var showFundsBreakdownDialog by remember { mutableStateOf(false) }
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
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }
    var showResetWalletDialog by remember { mutableStateOf(false) }

    var isTelegramEnabled by remember { mutableStateOf(appPreferences.isTelegramEnabled()) }
    var isTestSending by remember { mutableStateOf(false) }
    var customSimulatedMargin by remember { mutableStateOf<Double?>(null) }

    // Stat calculations strictly from Dhan / Broker Orders
    val isDhanConnected = userProfile.isDhanConnected || brokerStatuses["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
    val isAngelConnected = userProfile.isAngelConnected || brokerStatuses["Angel One"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
    val isMStockConnected = brokerStatuses["m.Stock"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
    val isAnyBrokerConnected = isDhanConnected || isAngelConnected || isMStockConnected || (userProfile.connectedBroker.isNotBlank() && (userProfile.isDhanConnected || userProfile.isAngelConnected))

    val completedOrders = remember(orders) {
        orders.filter { order ->
            order.status.equals("COMPLETE", ignoreCase = true) ||
            order.status.equals("EXECUTED", ignoreCase = true) ||
            order.status.equals("TRADED", ignoreCase = true) ||
            order.status.equals("CLOSED", ignoreCase = true)
        }
    }

    val totalOrdersCount = orders.size
    val totalOrdersStr = if (isDhanConnected || orders.isNotEmpty()) "$totalOrdersCount" else "0"

    val totalTradesCount = completedOrders.size
    val totalTradesStr = if (isDhanConnected || completedOrders.isNotEmpty()) "$totalTradesCount" else "0"

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

    val winRateStr = if (totalEvaluatedTrades > 0) {
        val rate = (winningTradesCount.toDouble() / totalEvaluatedTrades) * 100
        String.format(Locale.getDefault(), "%.1f%%", rate)
    } else {
        "--"
    }

    val todaysPnlVal = realizedPnlVal + unrealizedPnlVal
    val todaysPnlStr = String.format(Locale.getDefault(), "₹%,.2f", todaysPnlVal)
    val todaysPnlColor = if (todaysPnlVal > 0) ProfitGreen else if (todaysPnlVal < 0) LossRed else TextWhite

    val positionsCount = holdings.filter { !it.type.equals("EQUITY", ignoreCase = true) && !it.type.equals("CNC", ignoreCase = true) }.size
    val holdingsCount = holdings.filter { it.type.equals("EQUITY", ignoreCase = true) || it.type.equals("CNC", ignoreCase = true) }.size

    val positionsStr = "$positionsCount Active"
    val holdingsStr = "$holdingsCount Assets"

    val availableMargin = customSimulatedMargin ?: userProfile.availableMargin
    val availableBalanceStr = String.format(Locale.getDefault(), "₹%,.2f", availableMargin)
    val realizedPnlStr = String.format(Locale.getDefault(), "₹%,.2f", realizedPnlVal)
    val unrealizedPnlStr = String.format(Locale.getDefault(), "₹%,.2f", unrealizedPnlVal)

    val realizedPnlColor = if (realizedPnlVal > 0) ProfitGreen else if (realizedPnlVal < 0) LossRed else TextWhite
    val unrealizedPnlColor = if (unrealizedPnlVal > 0) ProfitGreen else if (unrealizedPnlVal < 0) LossRed else TextWhite

    val accountHolderName = remember(userProfile.name, userProfile.dhanClientId, userProfile.angelClientId) {
        if (userProfile.name.isNotBlank()) {
            userProfile.name
        } else if (userProfile.dhanClientId.isNotBlank()) {
            "Trader (${userProfile.dhanClientId})"
        } else if (userProfile.angelClientId.isNotBlank()) {
            "Trader (${userProfile.angelClientId})"
        } else {
            "Trader"
        }
    }

    val userClientId = if (userProfile.dhanClientId.isNotBlank()) userProfile.dhanClientId else if (userProfile.angelClientId.isNotBlank()) userProfile.angelClientId else "--"
    val userEmail = if (userProfile.email.isNotBlank()) userProfile.email else "--"

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
            // 1. Top Bar Header
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
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = onOpenNotificationCenter,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = PrimaryGold, modifier = Modifier.size(24.dp))
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

                    IconButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "About", tint = TextGray, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. VIP USER IDENTITY CARD
            GoldCard(
                borderColor = PrimaryGold.copy(alpha = 0.8f),
                borderWidth = 1.dp,
                backgroundColor = DarkCard
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Avatar Box
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF3E2D0A), Color(0xFF1E1708))
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(1.5.dp, PrimaryGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = accountHolderName.take(2).uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryGold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(accountHolderName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(Icons.Default.Verified, contentDescription = "KYC Verified", tint = Color(0xFF29B6F6), modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(userEmail, fontSize = 11.sp, color = TextGray)

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFF1E232F), RoundedCornerShape(4.dp))
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(userClientId))
                                            Toast.makeText(context, "Client ID ($userClientId) copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ID: $userClientId", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = TextGray, modifier = Modifier.size(11.dp))
                                }
                            }
                        }

                        // Dynamic Broker Live Status Badge
                        if (isAnyBrokerConnected) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0D2517),
                                border = BorderStroke(1.dp, ProfitGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(ProfitGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ProfitGreen)
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E232F),
                                border = BorderStroke(1.dp, TextGray.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(TextGray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("OFFLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. ACCOUNT OVERVIEW & FINANCIAL STATS CARD
            GoldCard(
                borderColor = DarkCardBorder,
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
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FINANCIAL & MARGIN HEALTH", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                        }

                        OutlinedButton(
                            onClick = { showFundsBreakdownDialog = true },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, PrimaryGold),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Ledger & Funds", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent Available Margin Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF131722),
                        border = BorderStroke(1.dp, Color(0xFF2A2E39))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("AVAILABLE TRADING CAPITAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(availableBalanceStr, fontSize = 20.sp, fontWeight = FontWeight.Black, color = ProfitGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3x3 Stat Grid
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountStatCard(
                            title = "Today's P&L",
                            value = todaysPnlStr,
                            valueColor = todaysPnlColor,
                            icon = Icons.Default.TrendingUp,
                            iconColor = if (todaysPnlVal >= 0) ProfitGreen else LossRed,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Realized P&L",
                            value = realizedPnlStr,
                            valueColor = realizedPnlColor,
                            icon = Icons.Default.Paid,
                            iconColor = ProfitGreen,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Unrealized P&L",
                            value = unrealizedPnlStr,
                            valueColor = unrealizedPnlColor,
                            icon = Icons.Default.PieChart,
                            iconColor = Color(0xFF29B6F6),
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
                            title = "Total Orders",
                            value = totalOrdersStr,
                            valueColor = TextWhite,
                            icon = Icons.Default.Description,
                            iconColor = SecondaryGold,
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Executed Trades",
                            value = totalTradesStr,
                            valueColor = TextWhite,
                            icon = Icons.Default.BarChart,
                            iconColor = Color(0xFFAB47BC),
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Win Rate",
                            value = winRateStr,
                            valueColor = ProfitGreen,
                            icon = Icons.Default.Adjust,
                            iconColor = ProfitGreen,
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
                            iconColor = Color(0xFF29B6F6),
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Equity Holdings",
                            value = holdingsStr,
                            valueColor = Color(0xFF29B6F6),
                            icon = Icons.Default.Inventory2,
                            iconColor = Color(0xFF26A69A),
                            modifier = Modifier.weight(1f)
                        )
                        AccountStatCard(
                            title = "Risk Multiplier",
                            value = "1.0x Normal",
                            valueColor = TextWhite,
                            icon = Icons.Default.Security,
                            iconColor = PrimaryGold,
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
                            Text("Dhan Live Engine • Synced at $currentTimeStr", fontSize = 10.sp, color = TextGray)
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

            // 4. BROKER CONNECTIONS & SESSIONS Card
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
                            Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BROKER CONNECTIONS & FEEDS", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E2838)
                    ) {
                        Text("Multi-Broker Sync", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29B6F6), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Roles Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ACTIVE ORDER BROKER: DHAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    Text("DATA FEED: ANGEL ONE (SMARTAPI)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29B6F6))
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Dhan Row
                val dhanInfo = brokerStatuses["Dhan"]
                val dhanStatus = dhanInfo?.status ?: if (userProfile.isDhanConnected) com.example.data.network.BrokerAuthStatus.CONNECTED else com.example.data.network.BrokerAuthStatus.OFFLINE
                BrokerStatusRow(
                    name = "Dhan",
                    subtitle = "Primary Order Execution Engine",
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
                val angelStatus = angelInfo?.status ?: if (userProfile.isAngelConnected) com.example.data.network.BrokerAuthStatus.CONNECTED else com.example.data.network.BrokerAuthStatus.OFFLINE
                BrokerStatusRow(
                    name = "Angel One",
                    subtitle = "Primary Live Market Data Feed (SmartAPI)",
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

            // 5. TELEGRAM ALERTS & BOT INTEGRATION (Self-Contained Option)
            GoldCard(
                borderColor = DarkCardBorder,
                borderWidth = 1.dp
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTelegramSettings() },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF2A200B), CircleShape)
                                    .border(1.dp, PrimaryGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "TELEGRAM ALERTS & BOT ENGINE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryGold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (isTelegramEnabled) "Active • Instant alerts & optional channel broadcast" else "Configure bot token, chat ID & channel ID",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isTelegramEnabled) ProfitGreen.copy(alpha = 0.15f) else TextGray.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (isTelegramEnabled) ProfitGreen else TextGray.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isTelegramEnabled) ProfitGreen else TextGray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        if (isTelegramEnabled) "LIVE" else "SETUP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTelegramEnabled) ProfitGreen else TextGray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Open Telegram Engine",
                                tint = PrimaryGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. SETTINGS & PREFERENCES HUB
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
                            Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STRATEGY & RISK SETTINGS", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                    }

                    OutlinedButton(
                        onClick = { showAccountOverviewDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, PrimaryGold),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Analytics", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal Row of 5 Quick Preset Square Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingSquareCard(
                        title = "Alert\nTriggers",
                        icon = Icons.Default.NotificationsActive,
                        badge = "LIVE",
                        onClick = { showAlertPrefDialog = true }
                    )
                    SettingSquareCard(
                        title = "Risk\nControls",
                        icon = Icons.Default.Shield,
                        badge = "PROTECT",
                        onClick = { showRiskDialog = true }
                    )
                    SettingSquareCard(
                        title = "Order\nRouting",
                        icon = Icons.Default.SettingsSuggest,
                        badge = "DHAN",
                        onClick = { showOrderPrefDialog = true }
                    )
                    SettingSquareCard(
                        title = "Option Lot\nSizes",
                        icon = Icons.Default.FormatListNumbered,
                        badge = "PRESETS",
                        onClick = { showLotSizeDialog = true }
                    )
                    SettingSquareCard(
                        title = "AI Signal\nEngine",
                        icon = Icons.Default.AutoAwesome,
                        badge = "GEMINI",
                        onClick = { showAiSignalDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("APP UTILITIES & SECURITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray)
                Spacer(modifier = Modifier.height(8.dp))

                // 2-Column Grid of Action Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Fingerprint,
                            title = "BIOMETRIC & PIN LOCK",
                            subtitle = "Secure local access",
                            onClick = { showSecurityDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.VolumeUp,
                            title = "SOUND & NOTIFICATIONS",
                            subtitle = "Push and sound alerts",
                            onClick = { showNotifPrefDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Assessment,
                            title = "PERFORMANCE REPORT",
                            subtitle = "P&L analytics & win rate",
                            onClick = { showReportDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.Speed,
                            title = "LIVE DIAGNOSTICS",
                            subtitle = "Ping & broker latency test",
                            onClick = onNavigateToDiagnostics,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.CleaningServices,
                            title = "CLEAR APP CACHE",
                            subtitle = "Free memory & data",
                            onClick = { showClearCacheConfirmDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.Gavel,
                            title = "TERMS & DISCLAIMER",
                            subtitle = "SEBI risk compliance",
                            onClick = { showTermsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Info,
                            title = "ABOUT KING KHAN",
                            subtitle = "v${com.example.BuildConfig.VERSION_NAME} Official",
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.SystemUpdate,
                            title = "CHECK FOR UPDATES",
                            subtitle = "Latest stable version",
                            onClick = { showUpdateDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. SAFE LOGOUT CARD (CENTERED)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutConfirmDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1F1214),
                border = BorderStroke(1.dp, Color(0xFF7F1D1D))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF450A0A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = LossRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOG OUT OF ACCOUNT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFCA5A5),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Safely terminate local encrypted broker sessions",
                        fontSize = 10.sp,
                        color = TextGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // ==========================================
        // INTERACTIVE DIALOGS & OVERLAYS
        // ==========================================

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

        if (showFundsBreakdownDialog) {
            FundsBreakdownDialog(
                availableMargin = availableMargin,
                realizedPnl = realizedPnlVal,
                unrealizedPnl = unrealizedPnlVal,
                holdingsCount = holdingsCount,
                onDismiss = { showFundsBreakdownDialog = false },
                onResetWallet = {
                    showFundsBreakdownDialog = false
                    showResetWalletDialog = true
                }
            )
        }

        if (showResetWalletDialog) {
            ResetWalletDialog(
                currentBalance = availableMargin,
                onDismiss = { showResetWalletDialog = false },
                onConfirm = { amount ->
                    customSimulatedMargin = amount
                    showResetWalletDialog = false
                    Toast.makeText(context, "Trading Capital successfully set to ₹%,.0f".format(amount), Toast.LENGTH_SHORT).show()
                    onRefresh()
                }
            )
        }

        if (showClearCacheConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PrimaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear App Cache?", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        "This will clear local temporary chart cache, cached market quotes, and diagnostic logs. Your broker credentials and API tokens will remain safely intact.",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearCacheConfirmDialog = false
                            Toast.makeText(context, "✅ App Cache and temporary data successfully cleared!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("Clear Now", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirmDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = DarkCard,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = LossRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Logout", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        "Are you sure you want to log out from King Khan AI Trade? This will disconnect your active broker session and clear local security authorization.",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirmDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                    ) {
                        Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text("Cancel", color = TextGray)
                    }
                },
                containerColor = DarkCard,
                shape = RoundedCornerShape(12.dp)
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
private fun FundsBreakdownDialog(
    availableMargin: Double,
    realizedPnl: Double,
    unrealizedPnl: Double,
    holdingsCount: Int,
    onDismiss: () -> Unit,
    onResetWallet: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PrimaryGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Funds & Margin Ledger", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = Color(0xFF131722),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF2A2E39)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Available Cash Margin", fontSize = 11.sp, color = TextGray)
                            Text(String.format(Locale.getDefault(), "₹%,.2f", availableMargin), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Collateral / Stock Margin", fontSize = 11.sp, color = TextGray)
                            Text("₹0.00", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextWhite)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Option Buying Exposure", fontSize = 11.sp, color = TextGray)
                            Text("100% Cash Ready", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF29B6F6))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Today's Realized Net P&L", fontSize = 11.sp, color = TextGray)
                            Text(String.format(Locale.getDefault(), "₹%,.2f", realizedPnl), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (realizedPnl >= 0) ProfitGreen else LossRed)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Unrealized MTM P&L", fontSize = 11.sp, color = TextGray)
                            Text(String.format(Locale.getDefault(), "₹%,.2f", unrealizedPnl), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (unrealizedPnl >= 0) ProfitGreen else LossRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFF0F2027),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "According to SEBI peak margin rules, 100% upfront premium is allocated for Index Options buying.",
                            fontSize = 10.sp,
                            color = Color(0xFF90CAF9)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onResetWallet,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
            ) {
                Text("Modify Capital", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextGray)
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun ResetWalletDialog(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var selectedAmount by remember { mutableStateOf(currentBalance) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adjust Trading Capital", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select your desired capital allocation for paper trading & margin simulation:", fontSize = 12.sp, color = TextGray)
                Spacer(modifier = Modifier.height(14.dp))

                listOf(
                    100000.0 to "₹1,00,000 (Conservative)",
                    250000.0 to "₹2,50,000 (Standard)",
                    500000.0 to "₹5,00,000 (Recommended)",
                    1000000.0 to "₹10,00,000 (Pro Trader)"
                ).forEach { (amt, label) ->
                    val isSelected = (selectedAmount == amt)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedAmount = amt },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF2A200B) else Color(0xFF131722),
                        border = BorderStroke(1.dp, if (isSelected) PrimaryGold else Color(0xFF2A2E39))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) PrimaryGold else TextWhite)
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedAmount = amt },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold, unselectedColor = TextGray)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedAmount) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
            ) {
                Text("Apply Capital", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(12.dp)
    )
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
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(100.dp)
            .height(95.dp)
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
                    .size(22.dp)
            )

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF2A200B),
                    border = BorderStroke(0.5.dp, PrimaryGold.copy(alpha = 0.6f)),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = badge,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryGold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                )
            }

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
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(58.dp)
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
                        .size(30.dp)
                        .background(Color(0xFF2A200B), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        fontSize = 8.sp,
                        color = TextGray,
                        maxLines = 1
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
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
        com.example.data.network.BrokerAuthStatus.CONNECTED -> "🟢 Live"
        com.example.data.network.BrokerAuthStatus.STANDBY -> "🟢 Live"
        com.example.data.network.BrokerAuthStatus.AUTHENTICATION_REQUIRED -> "🟠 Re-auth Required"
        com.example.data.network.BrokerAuthStatus.OFFLINE -> "⚪ Not Connected"
        com.example.data.network.BrokerAuthStatus.CONFIGURE -> "⚪ Not Connected"
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
