package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.KingKhanBottomBar
import com.example.ui.components.OrderDialog
import com.example.ui.screens.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.KingKhanTheme
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme ?: ""
        val host = uri.host ?: ""
        val fullUrl = uri.toString()
        android.util.Log.d("OAuthCallback", "MainActivity handleIntent deep link received: $fullUrl")

        // 1. Dhan OAuth check
        val genericTokenId = uri.getQueryParameter("tokenId") ?: uri.getQueryParameter("consentId")
        val callbackState = (uri.getQueryParameter("state") ?: "").trim()
        val isDhan = (viewModel.sessionManager.pendingOAuthSession?.provider?.equals("DHAN", ignoreCase = true) == true) ||
                callbackState.startsWith("dhan_") ||
                (!genericTokenId.isNullOrBlank() && scheme == "kingkhan") ||
                (host.contains("kingkhan") && genericTokenId != null) ||
                fullUrl.contains("dhan", ignoreCase = true)

        if (isDhan) {
            viewModel.handleOAuthRedirect(uri)
            return
        }

        // 2. Extract authorization code or token from URI
        val fyersAuthCode = uri.getQueryParameter("auth_code")
        val genericCode = uri.getQueryParameter("code")
        val token = uri.getQueryParameter("access_token") ?: uri.getQueryParameter("token")

        val code = fyersAuthCode
            ?: (if (genericCode != null && genericCode != "200" && genericCode != "0") genericCode else null)
            ?: token
            ?: Regex("""[?&#](?:auth_code|code|access_token|token)=([^&#]+)""", RegexOption.IGNORE_CASE)
                .find(fullUrl)?.groupValues?.get(1)

        val pendingProvider = viewModel.sessionManager.pendingOAuthSession?.provider?.uppercase()?.trim()
        val connectingBroker = viewModel.connectingBrokerName.value?.uppercase()?.trim()

        // 3. Check if Fyers OAuth callback
        val isFyers = (fyersAuthCode != null) ||
                callbackState.startsWith("fyers_") ||
                pendingProvider == "FYERS" ||
                connectingBroker == "FYERS" ||
                fullUrl.contains("fyers", ignoreCase = true)

        // 4. Check if Upstox OAuth callback
        val isUpstox = !isFyers && (
                callbackState.startsWith("upstox_") ||
                pendingProvider == "UPSTOX" ||
                connectingBroker == "UPSTOX" ||
                fullUrl.contains("upstox", ignoreCase = true) ||
                (!code.isNullOrBlank() && scheme == "kingkhan") ||
                (!code.isNullOrBlank() && (host.contains("application-beige-psi.vercel.app") || fullUrl.contains("application-beige-psi.vercel.app")))
        )

        if (isFyers && !code.isNullOrBlank()) {
            android.util.Log.i("OAuthCallback", "Fyers OAuth callback captured automatically, connecting with auth code...")
            viewModel.connectFyersWithAuthCode(code)
        } else if (isUpstox && !code.isNullOrBlank()) {
            android.util.Log.i("OAuthCallback", "Upstox OAuth callback captured automatically, connecting with auth code...")
            val upstoxKey = viewModel.sessionManager.upstoxApiKey.takeIf { it.isNotBlank() }
                ?: com.example.util.BrokerConfig.upstoxApiKey
            val upstoxSecret = viewModel.sessionManager.upstoxApiSecret.takeIf { it.isNotBlank() }
                ?: com.example.util.BrokerConfig.upstoxApiSecret
            viewModel.connectUpstox(upstoxKey, upstoxSecret, code)
        } else {
            viewModel.handleOAuthRedirect(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("KingKhanApp", "MainActivity starting...")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        handleIntent(intent)

        setContent {
            KingKhanTheme {
                val appPreferences = remember { com.example.util.AppPreferences(applicationContext) }
                var isAppUnlocked by remember { mutableStateOf(!appPreferences.isAppLockEnabled()) }

                if (!isAppUnlocked && appPreferences.isAppLockEnabled()) {
                    AppLockScreen(
                        appPreferences = appPreferences,
                        onUnlocked = { isAppUnlocked = true }
                    )
                } else {
                    val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "splash"

                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
                val orders by viewModel.orders.collectAsStateWithLifecycle()
                val holdings by viewModel.holdings.collectAsStateWithLifecycle()
                val aiSignals by viewModel.aiSignals.collectAsStateWithLifecycle()
                val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
                val optionStrikes by viewModel.optionStrikes.collectAsStateWithLifecycle()
                val selectedOptionIndex by viewModel.selectedOptionIndex.collectAsStateWithLifecycle()
                val availableOptionExpiries by viewModel.availableOptionExpiries.collectAsStateWithLifecycle()
                val selectedOptionExpiry by viewModel.selectedOptionExpiry.collectAsStateWithLifecycle()
                val marketDataSource by viewModel.marketDataSource.collectAsStateWithLifecycle()
                val marketDataLastUpdated by viewModel.marketDataLastUpdated.collectAsStateWithLifecycle()
                val apiError by viewModel.apiError.collectAsStateWithLifecycle()
                val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

                val telegramBotToken by viewModel.telegramBotToken.collectAsStateWithLifecycle()
                val telegramChatId by viewModel.telegramChatId.collectAsStateWithLifecycle()
                val telegramChannelId by viewModel.telegramChannelId.collectAsStateWithLifecycle()
                val isTelegramAlertsEnabled by viewModel.isTelegramAlertsEnabled.collectAsStateWithLifecycle()
                val isTelegramTesting by viewModel.isTelegramTesting.collectAsStateWithLifecycle()
                val telegramResponseInfo by viewModel.telegramResponseInfo.collectAsStateWithLifecycle()

                val alertPreferences by viewModel.alertPreferences.collectAsStateWithLifecycle()
                val isSmsAlertsEnabled by viewModel.isSmsAlertsEnabled.collectAsStateWithLifecycle()
                val smsAlertPhone by viewModel.smsAlertPhone.collectAsStateWithLifecycle()
                val smsGatewayUrl by viewModel.smsGatewayUrl.collectAsStateWithLifecycle()
                val smsApiKey by viewModel.smsApiKey.collectAsStateWithLifecycle()
                val isSmsTesting by viewModel.isSmsTesting.collectAsStateWithLifecycle()
                val smsStatusMessage by viewModel.smsStatusMessage.collectAsStateWithLifecycle()

                val showConnectDialog by viewModel.showConnectDialog.collectAsStateWithLifecycle()
                val connectingBrokerName by viewModel.connectingBrokerName.collectAsStateWithLifecycle()
                val isAuthInProgress by viewModel.isAuthInProgress.collectAsStateWithLifecycle()
                val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
                val authSuccessEvent by viewModel.authSuccessEvent.collectAsStateWithLifecycle()
                
                val topLevelBrokerStatuses by viewModel.brokerStatuses.collectAsStateWithLifecycle()
                val topLevelProviderHealth by viewModel.brokerAuthManager.providerHealth.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                var autoUpdateBannerInfo by remember { mutableStateOf<com.example.util.update.UpdateInfo?>(null) }
                val updateManager = remember { com.example.util.update.UpdateManager(applicationContext) }
                val updateDownloadState by updateManager.downloadState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    val postInstall = updateManager.checkAndVerifyInstalledVersion(appPreferences)
                    if (postInstall.isUpdated) {
                        snackbarHostState.showSnackbar(postInstall.message)
                    }

                    if (appPreferences.isAutoCheckUpdateEnabled()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val res = updateManager.checkForUpdates()
                                if (res is com.example.util.update.UpdateCheckResult.UpdateAvailable) {
                                    autoUpdateBannerInfo = res.info
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("MainActivity", "Auto-check update failed safely: ${e.localizedMessage}")
                            }
                        }
                    }
                }

                val tabRoutes = remember {
                    listOf("home", "market", "ai_signals", "orders", "algo", "profile")
                }
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { tabRoutes.size }
                )

                LaunchedEffect(authSuccessEvent) {
                    if (authSuccessEvent) {
                        if (currentRoute != "main") {
                            navController.navigate("main") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            }
                        }
                        viewModel.consumeAuthSuccessEvent()
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    viewModel.refreshBrokerData()
                }

                val brokerSwitchStatus by viewModel.brokerSwitchStatus.collectAsStateWithLifecycle()
                LaunchedEffect(brokerSwitchStatus) {
                    brokerSwitchStatus?.let { status ->
                        snackbarHostState.showSnackbar(status)
                        viewModel.clearBrokerSwitchStatus()
                    }
                }

                val notifications by viewModel.notifications.collectAsStateWithLifecycle()

                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                            if (appPreferences.isAppLockEnabled()) {
                                isAppUnlocked = false
                            }
                        } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            viewModel.onAppResume()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                var orderDialogState by remember { mutableStateOf<Triple<String, String, Pair<Double?, Int?>?>?>(null) } // symbol, side, (price, lot)
                var showNotificationCenter by remember { mutableStateOf(false) }

                val tabHistory = remember { mutableStateListOf<Int>(0) }

                LaunchedEffect(pagerState.currentPage) {
                    if (tabHistory.isEmpty() || tabHistory.last() != pagerState.currentPage) {
                        tabHistory.add(pagerState.currentPage)
                    }
                }

                val isMainPagerActive = currentRoute == "main" || currentRoute in tabRoutes
                val showBottomBar = isMainPagerActive || currentRoute == "portfolio"
                val activeBarRoute = if (isMainPagerActive) tabRoutes[pagerState.currentPage] else currentRoute

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            KingKhanBottomBar(
                                currentRoute = activeBarRoute,
                                onNavigate = { route ->
                                    val targetIndex = tabRoutes.indexOf(route)
                                    if (targetIndex != -1) {
                                        if (currentRoute != "main" && currentRoute !in tabRoutes) {
                                            navController.navigate("main") {
                                                popUpTo("main") { inclusive = true }
                                            }
                                        }
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(targetIndex)
                                        }
                                    } else {
                                        navController.navigate(route) {
                                            popUpTo("main") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        apiError?.let { err ->
                            com.example.ui.components.ApiErrorBanner(
                                errorMessage = err,
                                onRetry = { viewModel.refreshBrokerData() }
                            )
                        }

                        autoUpdateBannerInfo?.let { updateInfo ->
                            androidx.compose.material3.Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = com.example.ui.theme.DarkCardSecondary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.ProfitGreen)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val bannerText = when (val state = updateDownloadState) {
                                                is com.example.util.update.DownloadState.Downloading ->
                                                    "🚀 Auto-updating v${updateInfo.versionName} (${state.progress}%)..."
                                                is com.example.util.update.DownloadState.Completed ->
                                                    "✅ Update v${updateInfo.versionName} ready to install"
                                                is com.example.util.update.DownloadState.Error ->
                                                    "⚠️ Update download failed: ${state.message.take(40)}"
                                                com.example.util.update.DownloadState.Idle ->
                                                    "🚀 New Update v${updateInfo.versionName} available"
                                            }
                                            androidx.compose.material3.Text(
                                                bannerText,
                                                fontSize = 11.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = com.example.ui.theme.ProfitGreen
                                            )
                                        }
                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            when (val state = updateDownloadState) {
                                                is com.example.util.update.DownloadState.Completed -> {
                                                    androidx.compose.material3.TextButton(
                                                        onClick = { updateManager.installApk(state.apkFile) },
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        androidx.compose.material3.Text(
                                                            "INSTALL",
                                                            fontSize = 11.sp,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            color = com.example.ui.theme.ProfitGreen
                                                        )
                                                    }
                                                }
                                                is com.example.util.update.DownloadState.Error -> {
                                                    androidx.compose.material3.TextButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                updateManager.autoDownloadAndInstall(updateInfo)
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        androidx.compose.material3.Text(
                                                            "RETRY",
                                                            fontSize = 11.sp,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            color = com.example.ui.theme.SecondaryGold
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    androidx.compose.material3.TextButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                updateManager.autoDownloadAndInstall(updateInfo)
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        androidx.compose.material3.Text(
                                                            "UPDATE",
                                                            fontSize = 11.sp,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            color = com.example.ui.theme.ProfitGreen
                                                        )
                                                    }
                                                }
                                            }
                                            androidx.compose.material3.IconButton(
                                                onClick = { autoUpdateBannerInfo = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                androidx.compose.material3.Icon(
                                                    androidx.compose.material.icons.Icons.Default.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = com.example.ui.theme.TextGray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (updateDownloadState is com.example.util.update.DownloadState.Downloading) {
                                        val progress = (updateDownloadState as com.example.util.update.DownloadState.Downloading).progress
                                        Spacer(modifier = Modifier.height(4.dp))
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { progress / 100f },
                                            modifier = Modifier.fillMaxWidth().height(4.dp),
                                            color = com.example.ui.theme.ProfitGreen,
                                            trackColor = com.example.ui.theme.DarkCardBorder
                                        )
                                    }
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = "splash",
                            modifier = Modifier.weight(1f)
                        ) {
                            composable("splash") {
                                val isSessionRestoring by viewModel.isSessionRestoring.collectAsStateWithLifecycle()
                                SplashScreen(
                                    isRestoring = isSessionRestoring,
                                    onSplashFinished = {
                                        val isDisclaimerAccepted = appPreferences.isDisclaimerAccepted()
                                        val startRoute = when {
                                            !isDisclaimerAccepted -> "disclaimer"
                                            viewModel.isSessionActive() -> "main"
                                            else -> "login"
                                        }
                                        navController.navigate(startRoute) {
                                            popUpTo("splash") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable("disclaimer") {
                                DisclaimerScreen(
                                    appPreferences = appPreferences,
                                    onAgreeAndContinue = {
                                        val startRoute = if (viewModel.isSessionActive()) "main" else "login"
                                        navController.navigate(startRoute) {
                                            popUpTo("disclaimer") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable("login") {
                                val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
                                androidx.activity.compose.BackHandler(enabled = true) {
                                    activity?.moveTaskToBack(true)
                                }

                                LoginScreen(
                                    onConnectBroker = { broker ->
                                        viewModel.openConnectDialog(broker)
                                    },
                                    onSkipLogin = {
                                        navController.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable("main") {
                                val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
                                androidx.activity.compose.BackHandler(enabled = true) {
                                    when {
                                        orderDialogState != null -> {
                                            orderDialogState = null
                                        }
                                        showNotificationCenter -> {
                                            showNotificationCenter = false
                                        }
                                        showConnectDialog -> {
                                            viewModel.closeConnectDialog()
                                        }
                                        tabHistory.size > 1 -> {
                                            tabHistory.removeAt(tabHistory.lastIndex)
                                            val prevPage = tabHistory.lastOrNull() ?: 0
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(prevPage)
                                            }
                                        }
                                        pagerState.currentPage != 0 -> {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(0)
                                            }
                                        }
                                        else -> {
                                            activity?.moveTaskToBack(true)
                                        }
                                    }
                                }

                                LaunchedEffect(pagerState.currentPage) {
                                    viewModel.refreshBrokerData()
                                }

                                HorizontalPager(
                                    state = pagerState,
                                    beyondViewportPageCount = 1,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    when (tabRoutes[page]) {
                                        "home" -> HomeScreen(
                                            userProfile = userProfile,
                                            orders = orders,
                                            aiSignals = aiSignals,
                                            apiError = apiError,
                                            watchlist = watchlist,
                                            notifications = notifications,
                                            marketDataSource = marketDataSource,
                                            marketDataLastUpdated = marketDataLastUpdated,
                                            viewModel = viewModel,
                                            onNavigateToOrders = {
                                                coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                            },
                                            onNavigateToMarket = {
                                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                            },
                                            onNavigateToAISignals = {
                                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                            },
                                            onNavigateToProfile = {
                                                coroutineScope.launch { pagerState.animateScrollToPage(5) }
                                            },
                                            onNavigateToIndexDetails = { exchange, indexName ->
                                                val encodedIndexName = android.net.Uri.encode(indexName)
                                                navController.navigate("index_details/$exchange/$encodedIndexName")
                                            },
                                            onOpenNotificationCenter = { showNotificationCenter = true },
                                            onOpenOrderDialog = { symbol, side, price, lot ->
                                                orderDialogState = Triple(symbol, side, price to lot)
                                            },
                                            onRefresh = { viewModel.refreshBrokerData() }
                                        )
                                        "market" -> MarketScreen(
                                            userProfile = userProfile,
                                            watchlist = watchlist,
                                            notifications = notifications,
                                            recentSearches = recentSearches,
                                            marketDataSource = marketDataSource,
                                            marketDataLastUpdated = marketDataLastUpdated,
                                            onToggleFavorite = { symbol, currentStatus -> viewModel.toggleFavorite(symbol, currentStatus) },
                                            onAddRecentSearch = { query -> viewModel.addRecentSearch(query) },
                                            onClearRecentSearches = { viewModel.clearRecentSearches() },
                                            onOpenNotificationCenter = { showNotificationCenter = true },
                                            onOpenOrderDialog = { symbol, side, price, lot ->
                                                orderDialogState = Triple(symbol, side, price to lot)
                                            },
                                            onAddSymbolToWatchlist = { symbol, exchange ->
                                                viewModel.addSymbolToWatchlist(symbol, exchange)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Added $symbol ($exchange) to Watchlist")
                                                }
                                            },
                                            onNavigateToIndexDetails = { exchange, indexName ->
                                                val encodedIndexName = android.net.Uri.encode(indexName)
                                                navController.navigate("index_details/$exchange/$encodedIndexName")
                                            },
                                            onGetHistoricalCandles = { symbol, interval, callback ->
                                                viewModel.getHistoricalCandlesForIndex(symbol, interval, callback)
                                            },
                                            isRefreshing = isRefreshing,
                                            onRefresh = {
                                                viewModel.refreshMarketData()
                                            }
                                        )
                                        "ai_signals" -> AISignalsScreen(
                                            userProfile = userProfile,
                                            signals = aiSignals,
                                            notifications = notifications,
                                            marketDataSource = marketDataSource,
                                            appPreferences = appPreferences,
                                            onOpenNotificationCenter = { showNotificationCenter = true },
                                            onExecuteSignal = { signal ->
                                                orderDialogState = Triple(
                                                    "${signal.symbol} ${signal.actionType}",
                                                    "BUY",
                                                    signal.ltp to signal.lotSize
                                                )
                                            },
                                            onSendToTelegram = { signal ->
                                                viewModel.sendSignalToTelegram(signal)
                                            },
                                            onRefresh = { viewModel.refreshBrokerData() }
                                        )
                                        "orders" -> OrdersScreen(
                                            userProfile = userProfile,
                                            orders = orders,
                                            positions = holdings,
                                            watchlist = watchlist,
                                            notifications = notifications,
                                            availableMargin = userProfile.availableMargin,
                                            marketDataSource = marketDataSource,
                                            onOpenNotificationCenter = { showNotificationCenter = true },
                                            onOpenOrderDialog = { symbol, side, price, lot ->
                                                orderDialogState = Triple(symbol, side, price to lot)
                                            },
                                            onCancelOrder = { id ->
                                                viewModel.cancelOrder(id)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Order $id cancelled successfully")
                                                }
                                            },
                                            onModifyOrder = { id, price, qty, type, sl, tg ->
                                                viewModel.modifyOrder(id, price, qty, type, sl, tg)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Order $id modified successfully")
                                                }
                                            },
                                            onExitPosition = { id, exitPrice, pnl ->
                                                viewModel.exitPosition(id, exitPrice, pnl)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Position $id squared off at ₹$exitPrice")
                                                }
                                            },
                                            onPartialExitPosition = { id, lots, exitPrice, pnl ->
                                                viewModel.partialExitPosition(id, lots, exitPrice, pnl)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Exited $lots lots for $id at ₹$exitPrice")
                                                }
                                            },
                                            onUpdateStopLossTarget = { id, newSl, newTarget ->
                                                viewModel.updateStopLossTarget(id, newSl, newTarget)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Stop Loss & Target updated for $id")
                                                }
                                            },
                                            onSendToTelegram = { order ->
                                                viewModel.sendBrokerOrderToTelegram(order)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Order ${order.orderId} transmitted to Telegram & Alerts 🚀")
                                                }
                                            },
                                            isRefreshing = isRefreshing,
                                            onRefresh = { viewModel.refreshBrokerData() }
                                        )
                                        "algo" -> AlgoScreen(
                                            viewModel = viewModel,
                                            notifications = notifications,
                                            onOpenNotificationCenter = { showNotificationCenter = true },
                                            onNavigateToAISignals = {
                                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                            },
                                            isRefreshing = isRefreshing,
                                            onRefresh = { viewModel.refreshBrokerData() }
                                        )
                                        "profile" -> {
                                            val brokerStatuses by viewModel.brokerStatuses.collectAsStateWithLifecycle()
                                            ProfileScreen(
                                                userProfile = userProfile,
                                                orders = orders,
                                                holdings = holdings,
                                                notifications = notifications,
                                                marketDataSource = marketDataSource,
                                                appPreferences = remember { com.example.util.AppPreferences(applicationContext) },
                                                viewModel = viewModel,
                                                brokerStatuses = brokerStatuses,
                                                onSwitchBroker = { broker ->
                                                    viewModel.openConnectDialog(broker)
                                                },
                                                onReconnectBroker = { broker ->
                                                    viewModel.openConnectDialog(broker)
                                                },
                                                onDisconnectBroker = { broker ->
                                                    viewModel.disconnectBroker(broker)
                                                },
                                                onToggleBiometric = { enabled -> viewModel.toggleBiometric(enabled) },
                                                onRemoveAccountBroker = { broker -> viewModel.removeAccountBroker(broker) },
                                                onNavigateToTelegramSettings = { navController.navigate("telegram_settings") },
                                                onNavigateToHealthAutoFix = { navController.navigate("health_autofix") },
                                                onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                                                onOpenNotificationCenter = { showNotificationCenter = true },
                                                onLogout = {
                                                    viewModel.logout()
                                                    navController.navigate("login") {
                                                        popUpTo("main") { inclusive = true }
                                                    }
                                                },
                                                isRefreshing = isRefreshing,
                                                onRefresh = { viewModel.refreshBrokerData() }
                                            )
                                        }
                                    }
                                }
                            }

                            // Keep backward-compatible individual routes mapping to main pager tab indices
                            composable("home") {
                                LaunchedEffect(Unit) {
                                    pagerState.scrollToPage(0)
                                    navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                }
                            }
                            composable("market") {
                                LaunchedEffect(Unit) {
                                    pagerState.scrollToPage(1)
                                    navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                }
                            }
                            composable("orders") {
                                LaunchedEffect(Unit) {
                                    pagerState.scrollToPage(3)
                                    navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                }
                            }
                            composable("algo") {
                                LaunchedEffect(Unit) {
                                    pagerState.scrollToPage(4)
                                    navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                }
                            }
                            composable("health_autofix") {
                                HealthAndAutoFixScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable("profile") {
                                LaunchedEffect(Unit) {
                                    pagerState.scrollToPage(5)
                                    navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                }
                            }
                            composable("ai_signals") {
                                LaunchedEffect(Unit) {
                                    pagerState.scrollToPage(2)
                                    navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                }
                            }

                            composable("portfolio") {
                                androidx.activity.compose.BackHandler {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                    }
                                }
                                PortfolioScreen(
                                    holdings = holdings,
                                    userProfile = userProfile,
                                    marketDataSource = marketDataSource,
                                    isRefreshing = isRefreshing,
                                    onRefresh = { viewModel.refreshBrokerData() },
                                    onNavigateToPositions = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                        navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                    },
                                    onNavigateToOrders = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                        navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                    }
                                )
                            }

                            composable(
                                "index_details/{exchange}/{indexName}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("exchange") { type = androidx.navigation.NavType.StringType },
                                    androidx.navigation.navArgument("indexName") { type = androidx.navigation.NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val exchange = backStackEntry.arguments?.getString("exchange") ?: "NSE"
                                val indexName = backStackEntry.arguments?.getString("indexName") ?: "NIFTY 50"
                                
                                androidx.activity.compose.BackHandler {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                    }
                                }

                                IndexDetailsScreen(
                                    exchange = exchange,
                                    indexName = indexName,
                                    viewModel = viewModel,
                                    onBack = { 
                                        if (!navController.popBackStack()) {
                                            navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                        }
                                    },
                                    onOpenOrderDialog = { symbol, side, price, lot ->
                                        orderDialogState = Triple(symbol, side, price to lot)
                                    }
                                )
                            }

                            composable("telegram_settings") {
                                androidx.activity.compose.BackHandler {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                    }
                                }
                                TelegramSettingsScreen(
                                    botToken = telegramBotToken,
                                    chatId = telegramChatId,
                                    channelId = telegramChannelId,
                                    isAlertsEnabled = isTelegramAlertsEnabled,
                                    isTesting = isTelegramTesting,
                                    telegramResponseInfo = telegramResponseInfo,
                                    alertPreferences = alertPreferences,
                                    isSmsEnabled = isSmsAlertsEnabled,
                                    smsPhone = smsAlertPhone,
                                    smsGatewayUrl = smsGatewayUrl,
                                    smsApiKey = smsApiKey,
                                    isSmsTesting = isSmsTesting,
                                    smsStatusMessage = smsStatusMessage,
                                    onBack = { 
                                        if (!navController.popBackStack()) {
                                            navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                        }
                                    },
                                    onSaveSettings = { token, chatId, enabled, channelId ->
                                        viewModel.saveTelegramSettings(token, chatId, enabled, channelId)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Telegram settings saved successfully")
                                        }
                                    },
                                    onToggleAlertEvent = { key, enabled ->
                                        viewModel.toggleAlertEvent(key, enabled)
                                    },
                                    onSaveSmsSettings = { phone, enabled, gw, key ->
                                        viewModel.saveSmsSettings(phone, enabled, gw, key)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("SMS alert settings saved successfully")
                                        }
                                    },
                                    onTestTelegramBot = { token, chatId, channelId ->
                                        viewModel.testTelegramBot(token, chatId, channelId)
                                    },
                                    onTestAlert = { alertType, symbol, details ->
                                        viewModel.testSpecificTelegramAlert(alertType, symbol, details)
                                    },
                                    onTestSms = { phone, msg ->
                                        viewModel.testSmsAlert(phone, msg)
                                    },
                                    onClearResponse = {
                                        viewModel.clearTelegramResponseInfo()
                                    },
                                    onClearSmsStatus = {
                                        viewModel.clearSmsStatus()
                                    }
                                )
                            }
                            
                            composable("diagnostics") {
                                androidx.activity.compose.BackHandler {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                    }
                                }
                                com.example.ui.screens.DiagnosticsScreen(
                                    viewModel = viewModel,
                                    onBack = { 
                                        if (!navController.popBackStack()) {
                                            navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                        }
                                    }
                                )
                            }
                        }

                        // Global Notification Center Dialog Overlay
                        if (showNotificationCenter) {
                            com.example.ui.components.NotificationCenterDialog(
                                userProfile = userProfile,
                                notifications = notifications,
                                onClearAll = { viewModel.clearNotifications() },
                                onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
                                onDismiss = { showNotificationCenter = false }
                            )
                        }

                        // Global Broker Authentication Dialog Overlay
                        if (showConnectDialog) {
                            com.example.ui.components.BrokerConnectDialog(
                                initialBroker = connectingBrokerName,
                                isAuthInProgress = isAuthInProgress,
                                errorMessage = authErrorMessage,
                                brokerStatuses = topLevelBrokerStatuses,
                                providerHealth = topLevelProviderHealth,
                                sessionManager = viewModel.sessionManager,
                                onDisconnect = { broker -> viewModel.disconnectBroker(broker) },
                                onReconnect = { broker -> viewModel.reconnectBroker(broker) },
                                onRemoveAccount = { broker -> viewModel.removeAccountBroker(broker) },
                                onDismiss = { viewModel.closeConnectDialog() },
                                onAngelLogin = { clientCode, mpin, apiKey, totpSecret -> viewModel.loginAngel(clientCode, mpin, apiKey, totpSecret) },
                                onDhanLogin = { clientId, accessToken -> viewModel.connectDhan(clientId, accessToken) },
                                onUpstoxLogin = { clientId, secret, codeOrToken -> viewModel.connectUpstox(clientId, secret, codeOrToken) },
                                onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) },
                                onOpenUpstoxLogin = { apiKey, secret -> viewModel.initiateUpstoxLogin(apiKey, secret, this@MainActivity) },
                                onOpenFyersLogin = { appId, secret -> viewModel.initiateFyersLogin(appId, secret, this@MainActivity) }
                            )
                        }

                        // Global Order Placement Dialog Overlay
                        orderDialogState?.let { (symbol, side, priceLot) ->
                            val (price, lot) = priceLot ?: (null to null)
                            val exchange = when {
                                symbol.contains("CRUDE") -> "MCX"
                                symbol.contains("SENSEX") || symbol.contains("BANKEX") -> "BSE"
                                else -> "NSE"
                            }
                            OrderDialog(
                                symbol = symbol,
                                initialSide = side,
                                initialPrice = price,
                                lotSize = lot,
                                appPreferences = appPreferences,
                                onDismiss = { orderDialogState = null },
                                onConfirmOrder = { orderSide, orderType, qty, orderPrice ->
                                    viewModel.placeNewOrder(
                                        symbol = symbol,
                                        exchange = exchange,
                                        side = orderSide,
                                        orderType = orderType,
                                        qty = qty,
                                        price = orderPrice
                                    )
                                    orderDialogState = null
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Order submitted: $orderSide $qty $symbol @ ₹$orderPrice")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
