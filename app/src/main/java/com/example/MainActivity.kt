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
                fullUrl.contains("auth_code", ignoreCase = true) ||
                callbackState.startsWith("fyers_") ||
                pendingProvider == "FYERS" ||
                connectingBroker == "FYERS" ||
                fullUrl.contains("fyers", ignoreCase = true) ||
                viewModel.sessionManager.pendingFyersOAuthState.isNotBlank() ||
                ( (host.contains("application-beige-psi.vercel.app") || scheme == "kingkhan" || host == "oauth" || uri.path?.contains("oauth") == true) &&
                  !callbackState.startsWith("upstox_") && !callbackState.startsWith("dhan_") &&
                  pendingProvider != "UPSTOX" && pendingProvider != "DHAN" &&
                  connectingBroker != "UPSTOX" && connectingBroker != "DHAN" )

        // 4. Check if Upstox OAuth callback
        val isUpstox = !isFyers && (
                callbackState.startsWith("upstox_") ||
                pendingProvider == "UPSTOX" ||
                connectingBroker == "UPSTOX" ||
                fullUrl.contains("upstox", ignoreCase = true)
        )

        // 5. If not Dhan and Upstox/Fyers/Upstox/Angel logic
        if (isUpstox) {
            // Pass URI directly to ViewModel for Upstox handling
            viewModel.handleOAuthRedirect(uri)
            return
        }

        // If code present and not handled above, pass to generic handler
        if (!code.isNullOrBlank()) {
            viewModel.handleOAuthRedirect(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            KingKhanTheme(darkTheme = true) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                val snackbarHost = { SnackbarHost(hostState = snackbarHostState) }

                val appState by viewModel.appState.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    snackbarHost = snackbarHost
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        NavHost(
                            navController = navController,
                            startDestination = "splash",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("splash") { SplashScreen(navController = navController) }
                            composable("main") { MainScreen(navController = navController) }
                            composable("settings") { SettingsScreen(navController = navController) }
                            composable("orders") { OrdersScreen(navController = navController) }
                            composable("watchlist") { WatchlistScreen(navController = navController) }
                        }

                        // Global dialogs and overlays preserved by ViewModel state
                        val showConnectDialog by viewModel.showConnectDialog.collectAsStateWithLifecycle()
                        val connectingBrokerName by viewModel.connectingBrokerName.collectAsStateWithLifecycle()
                        val isAuthInProgress by viewModel.isAuthInProgress.collectAsStateWithLifecycle()
                        val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
                        val topLevelBrokerStatuses by viewModel.topLevelBrokerStatuses.collectAsStateWithLifecycle()
                        val topLevelProviderHealth by viewModel.topLevelProviderHealth.collectAsStateWithLifecycle()
                        val oauthWebViewState by viewModel.oauthWebViewState.collectAsStateWithLifecycle()
                        val orderDialogState by viewModel.orderDialogState.collectAsStateWithLifecycle()
                        val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                        val notifications by viewModel.notifications.collectAsStateWithLifecycle()
                        val appPreferences by viewModel.appPreferences.collectAsStateWithLifecycle()

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
                                onOpenFyersLogin = { appId, secret -> viewModel.initiateFyersLogin(appId, secret, this@MainActivity) },
                                onOpenDhanLogin = { clientId -> viewModel.initiateDhanOAuth(clientId) }
                            )
                        }

                        oauthWebViewState?.let { webViewState ->
                            com.example.ui.components.OAuthWebViewDialog(
                                url = webViewState.url,
                                brokerName = webViewState.brokerName,
                                onRedirectCaptured = { uri ->
                                    viewModel.closeOAuthWebView()
                                    viewModel.handleOAuthRedirect(uri)
                                },
                                onDismiss = {
                                    viewModel.closeOAuthWebView()
                                }
                            )
                        }

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
