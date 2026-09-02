package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private val tradingDao = TradingDatabase.getDatabase(application).tradingDao()
    private val repository = TradingRepository(tradingDao, brokerManager)
    val diagnosticEngine = com.example.util.diagnostic.SelfDiagnosticEngine(brokerManager)
    val alertService = com.example.util.alert.AlertService(
        context = application,
        sessionManager = sessionManager,
        appPreferences = appPrefs,
        telegramService = telegramService,
        repository = repository
    )

//     UI States
    private val _userProfile = MutableStateFlow(UserProfileEntity())
    val userProfile: StateFlow<UserProfileEntity> = _userProfile.asStateFlow()

    val watchlist: StateFlow<List<WatchlistItem>> = kotlinx.coroutines.flow.combine(
        tradingDao.getWatchlist("ALL"),
        com.example.data.model.MarketDataStore.ticks
    ) { dbList, liveTicks ->
        dbList.map { item ->
            val tick = liveTicks[item.symbol] 
                ?: liveTicks[item.symbol.replace(" ", "")] 
                ?: liveTicks[com.example.data.model.MarketUniverse.getCanonicalUnderlying(item.symbol)]
                ?: liveTicks.values.find { liveItem ->
                    val liveSymbol = liveItem.symbol.uppercase().trim()
                    val dbSymbol = item.symbol.uppercase().trim()
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
            if (tick != null && tick.price > 0.0) {
                val prevClose = if (item.change != 0.0) (item.ltp - item.change) else tick.price
                val liveChange = tick.price - prevClose
                val liveChangePct = if (prevClose > 0.0) (liveChange / prevClose) * 100.0 else 0.0
                item.copy(
                    ltp = tick.price,
                    change = liveChange,
                    changePercent = liveChangePct,
                    isPositive = liveChange >= 0
                )
            } else {
                item
            }
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    private val _holdings = MutableStateFlow<List<PortfolioHoldingEntity>>(emptyList())
    val holdings: StateFlow<List<PortfolioHoldingEntity>> = _holdings.asStateFlow()

    private val _aiSignals = MutableStateFlow<List<AISignalEntity>>(emptyList())
    val aiSignals: StateFlow<List<AISignalEntity>> = _aiSignals.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
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
    private val _exitingOrderIds = MutableStateFlow<Set<String>>(emptySet())
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

    private var lastReceivedOAuthCode: String? = null
    private var lastReceivedOAuthTime: Long = 0L

    private val _selectedExchange = MutableStateFlow("NSE")
    val selectedExchange: StateFlow<String> = _selectedExchange.asStateFlow()

    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()

    private val _isSessionRestoring = MutableStateFlow(true)
    val isSessionRestoring: StateFlow<Boolean> = _isSessionRestoring.asStateFlow()

    
    private val _marketDataSource = MutableStateFlow(brokerManager.currentMarketDataSource)
    val marketDataSource: StateFlow<String> = _marketDataSource.asStateFlow()
    val isLiveFeedActive: StateFlow<Boolean> = com.example.data.model.MarketDataStore.providerState
        .map { it.live && !it.stale && it.provider != com.example.data.model.MarketDataProviders.DHAN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


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

    private val _liveIndexExpiries = MutableStateFlow<Map<String, String>>(emptyMap())
    val liveIndexExpiries: StateFlow<Map<String, String>> = _liveIndexExpiries.asStateFlow()

    private val _isLoadingLiveExpiries = MutableStateFlow(false)
    val isLoadingLiveExpiries: StateFlow<Boolean> = _isLoadingLiveExpiries.asStateFlow()

    fun fetchLiveIndexExpiries() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingLiveExpiries.value = true
            val indices = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX", "CRUDEOIL", "CRUDEOIL M")
            val expMap = mutableMapOf<String, String>()
            
            // Try to fetch sequentially or concurrently
            indices.map { index ->
                async {
                    val expiries = repository.getOptionExpiries(index)
                    if (expiries.isNotEmpty()) {
                        expMap[index] = expiries.first()
                    } else {
//                         Fallback purely based on rules if completely empty
                        expMap[index] = com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(index).firstOrNull() ?: "--"
                    }
                }
            }.awaitAll()
            
            _liveIndexExpiries.value = expMap
            _isLoadingLiveExpiries.value = false
        }
    }

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
            try {
                repository.checkAndSeedInitialData()
                brokerAuthManager.initialize()
                runCatching { brokerManager.marketDataEngine.retryConnection() }
            } catch(e: Exception) { 
                e.printStackTrace() 
            } finally {
                validateAndRestoreSession()
            }
        }
        observeData()
        setSelectedOptionIndex("NIFTY")
        startLiveMarketFeed()
    }

    private val sessionRestoreMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun validateAndRestoreSession(): Boolean = sessionRestoreMutex.withLock {
        _isSessionRestoring.value = true
        try {
            android.util.Log.i("MainViewModel", "[SESSION_RESTORE] Starting full session validation and restoration flow...")
            brokerManager.brokerAuthManager.initialize()
            
            val hasConnectedBroker = brokerManager.brokerAuthManager.hasAnyConnectedBroker()
            _isSessionValid.value = hasConnectedBroker
            android.util.Log.i("MainViewModel", "[SESSION_RESTORE] Validated connected brokers present: $hasConnectedBroker")
            
            // Restore active broker based on persisted selection
            val savedActiveBroker = sessionManager.activeBroker.trim()
            val isSavedBrokerValid = when (savedActiveBroker) {
                "Dhan" -> sessionManager.isDhanConnected && brokerManager.brokerAuthManager.statuses.value["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                "Angel One" -> sessionManager.isAngelConnected && brokerManager.brokerAuthManager.statuses.value["Angel One"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                "Upstox" -> sessionManager.isUpstoxConnected && brokerManager.brokerAuthManager.statuses.value["Upstox"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                "Fyers" -> sessionManager.isFyersConnected && brokerManager.brokerAuthManager.statuses.value["Fyers"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                else -> false
            }
            if (isSavedBrokerValid) {
                brokerManager.setActiveBroker(savedActiveBroker)
            } else if (savedActiveBroker.isNotBlank()) {
                // If the previously selected broker is no longer valid, clear it rather than silently switching
                sessionManager.activeBroker = ""
            }
            
            if (hasConnectedBroker) {
                repository.syncWithBroker()
            }
            return@withLock hasConnectedBroker
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "[SESSION_RESTORE_ERROR] Error restoring session: ${e.message}", e)
            val fallbackValid = brokerManager.brokerAuthManager.hasAnyConnectedBroker()
            _isSessionValid.value = fallbackValid
            return@withLock fallbackValid
        } finally {
            _isSessionRestoring.value = false
        }
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
                val actualAngelStatus = statuses["Angel One"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                val actualUpstoxStatus = statuses["Upstox"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                val actualFyersStatus = statuses["Fyers"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                val currentProfile = _userProfile.value
                if (currentProfile.isDhanConnected != actualDhanStatus ||
                    currentProfile.isAngelConnected != actualAngelStatus ||
                    currentProfile.isUpstoxConnected != actualUpstoxStatus ||
                    currentProfile.isFyersConnected != actualFyersStatus) {
                    _userProfile.value = currentProfile.copy(
                        isDhanConnected = actualDhanStatus,
                        isAngelConnected = actualAngelStatus,
                        isUpstoxConnected = actualUpstoxStatus,
                        isFyersConnected = actualFyersStatus
                    )
                }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(repository.watchlistAll, com.example.data.model.MarketDataStore.ticks) { dbList, liveData ->
                var changed = false
                val updatedList = dbList.map { item ->
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
                    if (live != null && live.price > 0 && live.price != item.ltp) {
                        val prevClose = if (item.ltp > 0) item.ltp - item.change else 0.0
                        val newChange = if (prevClose > 0) live.price - prevClose else item.change
                        val newChangePct = if (prevClose > 0) (newChange / prevClose) * 100.0 else item.changePercent
                        changed = true
                        item.copy(
                            ltp = live.price,
                            change = newChange,
                            changePercent = newChangePct,
                            isPositive = newChange >= 0
                        )
                    } else {
                        item
                    }
                }
                Pair(updatedList, changed)
            }.collectLatest { pair: Pair<List<com.example.data.model.WatchlistItem>, Boolean> ->
                val combinedList = pair.first
                val changed = pair.second
                if (changed) {
                    val onlyWithLtp = (combinedList as List<com.example.data.model.WatchlistItem>).filter { it.ltp > 0.0 }
                    if (onlyWithLtp.isNotEmpty()) {
                        repository.updateWatchlistQuotes(onlyWithLtp)
                    }
                }
            }
        }
        
        viewModelScope.launch {
            com.example.data.model.MarketDataStore.ticks.collect { liveData ->
                val currentStrikes = _optionStrikes.value
                if (currentStrikes.isEmpty()) return@collect
                var changed = false
                val newStrikes = currentStrikes.map { strike ->
                    var newStrike = strike
                    
                    val callLive = liveData.values.find { liveItem ->
                        val liveSymbol = liveItem.symbol.uppercase().trim()
                        val callSymbol = strike.callSymbol.uppercase().trim()
                        liveSymbol == callSymbol || liveSymbol.endsWith("|$callSymbol") || liveSymbol.substringAfter("|") == callSymbol
                    }
                    if (callLive != null && callLive.price > 0.0 && callLive.price != strike.callLtp) {
                        newStrike = newStrike.copy(callLtp = callLive.price)
                        changed = true
                    }
                    
                    val putLive = liveData.values.find { liveItem ->
                        val liveSymbol = liveItem.symbol.uppercase().trim()
                        val putSymbol = strike.putSymbol.uppercase().trim()
                        liveSymbol == putSymbol || liveSymbol.endsWith("|$putSymbol") || liveSymbol.substringAfter("|") == putSymbol
                    }
                    if (putLive != null && putLive.price > 0.0 && putLive.price != newStrike.putLtp) {
                        newStrike = newStrike.copy(putLtp = putLive.price)
                        changed = true
                    }
                    
                    newStrike
                }
                
                if (changed) {
                    _optionStrikes.value = newStrikes
                }
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
            kotlinx.coroutines.flow.combine(repository.allHoldings, com.example.data.model.MarketDataStore.ticks) { list, liveTicks ->
                list.map { item ->
                    val tick = liveTicks.values.find { liveItem ->
                        val liveSym = liveItem.symbol.uppercase().trim()
                        val itemSym = item.symbol.uppercase().trim()
                        liveSym == itemSym ||
                        liveSym.endsWith("|$itemSym") ||
                        liveSym.substringAfter("|") == itemSym ||
                        liveSym.replace(" ", "") == itemSym.replace(" ", "")
                    } ?: com.example.data.model.MarketDataStore.getTick(item.symbol)

                    val liveLtp = tick?.price ?: item.ltp
                    if (liveLtp > 0.0) {
                        val isClosed = item.positionStatus.equals("CLOSED", ignoreCase = true) || item.qty == 0
                        val netQty = item.qty
                        val avgPrice = item.avgPrice
                        val invested = kotlin.math.abs(netQty * avgPrice)
                        val unrealized = if (!isClosed) {
                            if (netQty > 0) (liveLtp - avgPrice) * netQty
                            else (avgPrice - liveLtp) * kotlin.math.abs(netQty)
                        } else {
                            item.unrealizedPnl
                        }
                        val currVal = if (isClosed) 0.0 else (kotlin.math.abs(netQty) * liveLtp)
                        val totalPnl = item.realizedPnl + unrealized
                        val pnlPct = if (invested > 0.0) (totalPnl / invested) * 100.0 else 0.0
                        item.copy(
                            ltp = liveLtp,
                            currentValue = currVal,
                            unrealizedPnl = unrealized,
                            pnl = totalPnl,
                            pnlPercent = pnlPct
                        )
                    } else {
                        item
                    }
                }
            }.collectLatest { enrichedList ->
                _holdings.value = enrichedList
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
                    val upstoxStatus = brokerManager.brokerAuthManager.statuses.value["Upstox"]?.status
                    val isReady = brokerManager.upstoxMarketDataService.isConnectionLive() ||
                            (upstoxStatus != null && upstoxStatus != com.example.data.network.BrokerAuthStatus.NOT_CONFIGURED && upstoxStatus != com.example.data.network.BrokerAuthStatus.DISCONNECTED && upstoxStatus != com.example.data.network.BrokerAuthStatus.ERROR) ||
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
                    val fyersStatus = brokerManager.brokerAuthManager.statuses.value["Fyers"]?.status
                    val isReady = brokerManager.fyersMarketDataService.isConnectionLive() ||
                            (fyersStatus != null && fyersStatus != com.example.data.network.BrokerAuthStatus.NOT_CONFIGURED && fyersStatus != com.example.data.network.BrokerAuthStatus.DISCONNECTED && fyersStatus != com.example.data.network.BrokerAuthStatus.ERROR) ||
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
                    val angelStatus = brokerManager.brokerAuthManager.statuses.value["Angel One"]?.status
                    val isReady = brokerManager.angelMarketDataService.isConnectionLive() ||
                            (angelStatus != null && angelStatus != com.example.data.network.BrokerAuthStatus.NOT_CONFIGURED && angelStatus != com.example.data.network.BrokerAuthStatus.DISCONNECTED && angelStatus != com.example.data.network.BrokerAuthStatus.ERROR) ||
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
            _authErrorMessage.value = null
            val cleanClientId = clientId.trim()
            val cleanToken = accessToken.trim()
            if (cleanClientId.isNotBlank() && cleanToken.isNotBlank()) {
                sessionManager.dhanClientId = cleanClientId
                sessionManager.dhanAccessToken = cleanToken
                sessionManager.isDhanConnected = true
                sessionManager.activeBroker = "Dhan"
                brokerManager.setActiveBroker("Dhan")

                var liveMargin = 0.0
                var liveRealized = 0.0
                var liveUnrealized = 0.0
                var pnlState = PnlState.UNAVAILABLE.name
                var marginState = MarginState.UNAVAILABLE.name
                var lastPnlTime = 0L
                var lastMarginTime = 0L
                var fetchedName = "Dhan ($cleanClientId)"

                runCatching {
                    val profRes = brokerManager.getProfile()
                    if (profRes.isSuccess) {
                        val prof = profRes.getOrThrow()
                        liveMargin = prof.availableMargin
                        liveRealized = prof.realizedPnl
                        liveUnrealized = prof.unrealizedPnl
                        pnlState = prof.pnlStatus
                        marginState = prof.marginStatus
                        lastPnlTime = prof.lastPnlSyncTime
                        lastMarginTime = prof.lastMarginSyncTime
                        if (prof.name.isNotBlank()) {
                            fetchedName = prof.name
                        }
                    }
                }

                brokerManager.brokerAuthManager.updateStatus(
                    "Dhan",
                    "Primary Order Execution",
                    com.example.data.network.BrokerAuthStatus.CONNECTED,
                    "Active for Order Execution (Client: $cleanClientId • Margin: ₹${String.format(java.util.Locale.US, "%.2f", liveMargin)})"
                )
                val current = _userProfile.value
                val updated = current.copy(
                    name = fetchedName,
                    isDhanConnected = true,
                    dhanClientId = cleanClientId,
                    connectedBroker = "Dhan",
                    availableMargin = liveMargin,
                    accountBalance = liveMargin,
                    realizedPnl = liveRealized,
                    unrealizedPnl = liveUnrealized,
                    todaysPnl = liveRealized + liveUnrealized,
                    pnlStatus = pnlState,
                    marginStatus = marginState,
                    lastPnlSyncTime = lastPnlTime,
                    lastMarginSyncTime = lastMarginTime
                )
                _userProfile.value = updated
                repository.updateProfile(updated)
                _isSessionValid.value = true
                _authSuccessEvent.value = true
                _showConnectDialog.value = false
                repository.addNotification(
                    title = "Dhan Connected",
                    message = "DhanHQ account $cleanClientId connected (Margin: ₹${String.format(java.util.Locale.US, "%.2f", liveMargin)}) ⚡",
                    type = "SUCCESS"
                )
                refreshBrokerData()
            } else {
                _authErrorMessage.value = "Client ID and Access Token are required"
            }
            _isAuthInProgress.value = false
        }
    }

    fun loginAngel(clientCode: String, mpin: String, apiKey: String, totpSecret: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            val cleanCode = clientCode.trim().uppercase()
            val cleanMpin = mpin.trim()
            val cleanKey = apiKey.trim()
            val cleanTotp = totpSecret.trim()
            
            val res = brokerManager.brokerAuthManager.connectAngelOne(cleanCode, cleanMpin, cleanKey, cleanTotp)
            if (res.isSuccess) {
                val current = _userProfile.value
                val updated = current.copy(
                    isAngelConnected = true,
                    angelClientId = cleanCode,
                    connectedBroker = if (current.connectedBroker.isBlank()) "Angel One" else current.connectedBroker
                )
                _userProfile.value = updated
                repository.updateProfile(updated)
                _isSessionValid.value = true
                _authSuccessEvent.value = true
                _showConnectDialog.value = false
                repository.addNotification(
                    title = "Angel One Connected",
                    message = "Angel One account $cleanCode authenticated successfully 📊",
                    type = "SUCCESS"
                )
                refreshBrokerData()
            } else {
                _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Angel login failed"
            }
            _isAuthInProgress.value = false
        }
    }

     fun parseAuthCodeInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("code=") || trimmed.contains("auth_code=") || trimmed.contains("tokenId=") || trimmed.startsWith("http") || trimmed.startsWith("kingkhan")) {
            val authCodeMatch = Regex("""[?&#]auth_code=([^&#]+)""", RegexOption.IGNORE_CASE).find(trimmed)
            if (authCodeMatch != null && authCodeMatch.groupValues.size > 1 && authCodeMatch.groupValues[1] != "200") {
                return authCodeMatch.groupValues[1]
            }
            val codeMatch = Regex("""[?&#](?:code|tokenId|consentId)=([^&#]+)""", RegexOption.IGNORE_CASE).find(trimmed)
            if (codeMatch != null && codeMatch.groupValues.size > 1 && codeMatch.groupValues[1] != "200" && codeMatch.groupValues[1] != "0") {
                return codeMatch.groupValues[1]
            }
        }
        return trimmed
    }

    fun initiateUpstoxLogin(apiKey: String, apiSecret: String, context: android.content.Context) {
        startUpstoxOAuth(
            apiKey = apiKey,
            apiSecret = apiSecret,
            onUrlGenerated = { loginUrl ->
                _authErrorMessage.value = null
                _isAuthInProgress.value = true
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    _isAuthInProgress.value = false
                    _authErrorMessage.value = "Failed to open browser: ${e.localizedMessage}"
                }
            },
            onError = { err ->
                _authErrorMessage.value = err
            }
        )
    }

    fun initiateFyersLogin(appId: String, secretId: String, context: android.content.Context) {
        startFyersOAuth(
            appId = appId,
            secretId = secretId,
            onUrlGenerated = { loginUrl ->
                _authErrorMessage.value = null
                _isAuthInProgress.value = true
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    _isAuthInProgress.value = false
                    _authErrorMessage.value = "Failed to open browser: ${e.localizedMessage}"
                }
            },
            onError = { err ->
                _authErrorMessage.value = err
            }
        )
    }

    fun connectUpstox(apiKey: String, apiSecret: String, authCodeOrToken: String = "") {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            val cleanKey = apiKey.trim()
            val cleanSecret = apiSecret.trim()
            val cleanInput = authCodeOrToken.trim()
            
            try {
                if (cleanKey.isNotBlank()) sessionManager.upstoxApiKey = cleanKey
                if (cleanSecret.isNotBlank()) sessionManager.upstoxApiSecret = cleanSecret
                
                if (cleanInput.isNotBlank()) {
                    val res = brokerManager.upstoxAuthManager.exchangeAuthCode(cleanInput)
                    if (res.isSuccess) {
                        sessionManager.isUpstoxConnected = true
                        brokerManager.brokerAuthManager.updateStatus(
                            "Upstox",
                            "Secondary Data Feed",
                            com.example.data.network.BrokerAuthStatus.CONNECTED,
                            "Connected (Live Protobuf Market Stream)"
                        )
                        _showConnectDialog.value = false
                        repository.addNotification(
                            title = "Upstox Connected",
                            message = "Upstox market streamer connected successfully ⚡",
                            type = "SUCCESS"
                        )
                        refreshBrokerData()
                    } else {
                        _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Upstox authorization failed"
                    }
                } else if (cleanKey.isNotBlank()) {
                    _authErrorMessage.value = "Please click 'OPEN UPSTOX LOGIN' to authorize or paste your Token/Code"
                } else {
                    _authErrorMessage.value = "Upstox API Key is required"
                }
            } catch (e: Exception) {
                _authErrorMessage.value = e.message ?: "Upstox connection error"
            }
            _isAuthInProgress.value = false
        }
    }

    fun connectFyers(appId: String, secretId: String, authCodeOrToken: String = "") {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            val cleanAppId = appId.trim()
            val cleanSecret = secretId.trim()
            val cleanInput = authCodeOrToken.trim()
            
            try {
                if (cleanAppId.isNotBlank()) sessionManager.fyersAppId = cleanAppId
                if (cleanSecret.isNotBlank()) sessionManager.fyersSecretId = cleanSecret
                
                if (cleanInput.isNotBlank()) {
                    val res = brokerManager.fyersAuthManager.exchangeAuthCode(cleanInput)
                    if (res.isSuccess) {
                        sessionManager.isFyersConnected = true
                        val current = _userProfile.value
                        val updated = current.copy(
                            isFyersConnected = true,
                            connectedBroker = if (current.connectedBroker.isBlank()) "Fyers" else current.connectedBroker
                        )
                        _userProfile.value = updated
                        repository.updateProfile(updated)
                        brokerManager.brokerAuthManager.updateStatus(
                            "Fyers",
                            "Primary Market Data Feed",
                            com.example.data.network.BrokerAuthStatus.CONNECTED,
                            "Connected (Live V3 WebSocket Feeds)"
                        )
                        _showConnectDialog.value = false
                        repository.addNotification(
                            title = "Fyers Connected",
                            message = "Fyers market data feed connected successfully 🚀",
                            type = "SUCCESS"
                        )
                        refreshBrokerData()
                    } else {
                        _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Fyers authorization failed"
                    }
                } else if (cleanAppId.isNotBlank()) {
                    _authErrorMessage.value = "Please click 'OPEN FYERS LOGIN' to authorize or paste your Token/Code"
                } else {
                    _authErrorMessage.value = "Fyers App ID is required"
                }
            } catch (e: Exception) {
                _authErrorMessage.value = e.message ?: "Fyers connection error"
            }
            _isAuthInProgress.value = false
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
        val redirectUri = "https://application-beige-psi.vercel.app/oauth".takeIf { it.isNotBlank() } ?: "https://application-beige-psi.vercel.app/oauth"
        val randomState = com.example.util.UpstoxAuthHelper.generateSecureState()
        sessionManager.pendingUpstoxOAuthState = randomState
        sessionManager.pendingOAuthState = randomState
        sessionManager.pendingOAuthBroker = "Upstox"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "UPSTOX",
            state = randomState,
            createdAt = System.currentTimeMillis(),
            consumed = false
        )
        
        android.util.Log.i("UpstoxAuth", "[OAUTH_STARTED] Upstox OAuth started, state=$randomState")
        android.util.Log.i("UpstoxAuth", "[UPSTOX_AUTHORIZATION_STARTED] Initialized Upstox OAuth with state=$randomState")
        brokerManager.healthManager.reportAuthenticating(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX)
        
        android.util.Log.i("UpstoxAuth", "[UPSTOX_WAITING_FOR_CALLBACK] Waiting for redirect callback...")
        brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX, randomState)

        val loginUrl = com.example.util.UpstoxAuthHelper.buildLoginUrl(cleanKey, redirectUri)
        onUrlGenerated(loginUrl)
    }

    fun startFyersOAuth(appId: String, secretId: String, onUrlGenerated: (String) -> Unit, onError: (String) -> Unit) {
        val cleanAppId = com.example.util.FyersAuthHelper.getFullAppId(appId)
        val cleanSecretId = secretId.trim()
        if (cleanAppId.isBlank()) {
            brokerManager.healthManager.reportAuthFailure(
                com.example.data.network.ProviderHealthManager.PROVIDER_FYERS,
                com.example.data.network.ProviderHealthManager.STATE_CREDENTIALS_MISSING,
                "Credentials Missing"
            )
            onError("App ID is required")
            return
        }
        sessionManager.fyersAppId = cleanAppId
        if (cleanSecretId.isNotBlank()) {
            sessionManager.fyersSecretId = cleanSecretId
        }
        val rawRedirect = sessionManager.fyersRedirectUri
        val redirectUri = if (rawRedirect.isBlank() || rawRedirect.contains("kingkhan://")) {
            com.example.util.FyersAuthHelper.DEFAULT_REDIRECT_URI
        } else {
            rawRedirect
        }
        if (sessionManager.fyersRedirectUri != redirectUri) {
            sessionManager.fyersRedirectUri = redirectUri
        }
        val randomState = com.example.util.FyersAuthHelper.generateSecureState()
        sessionManager.pendingFyersOAuthState = randomState
        sessionManager.pendingOAuthState = randomState
        sessionManager.pendingOAuthBroker = "Fyers"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "FYERS",
            state = randomState,
            createdAt = System.currentTimeMillis(),
            consumed = false
        )
        
        android.util.Log.i("FyersAuth", "[OAUTH_STARTED] Fyers OAuth started, state=$randomState")
        android.util.Log.i("FyersAuth", "[FYERS_AUTHORIZATION_STARTED] Initialized Fyers OAuth with state=$randomState")
        brokerManager.healthManager.reportAuthenticating(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS)
        
        android.util.Log.i("FyersAuth", "[FYERS_WAITING_FOR_CALLBACK] Waiting for redirect callback...")
        brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS, randomState)

        val loginUrl = com.example.util.FyersAuthHelper.buildLoginUrl(cleanAppId, redirectUri)
        onUrlGenerated(loginUrl)
    }

    private var currentlyProcessingDhanFingerprint: String? = null

    fun startDhanOAuth(clientId: String? = null, apiKey: String? = null, clientSecret: String? = null, onUrlGenerated: (String) -> Unit, onError: (String) -> Unit) {
        val redirectUri = com.example.util.BrokerConfig.dhanRedirectUri.ifBlank { "kingkhan://oauth/callback" }
        val randomState = com.example.util.DhanAuthHelper.generateSecureState()

        sessionManager.pendingOAuthBroker = "Dhan"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "DHAN",
            state = randomState,
            createdAt = System.currentTimeMillis(),
            consumed = false
        )

        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            android.util.Log.i("DhanAuth", "[DHAN_CONSENT_CREATED] OAuth session created for Dhan")
            val consentRes = com.example.util.DhanAuthHelper.generateConsent(clientId, apiKey, clientSecret, )
            _isAuthInProgress.value = false
            consentRes.onSuccess { url ->
                android.util.Log.i("DhanAuth", "[DHAN_BROWSER_LOGIN_STARTED] Navigating user to Dhan login...")
                onUrlGenerated(url)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to generate Dhan OAuth consent URL"
                android.util.Log.e("DhanAuth", "Dhan consent URL generation failed: $msg")
                _authErrorMessage.value = msg
                onError(msg)
            }
        }
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
                BrokerType.UPSTOX -> current.copy(
                    isUpstoxConnected = false,
                    connectedBroker = if (current.connectedBroker == "Upstox") "" else current.connectedBroker
                )
                BrokerType.FYERS -> current.copy(
                    isFyersConnected = false,
                    connectedBroker = if (current.connectedBroker == "Fyers") "" else current.connectedBroker
                )
                else -> current.copy(
                    connectedBroker = if (current.connectedBroker == name) "" else current.connectedBroker
                )
            }
            _userProfile.value = updated
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
                BrokerType.UPSTOX -> current.copy(
                    isUpstoxConnected = false,
                    connectedBroker = if (current.connectedBroker == "Upstox") "" else current.connectedBroker
                )
                BrokerType.FYERS -> current.copy(
                    isFyersConnected = false,
                    connectedBroker = if (current.connectedBroker == "Fyers") "" else current.connectedBroker
                )
                else -> current.copy(
                    connectedBroker = if (current.connectedBroker == name) "" else current.connectedBroker
                )
            }
            _userProfile.value = updated
            repository.updateProfile(updated)
            repository.addNotification("Account Removed", "$name credentials and tokens cleared", "WARNING")
        }
    }

    fun handleOAuthRedirect(uri: Uri) {
        viewModelScope.launch {
            val scheme = uri.scheme ?: ""
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            val fullUrl = uri.toString()

            android.util.Log.d("DhanAuth", "Processing OAuth redirect callback: scheme=$scheme, host=$host, path=$path")

            var token = uri.getQueryParameter("access_token")
                ?: uri.getQueryParameter("token")
                ?: uri.getQueryParameter("accessToken")

            val fyersAuthCode = uri.getQueryParameter("auth_code")
            val genericCode = uri.getQueryParameter("code")
            var genericTokenId = uri.getQueryParameter("tokenId") ?: uri.getQueryParameter("consentId")

            val clientId = uri.getQueryParameter("client_id")
                ?: uri.getQueryParameter("clientId")

            if (token.isNullOrBlank()) {
                val tokenMatch = Regex("""[?&#](?:access_token|accessToken|token)=([^&#]+)""", RegexOption.IGNORE_CASE).find(fullUrl)
                token = tokenMatch?.groupValues?.get(1)
            }

            if (genericTokenId.isNullOrBlank()) {
                val tokenIdMatch = Regex("""[?&#](?:tokenId|consentId)=([^&#]+)""", RegexOption.IGNORE_CASE).find(fullUrl)
                genericTokenId = tokenIdMatch?.groupValues?.get(1)
            }

            val state = uri.getQueryParameter("state") ?: ""
            val callbackState = state.trim()

            val pendingSession = sessionManager.pendingOAuthSession

            val isDhanCallback = (pendingSession?.provider?.equals("DHAN", ignoreCase = true) == true) ||
                (callbackState.startsWith("dhan_")) ||
                (!genericTokenId.isNullOrBlank() && scheme == "kingkhan") ||
                (host.contains("kingkhan") && genericTokenId != null)

            if (isDhanCallback) {
                handleDhanOAuthCallback(
                    uri = uri,
                    fullUrl = fullUrl,
                    token = token,
                    genericTokenId = genericTokenId,
                    genericCode = genericCode,
                    clientId = clientId,
                    callbackState = callbackState,
                    pendingSession = pendingSession
                )
                return@launch
            }

            _isAuthInProgress.value = true
            _authErrorMessage.value = null

            // Determine provider intelligently
            val inferredProvider = pendingSession?.provider?.uppercase()?.trim()?.ifBlank { null }
                ?: if (callbackState.startsWith("upstox_") || fullUrl.contains("upstox") || scheme == "kingkhan" && !fullUrl.contains("fyers")) "UPSTOX"
                else if (callbackState.startsWith("fyers_") || fyersAuthCode != null || fullUrl.contains("fyers")) "FYERS"
                else "UPSTOX"

            // Extract correct authorization code based on provider
            var code: String? = null
            if (inferredProvider == "FYERS") {
                code = fyersAuthCode
                if (code.isNullOrBlank() && !genericCode.isNullOrBlank() && genericCode != "200" && genericCode != "0") {
                    code = genericCode
                }
            } else if (inferredProvider == "UPSTOX") {
                code = genericCode ?: fyersAuthCode
            } else {
                code = fyersAuthCode ?: genericCode
            }

            if (code.isNullOrBlank()) {
                val authCodeMatch = Regex("""[?&#]auth_code=([^&#]+)""", RegexOption.IGNORE_CASE).find(fullUrl)
                if (authCodeMatch != null && authCodeMatch.groupValues.size > 1 && authCodeMatch.groupValues[1] != "200") {
                    code = authCodeMatch.groupValues[1]
                } else {
                    val codeMatch = Regex("""[?&#]code=([^&#]+)""", RegexOption.IGNORE_CASE).find(fullUrl)
                    if (codeMatch != null && codeMatch.groupValues.size > 1 && codeMatch.groupValues[1] != "200" && codeMatch.groupValues[1] != "0") {
                        code = codeMatch.groupValues[1]
                    }
                }
            }

            val rawProvider = inferredProvider
            val providerName = when (rawProvider) {
                "UPSTOX" -> ProviderHealthManager.PROVIDER_UPSTOX
                "FYERS" -> ProviderHealthManager.PROVIDER_FYERS
                else -> ProviderHealthManager.PROVIDER_UPSTOX
            }
            val logPrefix = when (rawProvider) {
                "UPSTOX" -> "UPSTOX"
                "FYERS" -> "FYERS"
                else -> "UNKNOWN"
            }

            val cleanedCode = code?.trim() ?: ""
            val lastCode = sessionManager.lastReceivedOAuthCode
            val lastTime = sessionManager.lastReceivedOAuthTime
            if (cleanedCode.isNotBlank() && cleanedCode == lastCode && System.currentTimeMillis() - lastTime < 15_000L) {
                android.util.Log.w("Auth", "[$logPrefix" + "_DUPLICATE_CALLBACK_IGNORED] Ignoring duplicate authorization code within 15s window")
                _isAuthInProgress.value = false
                return@launch
            }

            // Strict OAuth State and Session Verification
            when (val validation = sessionManager.validateAndConsumeOAuthSession(rawProvider, callbackState)) {
                is SessionManager.OAuthValidationResult.Valid -> {
                    // State successfully matched and marked consumed
                }
                is SessionManager.OAuthValidationResult.MissingPendingSession -> {
                    _isAuthInProgress.value = false
                    _authErrorMessage.value = "$logPrefix Login Error: No matching pending OAuth session found."
                    brokerManager.healthManager.reportAuthFailure(providerName, "NO_PENDING_SESSION", "No pending session")
                    return@launch
                }
                is SessionManager.OAuthValidationResult.AlreadyConsumed -> {
                    _isAuthInProgress.value = false
                    _authErrorMessage.value = "$logPrefix OAuth Error: This authorization session has already been processed (duplicate)."
                    brokerManager.healthManager.reportAuthFailure(providerName, "ALREADY_CONSUMED", "Session reused")
                    return@launch
                }
                is SessionManager.OAuthValidationResult.SessionExpired -> {
                    val errMsg = "$logPrefix OAuth callback rejected: Pending session has expired"
                    android.util.Log.e("Auth", "[$logPrefix" + "_SESSION_EXPIRED] $errMsg")
                    _authErrorMessage.value = "$logPrefix Login Failed: Session Expired (Timeout)"
                    _isAuthInProgress.value = false
                    brokerManager.healthManager.reportAuthFailure(providerName, "SESSION_EXPIRED", errMsg)
                    return@launch
                }
                is SessionManager.OAuthValidationResult.MissingCallbackState,
                is SessionManager.OAuthValidationResult.MissingStoredState,
                is SessionManager.OAuthValidationResult.StateMismatch,
                is SessionManager.OAuthValidationResult.ProviderMismatch -> {
                    _isAuthInProgress.value = false
                    _authErrorMessage.value = "$logPrefix OAuth Error: State mismatch. Possible CSRF attack or invalid session."
                    brokerManager.healthManager.reportAuthFailure(providerName, "STATE_MISMATCH", "Invalid state")
                    return@launch
                }
            }

            brokerManager.healthManager.reportCallbackReceived(providerName, "")
            android.util.Log.i("Auth", "[$logPrefix" + "_CALLBACK_RECEIVED] Redirect callback received with URI parameters")

//             Check for cancellation or OAuth error query parameters
            val oauthError = uri.getQueryParameter("error") ?: uri.getQueryParameter("error_description")
            if (!oauthError.isNullOrBlank()) {
                val errMsg = "$logPrefix Authorization Failed/Cancelled: $oauthError"
                android.util.Log.e("Auth", "[$logPrefix" + "_AUTH_CANCELLED] $errMsg")
                _authErrorMessage.value = errMsg
                _isAuthInProgress.value = false
                brokerManager.healthManager.reportAuthFailure(providerName, "AUTH_CANCELLED", errMsg)
                return@launch
            }

//             Extract Authorization Code
            if (cleanedCode.isBlank()) {
                val errMsg = "$logPrefix Authorization code missing from callback response"
                android.util.Log.e("Auth", "[$logPrefix" + "_AUTH_CODE_MISSING] $errMsg")
                _authErrorMessage.value = "$logPrefix Login Failed: Authorization Code Missing"
                _isAuthInProgress.value = false
                brokerManager.healthManager.reportAuthFailure(providerName, "AUTH_CODE_MISSING", errMsg)
                return@launch
            }

            sessionManager.lastReceivedOAuthCode = cleanedCode
            sessionManager.lastReceivedOAuthTime = System.currentTimeMillis()
            lastReceivedOAuthCode = cleanedCode
            lastReceivedOAuthTime = System.currentTimeMillis()

            brokerManager.healthManager.reportAuthCodeReceived(providerName, cleanedCode)

            sessionManager.pendingOAuthBroker = ""
//             Process ONLY the broker stored in pendingSession.provider (UPSTOX or FYERS)
            if (rawProvider == "UPSTOX") {
                var upstoxKey = sessionManager.upstoxApiKey ?: ""
                var upstoxSecret = sessionManager.upstoxApiSecret ?: ""
                if (upstoxKey.isBlank() || upstoxSecret.isBlank()) {
                    upstoxKey = com.example.util.BrokerConfig.upstoxApiKey
                    upstoxSecret = com.example.util.BrokerConfig.upstoxApiSecret
                    sessionManager.upstoxApiKey = upstoxKey
                    sessionManager.upstoxApiSecret = upstoxSecret
                }
                connectUpstox(upstoxKey, upstoxSecret, cleanedCode)
            } else if (rawProvider == "FYERS") {
                var fyersAppId = sessionManager.fyersAppId ?: ""
                var fyersSecretId = sessionManager.fyersSecretId ?: ""
                if (fyersAppId.isBlank() || fyersSecretId.isBlank()) {
                    fyersAppId = com.example.util.BrokerConfig.fyersAppId
                    fyersSecretId = com.example.util.BrokerConfig.fyersSecretId
                    sessionManager.fyersAppId = fyersAppId
                    sessionManager.fyersSecretId = fyersSecretId
                }
                connectFyers(fyersAppId, fyersSecretId, cleanedCode)
            }
        }
    }

    private suspend fun handleDhanOAuthCallback(
        uri: Uri,
        fullUrl: String,
        token: String?,
        genericTokenId: String?,
        genericCode: String?,
        clientId: String?,
        callbackState: String,
        pendingSession: SessionManager.PendingOAuthSession?
    ) {
        // Strict Provider, Expiry, Single-Use, and Exact State Verification
        when (val validation = sessionManager.validateAndConsumeOAuthSession("DHAN", callbackState)) {
            is SessionManager.OAuthValidationResult.Valid -> {
                // Validated & consumed
            }
            is SessionManager.OAuthValidationResult.MissingPendingSession -> {
                android.util.Log.e("DhanAuth", "[DHAN_CALLBACK_REJECTED] No pending Dhan OAuth session found")
                _authErrorMessage.value = "Dhan Login Error: No matching pending OAuth session found."
                _isAuthInProgress.value = false
                return
            }
            is SessionManager.OAuthValidationResult.AlreadyConsumed -> {
                android.util.Log.w("DhanAuth", "[DHAN_CALLBACK_REJECTED] Session state already consumed")
                _authErrorMessage.value = "Dhan Login Error: This OAuth session has already been processed (duplicate)."
                _isAuthInProgress.value = false
                return
            }
            is SessionManager.OAuthValidationResult.SessionExpired -> {
                android.util.Log.e("DhanAuth", "[DHAN_SESSION_EXPIRED] Pending Dhan OAuth session expired")
                _authErrorMessage.value = "Dhan Login Failed: Session Expired (Timeout). Please initiate login again."
                _isAuthInProgress.value = false
                return
            }
            is SessionManager.OAuthValidationResult.MissingCallbackState,
            is SessionManager.OAuthValidationResult.MissingStoredState,
            is SessionManager.OAuthValidationResult.StateMismatch,
            is SessionManager.OAuthValidationResult.ProviderMismatch -> {
                android.util.Log.e("DhanAuth", "[DHAN_STATE_MISMATCH] Returned OAuth state does not match pending session")
                _authErrorMessage.value = "Dhan Login Error: State verification failed (Possible CSRF attack or invalid session)."
                _isAuthInProgress.value = false
                return
            }
        }

        // Check for cancellation or OAuth error query parameters
        android.util.Log.i("DhanAuth", "[DHAN_CALLBACK_RECEIVED] OAuth callback received from redirect URL")
        val oauthError = uri.getQueryParameter("error") ?: uri.getQueryParameter("error_description")
        if (!oauthError.isNullOrBlank()) {
            val cleanMsg = if (oauthError.contains("cancel", ignoreCase = true) || oauthError.contains("user", ignoreCase = true) || oauthError.contains("access_denied", ignoreCase = true)) {
                "Dhan authorization was cancelled by user."
            } else {
                "Dhan authorization failed: $oauthError"
            }
            android.util.Log.e("DhanAuth", "[DHAN_AUTH_CANCELLED] Dhan authorization returned error or was cancelled")
            _authErrorMessage.value = cleanMsg
            _isAuthInProgress.value = false
            return
        }

        var dhanTokenId = genericTokenId
        if (dhanTokenId.isNullOrBlank() && !genericCode.isNullOrBlank() && genericCode != "200" && genericCode != "0") {
            dhanTokenId = genericCode
        }

        if (!dhanTokenId.isNullOrBlank()) {
            android.util.Log.i("DhanAuth", "[DHAN_TOKEN_ID_RECEIVED] Token ID extracted")
        }

//         Build callback fingerprint: provider + tokenId
        val dhanFingerprint = when {
            !dhanTokenId.isNullOrBlank() -> "DHAN:${dhanTokenId.trim().hashCode()}"
            !token.isNullOrBlank() -> "DHAN_TOKEN:${token.take(16).hashCode()}"
            else -> "DHAN_URI:${fullUrl.hashCode()}"
        }

//         7. Idempotent Deduplication Check
        if (dhanFingerprint == sessionManager.lastCompletedDhanFingerprint) {
            android.util.Log.i("DhanAuth", "[DHAN_DUPLICATE_CALLBACK_IGNORED] Callback already processed successfully. Safely ignoring duplicate intent.")
            _isAuthInProgress.value = false
            return
        }

//         8. In-flight check
        if (dhanFingerprint == currentlyProcessingDhanFingerprint) {
            android.util.Log.i("DhanAuth", "[DHAN_IN_FLIGHT_IGNORED] Callback is already in-flight. Ignoring duplicate intent.")
            return
        }

//         9. One-time callback token consumption check
        if (!dhanTokenId.isNullOrBlank()) {
            if (sessionManager.isDhanTokenIdConsumed(dhanTokenId)) {
                android.util.Log.w("DhanAuth", "[DHAN_OAUTH] Dropping duplicate callback - tokenId already consumed previously")
                _isAuthInProgress.value = false
                return
            }
            sessionManager.markDhanTokenIdConsumed(dhanTokenId)
        }

//         Mark in-flight
        currentlyProcessingDhanFingerprint = dhanFingerprint
        _isAuthInProgress.value = true
        _authErrorMessage.value = null

        try {
            if (!clientId.isNullOrBlank()) {
                sessionManager.dhanClientId = clientId
            }

            if (!token.isNullOrBlank()) {
//                 Direct access token received
                sessionManager.dhanAccessToken = token
                
                val profileResult = brokerManager.getProfile()
                if (profileResult.isSuccess) {
                    sessionManager.activeBroker = "Dhan"
                    sessionManager.isDhanConnected = true
                    sessionManager.dhanTokenTimestamp = System.currentTimeMillis()
                    brokerManager.setActiveBroker("Dhan")

                    android.util.Log.i("DhanAuth", "[DHAN_PROFILE_VALIDATED] Dhan profile validation: SUCCESS")
                    sessionManager.lastCompletedDhanFingerprint = dhanFingerprint
                    sessionManager.pendingOAuthBroker = ""
                    
                    android.util.Log.i("DhanAuth", "[DHAN_READY] Dhan OAuth completed and ready for use")

                    brokerManager.brokerAuthManager.updateStatus("Dhan", "Primary Order Execution", com.example.data.network.BrokerAuthStatus.CONNECTED, "Active for Order Execution")
                    _brokerSwitchStatus.value = "✓ DHAN CONNECTED"
                    _authSuccessEvent.value = true
                    _showConnectDialog.value = false
                    repository.addNotification("Broker Connected", "Connected to Dhan via OAuth successfully", "SUCCESS")
                    val accountId = clientId?.takeIf { it.isNotBlank() } ?: sessionManager.dhanClientId.takeIf { it.isNotBlank() } ?: "Dhan User"
                    alertService.notifyBrokerConnected("Dhan", account = accountId)
                    
                    validateAndRestoreSession()
                } else {
                    android.util.Log.e("DhanAuth", "Dhan profile validation: FAILURE")
                    sessionManager.dhanAccessToken = ""
                    _authErrorMessage.value = "Failed to validate Dhan session. Please check your credentials."
                }
            } else if (!dhanTokenId.isNullOrBlank()) {
//                 Exchange tokenId for accessToken
                val exchangeRes = com.example.util.DhanAuthHelper.exchangeToken(
                    dhanTokenId,
                    clientIdOverride = sessionManager.dhanClientId,
                    apiKeyOverride = sessionManager.dhanApiKey,
                    clientSecretOverride = sessionManager.dhanClientSecret
                )
                if (exchangeRes.isSuccess) {
                    val accessToken = exchangeRes.getOrThrow()
                    android.util.Log.d("DhanAuth", "Dhan token exchange: SUCCESS")
                    sessionManager.dhanAccessToken = accessToken
                    
                    val profileResult = brokerManager.getProfile()
                    if (profileResult.isSuccess) {
                        sessionManager.activeBroker = "Dhan"
                        sessionManager.isDhanConnected = true
                        sessionManager.dhanTokenTimestamp = System.currentTimeMillis()
                        brokerManager.setActiveBroker("Dhan")
    
                        android.util.Log.d("DhanAuth", "Dhan profile validation: SUCCESS")
                        sessionManager.lastCompletedDhanFingerprint = dhanFingerprint
                        sessionManager.pendingOAuthBroker = ""
    
                        brokerManager.brokerAuthManager.updateStatus("Dhan", "Primary Order Execution", com.example.data.network.BrokerAuthStatus.CONNECTED, "Active for Order Execution")
                        _brokerSwitchStatus.value = "✓ DHAN CONNECTED"
                        _authSuccessEvent.value = true
                        _showConnectDialog.value = false
                        repository.addNotification("Broker Connected", "Connected to Dhan via OAuth successfully", "SUCCESS")
                        val accountId = clientId?.takeIf { it.isNotBlank() } ?: sessionManager.dhanClientId.takeIf { it.isNotBlank() } ?: "Dhan User"
                        alertService.notifyBrokerConnected("Dhan", account = accountId)
                        
                        validateAndRestoreSession()
                    } else {
                        android.util.Log.e("DhanAuth", "Dhan profile validation: FAILURE")
                        sessionManager.dhanAccessToken = ""
                        _authErrorMessage.value = "Failed to validate Dhan session after token exchange."
                    }
                } else {
                    val err = exchangeRes.exceptionOrNull()
                    val errMsg = err?.localizedMessage ?: "Unknown token exchange error"
                    android.util.Log.e("DhanAuth", "Dhan token exchange: FAILURE")
                    _authErrorMessage.value = "Failed to exchange Dhan token: $errMsg"
                }
            } else {
                android.util.Log.e("DhanAuth", "Dhan callback missing tokenId and token")
                _authErrorMessage.value = "Failed to parse authorization code or token from Dhan callback redirect"
            }
        } finally {
            currentlyProcessingDhanFingerprint = null
            _isAuthInProgress.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearActiveBrokerSession()
            _isSessionValid.value = false
            _userProfile.value = UserProfileEntity(id = 1)
            _orders.value = emptyList()
            _holdings.value = emptyList()
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
                brokerManager.marketDataEngine.lastTickTimeMs.collect { time ->
                    if (time > 0) {
                        _marketDataLastUpdated.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(time))
                    }
                }
            }
            
            // P0-3: Removed 5-second REST API Hammering
            // Initial snapshot only, then fallback to slow periodic background sync for non-live data
            syncMarketDataPipeline()
            
            while (true) {
                kotlinx.coroutines.delay(60000L) // 1 minute background sync interval for account/broker status
                if (sessionManager.hasValidSession()) {
                    runCatching { repository.syncWithBroker() }
                }
            }
        }
    }

    private suspend fun syncMarketDataPipeline() {
        runCatching {
            val isValidSession = sessionManager.hasValidSession()
            if (isValidSession) {
                repository.syncWithBroker()
            }

//             1. Fetch live quotes across all tracking symbols and SUBSCRIBE to live web sockets
            val symbols = getAllLiveTrackingSymbols()
            
            // Subscribe to live WebSocket feeds to replace the 5-second REST polling
            brokerManager.marketDataEngine.subscribeToMarketData(symbols)
            
            // Only do one immediate REST snapshot on start to seed the UI immediately before WebSocket ticks arrive
            val quotesRes = brokerManager.marketDataEngine.getMarketQuotes(symbols)
            quotesRes.getOrNull()?.let { quotes ->
                if (quotes.isNotEmpty()) {
                    repository.updateWatchlistQuotes(quotes)
                }
            }

//             2. Ensure historical candles are populated in CandleStore for active Algo Index
            val activeAlgoIndex = com.example.util.AlgoEngine.selectedIndex.value
            val strategyTimeframe = com.example.util.AlgoEngine.currentStrategy.value.timeframe
            if (!com.example.util.indicators.CandleStore.hasSufficientCandles(activeAlgoIndex, strategyTimeframe, 15)) {
                val candleInterval = when (com.example.util.indicators.CandleStore.normalizeTimeframe(strategyTimeframe)) {
                    "1 MIN" -> "1m"
                    "15 MIN" -> "15m"
                    "30 MIN" -> "30m"
                    "1 DAY" -> "1d"
                    else -> "5m"
                }
                val histRes = brokerManager.getHistoricalCandles(activeAlgoIndex, candleInterval)
                histRes.getOrNull()?.let { candles ->
                    if (candles.isNotEmpty()) {
                        com.example.util.indicators.CandleStore.setRealHistoricalCandles(activeAlgoIndex, strategyTimeframe, candles)
                    }
                }
            }

//             3. Synchronously fetch option chain for active option index / algo index
            val targetOptionIndex = if (_selectedOptionIndex.value.isNotBlank()) _selectedOptionIndex.value else activeAlgoIndex
            val expiry = _selectedOptionExpiry.value

            val optChainRes = brokerManager.getOptionChain(targetOptionIndex, expiry)
            val strikes = optChainRes.getOrNull()

            if (!strikes.isNullOrEmpty()) {
                strikes.forEach { strike ->
                    if (strike.callSymbol.isNotBlank()) com.example.util.InstrumentMapUtil.symbolToUnderlyingCache[strike.callSymbol.uppercase()] = targetOptionIndex
                    if (strike.putSymbol.isNotBlank()) com.example.util.InstrumentMapUtil.symbolToUnderlyingCache[strike.putSymbol.uppercase()] = targetOptionIndex
                }
                _optionStrikes.value = strikes
            }

            _marketDataLastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            com.example.util.AlgoEngine.processMarketFeed(
                quotes = watchlist.value,
                isLiveFeedActive = isLiveFeedActive.value,
                optionChain = _optionStrikes.value
            )
        }.onFailure { e ->
            Log.e("MainViewModel", "[MARKET_DATA_PIPELINE_ERROR] ${e.message}", e)
        }
    }
    private fun getAllLiveTrackingSymbols(): List<String> {
        return com.example.data.model.MarketUniverse.APPROVED_UNDERLYINGS
    }

    private fun syncMarketDataQuietly() {
        viewModelScope.launch {
            runCatching {
                if (sessionManager.hasValidSession()) {
                    repository.syncWithBroker()
                }
                val symbols = getAllLiveTrackingSymbols()
                val quotesRes = brokerManager.marketDataEngine.getMarketQuotes(symbols)
                
                // Force refresh the active option chain if selected
                val activeOptIdx = _selectedOptionIndex.value
                val activeOptExp = _selectedOptionExpiry.value
                if (activeOptIdx.isNotBlank()) {
                    val optChainRes = brokerManager.getOptionChain(activeOptIdx, activeOptExp, forceRefresh = true)
                    val strikes = optChainRes.getOrNull()
                    if (!strikes.isNullOrEmpty()) {
                        strikes.forEach { strike ->
                            if (strike.callSymbol.isNotBlank()) com.example.util.InstrumentMapUtil.symbolToUnderlyingCache[strike.callSymbol.uppercase()] = activeOptIdx
                            if (strike.putSymbol.isNotBlank()) com.example.util.InstrumentMapUtil.symbolToUnderlyingCache[strike.putSymbol.uppercase()] = activeOptIdx
                        }
                        _optionStrikes.value = strikes
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
                
                // Force refresh the active option chain if selected
                val activeOptIdx = _selectedOptionIndex.value
                val activeOptExp = _selectedOptionExpiry.value
                if (activeOptIdx.isNotBlank()) {
                    val optChainRes = brokerManager.getOptionChain(activeOptIdx, activeOptExp, forceRefresh = true)
                    val strikes = optChainRes.getOrNull()
                    if (!strikes.isNullOrEmpty()) {
                        strikes.forEach { strike ->
                            if (strike.callSymbol.isNotBlank()) com.example.util.InstrumentMapUtil.symbolToUnderlyingCache[strike.callSymbol.uppercase()] = activeOptIdx
                            if (strike.putSymbol.isNotBlank()) com.example.util.InstrumentMapUtil.symbolToUnderlyingCache[strike.putSymbol.uppercase()] = activeOptIdx
                        }
                        _optionStrikes.value = strikes
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
        _optionStrikes.value = emptyList() // Immediately clear strikes on index change to prevent stale contract data
        viewModelScope.launch {
            val expiries = repository.getOptionExpiries(index)
            _availableOptionExpiries.value = expiries
            if (expiries.isNotEmpty() && !expiries.contains(_selectedOptionExpiry.value)) {
                _selectedOptionExpiry.value = expiries.first()
            }
            fetchOptionChain()
        }
    }

    fun setSelectedOptionExpiry(expiry: String) {
        _selectedOptionExpiry.value = expiry
        _optionStrikes.value = emptyList() // Clear strikes while fetching new expiry
        fetchOptionChain()
    }

    private fun fetchOptionChain(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val indexName = _selectedOptionIndex.value
            val expiry = _selectedOptionExpiry.value
            val res = brokerManager.getOptionChain(indexName, expiry, forceRefresh)
            val strikes = res.getOrNull()
            
            if (strikes != null) {
                strikes.forEach { strike ->
                    if (strike.callSymbol.isNotBlank()) com.example.util.InstrumentMapUtil.symbolToUnderlyingCache[strike.callSymbol.uppercase()] = indexName
                    if (strike.putSymbol.isNotBlank()) com.example.util.InstrumentMapUtil.symbolToUnderlyingCache[strike.putSymbol.uppercase()] = indexName
                }
                _optionStrikes.value = strikes
                
                // Subscribe to live WebSocket feeds for these options
                val optionSymbols = strikes.flatMap { strike ->
                    listOfNotNull(
                        strike.callSymbol.takeIf { it.isNotBlank() },
                        strike.putSymbol.takeIf { it.isNotBlank() }
                    )
                }
                if (optionSymbols.isNotEmpty()) {
                    brokerManager.marketDataEngine.subscribeToMarketData(optionSymbols)
                }
            } else {
                _optionStrikes.value = emptyList()
            }
        }
    }

    fun getHistoricalCandlesForIndex(indexName: String, interval: String = "15m", onResult: (List<com.example.ui.components.CandleData>) -> Unit) {
        viewModelScope.launch {
            val res = brokerManager.getHistoricalCandles(indexName, interval)
            if (res.isSuccess) {
                val candles = res.getOrDefault(emptyList())
                if (candles.isNotEmpty()) {
                    com.example.util.indicators.CandleStore.setRealHistoricalCandles(indexName, interval, candles)
                }
                val uiCandles = candles.map { 
                    com.example.ui.components.CandleData(open = it.open.toFloat(), high = it.high.toFloat(), low = it.low.toFloat(), close = it.close.toFloat(), volume = it.volume.toFloat())
                }
                onResult(uiCandles)
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
        price: Double,
        strikePrice: Double = 0.0,
        expiry: String = "",
        underlying: String = ""
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
                    symbolToken = token,
                    strike = strikePrice,
                    expiry = expiry,
                    underlying = underlying,
                    
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
                val isTimeout = e is java.net.SocketTimeoutException || e is kotlinx.coroutines.TimeoutCancellationException
                val errorMsg = if (isTimeout) {
                    "Network Timeout. The order may have been executed. Please check your Orders book before retrying."
                } else {
                    e.message ?: "Order Placement Failed"
                }
                
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
                if (isTimeout) refreshBrokerData()
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
                val isTimeout = e is java.net.SocketTimeoutException || e is kotlinx.coroutines.TimeoutCancellationException
                _apiError.value = if (isTimeout) "Cancel Request Timeout. Status uncertain." else "Cancel Order Failed: ${e.message}"
                if (isTimeout) refreshBrokerData()
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
                val isTimeout = e is java.net.SocketTimeoutException || e is kotlinx.coroutines.TimeoutCancellationException
                _apiError.value = if (isTimeout) "Modify Request Timeout. Status uncertain." else "Modify Order Failed: ${e.message}"
                if (isTimeout) refreshBrokerData()
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
        if (_exitingOrderIds.value.contains(orderId)) return
        _exitingOrderIds.value = _exitingOrderIds.value + orderId
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
                val isTimeout = e is java.net.SocketTimeoutException || e is kotlinx.coroutines.TimeoutCancellationException
                _apiError.value = if (isTimeout) "Exit Request Timeout. Status uncertain." else "Exit Position Failed: ${e.message}"
                if (isTimeout) refreshBrokerData()
            } finally {
                _exitingOrderIds.value = _exitingOrderIds.value - orderId
            }
        }
    }

    fun partialExitPosition(orderId: String, exitLots: Int, exitPrice: Double, partialPnl: Double) {
        if (_exitingOrderIds.value.contains(orderId)) return
        _exitingOrderIds.value = _exitingOrderIds.value + orderId
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
                val isTimeout = e is java.net.SocketTimeoutException || e is kotlinx.coroutines.TimeoutCancellationException
                _apiError.value = if (isTimeout) "Partial Exit Timeout. Status uncertain." else "Partial Exit Failed: ${e.message}"
                if (isTimeout) refreshBrokerData()
            } finally {
                _exitingOrderIds.value = _exitingOrderIds.value - orderId
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
//             Toggle favorite status
        }
    }

    fun addRecentSearch(query: String) {
        if (query.isBlank()) {
            return
        }
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
            val isBullish = signal.trend.equals("BULLISH", ignoreCase = true) || signal.actionType.contains("CE", ignoreCase = true)
            if (isBullish) {
                alertService.notifyAiBuyCeSignal(
                    symbol = signal.symbol,
                    contract = "${signal.symbol} ${signal.actionType}",
                    entry = String.format(Locale.US, "%.2f", signal.ltp),
                    sl = String.format(Locale.US, "%.2f", signal.stopLoss),
                    t1 = String.format(Locale.US, "%.2f", signal.target1),
                    t2 = String.format(Locale.US, "%.2f", signal.target2),
                    t3 = if (signal.target3 > 0.0) String.format(Locale.US, "%.2f", signal.target3) else "",
                    t4 = if (signal.target4 > 0.0) String.format(Locale.US, "%.2f", signal.target4) else "",
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
                    t3 = if (signal.target3 > 0.0) String.format(Locale.US, "%.2f", signal.target3) else "",
                    t4 = if (signal.target4 > 0.0) String.format(Locale.US, "%.2f", signal.target4) else "",
                    confidence = signal.confidence
                )
            }
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

    fun notifyBreakingNews(articles: List<OptionBuyerNewsArticle>) {
        viewModelScope.launch {
            articles.filter { it.isBreaking }.forEach { article ->
                alertService.notifyMarketNews(article)
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        sessionManager.isBiometricEnabled = enabled
    }
}
