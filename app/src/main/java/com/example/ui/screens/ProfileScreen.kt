package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.NotificationEntity
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.network.BrokerAuthStatus
import com.example.data.network.BrokerConnectionState
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.AppPreferences
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    userProfile: UserProfileEntity,
    orders: List<OrderEntity> = emptyList(),
    holdings: List<PortfolioHoldingEntity> = emptyList(),
    notifications: List<NotificationEntity> = emptyList(),
    marketDataSource: String = "",
    appPreferences: AppPreferences,
    brokerStatuses: Map<String, BrokerConnectionState> = emptyMap(),
    viewModel: com.example.viewmodel.MainViewModel? = null,
    onSwitchBroker: (String) -> Unit,
    onReconnectBroker: (String) -> Unit = {},
    onDisconnectBroker: (String) -> Unit = {},
    onRemoveAccountBroker: (String) -> Unit = {},
    onToggleBiometric: (Boolean) -> Unit,
    onNavigateToTelegramSettings: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToHealthAutoFix: () -> Unit = {},
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

    val hasOpenDialog = showAccountOverviewDialog || showFundsBreakdownDialog || showAlertPrefDialog ||
            showRiskDialog || showOrderPrefDialog || showLotSizeDialog || showAiSignalDialog ||
            showSecurityDialog || showNotifPrefDialog || showReportDialog || showTermsDialog ||
            showAboutDialog || showUpdateDialog || showLogoutConfirmDialog || showClearCacheConfirmDialog ||
            showResetWalletDialog

    BackHandler(enabled = hasOpenDialog) {
        showAccountOverviewDialog = false
        showFundsBreakdownDialog = false
        showAlertPrefDialog = false
        showRiskDialog = false
        showOrderPrefDialog = false
        showLotSizeDialog = false
        showAiSignalDialog = false
        showSecurityDialog = false
        showNotifPrefDialog = false
        showReportDialog = false
        showTermsDialog = false
        showAboutDialog = false
        showUpdateDialog = false
        showLogoutConfirmDialog = false
        showClearCacheConfirmDialog = false
        showResetWalletDialog = false
    }

    var isBiometricActive by remember { mutableStateOf(userProfile.isBiometricEnabled || appPreferences.isBiometricEnabled()) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // =========================================================================
            // 1. TOP BAR / BRAND HEADER (Logo, Heading, Tagline)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CrownLogo(size = 38.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "KING KHAN AI TRADE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        KingKhanTagline(fontSize = 11.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveStatusBadge(
                        isLive = marketDataSource.isNotBlank() && marketDataSource != "OFFLINE" && !marketDataSource.contains("UNAVAILABLE"),
                        dataSource = marketDataSource.ifBlank { "OFFLINE" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onOpenNotificationCenter,
                        modifier = Modifier.size(36.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = LossRed,
                                        contentColor = Color.White
                                    ) {
                                        Text(text = if (unreadCount > 9) "9+" else "$unreadCount", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = PrimaryGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 2. USER PROFILE HERO CARD
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Circle
                        val holderName = when {
                            userProfile.name.isNotBlank() && userProfile.name != "King Khan Master Trader" -> userProfile.name
                            userProfile.isDhanConnected && userProfile.dhanClientId.isNotBlank() -> "Dhan Account (${userProfile.dhanClientId})"
                            userProfile.isDhanConnected -> "Dhan Account Holder"
                            userProfile.dhanClientId.isNotBlank() -> "Dhan Account (${userProfile.dhanClientId})"
                            userProfile.isAngelConnected && userProfile.angelClientId.isNotBlank() -> "Angel One (${userProfile.angelClientId})"
                            else -> "Trader Account"
                        }
                        val initial = holderName.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "D"

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(DarkGold, PrimaryGold, SecondaryGold)
                                    )
                                )
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(DarkBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryGold
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = holderName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            if (userProfile.isDhanConnected || userProfile.dhanClientId.isNotBlank()) {
                                Text(
                                    text = "⚡ Dhan Order Execution: Connected" + if (userProfile.dhanClientId.isNotBlank()) " (${userProfile.dhanClientId})" else "",
                                    fontSize = 11.sp,
                                    color = ProfitGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Order Execution: Dhan Disconnected",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                            if (userProfile.email.isNotBlank() || userProfile.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = listOfNotNull(
                                        userProfile.email.takeIf { it.isNotBlank() },
                                        userProfile.phone.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DividerDark)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Balance & Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("AVAILABLE MARGIN", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                            val marginDisplay = when {
                                userProfile.isMarginAvailable -> currencyFormatter.format(userProfile.availableMargin)
                                userProfile.isMarginStale -> "${currencyFormatter.format(userProfile.availableMargin)} (Stale)"
                                userProfile.isBrokerConnected -> "Unavailable"
                                userProfile.availableMargin > 0 -> "${currencyFormatter.format(userProfile.availableMargin)} (Paper)"
                                else -> "Unavailable"
                            }
                            Text(
                                text = marginDisplay,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userProfile.isMarginUnavailable && userProfile.isBrokerConnected) TextGray else TextWhite
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL ORDERS", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${orders.size.coerceAtLeast(userProfile.totalOrders)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("TODAY'S P&L", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                            val (pnlText, pnlColor) = when {
                                userProfile.isPnlAvailable -> {
                                    val pnl = userProfile.todaysPnl
                                    val prefix = if (pnl >= 0) "+" else ""
                                    "$prefix${currencyFormatter.format(pnl)}" to (if (pnl >= 0) ProfitGreen else LossRed)
                                }
                                userProfile.isPnlStale -> {
                                    val pnl = userProfile.todaysPnl
                                    val prefix = if (pnl >= 0) "+" else ""
                                    "$prefix${currencyFormatter.format(pnl)} (Stale)" to (if (pnl >= 0) ProfitGreen else LossRed)
                                }
                                userProfile.isBrokerConnected -> {
                                    "Unavailable" to TextGray
                                }
                                else -> {
                                    "₹0.00" to TextGray
                                }
                            }
                            Text(
                                text = pnlText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = pnlColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 3. SYSTEM HEALTH & AUTO-FIX DIAGNOSTICS BAR
            // =========================================================================
            Text(
                text = "DIAGNOSTICS & SYSTEM REPAIR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToHealthAutoFix() }
                        .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "HEALTH & AUTO-FIX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "A-Z self-healing scan",
                            fontSize = 9.sp,
                            color = TextGray
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTelegramSettings() }
                        .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = SecondaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TELEGRAM ALERTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Instant bot alerts",
                            fontSize = 9.sp,
                            color = TextGray
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToDiagnostics() }
                        .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "LIVE DATA TEST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "WebSocket latency",
                            fontSize = 9.sp,
                            color = TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 4. BROKER CONNECTIONS & MARKET FEEDS
            // =========================================================================
            Text(
                text = "BROKER ACCOUNTS & DATA FEEDS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 1. DhanHQ
            val dhanInfo = brokerStatuses["Dhan"]
            val dhanStatus = dhanInfo?.status ?: if (userProfile.isDhanConnected) BrokerAuthStatus.CONNECTED else BrokerAuthStatus.DISCONNECTED
            BrokerStatusRow(
                name = "DhanHQ",
                subtitle = "Primary Order Execution Engine (DhanHQ API v2)",
                status = dhanStatus,
                statusMessage = dhanInfo?.message ?: if (userProfile.isDhanConnected) "Connected (Client: ${userProfile.dhanClientId.ifBlank { "Active" }})" else "Disconnected",
                onConnect = { onSwitchBroker("Dhan") },
                onReconnect = { onReconnectBroker("Dhan") },
                onDisconnect = { onDisconnectBroker("Dhan") },
                onRemoveAccount = { onRemoveAccountBroker("Dhan") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Angel One
            val angelInfo = brokerStatuses["Angel One"]
            val angelStatus = angelInfo?.status ?: if (userProfile.isAngelConnected) BrokerAuthStatus.CONNECTED else BrokerAuthStatus.DISCONNECTED
            BrokerStatusRow(
                name = "Angel One",
                subtitle = "Market Data & Option Chain Feed (SmartAPI)",
                status = angelStatus,
                statusMessage = angelInfo?.message ?: if (userProfile.isAngelConnected) "Connected (Client: ${userProfile.angelClientId.ifBlank { "Active" }})" else "Disconnected",
                onConnect = { onSwitchBroker("Angel One") },
                onReconnect = { onReconnectBroker("Angel One") },
                onDisconnect = { onDisconnectBroker("Angel One") },
                onRemoveAccount = { onRemoveAccountBroker("Angel One") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Fyers
            val fyersInfo = brokerStatuses["Fyers"]
            val fyersStatus = fyersInfo?.status ?: BrokerAuthStatus.DISCONNECTED
            BrokerStatusRow(
                name = "Fyers",
                subtitle = "Market Data Feed (API v3 WebSocket)",
                status = fyersStatus,
                statusMessage = fyersInfo?.message ?: "WebSocket Live Tick Feeds",
                onConnect = { onSwitchBroker("Fyers") },
                onReconnect = { onReconnectBroker("Fyers") },
                onDisconnect = { onDisconnectBroker("Fyers") },
                onRemoveAccount = { onRemoveAccountBroker("Fyers") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Upstox
            val upstoxInfo = brokerStatuses["Upstox"]
            val upstoxStatus = upstoxInfo?.status ?: BrokerAuthStatus.DISCONNECTED
            BrokerStatusRow(
                name = "Upstox",
                subtitle = "Market Data Feed & Streamer (Protobuf API v3)",
                status = upstoxStatus,
                statusMessage = upstoxInfo?.message ?: "High-speed Market Stream",
                onConnect = { onSwitchBroker("Upstox") },
                onReconnect = { onReconnectBroker("Upstox") },
                onDisconnect = { onDisconnectBroker("Upstox") },
                onRemoveAccount = { onRemoveAccountBroker("Upstox") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 5. TRADING & RISK PREFERENCES GRID
            // =========================================================================
            Text(
                text = "TRADING & RISK CONFIGURATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingMenuGridCard(
                    icon = Icons.Default.Shield,
                    title = "RISK MANAGEMENT",
                    subtitle = "Daily loss & profit caps",
                    onClick = { showRiskDialog = true },
                    modifier = Modifier.weight(1f)
                )
                SettingMenuGridCard(
                    icon = Icons.Default.Tune,
                    title = "ORDER PREFERENCES",
                    subtitle = "Default limits & product type",
                    onClick = { showOrderPrefDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingMenuGridCard(
                    icon = Icons.Default.FormatListNumbered,
                    title = "LOT SIZES",
                    subtitle = "Index & MCX lot matrix",
                    onClick = { showLotSizeDialog = true },
                    modifier = Modifier.weight(1f)
                )
                SettingMenuGridCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI SIGNAL ENGINE",
                    subtitle = "Indicators & confidence",
                    onClick = { showAiSignalDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingMenuGridCard(
                    icon = Icons.Default.Assessment,
                    title = "PERFORMANCE REPORT",
                    subtitle = "P&L analytics & history",
                    onClick = { showReportDialog = true },
                    modifier = Modifier.weight(1f)
                )
                SettingMenuGridCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "ALERT PREFERENCES",
                    subtitle = "Signal & trade alerts",
                    onClick = { showAlertPrefDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 6. SECURITY & APPLICATION SETTINGS
            // =========================================================================
            Text(
                text = "SECURITY & APP PREFERENCES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Biometric Quick Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Biometric Lock",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Fingerprint / Face authentication on app launch",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricActive,
                        onCheckedChange = { enabled ->
                            isBiometricActive = enabled
                            appPreferences.setBiometricEnabled(enabled)
                            onToggleBiometric(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Biometric Lock Enabled" else "Biometric Lock Disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = PrimaryGold,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = DarkCardSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingMenuGridCard(
                    icon = Icons.Default.Lock,
                    title = "SECURITY SETTINGS",
                    subtitle = "PIN, biometrics & lock",
                    onClick = { showSecurityDialog = true },
                    modifier = Modifier.weight(1f)
                )
                SettingMenuGridCard(
                    icon = Icons.Default.Notifications,
                    title = "PUSH NOTIFICATIONS",
                    subtitle = "System sound & push",
                    onClick = { showNotifPrefDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingMenuGridCard(
                    icon = Icons.Default.SystemUpdate,
                    title = "CHECK FOR UPDATES",
                    subtitle = "v${com.example.BuildConfig.VERSION_NAME} Official",
                    onClick = { showUpdateDialog = true },
                    modifier = Modifier.weight(1f)
                )
                SettingMenuGridCard(
                    icon = Icons.Default.Info,
                    title = "ABOUT KING KHAN",
                    subtitle = "Version & Architecture",
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingMenuGridCard(
                    icon = Icons.Default.Gavel,
                    title = "TERMS & DISCLAIMER",
                    subtitle = "Risk disclosure policy",
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.weight(1f)
                )
                SettingMenuGridCard(
                    icon = Icons.Default.CleaningServices,
                    title = "CLEAR APP CACHE",
                    subtitle = "Purge transient memory",
                    onClick = { showClearCacheConfirmDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 7. DANGER ZONE / LOGOUT
            // =========================================================================
            OutlinedButton(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LossRed.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = LossRed.copy(alpha = 0.08f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = LossRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOGOUT ACCOUNT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LossRed,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================
    if (showRiskDialog) {
        RiskManagementDialog(
            appPreferences = appPreferences,
            onDismiss = { showRiskDialog = false },
            onSave = { newSettings ->
                appPreferences.saveRiskSettings(newSettings)
                showRiskDialog = false
                Toast.makeText(context, "Risk settings updated successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showOrderPrefDialog) {
        OrderPreferencesDialog(
            appPreferences = appPreferences,
            onDismiss = { showOrderPrefDialog = false },
            onSave = { newPrefs ->
                appPreferences.saveOrderPreferences(newPrefs)
                showOrderPrefDialog = false
                Toast.makeText(context, "Order preferences saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showLotSizeDialog) {
        LotSizeSettingsDialog(
            appPreferences = appPreferences,
            onDismiss = { showLotSizeDialog = false },
            onSave = {
                showLotSizeDialog = false
                Toast.makeText(context, "Lot sizes updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAiSignalDialog) {
        AiSignalSettingsDialog(
            appPreferences = appPreferences,
            onDismiss = { showAiSignalDialog = false }
        )
    }

    if (showReportDialog) {
        PerformanceReportDialog(
            orders = orders,
            holdings = holdings,
            onDismiss = { showReportDialog = false }
        )
    }

    if (showAlertPrefDialog) {
        AlertPreferencesDialog(
            appPreferences = appPreferences,
            onDismiss = { showAlertPrefDialog = false }
        )
    }

    if (showNotifPrefDialog) {
        NotificationSettingsDialog(
            appPreferences = appPreferences,
            onDismiss = { showNotifPrefDialog = false },
            onSave = { newSettings ->
                appPreferences.saveNotificationSettings(newSettings)
                showNotifPrefDialog = false
                Toast.makeText(context, "Notification preferences updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSecurityDialog) {
        SecuritySettingsDialog(
            appPreferences = appPreferences,
            onDismiss = { showSecurityDialog = false },
            onSecurityUpdated = {
                showSecurityDialog = false
                isBiometricActive = appPreferences.isBiometricEnabled()
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showTermsDialog) {
        TermsAndPolicyDialog(onDismiss = { showTermsDialog = false })
    }

    if (showUpdateDialog) {
        AppUpdateDialog(
            appPreferences = appPreferences,
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (showClearCacheConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmDialog = false },
            title = { Text("Clear App Cache", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = { Text("This will purge cached candles, temporary market data, and stale feed buffers. Your credentials and trading preferences will remain safe.", color = TextGray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheConfirmDialog = false
                        viewModel?.refreshMarketData()
                        Toast.makeText(context, "Market Cache Cleared Successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("Clear Cache", color = Color.Black, fontWeight = FontWeight.Bold)
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
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold, color = LossRed) },
            text = { Text("Are you sure you want to disconnect all active brokers and log out of King Khan AI?", color = TextGray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
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
}

// =========================================================================
// HELPER COMPOSABLES FOR PROFILE SCREEN
// =========================================================================

@Composable
fun SettingMenuGridCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCardSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = TextGray,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun BrokerStatusRow(
    name: String,
    subtitle: String,
    status: BrokerAuthStatus,
    statusMessage: String,
    onConnect: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRemoveAccount: () -> Unit
) {
    val isConnected = status == BrokerAuthStatus.CONNECTED
    val isPending = status == BrokerAuthStatus.AUTHENTICATION_REQUIRED || status == BrokerAuthStatus.CONFIGURE || status == BrokerAuthStatus.STANDBY
    val isError = status == BrokerAuthStatus.ERROR

    val statusColor = when {
        isConnected -> ProfitGreen
        isPending -> SecondaryGold
        isError -> LossRed
        else -> TextGray
    }

    val statusLabel = when (status) {
        BrokerAuthStatus.CONNECTED -> "CONNECTED"
        BrokerAuthStatus.AUTHENTICATION_REQUIRED -> "AUTH NEEDED"
        BrokerAuthStatus.CONFIGURE -> "CONFIGURE"
        BrokerAuthStatus.STANDBY -> "STANDBY"
        BrokerAuthStatus.ERROR -> "ERROR"
        BrokerAuthStatus.DISCONNECTED -> "DISCONNECTED"
        BrokerAuthStatus.NOT_CONFIGURED -> "OFFLINE"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isConnected) ProfitGreen.copy(alpha = 0.3f) else DarkCardBorder,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(2).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isConnected) PrimaryGold else TextGray
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = TextGray
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(0.5.dp, statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            if (statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMessage,
                    fontSize = 10.sp,
                    color = TextGray,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DividerDark)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConnected) {
                    TextButton(
                        onClick = onReconnect,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("RECONNECT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = onDisconnect,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("DISCONNECT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                    }
                } else {
                    TextButton(
                        onClick = onRemoveAccount,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("REMOVE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextGray)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onConnect,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("CONNECT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}
