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
import com.example.util.AlertPreferences
import com.example.util.AppPreferences
import com.example.util.OptionExpiryUtil
import com.example.util.alert.AlertPreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val appPrefs = AppPreferences.getInstance(application)
    val sessionManager = SessionManager(application)
    private val networkClient = BrokerNetworkClient(sessionManager)

    val instrumentMasterService = com.example.data.network.InstrumentMasterService(context = application)
    val angelOneService = com.example.data.network.AngelOneBrokerService(networkClient.angelOneApi, sessionManager, instrumentMasterService)
    val dhanService = com.example.data.network.DhanBrokerService(api = networkClient.dhanApi, sessionManager = sessionManager)
    val brokerManager = com.example.data.network.BrokerManager(sessionManager, angelOneService, dhanService, instrumentMasterService)
    val telegramService = TelegramService(sessionManager)
    private val repository = TradingRepository(TradingDatabase.getDatabase(application).tradingDao(), brokerManager)
    val alertService = com.example.util.alert.AlertService(
        context = application,
        sessionManager = sessionManager,
        appPreferences = appPrefs,
        telegramService = telegramService,
        repository = repository
    )

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

    private val _telegramChannelId = MutableStateFlow(sessionManager.telegramChannelId)
    val telegramChannelId: StateFlow<String> = _telegramChannelId.asStateFlow()

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

    private var lastProcessedOAuthCode: String? = null
    private var lastProcessedOAuthTime: Long = 0L

    private val _selectedExchange = MutableStateFlow("NSE")
    val selectedExchange: StateFlow<String> = _selectedExchange.asStateFlow()

    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()

    private val _isSessionRestoring = MutableStateFlow(true)
    val isSessionRestoring: StateFlow<Boolean> = _isSessionRestoring.asStateFlow()

    val marketDataProviderState: StateFlow<com.example.data.model.MarketDataProviderState> = brokerManager.marketDataEngine.providerState

    private val _marketDataSource = MutableStateFlow(brokerManager.currentMarketDataSource)
    val marketDataSource: StateFlow<String> = _marketDataSource.asStateFlow()
    val isLiveFeedActive: StateFlow<Boolean> = marketDataProviderState.map {
        it.live && !it.stale
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    private val _marketDataLastUpdated = MutableStateFlow("")
    val marketDataLastUpdated: StateFlow<String> = _marketDataLastUpdated.asStateFlow()

    private val _isSessionValid = MutableStateFlow(false)
    val isSessionValid: StateFlow<Boolean> = _isSessionValid.asStateFlow()

    private val _brokerSwitchStatus = MutableStateFlow<String?>(null)
    val brokerSwitchStatus: StateFlow<String?> = _brokerSwitchStatus.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _alertPreferences = MutableStateFlow(appPrefs.getAlertPreferences())
    val alertPreferences: StateFlow<AlertPreferences> = _alertPreferences.asStateFlow()

    private val _isSmsAlertsEnabled = MutableStateFlow(appPrefs.isSmsAlertsEnabled())
    val isSmsAlertsEnabled: StateFlow<Boolean> = _isSmsAlertsEnabled.asStateFlow()

    private val _smsAlertPhone = MutableStateFlow(appPrefs.getSmsAlertPhone())
    val smsAlertPhone: StateFlow<String> = _smsAlertPhone.asStateFlow()

    private val _smsGatewayUrl = MutableStateFlow(appPrefs.getSmsGatewayUrl())
    val smsGatewayUrl: StateFlow<String> = _smsGatewayUrl.asStateFlow()

    private val _smsApiKey = MutableStateFlow(appPrefs.getSmsApiKey())
    val smsApiKey: StateFlow<String> = _smsApiKey.asStateFlow()

    private val _isSmsTesting = MutableStateFlow(false)
    val isSmsTesting: StateFlow<Boolean> = _isSmsTesting.asStateFlow()

    private val _smsStatusMessage = MutableStateFlow<String?>(null)
    val smsStatusMessage: StateFlow<String?> = _smsStatusMessage.asStateFlow()

    val brokerAuthManager = brokerManager.brokerAuthManager
    val brokerStatuses = brokerAuthManager.statuses
    val isBrokerAuthInitializing = brokerAuthManager.isInitializing

    fun clearBrokerSwitchStatus() {
        _brokerSwitchStatus.value = null
    }

    init {
        com.example.util.InstrumentMapUtil.setInstrumentMaster(instrumentMasterService)
        com.example.util.AlgoEngine.telegramService = telegramService
        com.example.util.AlgoEngine.alertService = alertService
        viewModelScope.launch {
            repository.checkAndSeedInitialData()

            brokerAuthManager.initialize()
            if (sessionManager.isFyersConnected && !sessionManager.fyersAccessToken.isNullOrBlank()) {
                brokerManager.fyersMarketDataService.connect()
            }

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
            _isSessionValid.value = false
            _isSessionRestoring.value = false
            return@withLock false
        }
        android.util.Log.d("SessionRestore", "session exists: true")
        _isSessionValid.value = true
        _isSessionRestoring.value = false
        repository.syncWithBroker()
        return@withLock true
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.userProfile.collectLatest { prof ->
                prof?.let { _userProfile.value = it }
            }
        }
        viewModelScope.launch {
            brokerManager.brokerAuthManager.statuses.collectLatest { statuses ->
                val actualDhanStatus = statuses["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                val currentProfile = _userProfile.value
                if (currentProfile.isDhanConnected != actualDhanStatus) {
                    _userProfile.value = currentProfile.copy(isDhanConnected = actualDhanStatus)
                }
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
        val brokerType = BrokerType.fromString(brokerName)
        viewModelScope.launch {
            _isSessionRestoring.value = true

            when (brokerType) {
                BrokerType.UPSTOX -> {
                    _brokerSwitchStatus.value = "Setting primary market data provider to Upstox..."
                    val isReady = brokerManager.upstoxMarketDataService.isConnectionLive() ||
                            brokerManager.brokerAuthManager.statuses.value["Upstox"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED ||
                            sessionManager.hasUpstoxSession()

                    if (!isReady) {
                        _isSessionRestoring.value = false
                        _brokerSwitchStatus.value = null
                        _authErrorMessage.value = "Upstox is not connected. Please authenticate first."
                        openConnectDialog("Upstox")
                        return@launch
                    }

                    brokerManager.setPrimaryMarketDataProvider("Upstox")
                    _brokerSwitchStatus.value = "Primary Market Data • Upstox"
                    _isSessionRestoring.value = false
                    repository.addNotification(
                        title = "Market Data Provider Switched",
                        message = "Primary market data feed switched to Upstox",
                        type = "SUCCESS"
                    )
                }
                BrokerType.FYERS -> {
                    _brokerSwitchStatus.value = "Setting primary market data provider to Fyers..."
                    val isReady = brokerManager.fyersMarketDataService.isConnectionLive() ||
                            brokerManager.brokerAuthManager.statuses.value["Fyers"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED ||
                            sessionManager.hasFyersSession()

                    if (!isReady) {
                        _isSessionRestoring.value = false
                        _brokerSwitchStatus.value = null
                        _authErrorMessage.value = "Fyers is not connected. Please authenticate first."
                        openConnectDialog("Fyers")
                        return@launch
                    }

                    brokerManager.setPrimaryMarketDataProvider("Fyers")
                    _brokerSwitchStatus.value = "Primary Market Data • Fyers"
                    _isSessionRestoring.value = false
                    repository.addNotification(
                        title = "Market Data Provider Switched",
                        message = "Primary market data feed switched to Fyers",
                        type = "SUCCESS"
                    )
                }
                BrokerType.ANGEL_ONE -> {
                    _brokerSwitchStatus.value = "Setting primary market data provider to Angel One..."
                    val isReady = brokerManager.angelMarketDataService.isConnectionLive() ||
                            brokerManager.brokerAuthManager.statuses.value["Angel One"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED ||
                            sessionManager.hasAngelSession()

                    if (!isReady) {
                        _isSessionRestoring.value = false
                        _brokerSwitchStatus.value = null
                        _authErrorMessage.value = "Angel One is not connected. Please authenticate first."
                        openConnectDialog("Angel One")
                        return@launch
                    }

                    brokerManager.setPrimaryMarketDataProvider("Angel One")
                    _brokerSwitchStatus.value = "Primary Market Data • Angel One"
                    _isSessionRestoring.value = false
                    repository.addNotification(
                        title = "Market Data Provider Switched",
                        message = "Primary market data feed switched to Angel One",
                        type = "SUCCESS"
                    )
                }
                BrokerType.MSTOCK -> {
                    _brokerSwitchStatus.value = "Setting primary market data provider to m.Stock..."
                    val isReady = brokerManager.mStockMarketDataService.isConnectionLive() ||
                            brokerManager.brokerAuthManager.statuses.value["m.Stock"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED ||
                            sessionManager.hasMStockSession()

                    if (!isReady) {
                        _isSessionRestoring.value = false
                        _brokerSwitchStatus.value = null
                        _authErrorMessage.value = "m.Stock is not connected. Please authenticate first."
                        openConnectDialog("m.Stock")
                        return@launch
                    }

                    brokerManager.setPrimaryMarketDataProvider("m.Stock")
                    _brokerSwitchStatus.value = "Primary Market Data • m.Stock"
                    _isSessionRestoring.value = false
                    repository.addNotification(
                        title = "Market Data Provider Switched",
                        message = "Primary market data feed switched to m.Stock",
                        type = "SUCCESS"
                    )
                }
                BrokerType.DHAN, null -> {
                    _brokerSwitchStatus.value = "Setting active order execution broker to Dhan..."
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
    }

    fun switchBroker(brokerName: String) = switchActiveBroker(brokerName)

    fun openConnectDialog(brokerType: BrokerType) {
        openConnectDialog(brokerType.displayName)
    }

    fun openConnectDialog(brokerName: String = "Dhan") {
        val type = BrokerType.fromString(brokerName) ?: BrokerType.DHAN
        _connectingBrokerName.value = type.displayName
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
                alertService.notifyBrokerConnected("Dhan", account = sessionManager.dhanClientId)
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
                alertService.notifyBrokerConnected("Angel One", account = sessionManager.angelClientId)
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
                alertService.notifyBrokerConnected("m.Stock", account = clientCode)
            } else {
                val err = res.exceptionOrNull()?.message ?: "m.Stock login failed. Please verify credentials."
                _authErrorMessage.value = err
            }
        }
    }

    
    private fun parseAuthCodeInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("code=") || trimmed.contains("auth_code=") || trimmed.startsWith("http") || trimmed.startsWith("kingkhan")) {
            val match = Regex("""[?&#](?:code|auth_code|tokenId)=([^&#]+)""", RegexOption.IGNORE_CASE).find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return trimmed
    }

    fun connectUpstox(apiKey: String, apiSecret: String, authCode: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null

            val cleanedCode = parseAuthCodeInput(authCode)
            val cleanKey = apiKey.trim()
            val cleanSecret = apiSecret.trim()

            if (cleanKey.isBlank() || cleanSecret.isBlank()) {
                _isAuthInProgress.value = false
                _authErrorMessage.value = "Upstox API Key and API Secret are required"
                brokerManager.healthManager.reportAuthFailure(
                    com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX,
                    com.example.data.network.ProviderHealthManager.STATE_CREDENTIALS_MISSING,
                    "Credentials Missing"
                )
                return@launch
            }

            if (cleanedCode.isBlank()) {
                _isAuthInProgress.value = false
                _authErrorMessage.value = "Upstox Auth Code is required"
                brokerManager.healthManager.reportAuthFailure(
                    com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX,
                    com.example.data.network.ProviderHealthManager.STATE_AUTH_CODE_MISSING,
                    "Auth code is missing"
                )
                return@launch
            }

            sessionManager.upstoxApiKey = cleanKey
            sessionManager.upstoxApiSecret = cleanSecret
            brokerManager.healthManager.reportConfigured(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX, true)
            brokerManager.healthManager.reportTokenExchange(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX)

            val res = brokerManager.upstoxAuthManager.exchangeAuthCode(cleanedCode)
            _isAuthInProgress.value = false

            if (res.isSuccess) {
                brokerManager.healthManager.reportTokenValidated(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX)
                brokerManager.healthManager.reportAuthentication(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX, true)
                _brokerSwitchStatus.value = "✓ UPSTOX CONNECTED"
                _authSuccessEvent.value = true
                _showConnectDialog.value = false
                brokerManager.upstoxMarketDataService.connect()
                alertService.notifyBrokerConnected("Upstox", account = cleanKey)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Unknown error"
                val failureState = when {
                    err.contains("TOKEN_INVALID") -> com.example.data.network.ProviderHealthManager.STATE_TOKEN_INVALID
                    err.contains("TOKEN_EXCHANGE_FAILED") -> com.example.data.network.ProviderHealthManager.STATE_TOKEN_EXCHANGE_FAILED
                    else -> com.example.data.network.ProviderHealthManager.STATE_AUTH_FAILED
                }
                brokerManager.healthManager.reportAuthFailure(
                    com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX,
                    failureState,
                    err
                )
                _authErrorMessage.value = "Upstox Authentication Failed: $err"
            }
        }
    }

    fun connectFyers(appId: String, secretId: String, authCode: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null

            val cleanedCode = parseAuthCodeInput(authCode)
            val cleanAppId = appId.trim()
            val cleanSecretId = secretId.trim()

            if (cleanAppId.isBlank() || cleanSecretId.isBlank()) {
                _isAuthInProgress.value = false
                _authErrorMessage.value = "Fyers App ID and Secret ID are required"
                brokerManager.healthManager.reportAuthFailure(
                    com.example.data.network.ProviderHealthManager.PROVIDER_FYERS,
                    com.example.data.network.ProviderHealthManager.STATE_CREDENTIALS_MISSING,
                    "Credentials Missing"
                )
                return@launch
            }

            if (cleanedCode.isBlank()) {
                _isAuthInProgress.value = false
                _authErrorMessage.value = "Fyers Auth Code is required"
                brokerManager.healthManager.reportAuthFailure(
                    com.example.data.network.ProviderHealthManager.PROVIDER_FYERS,
                    com.example.data.network.ProviderHealthManager.STATE_AUTH_CODE_MISSING,
                    "Auth code is missing"
                )
                return@launch
            }

            sessionManager.fyersAppId = cleanAppId
            sessionManager.fyersSecretId = cleanSecretId
            brokerManager.healthManager.reportConfigured(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS, true)
            brokerManager.healthManager.reportTokenExchange(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS)

            val res = brokerManager.fyersAuthManager.exchangeAuthCode(cleanedCode)
            _isAuthInProgress.value = false

            if (res.isSuccess) {
                brokerManager.healthManager.reportTokenValidated(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS)
                brokerManager.healthManager.reportAuthentication(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS, true)
                _brokerSwitchStatus.value = "✓ FYERS CONNECTED"
                _authSuccessEvent.value = true
                _showConnectDialog.value = false
                brokerManager.fyersMarketDataService.connect()
                alertService.notifyBrokerConnected("Fyers", account = cleanAppId)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Unknown error"
                val failureState = when {
                    err.contains("TOKEN_INVALID") -> com.example.data.network.ProviderHealthManager.STATE_TOKEN_INVALID
                    err.contains("TOKEN_EXCHANGE_FAILED") -> com.example.data.network.ProviderHealthManager.STATE_TOKEN_EXCHANGE_FAILED
                    else -> com.example.data.network.ProviderHealthManager.STATE_AUTH_FAILED
                }
                brokerManager.healthManager.reportAuthFailure(
                    com.example.data.network.ProviderHealthManager.PROVIDER_FYERS,
                    failureState,
                    err
                )
                _authErrorMessage.value = "Fyers Authentication Failed: $err"
            }
        }
    }

    fun startUpstoxOAuth(apiKey: String, apiSecret: String, onUrlGenerated: (String) -> Unit, onError: (String) -> Unit) {
        val cleanKey = apiKey.trim()
        val cleanSecret = apiSecret.trim()
        if (cleanKey.isBlank() || cleanSecret.isBlank()) {
            brokerManager.healthManager.reportAuthFailure(
                com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX,
                com.example.data.network.ProviderHealthManager.STATE_CREDENTIALS_MISSING,
                "Credentials Missing"
            )
            onError("API Key and API Secret are required")
            return
        }
        sessionManager.upstoxApiKey = cleanKey
        sessionManager.upstoxApiSecret = cleanSecret
        val redirectUri = sessionManager.upstoxRedirectUri.takeIf { it.isNotBlank() } ?: "https://application-beige-psi.vercel.app/oauth"
        val randomState = "upstox_" + java.util.UUID.randomUUID().toString()
        sessionManager.pendingUpstoxOAuthState = randomState
        sessionManager.pendingOAuthState = randomState
        sessionManager.pendingOAuthBroker = "Upstox"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "Upstox",
            state = randomState,
            createdAt = System.currentTimeMillis(),
            redirectUri = redirectUri,
            consumed = false
        )
        
        android.util.Log.i("UpstoxAuth", "[OAUTH_STARTED] Upstox OAuth started, state=$randomState")
        android.util.Log.i("UpstoxAuth", "[UPSTOX_AUTHORIZATION_STARTED] Initialized Upstox OAuth with state=$randomState")
        brokerManager.healthManager.reportAuthenticating(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX)
        brokerManager.healthManager.reportAuthFailure(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX, com.example.data.network.ProviderHealthManager.STATE_AUTHORIZATION_STARTED, "Authorization started")
        
        android.util.Log.i("UpstoxAuth", "[UPSTOX_WAITING_FOR_CALLBACK] Waiting for redirect callback...")
        brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX)

        val loginUrl = com.example.util.UpstoxAuthHelper.buildLoginUrl(cleanKey, redirectUri, state = randomState)
        onUrlGenerated(loginUrl)
    }

    fun startFyersOAuth(appId: String, secretId: String, onUrlGenerated: (String) -> Unit, onError: (String) -> Unit) {
        val cleanAppId = appId.trim()
        val cleanSecretId = secretId.trim()
        if (cleanAppId.isBlank() || cleanSecretId.isBlank()) {
            brokerManager.healthManager.reportAuthFailure(
                com.example.data.network.ProviderHealthManager.PROVIDER_FYERS,
                com.example.data.network.ProviderHealthManager.STATE_CREDENTIALS_MISSING,
                "Credentials Missing"
            )
            onError("App ID and Secret ID are required")
            return
        }
        sessionManager.fyersAppId = cleanAppId
        sessionManager.fyersSecretId = cleanSecretId
        val redirectUri = sessionManager.fyersRedirectUri.takeIf { it.isNotBlank() } ?: com.example.util.FyersAuthHelper.DEFAULT_REDIRECT_URI
        val randomState = "fyers_" + java.util.UUID.randomUUID().toString()
        sessionManager.pendingFyersOAuthState = randomState
        sessionManager.pendingOAuthState = randomState
        sessionManager.pendingOAuthBroker = "Fyers"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "Fyers",
            state = randomState,
            createdAt = System.currentTimeMillis(),
            redirectUri = redirectUri,
            consumed = false
        )
        
        android.util.Log.i("FyersAuth", "[OAUTH_STARTED] Fyers OAuth started, state=$randomState")
        android.util.Log.i("FyersAuth", "[FYERS_AUTHORIZATION_STARTED] Initialized Fyers OAuth with state=$randomState")
        brokerManager.healthManager.reportAuthenticating(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS)
        brokerManager.healthManager.reportAuthFailure(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS, com.example.data.network.ProviderHealthManager.STATE_AUTHORIZATION_STARTED, "Authorization started")
        
        android.util.Log.i("FyersAuth", "[FYERS_WAITING_FOR_CALLBACK] Waiting for redirect callback...")
        brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS)

        val loginUrl = com.example.util.FyersAuthHelper.buildLoginUrl(cleanAppId, redirectUri, state = randomState)
        onUrlGenerated(loginUrl)
    }

    fun reconnectBroker(brokerName: String) {
        viewModelScope.launch {
            _isSessionRestoring.value = true
            val res = brokerAuthManager.reconnectBroker(brokerName)
            _isSessionRestoring.value = false
            if (res.isSuccess && res.getOrThrow()) {
                alertService.notifyBrokerConnected(brokerName)
            } else {
                _authErrorMessage.value = "Failed to refresh $brokerName session. Re-authentication required."
                openConnectDialog(brokerName)
            }
        }
    }

    fun disconnectBroker(brokerName: String) {
        val type = BrokerType.fromString(brokerName) ?: BrokerType.DHAN
        val name = type.displayName
        viewModelScope.launch {
            brokerAuthManager.disconnectBroker(name)
            val current = _userProfile.value
            val updated = when (type) {
                BrokerType.DHAN -> current.copy(
                    isDhanConnected = false,
                    connectedBroker = if (current.connectedBroker == "Dhan") "" else current.connectedBroker
                )
                BrokerType.ANGEL_ONE -> current.copy(
                    isAngelConnected = false,
                    connectedBroker = if (current.connectedBroker == "Angel One") "" else current.connectedBroker
                )
                else -> current.copy(
                    connectedBroker = if (current.connectedBroker == name) "" else current.connectedBroker
                )
            }
            _userProfile.value = updated.copy(isDhanConnected = brokerManager.brokerAuthManager.statuses.value["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED)
            repository.updateProfile(updated)
            alertService.notifyBrokerDisconnected(name)
        }
    }

    fun removeAccountBroker(brokerName: String) {
        val type = BrokerType.fromString(brokerName) ?: BrokerType.DHAN
        val name = type.displayName
        viewModelScope.launch {
            brokerAuthManager.removeAccount(name)
            val current = _userProfile.value
            val updated = when (type) {
                BrokerType.DHAN -> current.copy(
                    isDhanConnected = false,
                    dhanClientId = "",
                    connectedBroker = if (current.connectedBroker == "Dhan") "" else current.connectedBroker
                )
                BrokerType.ANGEL_ONE -> current.copy(
                    isAngelConnected = false,
                    angelClientId = "",
                    connectedBroker = if (current.connectedBroker == "Angel One") "" else current.connectedBroker
                )
                else -> current.copy(
                    connectedBroker = if (current.connectedBroker == name) "" else current.connectedBroker
                )
            }
            _userProfile.value = updated.copy(isDhanConnected = brokerManager.brokerAuthManager.statuses.value["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED)
            repository.updateProfile(updated)
            repository.addNotification("Account Removed", "$name credentials and tokens cleared", "WARNING")
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

            val state = uri.getQueryParameter("state") ?: ""
            val callbackState = state.trim()
            val stateHasUpstox = callbackState.startsWith("upstox_") || callbackState.contains("upstox", ignoreCase = true)
            val stateHasFyers = callbackState.startsWith("fyers_") || callbackState.contains("fyers", ignoreCase = true)

            val pendingSession = sessionManager.pendingOAuthSession

            val isUpstox = (pendingSession != null && pendingSession.provider == "Upstox")
            val isFyers = (pendingSession != null && pendingSession.provider == "Fyers")

            if (isUpstox || isFyers || stateHasUpstox || stateHasFyers) {
                val providerName = if (isUpstox || stateHasUpstox) ProviderHealthManager.PROVIDER_UPSTOX else ProviderHealthManager.PROVIDER_FYERS
                val logPrefix = if (isUpstox || stateHasUpstox) "UPSTOX" else "FYERS"

                // 1. Pending OAuth session must exist
                if (pendingSession == null) {
                    val errMsg = "$logPrefix OAuth Session Mismatch/Expired: Pending OAuth Session is null"
                    android.util.Log.e("Auth", "[$logPrefix" + "_STATE_MISSING] $errMsg")
                    _authErrorMessage.value = "$logPrefix Login Failed: OAuth Session Expired / Missing"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(
                        providerName,
                        "STATE_MISSING",
                        "STATE_MISSING: Pending OAuth Session is null"
                    )
                    return@launch
                }

                // 2. Expected state must exist (not blank)
                val expectedState = pendingSession.state.trim()
                if (expectedState.isBlank()) {
                    val errMsg = "$logPrefix Expected State is blank"
                    android.util.Log.e("Auth", "[$logPrefix" + "_STATE_MISSING] $errMsg")
                    _authErrorMessage.value = "$logPrefix Login Failed: State Missing"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(
                        providerName,
                        "STATE_MISSING",
                        "STATE_MISSING: Expected State is blank"
                    )
                    return@launch
                }

                // 3. Callback state must exist (not blank)
                if (callbackState.isBlank()) {
                    val errMsg = "$logPrefix Callback State is blank"
                    android.util.Log.e("Auth", "[$logPrefix" + "_STATE_MISSING] $errMsg")
                    _authErrorMessage.value = "$logPrefix Login Failed: Callback State Missing"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(
                        providerName,
                        "STATE_MISSING",
                        "STATE_MISSING: Callback State is blank"
                    )
                    return@launch
                }

                // 4. Callback state must exactly equal expected state
                if (callbackState != expectedState) {
                    val errMsg = "$logPrefix OAuth State Mismatch! Expected '$expectedState', got '$callbackState'."
                    android.util.Log.e("Auth", "[$logPrefix" + "_STATE_MISMATCH] $errMsg")
                    _authErrorMessage.value = "$logPrefix Login Failed: State Mismatch"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(
                        providerName,
                        "STATE_MISMATCH",
                        "STATE_MISMATCH: Got '$callbackState', expected '$expectedState'"
                    )
                    return@launch
                }

                // 5. Session must not be already consumed (replay prevention)
                if (pendingSession.consumed) {
                    val errMsg = "$logPrefix OAuth callback replay detected (already consumed)"
                    android.util.Log.e("Auth", "[$logPrefix" + "_SESSION_EXPIRED] $errMsg")
                    _authErrorMessage.value = "$logPrefix Login Failed: OAuth Session Expired / Already Used"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(
                        providerName,
                        "OAUTH_SESSION_EXPIRED",
                        "OAUTH_SESSION_EXPIRED: Replay prevention triggered"
                    )
                    return@launch
                }

                // 6. Provider matching check: Never allow Upstox callback to complete Fyers, or vice versa
                if (stateHasUpstox && pendingSession.provider != "Upstox") {
                    val errMsg = "Mismatched provider callback: Got Upstox state, but pending session is for ${pendingSession.provider}"
                    android.util.Log.e("Auth", "[PROVIDER_MISMATCH] $errMsg")
                    _authErrorMessage.value = "Login Failed: Provider Mismatch"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(providerName, "STATE_MISMATCH", errMsg)
                    return@launch
                }
                if (stateHasFyers && pendingSession.provider != "Fyers") {
                    val errMsg = "Mismatched provider callback: Got Fyers state, but pending session is for ${pendingSession.provider}"
                    android.util.Log.e("Auth", "[PROVIDER_MISMATCH] $errMsg")
                    _authErrorMessage.value = "Login Failed: Provider Mismatch"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(providerName, "STATE_MISMATCH", errMsg)
                    return@launch
                }

                // Mark as consumed immediately to prevent replay
                sessionManager.pendingOAuthSession = pendingSession.copy(consumed = true)

                brokerManager.healthManager.reportCallbackReceived(providerName)
                android.util.Log.i("Auth", "[$logPrefix" + "_CALLBACK_RECEIVED] Redirect callback received with URI parameters")
                android.util.Log.i("Auth", "[CALLBACK_RECEIVED] Redirect callback received successfully")
                android.util.Log.i("Auth", "[BROKER_DETECTED] Pending broker detected as $logPrefix")

                // Check for cancellation or OAuth error query parameters
                val oauthError = uri.getQueryParameter("error") ?: uri.getQueryParameter("error_description")
                if (!oauthError.isNullOrBlank()) {
                    val errMsg = "$logPrefix Authorization Failed/Cancelled: $oauthError"
                    android.util.Log.e("Auth", "[$logPrefix" + "_AUTH_CANCELLED] $errMsg")
                    android.util.Log.e("Auth", "[DEEP_LINK_FAILED] Deep link error: $oauthError")
                    _authErrorMessage.value = errMsg
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(providerName, "AUTH_CANCELLED", "DEEP_LINK_FAILED: $oauthError")
                    return@launch
                }

                // State Machine Step 6: Validate OAuth State
                android.util.Log.i("Auth", "[$logPrefix" + "_VALIDATING_STATE] Validating OAuth State: state=$callbackState")
                brokerManager.healthManager.reportValidatingState(providerName)
                android.util.Log.i("Auth", "[$logPrefix" + "_STATE_VALID] OAuth State Validation: PASS")
                android.util.Log.i("Auth", "[STATE_VALID] OAuth State Validation: PASS")

                // Extract Authorization Code
                if (code.isNullOrBlank()) {
                    val errMsg = "$logPrefix Authorization code missing from callback response"
                    android.util.Log.e("Auth", "[$logPrefix" + "_AUTH_CODE_MISSING] $errMsg")
                    android.util.Log.e("Auth", "[AUTH_CODE_MISSING] Authorization code is missing")
                    _authErrorMessage.value = "$logPrefix Login Failed: Authorization Code Missing"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(
                        providerName,
                        "AUTH_CODE_MISSING",
                        "AUTH_CODE_MISSING"
                    )
                    return@launch
                }

                val cleanedCode = code.trim()
                // Deduplication Check
                if (cleanedCode == lastProcessedOAuthCode && System.currentTimeMillis() - lastProcessedOAuthTime < 60_000L) {
                    android.util.Log.w("Auth", "[$logPrefix" + "_DUPLICATE_CALLBACK_IGNORED] Ignoring duplicate authorization code within 60 seconds")
                    _isAuthInProgress.value = false
                    return@launch
                }
                lastProcessedOAuthCode = cleanedCode
                lastProcessedOAuthTime = System.currentTimeMillis()

                // State Machine Step 7: Auth Code Received
                brokerManager.healthManager.reportAuthCodeReceived(providerName)
                android.util.Log.i("Auth", "[$logPrefix" + "_AUTH_CODE_RECEIVED] Authorization code received successfully")
                android.util.Log.i("Auth", "[AUTH_CODE_RECEIVED] Authorization code parsed successfully")

                sessionManager.pendingOAuthBroker = ""
                if (pendingSession.provider == "Upstox") {
                    val upstoxKey = sessionManager.upstoxApiKey ?: ""
                    val upstoxSecret = sessionManager.upstoxApiSecret ?: ""
                    connectUpstox(upstoxKey, upstoxSecret, cleanedCode)
                } else {
                    val fyersAppId = sessionManager.fyersAppId ?: ""
                    val fyersSecretId = sessionManager.fyersSecretId ?: ""
                    connectFyers(fyersAppId, fyersSecretId, cleanedCode)
                }
                return@launch
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
                val isValidSession = sessionManager.hasValidSession()
                syncMarketDataQuietly()
                if (isValidSession) {
                    fetchOptionChain()
                }
                if (_marketDataLastUpdated.value.isBlank()) {
                    _marketDataLastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                }
                com.example.util.AlgoEngine.processMarketFeed(_watchlist.value, isLiveFeedActive.value)
                kotlinx.coroutines.delay(10000L)
            }
        }
    }

    private fun getAllLiveTrackingSymbols(): List<String> {
        val baseSymbols = listOf(
            "NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY",
            "SENSEX", "BANKEX",
            "CRUDEOIL", "CRUDEOIL M", "GOLD", "GOLD M", "SILVER", "SILVER M", "COPPER", "COPPER M", "NATURALGAS", "NATURALGAS M",
            "RELIANCE", "TCS", "INFY", "SBIN", "HDFCBANK", "ICICIBANK", "TATAMOTORS", "TATASTEEL"
        )
        val watchSymbols = _watchlist.value.map { it.symbol }
        return (baseSymbols + watchSymbols).distinct()
    }

    private fun syncMarketDataQuietly() {
        viewModelScope.launch {
            runCatching {
                if (sessionManager.hasValidSession()) {
                    repository.syncWithBroker()
                }
                val symbols = getAllLiveTrackingSymbols()
                val quotesRes = brokerManager.marketDataEngine.getMarketQuotes(symbols)
                quotesRes.getOrNull()?.let { quotes ->
                    if (quotes.isNotEmpty()) {
                        repository.updateWatchlistQuotes(quotes)
                    }
                }
                _marketDataLastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            }
        }
    }

    fun refreshBrokerData() {
        _apiError.value = null
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
                val symbols = getAllLiveTrackingSymbols()
                val quotesRes = brokerManager.marketDataEngine.getMarketQuotes(symbols)
                quotesRes.getOrNull()?.let { quotes ->
                    if (quotes.isNotEmpty()) {
                        repository.updateWatchlistQuotes(quotes)
                    }
                }
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
            
            val sourceExpiries = if (apiExpiries.isNotEmpty()) {
                apiExpiries
            } else {
                instrumentMasterService.getOptionExpiries(index)
            }
            
            val parsedExpiries = OptionExpiryUtil.getUpcomingExpiriesForSymbol(index, sourceExpiries)
            _availableOptionExpiries.value = parsedExpiries
            if (parsedExpiries.isNotEmpty() && !parsedExpiries.contains(_selectedOptionExpiry.value)) {
                _selectedOptionExpiry.value = parsedExpiries.first()
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
                // Strict Real Data Rule: Set empty list if no real option chain feed
                _optionStrikes.value = emptyList()
            }
        }
    }

    fun getHistoricalCandlesForIndex(indexName: String, interval: String = "15m", onResult: (List<com.example.ui.components.CandleData>) -> Unit) {
        viewModelScope.launch {
            val res = brokerManager.getHistoricalCandles(indexName, interval)
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
                val realOrderId = repository.placeOrder(order)
                val notif = appPrefs.getNotificationSettings()
                if (notif.notifyOrderUpdates) {
                    repository.addNotification("Order Submitted", "Order ID: $realOrderId\n$side $qty of $symbol @ ₹$price", type = "SUCCESS")
                }
                alertService.notifyOrderExecuted(
                    symbol = symbol,
                    contract = symbol,
                    side = side,
                    price = String.format(Locale.US, "%.2f", price),
                    quantity = qty.toString(),
                    orderId = realOrderId,
                    broker = sessionManager.activeBroker
                )
                refreshBrokerData()
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Order Placement Failed"
                _apiError.value = errorMsg
                alertService.notifyOrderRejected(
                    symbol = symbol,
                    contract = symbol,
                    side = side,
                    quantity = qty.toString(),
                    rejectionReason = errorMsg,
                    orderId = "REJ_${System.currentTimeMillis()}",
                    broker = sessionManager.activeBroker
                )
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

    fun saveTelegramSettings(token: String, chatId: String, isEnabled: Boolean, channelId: String = "") {
        val cleanToken = token.trim()
        val cleanChatId = chatId.trim()
        val cleanChannelId = channelId.trim()
        sessionManager.telegramBotToken = cleanToken
        sessionManager.telegramChatId = cleanChatId
        sessionManager.telegramChannelId = cleanChannelId
        sessionManager.isTelegramAlertsEnabled = isEnabled
        _telegramBotToken.value = cleanToken
        _telegramChatId.value = cleanChatId
        _telegramChannelId.value = cleanChannelId
        _isTelegramAlertsEnabled.value = isEnabled
    }

    fun testTelegramBot(token: String, chatId: String, channelId: String = "") {
        viewModelScope.launch {
            _isTelegramTesting.value = true
            val msg = com.example.data.network.TelegramMessageFormatter.formatTestMessage()
            val res = telegramService.sendAlert(
                text = msg,
                botToken = token,
                chatId = chatId,
                channelId = channelId.ifBlank { sessionManager.telegramChannelId }
            )
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

    fun saveAlertPreferences(prefs: AlertPreferences) {
        appPrefs.saveAlertPreferences(prefs)
        _alertPreferences.value = prefs
        alertService.preferenceManager.saveAllPreferences(prefs)
    }

    fun toggleAlertEvent(key: String, enabled: Boolean) {
        val current = _alertPreferences.value
        val updated = when (key) {
            "brokerConnected" -> current.copy(brokerConnected = enabled)
            "brokerDisconnected" -> current.copy(brokerDisconnected = enabled)
            "buyCeSignal" -> current.copy(buyCeSignal = enabled)
            "buyPeSignal" -> current.copy(buyPeSignal = enabled)
            "entryPosition" -> current.copy(entryPosition = enabled)
            "stopLossHit" -> current.copy(stopLossHit = enabled)
            "target1Hit" -> current.copy(target1Hit = enabled)
            "target2Hit" -> current.copy(target2Hit = enabled)
            "target3Hit" -> current.copy(target3Hit = enabled)
            "target4Hit" -> current.copy(target4Hit = enabled)
            "trailingSlHit" -> current.copy(trailingSlHit = enabled)
            "orderExecuted" -> current.copy(orderExecuted = enabled)
            "orderRejected" -> current.copy(orderRejected = enabled)
            "algoStarted" -> current.copy(algoStarted = enabled)
            "algoStopped" -> current.copy(algoStopped = enabled)
            "riskLimitReached" -> current.copy(riskLimitReached = enabled)
            else -> current
        }
        saveAlertPreferences(updated)
    }

    fun sendSignalToTelegram(signal: com.example.data.model.AISignalEntity) {
        viewModelScope.launch {
            if (!isLiveFeedActive.value || marketDataProviderState.value.stale) {
                repository.addNotification(
                    title = "Signal Transmission Blocked",
                    message = "Market feed is ${if (marketDataProviderState.value.stale) "STALE" else "UNAVAILABLE"}. Signals are blocked until fresh verified market ticks arrive.",
                    type = "ERROR"
                )
                return@launch
            }

            val isBullish = signal.trend.equals("BULLISH", ignoreCase = true) || signal.actionType.contains("CE", ignoreCase = true)
            if (isBullish) {
                alertService.notifyAiBuyCeSignal(
                    symbol = signal.symbol,
                    contract = "${signal.symbol} ${signal.actionType}",
                    entry = String.format(Locale.US, "%.2f", signal.ltp),
                    sl = String.format(Locale.US, "%.2f", signal.stopLoss),
                    t1 = String.format(Locale.US, "%.2f", signal.target1),
                    t2 = String.format(Locale.US, "%.2f", signal.target2),
                    t3 = if (signal.target3 > 0) String.format(Locale.US, "%.2f", signal.target3) else "",
                    t4 = if (signal.target4 > 0) String.format(Locale.US, "%.2f", signal.target4) else "",
                    confidence = signal.confidence
                )
            } else {
                alertService.notifyAiBuyPeSignal(
                    symbol = signal.symbol,
                    contract = "${signal.symbol} ${signal.actionType}",
                    entry = String.format(Locale.US, "%.2f", signal.ltp),
                    sl = String.format(Locale.US, "%.2f", signal.stopLoss),
                    t1 = String.format(Locale.US, "%.2f", signal.target1),
                    t2 = String.format(Locale.US, "%.2f", signal.target2),
                    t3 = if (signal.target3 > 0) String.format(Locale.US, "%.2f", signal.target3) else "",
                    t4 = if (signal.target4 > 0) String.format(Locale.US, "%.2f", signal.target4) else "",
                    confidence = signal.confidence
                )
            }
            repository.addNotification(
                title = "Signal Dispatched",
                message = "AI Signal for ${signal.symbol} routed to Telegram & Alert channels.",
                type = "SUCCESS"
            )
        }
    }

    fun sendBrokerOrderToTelegram(order: OrderEntity) {
        viewModelScope.launch {
            if (order.status.equals("REJECTED", ignoreCase = true) || order.status.equals("CANCELLED", ignoreCase = true)) {
                alertService.notifyOrderRejected(
                    symbol = order.symbol,
                    contract = order.symbol,
                    side = order.side,
                    quantity = order.qty.toString(),
                    rejectionReason = "Status: ${order.status}",
                    orderId = order.brokerOrderId.ifBlank { order.orderId },
                    broker = sessionManager.activeBroker
                )
            } else {
                alertService.notifyOrderExecuted(
                    symbol = order.symbol,
                    contract = order.symbol,
                    side = order.side,
                    price = String.format(Locale.US, "%.2f", order.price),
                    quantity = order.qty.toString(),
                    orderId = order.brokerOrderId.ifBlank { order.orderId },
                    broker = sessionManager.activeBroker
                )
            }
        }
    }

    fun saveSmsSettings(phone: String, enabled: Boolean, gatewayUrl: String = "", apiKey: String = "") {
        appPrefs.setSmsAlertPhone(phone)
        appPrefs.setSmsAlertsEnabled(enabled)
        appPrefs.setSmsGatewayUrl(gatewayUrl)
        appPrefs.setSmsApiKey(apiKey)
        _smsAlertPhone.value = phone
        _isSmsAlertsEnabled.value = enabled
        _smsGatewayUrl.value = gatewayUrl
        _smsApiKey.value = apiKey
    }

    fun testSmsAlert(phone: String, message: String) {
        viewModelScope.launch {
            _isSmsTesting.value = true
            _smsStatusMessage.value = null
            val testMsg = if (message.isNotBlank()) message else "KK AI TRADE TEST: SMS alerts configured successfully for +91${phone.takeLast(10)} at ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
            val res = alertService.smsService.sendAlertSms(testMsg, phone)
            _isSmsTesting.value = false
            if (res.isSuccess) {
                _smsStatusMessage.value = "SMS Alert dispatched successfully to $phone"
            } else {
                _smsStatusMessage.value = "SMS Dispatch failed: ${res.exceptionOrNull()?.message ?: "Unknown Error"}"
            }
        }
    }

    fun clearSmsStatus() {
        _smsStatusMessage.value = null
    }

    fun toggleBiometric(enabled: Boolean) {
        sessionManager.isBiometricEnabled = enabled
    }
}
