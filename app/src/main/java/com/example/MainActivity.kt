package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                val isTelegramAlertsEnabled by viewModel.isTelegramAlertsEnabled.collectAsStateWithLifecycle()
                val isTelegramTesting by viewModel.isTelegramTesting.collectAsStateWithLifecycle()
                val telegramResponseInfo by viewModel.telegramResponseInfo.collectAsStateWithLifecycle()

                val showConnectDialog by viewModel.showConnectDialog.collectAsStateWithLifecycle()
                val connectingBrokerName by viewModel.connectingBrokerName.collectAsStateWithLifecycle()
                val isAuthInProgress by viewModel.isAuthInProgress.collectAsStateWithLifecycle()
                val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
                val authSuccessEvent by viewModel.authSuccessEvent.collectAsStateWithLifecycle()

                var autoUpdateBannerInfo by remember { mutableStateOf<com.example.util.update.UpdateInfo?>(null) }

                LaunchedEffect(Unit) {
                    if (appPreferences.isAutoCheckUpdateEnabled()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val updateManager = com.example.util.update.UpdateManager(applicationContext)
                            val res = updateManager.checkForUpdates()
                            if (res is com.example.util.update.UpdateCheckResult.UpdateAvailable) {
                                autoUpdateBannerInfo = res.info
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
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                            popUpTo("splash") { inclusive = true }
                        }
                        viewModel.consumeAuthSuccessEvent()
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    viewModel.refreshBrokerData()
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

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
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        androidx.compose.material3.Text(
                                            "🚀 New Update v${updateInfo.versionName} available!",
                                            fontSize = 11.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = com.example.ui.theme.ProfitGreen
                                        )
                                    }
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                navController.navigate("main")
                                                autoUpdateBannerInfo = null
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            androidx.compose.material3.Text(
                                                "VIEW",
                                                fontSize = 11.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = com.example.ui.theme.SecondaryGold
                                            )
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
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("login") {
                                LoginScreen(
                                    onConnectBroker = { broker ->
                                        viewModel.openConnectDialog(broker)
                                    },
                                    onSkipLogin = {
                                        navController.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("main") {
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
                                            isRefreshing = isRefreshing,
                                            onRefresh = {
                                                viewModel.refreshMarketData()
                                            }
                                        )
                                        "ai_signals" -> AISignalsScreen(
                                            userProfile = userProfile,
                                            signals = aiSignals,
                                            appPreferences = appPreferences,
                                            onOpenNotificationCenter = { showNotificationCenter = true },
                                            onExecuteSignal = { signal ->
                                                orderDialogState = Triple(
                                                    "${signal.symbol} ${signal.actionType}",
                                                    "BUY",
                                                    signal.ltp to signal.lotSize
                                                )
                                            },
                                            onRefresh = { viewModel.refreshBrokerData() }
                                        )
                                        "orders" -> OrdersScreen(
                                            userProfile = userProfile,
                                            orders = orders,
                                            positions = holdings,
                                            watchlist = watchlist,
                                            availableMargin = userProfile.availableMargin,
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
                                            isRefreshing = isRefreshing,
                                            onRefresh = { viewModel.refreshBrokerData() }
                                        )
                                        "algo" -> AlgoScreen(
                                            viewModel = viewModel,
                                            onOpenNotificationCenter = { showNotificationCenter = true },
                                            onNavigateToAISignals = { navController.navigate("ai_signals") },
                                            isRefreshing = isRefreshing,
                                            onRefresh = { viewModel.refreshBrokerData() }
                                        )
                                        "profile" -> {
                                            val brokerStatuses by viewModel.brokerStatuses.collectAsStateWithLifecycle()
                                            ProfileScreen(
                                                userProfile = userProfile,
                                                orders = orders,
                                                holdings = holdings,
                                                appPreferences = remember { com.example.util.AppPreferences(applicationContext) },
                                                brokerStatuses = brokerStatuses,
                                                onSwitchBroker = { broker ->
                                                    viewModel.switchActiveBroker(broker)
                                                },
                                                onReconnectBroker = { broker ->
                                                    viewModel.reconnectBroker(broker)
                                                },
                                                onDisconnectBroker = { broker ->
                                                    viewModel.disconnectBroker(broker)
                                                },
                                                onToggleBiometric = { enabled -> viewModel.toggleBiometric(enabled) },
                                                onRemoveAccountBroker = { broker -> viewModel.removeAccountBroker(broker) },
                                                onNavigateToTelegramSettings = { navController.navigate("telegram_settings") },
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
                            composable("profile") {
                                LaunchedEffect(Unit) {
                                    pagerState.scrollToPage(5)
                                    navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                }
                            }
                            composable("ai_signals") {
                                val aiSignals by viewModel.aiSignals.collectAsStateWithLifecycle()
                                AISignalsScreen(
                                    userProfile = userProfile,
                                    signals = aiSignals,
                                    onOpenNotificationCenter = { showNotificationCenter = true },
                                    onExecuteSignal = { signal ->
                                        orderDialogState = Triple(
                                            "${signal.symbol} ${signal.actionType}",
                                            "BUY",
                                            signal.ltp to signal.lotSize
                                        )
                                    },
                                    onRefresh = { viewModel.refreshBrokerData() }
                                )
                            }

                            composable("portfolio") {
                                PortfolioScreen(
                                    holdings = holdings,
                                    userProfile = userProfile,
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
                                
                                IndexDetailsScreen(
                                    exchange = exchange,
                                    indexName = indexName,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenOrderDialog = { symbol, side, price, lot ->
                                        orderDialogState = Triple(symbol, side, price to lot)
                                    }
                                )
                            }

                            composable("telegram_settings") {
                                TelegramSettingsScreen(
                                    botToken = telegramBotToken,
                                    chatId = telegramChatId,
                                    isAlertsEnabled = isTelegramAlertsEnabled,
                                    isTesting = isTelegramTesting,
                                    telegramResponseInfo = telegramResponseInfo,
                                    onBack = { navController.popBackStack() },
                                    onSaveSettings = { token, chatId, enabled ->
                                        viewModel.saveTelegramSettings(token, chatId, enabled)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Telegram settings saved successfully")
                                        }
                                    },
                                    onTestTelegramBot = { token, chatId ->
                                        viewModel.testTelegramBot(token, chatId)
                                    },
                                    onTestAlert = { alertType, symbol, details ->
                                        viewModel.testSpecificTelegramAlert(alertType, symbol, details)
                                    },
                                    onClearResponse = {
                                        viewModel.clearTelegramResponseInfo()
                                    }
                                )
                            }
                            
                            composable("diagnostics") {
                                com.example.ui.screens.DiagnosticsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
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
                                onDismiss = { viewModel.closeConnectDialog() },
                                onAngelLogin = { clientCode, mpin, apiKey, totpSecret -> viewModel.loginAngel(clientCode, mpin, apiKey, totpSecret) },
                                onMStockLogin = { clientCode, apiKey, totpSecret -> viewModel.connectMStock(clientCode, apiKey, totpSecret) },
                                onTradeSmartLogin = { apiKey, clientId, token -> viewModel.connectTradeSmart(apiKey, clientId, token) }
                            )
                        }

                        // Global Order Dialog Overlay
                        orderDialogState?.let { (symbol, side, extra) ->
                            OrderDialog(
                                symbol = symbol,
                                initialSide = side,
                                initialPrice = extra?.first,
                                lotSize = extra?.second,
                                appPreferences = appPreferences,
                                onDismiss = { orderDialogState = null },
                                onConfirmOrder = { newSide, orderType, qty, price ->
                                    viewModel.placeNewOrder(symbol, "NSE", newSide, orderType, qty, price)
                                    orderDialogState = null
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Order Placed: $newSide $qty x $symbol @ ₹$price")
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

    override fun onResume() {
        super.onResume()
        viewModel.onAppResume()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val uri = intent?.data
        if (uri != null) {
            val scheme = uri.scheme ?: ""
            val host = uri.host ?: ""
            if (scheme == "kingkhan" || host.contains("kingkhan") || host.contains("application-beige-psi.vercel.app") || host.contains("vercel.app")) {
                viewModel.handleOAuthRedirect(uri)
            }
        }
    }
}
