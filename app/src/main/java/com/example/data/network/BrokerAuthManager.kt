package com.example.data.network

import android.util.Log
import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import com.example.util.AngelAuthHelper
import com.example.util.BrokerConfig
import com.example.util.DhanAuthHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class BrokerAuthStatus {
    CONNECTED,                // Authenticated + active in its designated role
    STANDBY,                  // Authenticated & ready as backup/fallback
    AUTHENTICATION_REQUIRED,  // Token/session expired and cannot be renewed automatically
    OFFLINE,                  // Provider unreachable or explicitly disconnected
    CONFIGURE,                // Credentials not yet configured
    ERROR                     // Authentication or network error
}

data class BrokerConnectionState(
    val brokerName: String,
    val role: String,
    val status: BrokerAuthStatus,
    val message: String = "",
    val lastSyncTimestamp: Long = 0L
)

/**
 * KING KHAN AI TRADE - Centralized Broker Authentication & Token Manager
 * 
 * Strict Role Separation:
 * 1. Dhan -> PRIMARY ORDER EXECUTION ONLY
 * 2. Angel One -> PRIMARY LIVE MARKET DATA ONLY
 * 3. m.Stock -> SECONDARY MARKET DATA FALLBACK
 * 4. TradeSmart -> TERTIARY MARKET DATA FALLBACK
 * 5. NSE & Yahoo -> REFERENCE ONLY
 * 
 * Startup Flow:
 * Initialize -> Check stored secure credentials/tokens -> Validate each broker session ->
 * Auto-renew where supported -> Connect live feeds -> Update connection statuses.
 */
class BrokerAuthManager(
    private val sessionManager: SessionManager,
    private val dhanService: DhanTradingService,
    private val angelOneService: AngelOneBrokerService,
    private val angelMarketDataService: AngelOneMarketDataService,
    private val mStockMarketDataService: MStockMarketDataService,
    private val tradeSmartMarketDataService: TradeSmartMarketDataService,
    private val brokerManager: BrokerManager
) {
    companion object {
        private const val TAG = "BrokerAuthManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val authMutex = Mutex()

    private val _statuses = MutableStateFlow<Map<String, BrokerConnectionState>>(
        mapOf(
            "Dhan" to BrokerConnectionState("Dhan", "Primary Order Execution", BrokerAuthStatus.CONFIGURE),
            "Angel One" to BrokerConnectionState("Angel One", "Primary Market Data", BrokerAuthStatus.CONFIGURE),
            "m.Stock" to BrokerConnectionState("m.Stock", "Secondary Data Fallback", BrokerAuthStatus.CONFIGURE),
            "TradeSmart" to BrokerConnectionState("TradeSmart", "Tertiary Data Fallback", BrokerAuthStatus.CONFIGURE),
            "NSE" to BrokerConnectionState("NSE", "Reference Only", BrokerAuthStatus.STANDBY),
            "Yahoo" to BrokerConnectionState("Yahoo", "Reference Only", BrokerAuthStatus.STANDBY)
        )
    )
    val statuses: StateFlow<Map<String, BrokerConnectionState>> = _statuses.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    /**
     * Startup Flow:
     * App starts -> Auth Manager initializes -> Check stored secure credentials/tokens ->
     * Validate each broker session -> Auto renew/re-auth where supported -> Connect feeds.
     */
    suspend fun initialize() = authMutex.withLock {
        _isInitializing.value = true
        Log.d(TAG, "=== BrokerAuthManager Startup Sequence Started ===")

        try {
            // 1. Dhan (Primary Order Execution)
            validateDhanSession()

            // 2. Angel One (Primary Market Data)
            validateAngelOneSession()

            // 3. m.Stock (Secondary Fallback Data)
            validateMStockSession()

            // 4. TradeSmart (Tertiary Fallback Data)
            validateTradeSmartSession()

            // 5. Connect Active Live Feeds
            connectActiveMarketFeeds()
        } catch (e: Exception) {
            Log.e(TAG, "Startup sequence encountered error: ${e.message}", e)
        } finally {
            _isInitializing.value = false
            Log.d(TAG, "=== BrokerAuthManager Startup Sequence Completed ===")
        }
    }

    // ==========================================
    // DHAN (PRIMARY ORDER EXECUTION)
    // ==========================================

    private suspend fun validateDhanSession() {
        val hasDhanSession = sessionManager.hasValidSession() && !sessionManager.dhanAccessToken.isNullOrBlank()
        if (!hasDhanSession) {
            updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }

        try {
            val profileRes = dhanService.getProfile()
            if (profileRes.isSuccess) {
                Log.d(TAG, "Dhan session valid and active for order execution")
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONNECTED, "Active for Order Execution")
                brokerManager.setActiveBroker("Dhan")
            } else {
                val err = profileRes.exceptionOrNull()?.message ?: "Unknown error"
                if (isAuthExpiredError(err)) {
                    Log.w(TAG, "Dhan token expired. Re-authentication required.")
                    updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Session expired. Re-authentication required.")
                } else {
                    Log.w(TAG, "Dhan network warning: $err")
                    updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONNECTED, "Connected (Network Notice)")
                    brokerManager.setActiveBroker("Dhan")
                }
            }
        } catch (e: Exception) {
            handleAuthenticationError("Dhan", e)
        }
    }

    suspend fun connectDhan(onConsentUrl: (String) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Initiating Dhan official OAuth flow...")
            val consentResult = DhanAuthHelper.generateConsent()
            if (consentResult.isSuccess) {
                val url = consentResult.getOrThrow()
                onConsentUrl(url)
            } else {
                throw consentResult.exceptionOrNull() ?: Exception("Failed to generate Dhan OAuth consent URL")
            }
        }
    }

    suspend fun exchangeDhanToken(code: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val tokenRes = DhanAuthHelper.exchangeToken(code)
            val token = tokenRes.getOrThrow()
            sessionManager.dhanAccessToken = token
            sessionManager.activeBroker = "Dhan"
            sessionManager.isDhanConnected = true
            validateDhanSession()
            token
        }
    }

    suspend fun refreshDhan(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            validateDhanSession()
            getConnectionStatus("Dhan") == BrokerAuthStatus.CONNECTED
        }
    }

    // ==========================================
    // ANGEL ONE (PRIMARY LIVE MARKET DATA)
    // ==========================================

    private suspend fun validateAngelOneSession() {
        val hasAngelToken = !sessionManager.angelJwtToken.isNullOrBlank()
        val hasRefreshToken = !sessionManager.angelRefreshToken.isNullOrBlank()
        val hasCredentials = sessionManager.angelClientId.isNotBlank() && sessionManager.angelMpin.isNotBlank() && sessionManager.angelTotpSecret.isNotBlank()

        if (!hasAngelToken && !hasRefreshToken && !hasCredentials) {
            updateStatus("Angel One", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }

        if (hasAngelToken) {
            val profileRes = angelOneService.getProfile()
            if (profileRes.isSuccess) {
                Log.d(TAG, "Angel One SmartAPI session valid")
                updateStatus("Angel One", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active")
                angelMarketDataService.connect()
                return
            }
        }

        // Try Auto-Renewal using official refresh token
        if (hasRefreshToken) {
            Log.d(TAG, "Angel One JWT expired, attempting automatic session renewal via refresh token...")
            val renewed = refreshAngelOne()
            if (renewed.isSuccess && renewed.getOrThrow()) {
                Log.d(TAG, "Angel One auto-renewal succeeded")
                updateStatus("Angel One", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active (Renewed)")
                return
            }
        }

        // Try Auto-Authentication using stored Client ID + MPIN + TOTP Secret
        if (hasCredentials) {
            Log.d(TAG, "Angel One session expired, attempting auto-login using stored TOTP secret...")
            val autoLoginRes = connectAngelOne(
                clientCode = sessionManager.angelClientId,
                mpin = sessionManager.angelMpin,
                totpSecret = sessionManager.angelTotpSecret,
                apiKey = sessionManager.angelApiKey
            )
            if (autoLoginRes.isSuccess && autoLoginRes.getOrThrow()) {
                Log.d(TAG, "Angel One auto-login via TOTP secret succeeded")
                return
            }
        }

        Log.w(TAG, "Angel One session expired and auto-renew unavailable. Re-auth required.")
        updateStatus("Angel One", "Primary Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Session expired. MPIN/TOTP login required.")
    }

    suspend fun connectAngelOne(
        clientCode: String? = null,
        mpin: String? = null,
        totp: String? = null,
        totpSecret: String? = null,
        apiKey: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val code = clientCode?.trim() ?: sessionManager.angelClientId
            val pass = mpin?.trim() ?: sessionManager.angelMpin
            val secret = totpSecret?.trim() ?: sessionManager.angelTotpSecret
            val explicitTotp = totp?.trim() ?: ""
            val key = apiKey?.trim() ?: sessionManager.angelApiKey

            if (code.isBlank() || pass.isBlank()) {
                throw Exception("Client ID and MPIN are required for Angel One login.")
            }

            // Determine effective TOTP: Use explicit TOTP if supplied, otherwise generate from TOTP Secret
            val effectiveTotpInput = if (explicitTotp.isNotBlank()) {
                explicitTotp
            } else if (secret.isNotBlank()) {
                secret
            } else {
                throw Exception("Either TOTP code or TOTP Secret must be provided.")
            }

            Log.d(TAG, "Authenticating with Angel One SmartAPI for client: $code")
            val loginResult = AngelAuthHelper.generateSession(code, pass, effectiveTotpInput, key)
            val tokens = loginResult.getOrThrow()

            // Securely store credentials and tokens in Encrypted Storage
            sessionManager.angelClientId = code
            sessionManager.angelMpin = pass
            if (secret.isNotBlank()) {
                sessionManager.angelTotpSecret = secret
            }
            if (key.isNotBlank()) {
                sessionManager.angelApiKey = key
            }
            sessionManager.angelJwtToken = tokens.jwtToken
            sessionManager.angelRefreshToken = tokens.refreshToken
            sessionManager.angelFeedToken = tokens.feedToken
            sessionManager.isAngelConnected = true

            updateStatus("Angel One", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Authenticated successfully")

            // Auto connect WebSocket after session creation
            angelMarketDataService.connect()
            true
        }
    }

    suspend fun refreshAngelOne(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val refreshToken = sessionManager.angelRefreshToken
            if (refreshToken.isNullOrBlank()) {
                throw Exception("No Angel One refresh token found.")
            }

            val refreshRes = AngelAuthHelper.renewSession(refreshToken)
            val tokens = refreshRes.getOrThrow()

            if (tokens.jwtToken.isNotBlank()) sessionManager.angelJwtToken = tokens.jwtToken
            if (tokens.refreshToken.isNotBlank()) sessionManager.angelRefreshToken = tokens.refreshToken
            if (tokens.feedToken.isNotBlank()) sessionManager.angelFeedToken = tokens.feedToken

            updateStatus("Angel One", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Session auto-refreshed")
            angelMarketDataService.reconnect()
            true
        }
    }

    // ==========================================
    // m.STOCK (SECONDARY MARKET DATA FALLBACK)
    // ==========================================

    private suspend fun validateMStockSession() {
        if (!sessionManager.isMStockConfigured()) {
            updateStatus("m.Stock", "Secondary Data Fallback", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }

        val hasAngelLive = getConnectionStatus("Angel One") == BrokerAuthStatus.CONNECTED
        val status = if (hasAngelLive) BrokerAuthStatus.STANDBY else BrokerAuthStatus.CONNECTED
        val msg = if (hasAngelLive) "Configured • Standby Fallback" else "Active Fallback Market Data"

        updateStatus("m.Stock", "Secondary Data Fallback", status, msg)
    }

    suspend fun connectMStock(
        apiKey: String? = null,
        clientId: String? = null,
        passwordPin: String? = null,
        totp: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!apiKey.isNullOrBlank()) sessionManager.mstockApiKey = apiKey
            if (!clientId.isNullOrBlank()) sessionManager.mstockClientId = clientId
            if (!passwordPin.isNullOrBlank()) sessionManager.mstockPasswordPin = passwordPin
            if (!totp.isNullOrBlank()) sessionManager.mstockAccessToken = totp

            sessionManager.mstockTokenTimestamp = System.currentTimeMillis()
            mStockMarketDataService.connect()

            validateMStockSession()
            true
        }
    }

    suspend fun refreshMStock(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sessionManager.isMStockConfigured()) {
                throw Exception("m.Stock is not configured.")
            }
            mStockMarketDataService.connect()
            validateMStockSession()
            true
        }
    }

    // ==========================================
    // TRADESMART (TERTIARY MARKET DATA FALLBACK)
    // ==========================================

    private suspend fun validateTradeSmartSession() {
        if (!sessionManager.isTradeSmartConfigured()) {
            updateStatus("TradeSmart", "Tertiary Data Fallback", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }

        val higherLive = getConnectionStatus("Angel One") == BrokerAuthStatus.CONNECTED ||
                getConnectionStatus("m.Stock") == BrokerAuthStatus.CONNECTED
        val status = if (higherLive) BrokerAuthStatus.STANDBY else BrokerAuthStatus.CONNECTED
        val msg = if (higherLive) "Configured • Standby Fallback" else "Active Tertiary Market Data"

        updateStatus("TradeSmart", "Tertiary Data Fallback", status, msg)
    }

    suspend fun connectTradeSmart(
        apiKey: String? = null,
        clientId: String? = null,
        token: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!apiKey.isNullOrBlank()) sessionManager.tradesmartApiKey = apiKey
            if (!clientId.isNullOrBlank()) sessionManager.tradesmartClientId = clientId
            if (!token.isNullOrBlank()) sessionManager.tradesmartAccessToken = token

            sessionManager.tradesmartTokenTimestamp = System.currentTimeMillis()
            tradeSmartMarketDataService.connect()

            validateTradeSmartSession()
            true
        }
    }

    suspend fun refreshTradeSmart(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sessionManager.isTradeSmartConfigured()) {
                throw Exception("TradeSmart is not configured.")
            }
            tradeSmartMarketDataService.connect()
            validateTradeSmartSession()
            true
        }
    }

    // ==========================================
    // ORCHESTRATION & COMMON METHODS
    // ==========================================

    private fun connectActiveMarketFeeds() {
        // Angel One is primary feed
        if (sessionManager.hasAngelSession()) {
            angelMarketDataService.connect()
        }

        // Connect fallbacks if configured
        if (sessionManager.isMStockConfigured()) {
            mStockMarketDataService.connect()
        }

        if (sessionManager.isTradeSmartConfigured()) {
            tradeSmartMarketDataService.connect()
        }
    }

    fun getConnectionStatus(): Map<String, BrokerConnectionState> {
        return _statuses.value
    }

    fun getConnectionStatus(brokerName: String): BrokerAuthStatus {
        return _statuses.value[brokerName]?.status ?: BrokerAuthStatus.OFFLINE
    }

    fun logoutBroker(brokerName: String) {
        when (brokerName) {
            "Dhan" -> {
                sessionManager.clearDhanSession()
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONFIGURE, "Disconnected")
            }
            "Angel One" -> {
                sessionManager.clearAngelSession()
                angelMarketDataService.disconnect()
                updateStatus("Angel One", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Disconnected")
            }
            "m.Stock" -> {
                sessionManager.clearMStockSession()
                mStockMarketDataService.disconnect()
                updateStatus("m.Stock", "Secondary Data Fallback", BrokerAuthStatus.CONFIGURE, "Disconnected")
            }
            "TradeSmart" -> {
                sessionManager.clearTradeSmartSession()
                tradeSmartMarketDataService.disconnect()
                updateStatus("TradeSmart", "Tertiary Data Fallback", BrokerAuthStatus.CONFIGURE, "Disconnected")
            }
        }
    }

    suspend fun reconnectBroker(brokerName: String): Result<Boolean> {
        return when (brokerName) {
            "Dhan" -> refreshDhan()
            "Angel One" -> {
                angelMarketDataService.reconnect()
                refreshAngelOne()
            }
            "m.Stock" -> refreshMStock()
            "TradeSmart" -> refreshTradeSmart()
            else -> Result.failure(Exception("Unknown broker: $brokerName"))
        }
    }

    fun handleAuthenticationError(brokerName: String, error: Throwable) {
        val msg = error.message ?: "Authentication error"
        Log.e(TAG, "Auth error for $brokerName: $msg", error)

        val status = if (isAuthExpiredError(msg)) {
            BrokerAuthStatus.AUTHENTICATION_REQUIRED
        } else {
            BrokerAuthStatus.ERROR
        }

        val role = when (brokerName) {
            "Dhan" -> "Primary Order Execution"
            "Angel One" -> "Primary Market Data"
            "m.Stock" -> "Secondary Data Fallback"
            "TradeSmart" -> "Tertiary Data Fallback"
            else -> "Market Provider"
        }

        updateStatus(brokerName, role, status, msg)
    }

    private fun updateStatus(brokerName: String, role: String, status: BrokerAuthStatus, message: String) {
        val current = _statuses.value.toMutableMap()
        current[brokerName] = BrokerConnectionState(
            brokerName = brokerName,
            role = role,
            status = status,
            message = message,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        _statuses.value = current
    }

    private fun isAuthExpiredError(msg: String): Boolean {
        val lower = msg.lowercase()
        return lower.contains("401") ||
                lower.contains("403") ||
                lower.contains("expired") ||
                lower.contains("unauthenticated") ||
                lower.contains("invalid token") ||
                lower.contains("token error") ||
                lower.contains("re-auth")
    }
}
