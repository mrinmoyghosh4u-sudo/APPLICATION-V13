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
import androidx.compose.material.icons.filled.MedicalServices
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    marketDataSource: String = "",
    appPreferences: AppPreferences,
    brokerStatuses: Map<String, com.example.data.network.BrokerConnectionState> = emptyMap(),
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

    var isTelegramEnabled by remember { mutableStateOf(appPreferences.isTelegramEnabled()) }
    var isTestSending by remember { mutableStateOf(false) }
    var customSimulatedMargin by remember { mutableStateOf<Double?>(null) }

    // Stat calculations strictly from Dhan / Broker Orders
    val isDhanConnected = userProfile.isDhanConnected || brokerStatuses["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
    val isUpstoxConnected = brokerStatuses["Upstox"]?.let { it.status != com.example.data.network.BrokerAuthStatus.NOT_CONFIGURED && it.status != com.example.data.network.BrokerAuthStatus.DISCONNECTED && it.status != com.example.data.network.BrokerAuthStatus.ERROR } ?: false
    val isFyersConnected = brokerStatuses["Fyers"]?.let { it.status != com.example.data.network.BrokerAuthStatus.NOT_CONFIGURED && it.status != com.example.data.network.BrokerAuthStatus.DISCONNECTED && it.status != com.example.data.network.BrokerAuthStatus.ERROR } ?: false
    val isAngelConnected = userProfile.isAngelConnected || (brokerStatuses["Angel One"]?.let { it.status != com.example.data.network.BrokerAuthStatus.NOT_CONFIGURED && it.status != com.example.data.network.BrokerAuthStatus.DISCONNECTED && it.status != com.example.data.network.BrokerAuthStatus.ERROR } ?: false)
}