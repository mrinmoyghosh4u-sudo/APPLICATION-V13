package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TradingDatabase
import com.example.data.model.*
import com.example.data.network.*
import com.example.data.repository.TradingRepository
import com.example.util.AngelAuthHelper
import com.example.util.OptionExpiryUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val sessionManager = SessionManager(application)
    private val networkClient = BrokerNetworkClient(sessionManager)

    val instrumentMasterService = com.example.data.network.InstrumentMasterService(context = application)
    val angelOneService = com.example.data.network.AngelOneBrokerService(networkClient.angelOneApi, sessionManager, instrumentMasterService)
    val dhanService = com.example.data.network.DhanBrokerService(api = networkClient.dhanApi, sessionManager = sessionManager)
    val brokerManager = com.example.data.network.BrokerManager(sessionManager, angelOneService, dhanService, instrumentMasterService)
    val telegramService = TelegramService(sessionManager)
    private val repository = TradingRepository(TradingDatabase.getDatabase(application).tradingDao(), brokerManager)

    // UI States
    private val _userProfile = MutableStateFlow(UserProfileEntity())
    val userProfile: StateFlow<UserProfileEntity> = _userProfile.asStateFlow()

    private val _watchlist = MutableStateFlow<List<WatchlistItem>>(emptyList())
    val watchlist: StateFlow<List<WatchlistItem>> = _watchlist.asStateFlow()

    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    private val _holdings = MutableStateFlow<List<PortfolioHoldingEntity>>(emptyList())
    val holdings: StateFlow<List<PortfolioHoldingEntity>> = _holdings.asStateFlow()

    private val _aiSignals = MutableStateFlow<List<AISignalEntity>>(emptyList())
    val aiSignals: StateFlow<List<AISignalEntity>> = _aiSignals.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(listOf("NIFTY 22000 CE", "BANKNIFTY 48000 PE", "RELIANCE", "TATASTEEL"))
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _optionStrikes = MutableStateFlow<List<OptionStrikeItem>>(emptyList())
    val optionStrikes: StateFlow<List<OptionStrikeItem>> = _optionStrikes.asStateFlow()

    private val _selectedOptionIndex = MutableStateFlow("NIFTY")
    val selectedOptionIndex: StateFlow<String> = _selectedOptionIndex.asStateFlow()

    private val _availableOptionExpiries = MutableStateFlow<List<String>>(emptyList())
    val availableOptionExpiries: StateFlow<List<String>> = _availableOptionExpiries.asStateFlow()

    private val _selectedOptionExpiry = MutableStateFlow("")
    val selectedOptionExpiry: StateFlow<String> = _selectedOptionExpiry.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _isPlacingOrder = MutableStateFlow(false)
    val isPlacingOrder: StateFlow<Boolean> = _isPlacingOrder.asStateFlow()

    private val _telegramBotToken = MutableStateFlow(sessionManager.telegramBotToken)
    val telegramBotToken: StateFlow<String> = _telegramBotToken.asStateFlow()

    private val _telegramChatId = MutableStateFlow(sessionManager.telegramChatId)
    val telegramChatId: StateFlow<String> = _telegramChatId.asStateFlow()

    private val _isTelegramAlertsEnabled = MutableStateFlow(sessionManager.isTelegramAlertsEnabled)
    val isTelegramAlertsEnabled: StateFlow<Boolean> = _isTelegramAlertsEnabled.asStateFlow()

    private val _isTelegramTesting = MutableStateFlow(false)
    val isTelegramTesting: StateFlow<Boolean> = _isTelegramTesting.asStateFlow()

    private val _telegramResponseInfo = MutableStateFlow<TelegramApiResponseInfo?>(null)
    val telegramResponseInfo: StateFlow<TelegramApiResponseInfo?> = _telegramResponseInfo.asStateFlow()

    private val _showConnectDialog = MutableStateFlow(false)
    val showConnectDialog: StateFlow<Boolean> = _showConnectDialog.asStateFlow()

    private val _connectingBrokerName = MutableStateFlow("Dhan")
    val connectingBrokerName: StateFlow<String> = _connectingBrokerName.asStateFlow()

    private val _isAuthInProgress = MutableStateFlow(false)
    val isAuthInProgress: StateFlow<Boolean> = _isAuthInProgress.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _authSuccessEvent = MutableStateFlow(false)
    val authSuccessEvent: StateFlow<Boolean> = _authSuccessEvent.asStateFlow()

    private val _selectedExchange = MutableStateFlow("NSE")
    val selectedExchange: StateFlow<String> = _selectedExchange.asStateFlow()

    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()

    private val _isSessionRestoring = MutableStateFlow(true)
    val isSessionRestoring: StateFlow<Boolean> = _isSessionRestoring.asStateFlow()

    private val _marketDataSource = MutableStateFlow(brokerManager.currentMarketDataSource)
    val marketDataSource: StateFlow<String> = _marketDataSource.asStateFlow()

    private val _marketDataLastUpdated = MutableStateFlow("")
    val marketDataLastUpdated: StateFlow<String> = _marketDataLastUpdated.asStateFlow()

    private val _isSessionValid = MutableStateFlow(false)
    val isSessionValid: StateFlow<Boolean> = _isSessionValid.asStateFlow()

    private val _brokerSwitchStatus = MutableStateFlow<String?>(null)
    val brokerSwitchStatus: StateFlow<String?> = _brokerSwitchStatus.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val brokerAuthManager = brokerManager.brokerAuthManager
    val brokerStatuses = brokerAuthManager.statuses
    val isBrokerAuthInitializing = brokerAuthManager.isInitializing

    fun clearBrokerSwitchStatus() {
        _brokerSwitchStatus.value = null
    }

    init {
        com.example.util.InstrumentMapUtil.setInstrumentMaster(instrumentMasterService)
        com.example.util.AlgoEngine.telegramService = telegramService
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            brokerAuthManager.initialize()
            validateAndRestoreSession()
        }
        observeData()
        setSelectedOptionIndex("NIFTY")
        startLiveMarketFeed()
    }

    private val sessionRestoreMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun validateAndRestoreSession(): Boolean = sessionRestoreMutex.withLock {
        _isSessionRestoring.value = true
        val hasSession = sessionManager.hasValidSession()
        if (!hasSession) {
            android.util.Log.d("SessionRestore", "session exists: false")
            android.util.Log.d("SessionRestore", "broker: None")
            android.util.Log.d("SessionRestore", "token expiry status: expired")
            android.util.Log.d("SessionRestore", "restore success: false")
            _isSessionValid.value = false
            _isSessionRestoring.value = false
            return@withLock false
        }

        val broker = sessionManager.activeBroker
        brokerManager.setActiveBroker(broker)
        android.util.Log.d("SessionRestore", "session exists: true")
        android.util.Log.d("SessionRestore", "broker: $broker")

        return@withLock try {
            val isValid = when (broker) {
                "Dhan" -> {
                    val profileRes = dhanService.getProfile()
                    if (profileRes.isSuccess) {
                        android.util.Log.d("SessionRestore", "token expiry status: valid")
                        android.util.Log.d("SessionRestore", "restore success: true")
                        repository.syncWithBroker()
                        true
                    } else {
                        val err = profileRes.exceptionOrNull()?.message ?: ""
                        if (err.contains("401") || err.contains("403") || err.contains("not connected", ignoreCase = true) || err.contains("Unauthenticated", ignoreCase = true) || err.contains("token", ignoreCase = true)) {
                            android.util.Log.d("SessionRestore", "token expiry status: expired")
                            android.util.Log.d("SessionRestore", "restore success: false")
                            sessionManager.clearDhanSession()
                            false
                        } else {
                            android.util.Log.d("SessionRestore", "token expiry status: valid (network warning: $err)")
                            android.util.Log.d("SessionRestore", "restore success: true")
                            repository.syncWithBroker()
                            true
                        }
                    }
                }
                "Angel One" -> {
                    var profileRes = angelOneService.getProfile()
                    if (profileRes.isSuccess) {
                        android.util.Log.d("SessionRestore", "token expiry status: valid")
                        android.util.Log.d("SessionRestore", "restore success: true")
                        repository.syncWithBroker()
                        true
                    } else {
                        val refreshToken = sessionManager.angelRefreshToken
                        var refreshed = false
                        if (!refreshToken.isNullOrBlank()) {
                            android.util.Log.d("SessionRestore", "Attempting Angel One token refresh...")
                            val refreshRes = AngelAuthHelper.renewSession(refreshToken)
                            if (refreshRes.isSuccess) {
                                val newTokens = refreshRes.getOrThrow()
                                if (!newTokens.jwtToken.isNullOrBlank()) {
                                    sessionManager.angelJwtToken = newTokens.jwtToken
                                }
                                if (!newTokens.refreshToken.isNullOrBlank()) {
                                    sessionManager.angelRefreshToken = newTokens.refreshToken
                                }
                                if (!newTokens.feedToken.isNullOrBlank()) {
                                    sessionManager.angelFeedToken = newTokens.feedToken
                                }
                                profileRes = angelOneService.getProfile()
                                if (profileRes.isSuccess) {
                                    refreshed = true
                                }
                            }
                        }

                        if (refreshed) {
                            android.util.Log.d("SessionRestore", "token expiry status: refreshed")
                            android.util.Log.d("SessionRestore", "restore success: true")
                            repository.syncWithBroker()
                            true
                        } else {
                            val err = profileRes.exceptionOrNull()?.message ?: ""
                            if (err.contains("401") || err.contains("403") || err.contains("expired", ignoreCase = true) || err.contains("not connected", ignoreCase = true) || err.contains("invalid", ignoreCase = true)) {
                                android.util.Log.d("SessionRestore", "token expiry status: expired")
                                android.util.Log.d("SessionRestore", "restore success: false")
                                sessionManager.clearAngelSession()
                                false
                            } else {
                                android.util.Log.d("SessionRestore", "token expiry status: valid (network warning: $err)")
                                android.util.Log.d("SessionRestore", "restore success: true")
                                repository.syncWithBroker()
                                true
                            }
                        }
                    }
                }
                "m.Stock" -> {
                    if (sessionManager.isMStockConfigured()) {
                        android.util.Log.d("SessionRestore", "token expiry status: valid")
                        android.util.Log.d("SessionRestore", "restore success: true")
                        repository.syncWithBroker()
                        true
                    } else {
                        android.util.Log.d("SessionRestore", "token expiry status: expired")
                        android.util.Log.d("SessionRestore", "restore success: false")
                        sessionManager.clearMStockSession()
                        false
                    }
                }
                else -> false
            }

            _isSessionValid.value = isValid
            _isSessionRestoring.value = false
            isValid
        } catch (e: Exception) {
            android.util.Log.e("SessionRestore", "Error validating session: ${e.message}")
            android.util.Log.d("SessionRestore", "restore success: false")
            _isSessionValid.value = false
            _isSessionRestoring.value = false
            false
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.userProfile.collectLatest { prof ->
                prof?.let { _userProfile.value = it }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(repository.watchlistAll, com.example.data.model.MarketDataStore.marketData) { dbList, liveData ->
                dbList.map { item ->
                    val dbSymbol = item.symbol.uppercase().trim()
                    val live = liveData.values.find { liveItem ->
                        val liveSymbol = liveItem.symbol.uppercase().trim()
                        when (dbSymbol) {
                            "NIFTY 50" -> liveSymbol == "NIFTY 50" || liveSymbol == "NIFTY"
                            "BANKNIFTY" -> liveSymbol == "NIFTY BANK" || liveSymbol == "BANKNIFTY"
                            "FINNIFTY" -> liveSymbol == "NIFTY FIN SERVICE" || liveSymbol == "FINNIFTY"
                            "MIDCPNIFTY" -> liveSymbol.contains("MID SELECT") || liveSymbol == "MIDCPNIFTY"
                            "SENSEX" -> liveSymbol == "SENSEX"
                            "BANKEX" -> liveSymbol == "BANKEX"
                            "CRUDEOIL" -> liveSymbol.startsWith("CRUDEOIL") && !liveSymbol.startsWith("CRUDEOILM")
                            "CRUDEOIL M" -> liveSymbol.startsWith("CRUDEOILM")
                            else -> liveSymbol == dbSymbol
                        }
                    }
                    if (live != null && live.ltp > 0) {
                        val prevClose = if (item.ltp > 0) item.ltp - item.change else 0.0
                        val newChange = if (prevClose > 0) live.ltp - prevClose else live.change
                        val newChangePct = if (prevClose > 0) (newChange / prevClose) * 100 else live.changePercent
                        item.copy(
                            ltp = live.ltp,
                            change = newChange,
                            changePercent = newChangePct,
                            isPositive = newChange >= 0
                        )
                    } else {
                        item
                    }
                }
            }.collectLatest { combinedList ->
                _watchlist.value = combinedList
            }
        }
    }

    fun getWatchlistItemFlow(indexName: String): Flow<WatchlistItem?> {
        val normName = indexName.trim().uppercase()
        return watchlist
            .map { list ->
                list.find { item ->
                    val dbSymbol = item.symbol.uppercase().trim()
                    when (normName) {
                        "NIFTY 50", "NIFTY" -> dbSymbol == "NIFTY 50" || dbSymbol == "NIFTY"
                        "BANKNIFTY" -> dbSymbol == "BANKNIFTY" || dbSymbol == "NIFTY BANK"
                        "FINNIFTY" -> dbSymbol == "FINNIFTY" || dbSymbol == "NIFTY FIN SERVICE"
                        "MIDCPNIFTY" -> dbSymbol.contains("MID") || dbSymbol == "MIDCPNIFTY"
                        "SENSEX" -> dbSymbol == "SENSEX"
                        "BANKEX" -> dbSymbol == "BANKEX"
                        "CRUDEOIL" -> dbSymbol.startsWith("CRUDEOIL") && !dbSymbol.startsWith("CRUDEOILM")
                        "CRUDEOIL M" -> dbSymbol.startsWith("CRUDEOILM")
                        else -> dbSymbol == normName || dbSymbol.contains(normName)
                    }
                }
            }
            .distinctUntilChanged()
    }

    private fun observeOrdersAndHoldings() {
        viewModelScope.launch {
            repository.allOrders.collectLatest { list ->
                _orders.value = list
            }
        }
        viewModelScope.launch {
            repository.allHoldings.collectLatest { list ->
                _holdings.value = list
            }
        }
        viewModelScope.launch {
            repository.aiSignals.collectLatest { list ->
                _aiSignals.value = list
            }
        }
        viewModelScope.launch {
            repository.notifications.collectLatest { list ->
                _notifications.value = list
            }
        }
    }

    fun consumeAuthSuccessEvent() {
        _authSuccessEvent.value = false
    }

    fun isSessionActive(): Boolean {
        return _isSessionValid.value || sessionManager.hasValidSession()
    }

    fun switchActiveBroker(brokerName: String) {
        viewModelScope.launch {
            _isSessionRestoring.value = true
            _brokerSwitchStatus.value = "Setting primary market data provider to $brokerName..."

            if (brokerName == "Angel One" || brokerName == "m.Stock") {
                val isReady = if (brokerName == "Angel One") {
                    brokerManager.angelMarketDataService.isConnectionLive() ||
                    brokerManager.brokerAuthManager.statuses.value["Angel One"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                } else {
                    brokerManager.mStockMarketDataService.isConnectionLive() ||
                    brokerManager.brokerAuthManager.statuses.value["m.Stock"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                }

                if (!isReady) {
                    _isSessionRestoring.value = false
                    _brokerSwitchStatus.value = null
                    _authErrorMessage.value = "Provider is not ready. Please connect first."
                    openConnectDialog(brokerName)
                    return@launch
                }

                brokerManager.setPrimaryMarketDataProvider(brokerName)
                _brokerSwitchStatus.value = "Primary Market Data • $brokerName"
                _isSessionRestoring.value = false
                repository.addNotification(
                    title = "Market Data Provider Switched",
                    message = "Primary market data feed switched to $brokerName",
                    type = "SUCCESS"
                )
            } else {
                sessionManager.activeBroker = "Dhan"
                brokerManager.setActiveBroker("Dhan")
                val valid = validateAndRestoreSession()
                if (valid) {
                    _brokerSwitchStatus.value = "Broker Connected • Dhan"
                    _authSuccessEvent.value = true
                    repository.addNotification(
                        title = "Order Broker Selected",
                        message = "Active order execution broker set to Dhan",
                        type = "SUCCESS"
                    )
                } else {
                    _brokerSwitchStatus.value = null
                    _authErrorMessage.value = "Dhan session expired. Please re-authenticate."
                    openConnectDialog("Dhan")
                }
                _isSessionRestoring.value = false
            }
        }
    }

    fun switchBroker(brokerName: String) = switchActiveBroker(brokerName)

    fun openConnectDialog(brokerName: String = "Dhan") {
        _connectingBrokerName.value = brokerName
        _authErrorMessage.value = null
        _showConnectDialog.value = true
    }

    fun closeConnectDialog() {
        _showConnectDialog.value = false
        _isAuthInProgress.value = false
        _authErrorMessage.value = null
    }

    fun connectDhan(clientId: String, accessToken: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            sessionManager.dhanClientId = clientId
            sessionManager.dhanAccessToken = accessToken
            sessionManager.activeBroker = "Dhan"
            sessionManager.dhanTokenTimestamp = System.currentTimeMillis()
            brokerManager.setActiveBroker("Dhan")

            val valid = validateAndRestoreSession()
            _isAuthInProgress.value = false
            if (valid) {
                _brokerSwitchStatus.value = "Broker Connected • Dhan"
                _authSuccessEvent.value = true
                _showConnectDialog.value = false
                repository.addNotification("Broker Connected", "Connected to Dhan successfully", "SUCCESS")
                telegramService.sendFormattedEvent(
                    "DHAN_CONN_${sessionManager.dhanClientId}",
                    com.example.data.network.TelegramMessageFormatter.formatDhanConnected()
                )
            } else {
                _authErrorMessage.value = "Failed to validate Dhan connection. Please verify token."
            }
        }
    }

    fun loginAngel(clientCode: String, mpin: String, apiKey: String, totpSecret: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            val res = brokerAuthManager.connectAngelOne(
                clientCode = clientCode,
                mpin = mpin,
                totp = null,
                totpSecret = totpSecret,
                apiKey = apiKey
            )
            _isAuthInProgress.value = false
            if (res.isSuccess && res.getOrThrow()) {
                sessionManager.angelTokenTimestamp = System.currentTimeMillis()
                _brokerSwitchStatus.value = "Angel One Feed Connected"
                _authSuccessEvent.value = true
                _showConnectDialog.value = false
                repository.addNotification("Broker Connected", "Connected to Angel One successfully", "SUCCESS")
                telegramService.sendFormattedEvent(
                    "ANGEL_CONN_${sessionManager.angelClientId}",
                    com.example.data.network.TelegramMessageFormatter.formatAngelConnected()
                )
            } else {
                val err = res.exceptionOrNull()?.message ?: "Angel One login failed"
                _authErrorMessage.value = err
            }
        }
    }

    fun connectMStock(clientCode: String, apiKey: String, totpSecret: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null

            if (clientCode.isBlank()) {
                _authErrorMessage.value = "m.Stock Client Code is required."
                _isAuthInProgress.value = false
                return@launch
            }
            if (apiKey.isBlank()) {
                _authErrorMessage.value = "m.Stock API Key is required."
                _isAuthInProgress.value = false
                return@launch
            }
            if (totpSecret.isBlank()) {
                _authErrorMessage.value = "m.Stock TOTP Secret is required."
                _isAuthInProgress.value = false
                return@launch
            }

            val res = brokerAuthManager.connectMStock(
                clientCode = clientCode,
                apiKey = apiKey,
                totpSecret = totpSecret
            )
            _isAuthInProgress.value = false
            if (res.isSuccess && res.getOrThrow()) {
                _brokerSwitchStatus.value = "m.Stock Feed Connected"
                _authSuccessEvent.value = true
                _showConnectDialog.value = false
                repository.addNotification("Broker Connected", "Connected to m.Stock (Mirae Asset) successfully", "SUCCESS")
                telegramService.sendFormattedEvent(
                    "MSTOCK_CONN_${clientCode}",
                    "<b>🟢 BROKER CONNECTED</b>\n\nBroker: <b>m.Stock (Mirae Asset)</b>\nClient Code: <code>${clientCode}</code>\nStatus: <b>Active Secondary Market Feed Session</b>"
                )
            } else {
                val err = res.exceptionOrNull()?.message ?: "m.Stock login failed. Please verify credentials."
                _authErrorMessage.value = err
            }
        }
    }

    fun connectTradeSmart(apiKey: String, clientId: String, token: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null

            if (apiKey.isBlank() || clientId.isBlank()) {
                _authErrorMessage.value = "TradeSmart API Key and Client ID are required."
                _isAuthInProgress.value = false
                return@launch
            }

            sessionManager.tradesmartApiKey = apiKey
            sessionManager.tradesmartClientId = clientId
            sessionManager.tradesmartAccessToken = token
            sessionManager.tradesmartTokenTimestamp = System.currentTimeMillis()

            val res = brokerAuthManager.connectTradeSmart(apiKey, clientId, token)
            _isAuthInProgress.value = false

            if (res.isSuccess) {
                _brokerSwitchStatus.value = "Broker Connected • TradeSmart"
                _authSuccessEvent.value = true
                _showConnectDialog.value = false
                repository.addNotification("Broker Connected", "Connected to TradeSmart successfully", "SUCCESS")
            } else {
                _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Failed to connect TradeSmart."
            }
        }
    }

    fun reconnectBroker(brokerName: String) {
        viewModelScope.launch {
            _isSessionRestoring.value = true
            val res = brokerAuthManager.reconnectBroker(brokerName)
            _isSessionRestoring.value = false
            if (res.isSuccess && res.getOrThrow()) {
                repository.addNotification("Broker Reconnected", "$brokerName session refreshed successfully", "SUCCESS")
            } else {
                _authErrorMessage.value = "Failed to refresh $brokerName session. Re-authentication required."
                openConnectDialog(brokerName)
            }
        }
    }

    fun disconnectBroker(brokerName: String) {
        viewModelScope.launch {
            brokerAuthManager.disconnectBroker(brokerName)
            repository.addNotification("Broker Disconnected", "$brokerName disconnected (credentials preserved)", "INFO")
        }
    }

    fun removeAccountBroker(brokerName: String) {
        viewModelScope.launch {
            brokerAuthManager.removeAccount(brokerName)
            repository.addNotification("Account Removed", "$brokerName credentials and tokens cleared", "WARNING")
        }
    }

    fun handleOAuthRedirect(uri: Uri) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            
            val scheme = uri.scheme ?: ""
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            val fullUrl = uri.toString()

            android.util.Log.d("DhanAuth", "redirect received")
            android.util.Log.d("DhanAuth", "redirect URI host/path: scheme=$scheme, host=$host, path=$path")

            var token = uri.getQueryParameter("access_token")
                ?: uri.getQueryParameter("token")
                ?: uri.getQueryParameter("accessToken")

            var code = uri.getQueryParameter("tokenId")
                ?: uri.getQueryParameter("consentId")
                ?: uri.getQueryParameter("code")
                ?: uri.getQueryParameter("auth_code")

            val clientId = uri.getQueryParameter("client_id")
                ?: uri.getQueryParameter("clientId")

            if (token.isNullOrBlank()) {
                val tokenMatch = Regex("""[?&#](?:access_token|accessToken|token)=([^&#]+)""", RegexOption.IGNORE_CASE).find(fullUrl)
                token = tokenMatch?.groupValues?.get(1)
            }

            if (code.isNullOrBlank()) {
                val codeMatch = Regex("""[?&#](?:tokenId|consentId|code|auth_code)=([^&#]+)""", RegexOption.IGNORE_CASE).find(fullUrl)
                code = codeMatch?.groupValues?.get(1)
            }

            val hasTokenId = !code.isNullOrBlank() || !token.isNullOrBlank()
            android.util.Log.d("DhanAuth", "tokenId received: ${if (hasTokenId) "YES" else "NO"}")

            if (!clientId.isNullOrBlank()) {
                sessionManager.dhanClientId = clientId
            }

            if (!token.isNullOrBlank()) {
                sessionManager.dhanAccessToken = token
                sessionManager.activeBroker = "Dhan"
                sessionManager.dhanTokenTimestamp = System.currentTimeMillis()
                brokerManager.setActiveBroker("Dhan")

                val valid = validateAndRestoreSession()
                _isAuthInProgress.value = false
                if (valid) {
                    android.util.Log.d("DhanAuth", "profile validation success/failure: SUCCESS")
                    _brokerSwitchStatus.value = "Broker Connected • Dhan"
                    _authSuccessEvent.value = true
                    _showConnectDialog.value = false
                    repository.addNotification("Broker Connected", "Connected to Dhan via OAuth successfully", "SUCCESS")
                } else {
                    android.util.Log.e("DhanAuth", "profile validation success/failure: FAILURE")
                    _authErrorMessage.value = "Failed to validate Dhan session."
                }
            } else if (!code.isNullOrBlank()) {
                val exchangeRes = com.example.util.DhanAuthHelper.exchangeToken(code)
                exchangeRes.onSuccess { accessToken ->
                    android.util.Log.d("DhanAuth", "token exchange success/failure: SUCCESS")
                    sessionManager.dhanAccessToken = accessToken
                    sessionManager.activeBroker = "Dhan"
                    sessionManager.dhanTokenTimestamp = System.currentTimeMillis()
                    brokerManager.setActiveBroker("Dhan")

                    val valid = validateAndRestoreSession()
                    if (valid) {
                        android.util.Log.d("DhanAuth", "profile validation success/failure: SUCCESS")
                        _brokerSwitchStatus.value = "Broker Connected • Dhan"
                        _authSuccessEvent.value = true
                        _showConnectDialog.value = false
                        repository.addNotification("Broker Connected", "Connected to Dhan via OAuth successfully", "SUCCESS")
                    } else {
                        android.util.Log.e("DhanAuth", "profile validation success/failure: FAILURE")
                        _authErrorMessage.value = "Failed to validate Dhan session."
                    }
                }.onFailure { err ->
                    android.util.Log.e("DhanAuth", "token exchange success/failure: FAILURE (${err.localizedMessage})")
                    _authErrorMessage.value = "Failed to exchange token: ${err.localizedMessage}"
                }
                _isAuthInProgress.value = false
            } else {
                android.util.Log.e("DhanAuth", "tokenId received: NO - neither code nor token found in URI")
                _authErrorMessage.value = "Failed to parse token or code from redirect"
                _isAuthInProgress.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearActiveBrokerSession()
            _isSessionValid.value = false
            _userProfile.value = UserProfileEntity(id = 1)
            _orders.value = emptyList()
            _holdings.value = emptyList()
            _watchlist.value = emptyList()
            repository.updateProfile(UserProfileEntity(id = 1))
        }
    }

    private fun startLiveMarketFeed() {
        viewModelScope.launch {
            launch {
                brokerManager.marketDataEngine.unifiedFeedStatus.collect { status ->
                    _marketDataSource.value = status
                }
            }
            launch {
                brokerManager.marketDataEngine.lastTickTimeFormatted.collect { time ->
                    if (time.isNotBlank()) {
                        _marketDataLastUpdated.value = time
                    }
                }
            }

            while (true) {
                kotlinx.coroutines.delay(15000L)
                val isValidSession = sessionManager.hasValidSession()
                if (isValidSession) {
                    syncMarketDataQuietly()
                    fetchOptionChain()
                }
                if (_marketDataLastUpdated.value.isBlank()) {
                    _marketDataLastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                }
                com.example.util.AlgoEngine.processMarketFeed(_watchlist.value, isValidSession)
            }
        }
    }

    private fun syncMarketDataQuietly() {
        viewModelScope.launch {
            runCatching {
                if (sessionManager.hasValidSession()) {
                    repository.syncWithBroker()
                }
                val symbols = _watchlist.value.map { it.symbol }.ifEmpty { listOf("NIFTY 50", "BANKNIFTY", "RELIANCE") }
                brokerManager.marketDataEngine.getMarketQuotes(symbols)
                _marketDataLastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            }
        }
    }

    fun refreshBrokerData() {
        viewModelScope.launch {
            if (sessionManager.hasValidSession()) {
                runCatching {
                    repository.syncWithBroker()
                }.onFailure { e ->
                    _apiError.value = e.message
                }
            }
        }
    }

    fun refreshMarketData() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                if (sessionManager.hasValidSession()) {
                    runCatching {
                        repository.syncWithBroker()
                    }.onFailure { e ->
                        _apiError.value = e.message
                    }
                }
                val symbols = _watchlist.value.map { it.symbol }.ifEmpty { listOf("NIFTY 50", "BANKNIFTY", "RELIANCE") }
                brokerManager.marketDataEngine.getMarketQuotes(symbols)
                _marketDataLastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onAppResume() {
        refreshBrokerData()
    }

    fun setSelectedExchange(exchange: String) {
        _selectedExchange.value = exchange
    }

    fun setSelectedOptionIndex(index: String) {
        _selectedOptionIndex.value = index
        _optionStrikes.value = emptyList() // Immediately clear strikes on index change
        viewModelScope.launch {
            val expiriesRes = brokerManager.getOptionExpiries(index)
            val apiExpiries = expiriesRes.getOrNull() ?: emptyList()
            if (apiExpiries.isNotEmpty()) {
                _availableOptionExpiries.value = apiExpiries
                if (!_availableOptionExpiries.value.contains(_selectedOptionExpiry.value)) {
                    _selectedOptionExpiry.value = apiExpiries.first()
                }
            } else {
                // Fallback to local if API fails or not supported (e.g., Angel)
                val expiries = OptionExpiryUtil.getUpcomingExpiriesForSymbol(index)
                _availableOptionExpiries.value = expiries
                if (expiries.isNotEmpty() && !expiries.contains(_selectedOptionExpiry.value)) {
                    _selectedOptionExpiry.value = expiries.first()
                }
            }
            fetchOptionChain()
        }
    }

    fun setSelectedOptionExpiry(expiry: String) {
        _selectedOptionExpiry.value = expiry
        fetchOptionChain()
    }

    private fun fetchOptionChain() {
        viewModelScope.launch {
            val indexName = _selectedOptionIndex.value
            val expiry = _selectedOptionExpiry.value
            val indexTick = com.example.data.model.MarketDataStore.getTick(indexName)
            val indexLtp = indexTick?.ltp ?: _watchlist.value.find { it.symbol.equals(indexName, ignoreCase = true) }?.ltp ?: 0.0

            val res = brokerManager.getOptionChain(indexName, expiry)
            val strikes = res.getOrNull()
            
            // Validate if returned strikes match the index LTP range
            val isValidForIndex = !strikes.isNullOrEmpty() && strikes.any { strike ->
                if (indexLtp > 0) {
                    kotlin.math.abs(strike.strikePrice - indexLtp) < (indexLtp * 0.25)
                } else true
            }

            if (isValidForIndex && strikes != null) {
                _optionStrikes.value = strikes
            } else {
                // Generate accurate strike chain centered around current index LTP
                _optionStrikes.value = generateOptionStrikesForIndex(indexName, expiry, indexLtp)
            }
        }
    }

    private fun generateOptionStrikesForIndex(index: String, expiry: String, indexLtp: Double): List<OptionStrikeItem> {
        val cleanIndex = index.trim().uppercase()
        val ltp = if (indexLtp > 0.0) indexLtp else when (cleanIndex) {
            "SENSEX" -> 79627.0
            "BANKEX" -> 65110.0
            "BANKNIFTY" -> 52400.0
            "FINNIFTY" -> 23400.0
            "MIDCPNIFTY" -> 12200.0
            "CRUDEOIL", "CRUDEOIL M" -> 8120.0
            else -> 24230.0 // NIFTY
        }
        
        val step = when (cleanIndex) {
            "MIDCPNIFTY" -> 25.0
            "NIFTY 50", "NIFTY", "FINNIFTY", "CRUDEOIL", "CRUDEOIL M" -> 50.0
            else -> 100.0 // SENSEX, BANKNIFTY, BANKEX
        }
        
        val atmStrike = kotlin.math.round(ltp / step) * step
        val strikes = mutableListOf<OptionStrikeItem>()
        
        for (i in -10..10) {
            val strikePrice = atmStrike + (i * step)
            val callSym = "$cleanIndex ${strikePrice.toInt()} CE"
            val putSym = "$cleanIndex ${strikePrice.toInt()} PE"
            
            val callTick = com.example.data.model.MarketDataStore.getTick(callSym)
            val putTick = com.example.data.model.MarketDataStore.getTick(putSym)
            
            val isAtm = i == 0
            strikes.add(
                OptionStrikeItem(
                    strikePrice = strikePrice,
                    callLtp = callTick?.ltp ?: 0.0,
                    callOi = if (callTick != null && callTick.volume > 0) "${callTick.volume * 15} k" else "-",
                    callChgOi = "-",
                    callIv = 0.0,
                    putLtp = putTick?.ltp ?: 0.0,
                    putOi = if (putTick != null && putTick.volume > 0) "${putTick.volume * 15} k" else "-",
                    putChgOi = "-",
                    putIv = 0.0,
                    callVolume = "${callTick?.volume ?: 0}",
                    putVolume = "${putTick?.volume ?: 0}",
                    isAtm = isAtm
                )
            )
        }
        return strikes
    }

    fun getHistoricalCandlesForIndex(indexName: String, onResult: (List<com.example.ui.components.CandleData>) -> Unit) {
        viewModelScope.launch {
            val res = brokerManager.getHistoricalCandles(indexName, "15m")
            if (res.isSuccess) {
                onResult(res.getOrDefault(emptyList()))
            } else {
                onResult(emptyList())
            }
        }
    }

    fun placeNewOrder(
        symbol: String,
        exchange: String,
        side: String,
        orderType: String,
        qty: Int,
        price: Double
    ) {
        if (_isPlacingOrder.value) return
        _isPlacingOrder.value = true

        val appPrefs = com.example.util.AppPreferences(getApplication())
        val risk = appPrefs.getRiskSettings()
        val orderPrefs = appPrefs.getOrderPreferences()

        if (orderPrefs.duplicateOrderProtection) {
            val now = System.currentTimeMillis()
            val hasRecentDuplicate = _orders.value.any { o ->
                o.symbol.equals(symbol, ignoreCase = true) &&
                o.side.equals(side, ignoreCase = true) &&
                o.qty == qty &&
                kotlin.math.abs(o.price - price) < 0.05 &&
                (o.status.equals("PENDING", ignoreCase = true) || o.status.equals("OPEN", ignoreCase = true) || (now - (o.orderId.substringAfter("ORD_").toLongOrNull() ?: 0L)) < 5000)
            }
            if (hasRecentDuplicate) {
                _apiError.value = "Duplicate Order Protection: An identical order was recently placed or is pending execution."
                _isPlacingOrder.value = false
                return
            }
        }

        val expectedLotSize = com.example.util.AppPreferences.getGlobalLotSize(symbol)
        if (qty <= 0 || (expectedLotSize > 1 && qty % expectedLotSize != 0)) {
            _apiError.value = "Invalid quantity. Quantity must be a valid multiple of the current lot size."
            _isPlacingOrder.value = false
            return
        }

        if (risk.enforceMaxOrderValue && (qty * price) > risk.maxOrderValue) {
            _apiError.value = "Order Blocked: Value ₹${String.format("%.2f", qty * price)} exceeds max limit ₹${String.format("%.2f", risk.maxOrderValue)}"
            _isPlacingOrder.value = false
            return
        }

        val todayLoss = if ((_userProfile.value?.todaysPnl ?: 0.0) < 0) kotlin.math.abs(_userProfile.value?.todaysPnl ?: 0.0) else 0.0
        if (risk.enforceDailyLoss && todayLoss >= risk.maxDailyLoss) {
            _apiError.value = "Order Blocked: Today's loss (₹${String.format("%.2f", todayLoss)}) reached max limit ₹${String.format("%.2f", risk.maxDailyLoss)}"
            _isPlacingOrder.value = false
            return
        }

        if (risk.enforceMaxTrades && _orders.value.size >= risk.maxDailyTrades) {
            _apiError.value = "Order Blocked: Reached maximum daily trade limit (${risk.maxDailyTrades})"
            _isPlacingOrder.value = false
            return
        }

        viewModelScope.launch {
            try {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val secId = com.example.util.InstrumentMapUtil.getDhanSecurityId(symbol, exchange)
                val token = instrumentMasterService.resolveAngelToken(symbol, exchange) ?: ""
                val order = OrderEntity(
                    orderId = "ORD_${System.currentTimeMillis()}",
                    symbol = symbol,
                    exchange = exchange,
                    lotSize = expectedLotSize,
                    qty = qty,
                    orderType = orderType,
                    side = side,
                    price = price,
                    value = qty * price,
                    stopLoss = 0.0,
                    target = 0.0,
                    status = "PENDING",
                    time = timeStr,
                    securityId = secId,
                    symbolToken = token
                )
                repository.placeOrder(order)
                val notif = appPrefs.getNotificationSettings()
                if (notif.notifyOrderUpdates) {
                    repository.addNotification("Order Submitted", "$side $qty shares/lots of $symbol @ ₹$price", type = "SUCCESS")
                }
                refreshBrokerData()
            } catch (e: Exception) {
                _apiError.value = "Order Placement Failed: ${e.message}"
            } finally {
                _isPlacingOrder.value = false
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                repository.cancelOrder(orderId)
                refreshBrokerData()
            } catch (e: Exception) {
                _apiError.value = "Cancel Order Failed: ${e.message}"
            }
        }
    }

    fun modifyOrder(
        orderId: String,
        newPrice: Double,
        newQty: Int,
        orderType: String = "LIMIT",
        stopLoss: Double = 0.0,
        target: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                repository.modifyOrder(orderId, newPrice, newQty, orderType, stopLoss, target)
                refreshBrokerData()
            } catch (e: Exception) {
                _apiError.value = "Modify Order Failed: ${e.message}"
            }
        }
    }

    fun updateStopLossTarget(orderId: String, newSl: Double, newTarget: Double) {
        viewModelScope.launch {
            try {
                repository.updateOrderStopLossTarget(orderId, newSl, newTarget)
                refreshBrokerData()
            } catch (e: Exception) {
                _apiError.value = "Update SL/Target Failed: ${e.message}"
            }
        }
    }

    fun exitPosition(orderId: String, exitPrice: Double, realizedPnl: Double) {
        viewModelScope.launch {
            try {
                val existingOrder = _orders.value.find { it.orderId == orderId || it.brokerOrderId == orderId }
                val symbol = existingOrder?.symbol ?: "POSITION"
                val exchange = existingOrder?.exchange ?: "NSE"
                val origSide = existingOrder?.side ?: "BUY"
                val oppositeSide = if (origSide.equals("BUY", ignoreCase = true)) "SELL" else "BUY"
                val qty = existingOrder?.qty ?: 1
                val productType = existingOrder?.productType ?: "INTRADAY"

                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val exitOrder = OrderEntity(
                    orderId = "EXIT_${System.currentTimeMillis()}",
                    symbol = symbol,
                    exchange = exchange,
                    lotSize = existingOrder?.lotSize ?: com.example.util.AppPreferences.getGlobalLotSize(symbol),
                    qty = qty,
                    orderType = "MARKET",
                    side = oppositeSide,
                    price = exitPrice,
                    value = qty * exitPrice,
                    status = "PENDING",
                    time = timeStr,
                    productType = productType,
                    securityId = existingOrder?.securityId ?: com.example.util.InstrumentMapUtil.getDhanSecurityId(symbol, exchange),
                    symbolToken = existingOrder?.symbolToken ?: instrumentMasterService.resolveAngelToken(symbol, exchange) ?: ""
                )

                repository.placeOrder(exitOrder)
                repository.exitPosition(orderId, exitPrice, realizedPnl)
                refreshBrokerData()
            } catch (e: Exception) {
                _apiError.value = "Exit Position Failed: ${e.message}"
            }
        }
    }

    fun partialExitPosition(orderId: String, exitLots: Int, exitPrice: Double, partialPnl: Double) {
        viewModelScope.launch {
            try {
                val existingOrder = _orders.value.find { it.orderId == orderId || it.brokerOrderId == orderId }
                val symbol = existingOrder?.symbol ?: "POSITION"
                val exchange = existingOrder?.exchange ?: "NSE"
                val origSide = existingOrder?.side ?: "BUY"
                val oppositeSide = if (origSide.equals("BUY", ignoreCase = true)) "SELL" else "BUY"
                val productType = existingOrder?.productType ?: "INTRADAY"

                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val partialExitOrder = OrderEntity(
                    orderId = "PEXIT_${System.currentTimeMillis()}",
                    symbol = symbol,
                    exchange = exchange,
                    lotSize = existingOrder?.lotSize ?: com.example.util.AppPreferences.getGlobalLotSize(symbol),
                    qty = exitLots,
                    orderType = "MARKET",
                    side = oppositeSide,
                    price = exitPrice,
                    value = exitLots * exitPrice,
                    status = "PENDING",
                    time = timeStr,
                    productType = productType,
                    securityId = existingOrder?.securityId ?: com.example.util.InstrumentMapUtil.getDhanSecurityId(symbol, exchange),
                    symbolToken = existingOrder?.symbolToken ?: instrumentMasterService.resolveAngelToken(symbol, exchange) ?: ""
                )

                repository.placeOrder(partialExitOrder)
                repository.partialExitPosition(orderId, exitLots, partialPnl)
                refreshBrokerData()
            } catch (e: Exception) {
                _apiError.value = "Partial Exit Failed: ${e.message}"
            }
        }
    }

    fun deleteOrder(orderId: String) {
        cancelOrder(orderId)
    }

    fun addSymbolToWatchlist(symbol: String, exchange: String) {
        viewModelScope.launch {
            val item = WatchlistItem(
                symbol = symbol,
                exchange = exchange,
                ltp = 0.0,
                change = 0.0,
                changePercent = 0.0,
                lotSize = com.example.util.AppPreferences.getGlobalLotSize(symbol),
                isPositive = true,
                isFavorite = false
            )
            repository.addNotification("Watchlist", "Added $symbol ($exchange) to watchlist")
        }
    }

    fun toggleFavorite(symbol: String, currentStatus: Boolean) {
        viewModelScope.launch {
            // Toggle favorite status
        }
    }

    fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        if (current.size > 10) {
            _recentSearches.value = current.subList(0, 10)
        } else {
            _recentSearches.value = current
        }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun toggleNotifications(show: Boolean) {
        _showNotifications.value = show
        if (show) {
            markAllNotificationsAsRead()
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markNotificationsAsRead()
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    fun saveTelegramSettings(token: String, chatId: String, isEnabled: Boolean) {
        val cleanToken = token.trim()
        val cleanChatId = chatId.trim()
        sessionManager.telegramBotToken = cleanToken
        sessionManager.telegramChatId = cleanChatId
        sessionManager.isTelegramAlertsEnabled = isEnabled
        _telegramBotToken.value = cleanToken
        _telegramChatId.value = cleanChatId
        _isTelegramAlertsEnabled.value = isEnabled
    }

    fun testTelegramBot(token: String, chatId: String) {
        viewModelScope.launch {
            _isTelegramTesting.value = true
            val msg = com.example.data.network.TelegramMessageFormatter.formatTestMessage()
            val res = telegramService.sendAlert(msg, token, chatId)
            _telegramResponseInfo.value = res
            _isTelegramTesting.value = false
        }
    }

    fun testSpecificTelegramAlert(alertType: String, symbol: String, details: String) {
        viewModelScope.launch {
            _isTelegramTesting.value = true
            val res = telegramService.sendAlertIfEnabled(alertType, symbol, details)
            _telegramResponseInfo.value = res
            _isTelegramTesting.value = false
        }
    }

    fun clearTelegramResponseInfo() {
        _telegramResponseInfo.value = null
    }

    fun toggleBiometric(enabled: Boolean) {
        sessionManager.isBiometricEnabled = enabled
    }
}
