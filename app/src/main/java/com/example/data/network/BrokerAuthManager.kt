package com.example.data.network

import android.util.Log
import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import com.example.util.AngelAuthHelper
import com.example.util.BrokerConfig
import com.example.util.DhanAuthHelper
import com.example.util.MStockAuthHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class BrokerAuthStatus {
    NOT_CONFIGURED,
    AUTHENTICATING,
    AUTHENTICATED,
    CONNECTING,
    CONNECTED,
    SUBSCRIBING,
    LIVE,
    STALE,
    DISCONNECTED,
    ERROR
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
 * 2. Upstox -> PRIMARY MARKET DATA FEED
 * 3. Fyers -> FALLBACK #1 MARKET DATA FEED
 * 4. Angel One -> FALLBACK #2 MARKET DATA FEED
 * 5. m.Stock -> FALLBACK #3 MARKET DATA FEED
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
    private val brokerManager: BrokerManager
) {
    companion object {
        private const val TAG = "BrokerAuthManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val authMutex = Mutex()

    private val _statuses = MutableStateFlow<Map<String, BrokerConnectionState>>(
        mapOf(
            "Dhan" to BrokerConnectionState("Dhan", "Primary Order Execution", BrokerAuthStatus.NOT_CONFIGURED),
            "Upstox" to BrokerConnectionState("Upstox", "Primary Market Data", BrokerAuthStatus.NOT_CONFIGURED),
            "Fyers" to BrokerConnectionState("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.NOT_CONFIGURED),
            "Angel One" to BrokerConnectionState("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.NOT_CONFIGURED),
            "m.Stock" to BrokerConnectionState("m.Stock", "Fallback #3 Market Data", BrokerAuthStatus.NOT_CONFIGURED)
        )
    )
    val statuses: StateFlow<Map<String, BrokerConnectionState>> = _statuses.asStateFlow()

    init {
        scope.launch {
            com.example.data.model.MarketDataStore.upstoxHealth.collect { health ->
                val current = _statuses.value["Upstox"] ?: return@collect
                val mappedStatus = when (health) {
                    "LIVE" -> BrokerAuthStatus.LIVE
                    "STALE" -> BrokerAuthStatus.STALE
                    "OFFLINE" -> BrokerAuthStatus.DISCONNECTED
                    "ERROR" -> BrokerAuthStatus.ERROR
                    "CONNECTED" -> BrokerAuthStatus.CONNECTED
                    "STANDBY" -> BrokerAuthStatus.CONNECTED
                    else -> current.status
                }
                if (current.status != mappedStatus) {
                    updateStatus("Upstox", "Primary Market Data", mappedStatus, "Health state updated to $health")
                }
            }
        }
        scope.launch {
            com.example.data.model.MarketDataStore.fyersHealth.collect { health ->
                val current = _statuses.value["Fyers"] ?: return@collect
                val mappedStatus = when (health) {
                    "LIVE" -> BrokerAuthStatus.LIVE
                    "STALE" -> BrokerAuthStatus.STALE
                    "OFFLINE" -> BrokerAuthStatus.DISCONNECTED
                    "ERROR" -> BrokerAuthStatus.ERROR
                    "CONNECTED" -> BrokerAuthStatus.CONNECTED
                    "STANDBY" -> BrokerAuthStatus.CONNECTED
                    else -> current.status
                }
                if (current.status != mappedStatus) {
                    updateStatus("Fyers", "Fallback #1 Market Data", mappedStatus, "Health state updated to $health")
                }
            }
        }
        scope.launch {
            com.example.data.model.MarketDataStore.angelOneHealth.collect { health ->
                val current = _statuses.value["Angel One"] ?: return@collect
                val mappedStatus = when (health) {
                    "LIVE" -> BrokerAuthStatus.LIVE
                    "STALE" -> BrokerAuthStatus.STALE
                    "OFFLINE" -> BrokerAuthStatus.DISCONNECTED
                    "ERROR" -> BrokerAuthStatus.ERROR
                    "CONNECTED" -> BrokerAuthStatus.CONNECTED
                    "STANDBY" -> BrokerAuthStatus.CONNECTED
                    else -> current.status
                }
                if (current.status != mappedStatus) {
                    updateStatus("Angel One", "Fallback #2 Market Data", mappedStatus, "Health state updated to $health")
                }
            }
        }
        scope.launch {
            com.example.data.model.MarketDataStore.mStockHealth.collect { health ->
                val current = _statuses.value["m.Stock"] ?: return@collect
                val mappedStatus = when (health) {
                    "LIVE" -> BrokerAuthStatus.LIVE
                    "STALE" -> BrokerAuthStatus.STALE
                    "OFFLINE" -> BrokerAuthStatus.DISCONNECTED
                    "ERROR" -> BrokerAuthStatus.ERROR
                    "CONNECTED" -> BrokerAuthStatus.CONNECTED
                    "STANDBY" -> BrokerAuthStatus.CONNECTED
                    else -> current.status
                }
                if (current.status != mappedStatus) {
                    updateStatus("m.Stock", "Fallback #3 Market Data", mappedStatus, "Health state updated to $health")
                }
            }
        }
    }

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

            // 2. Upstox (Primary Market Data)
            validateUpstoxSession()

            // 3. Fyers & Angel One Market Data
            validateFyersSession()
            validateAngelOneSession()

            // 4. m.Stock (Fallback #3 Data)
            validateMStockSession()

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
            updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.NOT_CONFIGURED, "Credentials not configured")
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
                    updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.ERROR, "Session expired. Re-authentication required.")
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
    // UPSTOX (PRIMARY REAL MARKET DATA)
    // ==========================================

    private suspend fun validateUpstoxSession() {
        val hasSession = !sessionManager.upstoxAccessToken.isNullOrBlank()
        val isConfigured = sessionManager.isUpstoxConfigured()

        brokerManager.healthManager.reportConfigured(ProviderHealthManager.PROVIDER_UPSTOX, isConfigured || hasSession)

        if (!isConfigured && !hasSession) {
            updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.NOT_CONFIGURED, "Credentials not configured")
            return
        }

        val currentHealth = brokerManager.healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        if (!hasSession && (currentHealth.authenticationState == ProviderHealthManager.STATE_AUTHENTICATING || currentHealth.authenticationState == ProviderHealthManager.STATE_WAITING_FOR_CALLBACK || currentHealth.authenticationState == ProviderHealthManager.STATE_TOKEN_EXCHANGE)) {
            Log.d(TAG, "Upstox OAuth currently in progress: preserving active state '${currentHealth.authenticationState}'")
            return
        }

        val timestamp = sessionManager.upstoxTokenTimestamp
        val isExpired = (System.currentTimeMillis() - timestamp) > 20 * 60 * 60 * 1000L

        if (isExpired) {
            brokerManager.healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, false, "TOKEN_EXPIRED")
            updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.ERROR, "Session Expired. Login Required.")
            return
        }

        if (hasSession) {
            try {
                val token = sessionManager.upstoxAccessToken ?: ""
                val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
                val profileRes = brokerManager.networkClient.upstoxApi.getUserProfile(token = authHeader)
                if (profileRes.isSuccessful && profileRes.body()?.status?.equals("success", ignoreCase = true) == true) {
                    brokerManager.healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
                    updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active")
                    brokerManager.upstoxMarketDataService.connect()
                    return
                } else {
                    val code = profileRes.code()
                    val msg = profileRes.errorBody()?.string() ?: "HTTP $code"
                    if (code == 401 || code == 403) {
                        sessionManager.clearUpstoxSession()
                        brokerManager.healthManager.reportAuthFailure(ProviderHealthManager.PROVIDER_UPSTOX, "TOKEN_INVALID", "TOKEN_INVALID: $msg")
                        updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.ERROR, "Session Expired. Login Required.")
                    } else {
                        brokerManager.healthManager.reportAuthFailure(ProviderHealthManager.PROVIDER_UPSTOX, "AUTH_FAILED", "AUTH_FAILED: $msg")
                        updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.ERROR, "Profile Validation Failed: $msg")
                    }
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upstox profile validation network error: ${e.message}")
                brokerManager.healthManager.reportAuthFailure(
                    ProviderHealthManager.PROVIDER_UPSTOX,
                    "TEMPORARY_NETWORK_ERROR",
                    "TEMPORARY_NETWORK_ERROR: ${e.localizedMessage}"
                )
                if (sessionManager.isUpstoxConnected) {
                    updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active (Offline Cache)")
                } else {
                    updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.ERROR, "Temporary Network Error during Validation")
                }
                return
            }
        }

        updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.ERROR, "Authentication Required. Tap LOGIN VIA BROWSER.")
    }

    suspend fun exchangeUpstoxCode(code: String): Result<String> = withContext(Dispatchers.IO) {
        val res = brokerManager.upstoxAuthManager.exchangeAuthCode(code)
        if (res.isSuccess) {
            brokerManager.healthManager.reportConfigured(ProviderHealthManager.PROVIDER_UPSTOX, true)
            brokerManager.healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
            updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active")
            brokerManager.upstoxMarketDataService.connect()
        } else {
            val err = res.exceptionOrNull()?.message ?: "Exchange failed"
            val failureState = if (err.contains("PROFILE_VALIDATION_FAILED")) "PROFILE_VALIDATION_FAILED" else "TOKEN_EXCHANGE_FAILED"
            brokerManager.healthManager.reportAuthFailure(ProviderHealthManager.PROVIDER_UPSTOX, failureState, err)
            updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.ERROR, "Authentication failed: $err")
        }
        res
    }

    // ==========================================
    // FYERS (FALLBACK #1 LIVE MARKET DATA)
    // ==========================================
    
    private suspend fun validateFyersSession() {
        val hasSession = !sessionManager.fyersAccessToken.isNullOrBlank()
        val hasRefreshToken = !sessionManager.fyersRefreshToken.isNullOrBlank()
        val isConfigured = !sessionManager.fyersAppId.isNullOrBlank()
        
        brokerManager.healthManager.reportConfigured(ProviderHealthManager.PROVIDER_FYERS, isConfigured || hasSession)

        if (!isConfigured && !hasSession) {
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.NOT_CONFIGURED, "Credentials not configured")
            return
        }

        val currentHealth = brokerManager.healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        if (!hasSession && (currentHealth.authenticationState == ProviderHealthManager.STATE_AUTHENTICATING || currentHealth.authenticationState == ProviderHealthManager.STATE_WAITING_FOR_CALLBACK || currentHealth.authenticationState == ProviderHealthManager.STATE_TOKEN_EXCHANGE)) {
            Log.d(TAG, "Fyers OAuth currently in progress: preserving active state '${currentHealth.authenticationState}'")
            return
        }
        
        // Check if token is older than 20 hours (expires daily)
        val timestamp = sessionManager.fyersTokenTimestamp
        val isExpired = (System.currentTimeMillis() - timestamp) > 20 * 60 * 60 * 1000L
        
        if (hasSession && !isExpired) {
            try {
                val token = sessionManager.fyersAccessToken ?: ""
                val appId = sessionManager.fyersAppId
                val authHeader = "$appId:$token"
                val profileRes = brokerManager.networkClient.fyersApi.getProfile(authHeader)
                if (profileRes.isSuccessful && profileRes.body()?.s == "ok") {
                    brokerManager.healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
                    updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active")
                    // Reconnect WebSocket
                    brokerManager.fyersMarketDataService.connect()
                    return
                } else {
                    val code = profileRes.code()
                    val pErr = profileRes.body()?.message ?: "HTTP $code"
                    Log.e(TAG, "Fyers profile validation failed: $pErr")
                    if (code == 401 || code == 403 || pErr.contains("invalid", ignoreCase = true) || pErr.contains("expire", ignoreCase = true)) {
                        sessionManager.clearFyersSession()
                        brokerManager.healthManager.reportAuthFailure(ProviderHealthManager.PROVIDER_FYERS, "TOKEN_INVALID", "TOKEN_INVALID: $pErr")
                        updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.ERROR, "Session Expired. Login Required.")
                    } else {
                        brokerManager.healthManager.reportAuthFailure(ProviderHealthManager.PROVIDER_FYERS, "AUTH_FAILED", "AUTH_FAILED: $pErr")
                        updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.ERROR, "Profile Validation Failed: $pErr")
                    }
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fyers profile validation request failed: ${e.message}")
                brokerManager.healthManager.reportAuthFailure(
                    ProviderHealthManager.PROVIDER_FYERS,
                    "TEMPORARY_NETWORK_ERROR",
                    "TEMPORARY_NETWORK_ERROR: ${e.localizedMessage}"
                )
                if (sessionManager.isFyersConnected) {
                    updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active (Offline Cache)")
                } else {
                    updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.ERROR, "Temporary Network Error during Validation")
                }
                return
            }
        }
        
        if (hasRefreshToken) {
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Restoring Session...")
            val result = reconnectBroker("Fyers")
            if (result.isSuccess) {
                try {
                    val token = sessionManager.fyersAccessToken ?: ""
                    val appId = sessionManager.fyersAppId
                    val authHeader = "$appId:$token"
                    val profileRes = brokerManager.networkClient.fyersApi.getProfile(authHeader)
                    if (profileRes.isSuccessful && profileRes.body()?.s == "ok") {
                        brokerManager.healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
                        updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active (Restored)")
                        brokerManager.fyersMarketDataService.connect()
                        return
                    } else {
                        val pErr = profileRes.body()?.message ?: "HTTP ${profileRes.code()}"
                        brokerManager.healthManager.reportAuthFailure(ProviderHealthManager.PROVIDER_FYERS, "AUTH_FAILED", "AUTH_FAILED: $pErr")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fyers refreshed profile check failed: ${e.message}")
                    brokerManager.healthManager.reportAuthFailure(
                        ProviderHealthManager.PROVIDER_FYERS,
                        "TEMPORARY_NETWORK_ERROR",
                        "TEMPORARY_NETWORK_ERROR: ${e.localizedMessage}"
                    )
                }
            }
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.ERROR, "Session Expired. Login Required.")
        } else {
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.ERROR, "Session Expired. Login Required.")
        }
    }

    // ==========================================
    // ANGEL ONE (FALLBACK #2 LIVE MARKET DATA)
    // ==========================================

    private suspend fun validateAngelOneSession() {
        val hasAngelToken = !sessionManager.angelJwtToken.isNullOrBlank()
        val hasRefreshToken = !sessionManager.angelRefreshToken.isNullOrBlank()
        val hasCredentials = sessionManager.angelClientId.isNotBlank() && sessionManager.angelMpin.isNotBlank() && sessionManager.angelTotpSecret.isNotBlank()

        if (!hasAngelToken && !hasRefreshToken && !hasCredentials) {
            updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.NOT_CONFIGURED, "Credentials not configured")
            return
        }

        if (hasAngelToken) {
            val profileRes = angelOneService.getProfile()
            if (profileRes.isSuccess) {
                Log.d(TAG, "Angel One SmartAPI session valid")
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #2 Active")
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
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #2 Active (Renewed)")
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
        updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.ERROR, "Session expired. MPIN/TOTP login required.")
    }

    suspend fun connectAngelOne(
        clientCode: String? = null,
        mpin: String? = null,
        totp: String? = null,
        totpSecret: String? = null,
        apiKey: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = runCatching {
            val code = clientCode?.trim()?.takeIf { it.isNotBlank() } ?: sessionManager.angelClientId
            val pass = mpin?.trim()?.takeIf { it.isNotBlank() } ?: sessionManager.angelMpin
            val secret = totpSecret?.trim()?.takeIf { it.isNotBlank() } ?: sessionManager.angelTotpSecret
            val explicitTotp = totp?.trim() ?: ""
            val key = apiKey?.trim()?.takeIf { it.isNotBlank() } ?: sessionManager.angelApiKey

            if (code.isBlank() || pass.isBlank()) {
                throw Exception("Client ID and MPIN are required for Angel One login.")
            }

            // Always save credentials to SessionManager BEFORE network call so they persist even if auth fails
            sessionManager.saveAngelOneCredentials(
                clientCode = code,
                mpin = pass,
                apiKey = key,
                totpSecret = secret
            )

            // Determine effective TOTP: Use explicit TOTP if supplied, otherwise generate from TOTP Secret
            val effectiveTotpInput = if (explicitTotp.isNotBlank()) {
                explicitTotp
            } else if (secret.isNotBlank()) {
                secret
            } else {
                throw Exception("Either 6-digit TOTP code or Base32 TOTP Secret must be provided.")
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
            sessionManager.angelTokenTimestamp = System.currentTimeMillis()
            sessionManager.isAngelConnected = true

            // Verify session with Get Profile API call
            val profileRes = angelOneService.getProfile()
            if (profileRes.isFailure) {
                Log.w(TAG, "Angel One profile warning: ${profileRes.exceptionOrNull()?.message}")
            }

            updateStatus("Angel One", getBrokerRole("Angel One"), BrokerAuthStatus.CONNECTED, "Authenticated successfully")

            // Auto connect SmartAPI WebSocket after session creation
            angelMarketDataService.connect()
            true
        }

        if (result.isFailure) {
            val err = result.exceptionOrNull()?.message ?: "Angel One authentication failed"
            updateStatus("Angel One", getBrokerRole("Angel One"), BrokerAuthStatus.ERROR, err)
        }
        result
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

            updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONNECTED, "Session auto-refreshed")
            angelMarketDataService.reconnect()
            true
        }
    }

    // ==========================================
    // m.STOCK (FALLBACK #3 MARKET DATA)
    // ==========================================

    private suspend fun validateMStockSession() {
        if (!sessionManager.isMStockConfigured()) {
            updateStatus("m.Stock", "Fallback #3 Market Data", BrokerAuthStatus.NOT_CONFIGURED, "Credentials not configured")
            return
        }

        val clientCode = sessionManager.mstockClientId
        val apiKey = sessionManager.mstockApiKey
        val totpSecret = sessionManager.mstockTotpSecret

        if (clientCode.isNotBlank() && apiKey.isNotBlank() && totpSecret.isNotBlank()) {
            Log.d(TAG, "Auto-authenticating m.Stock using stored TOTP secret...")
            val autoAuthRes = connectMStock(
                clientCode = clientCode,
                apiKey = apiKey,
                totpSecret = totpSecret
            )
            if (autoAuthRes.isSuccess) {
                Log.d(TAG, "m.Stock auto-login with TOTP secret succeeded.")
                return
            } else {
                Log.w(TAG, "m.Stock auto-login failed: ${autoAuthRes.exceptionOrNull()?.message}")
                updateStatus(
                    "m.Stock",
                    "Fallback #3 Market Data",
                    BrokerAuthStatus.ERROR,
                    "Auto-login failed: ${autoAuthRes.exceptionOrNull()?.message ?: "Re-authentication required"}"
                )
                mStockMarketDataService.connect()
                return
            }
        }

        updateStatus("m.Stock", "Fallback #3 Market Data", BrokerAuthStatus.CONNECTED, "Configured • Fallback #3")
        mStockMarketDataService.connect()
    }

    suspend fun connectMStock(
        clientCode: String? = null,
        apiKey: String? = null,
        totpSecret: String? = null,
        directTotp: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = runCatching {
            val code = clientCode?.takeIf { it.isNotBlank() } ?: sessionManager.mstockClientId
            val key = apiKey?.takeIf { it.isNotBlank() } ?: sessionManager.mstockApiKey
            val secret = totpSecret?.takeIf { it.isNotBlank() } ?: sessionManager.mstockTotpSecret
            val direct = directTotp?.takeIf { it.isNotBlank() }

            if (code.isBlank()) throw Exception("m.Stock Client Code is required.")
            if (key.isBlank()) throw Exception("m.Stock API Key is required.")

            // Always save credentials to SessionManager BEFORE network call so they persist even if auth fails
            sessionManager.saveMStockCredentials(
                clientCode = code,
                apiKey = key,
                totpSecret = secret
            )

            val effectiveTotpInput = when {
                !direct.isNullOrBlank() -> direct
                secret.isNotBlank() -> secret
                else -> throw Exception("m.Stock TOTP Secret or 6-digit TOTP is required.")
            }

            Log.d(TAG, "Authenticating with m.Stock verifytotp for client: $code")
            val authResult = MStockAuthHelper.verifyTotp(
                clientCode = code,
                apiKey = key,
                totpOrSecret = effectiveTotpInput,
                refreshToken = sessionManager.mstockRefreshToken
            )
            val tokens = authResult.getOrThrow()

            // Securely store credentials and tokens in Encrypted Storage
            sessionManager.mstockClientId = code
            sessionManager.mstockApiKey = key
            if (secret.isNotBlank()) {
                sessionManager.mstockTotpSecret = secret
            }
            sessionManager.mstockAccessToken = tokens.accessToken
            if (tokens.refreshToken.isNotBlank()) {
                sessionManager.mstockRefreshToken = tokens.refreshToken
            }
            if (tokens.feedToken.isNotBlank()) {
                sessionManager.mstockFeedToken = tokens.feedToken
            }
            sessionManager.mstockTokenTimestamp = System.currentTimeMillis()

            updateStatus("m.Stock", getBrokerRole("m.Stock"), BrokerAuthStatus.CONNECTED, "Configured • Fallback #3")

            mStockMarketDataService.connect()
            true
        }

        if (result.isFailure) {
            val err = result.exceptionOrNull()?.message ?: "m.Stock authentication failed"
            updateStatus("m.Stock", getBrokerRole("m.Stock"), BrokerAuthStatus.ERROR, err)
        }
        result
    }

    suspend fun refreshMStock(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val clientCode = sessionManager.mstockClientId
            val apiKey = sessionManager.mstockApiKey
            val refreshToken = sessionManager.mstockRefreshToken ?: ""
            val totpSecret = sessionManager.mstockTotpSecret

            if (clientCode.isBlank() || apiKey.isBlank()) {
                throw Exception("m.Stock is not configured.")
            }

            val renewResult = MStockAuthHelper.renewSession(
                clientCode = clientCode,
                apiKey = apiKey,
                refreshToken = refreshToken,
                totpSecret = totpSecret
            )
            val tokens = renewResult.getOrThrow()

            if (tokens.accessToken.isNotBlank()) sessionManager.mstockAccessToken = tokens.accessToken
            if (tokens.refreshToken.isNotBlank()) sessionManager.mstockRefreshToken = tokens.refreshToken
            if (tokens.feedToken.isNotBlank()) sessionManager.mstockFeedToken = tokens.feedToken
            sessionManager.mstockTokenTimestamp = System.currentTimeMillis()

            updateStatus("m.Stock", "Fallback #3 Market Data", BrokerAuthStatus.CONNECTED, "Session auto-refreshed")

            mStockMarketDataService.connect()
            true
        }
    }

    // ==========================================
    // ORCHESTRATION & COMMON METHODS
    // ==========================================

    private fun connectActiveMarketFeeds() {
        // Primary: Upstox
        if (sessionManager.isUpstoxConfigured()) {
            scope.launch {
                brokerManager.upstoxMarketDataService.connect()
            }
        }

        // Fallback #1: Fyers
        if (sessionManager.isFyersConfigured()) {
            scope.launch {
                brokerManager.fyersMarketDataService.connect()
            }
        }

        // Fallback #2: Angel One
        if (sessionManager.hasAngelSession()) {
            scope.launch {
                angelMarketDataService.connect()
            }
        }

        // Fallback #3: m.Stock
        if (sessionManager.isMStockConfigured()) {
            scope.launch {
                mStockMarketDataService.connect()
            }
        }
    }

    fun getConnectionStatus(): Map<String, BrokerConnectionState> {
        return _statuses.value
    }

    fun getConnectionStatus(brokerName: String): BrokerAuthStatus {
        return _statuses.value[brokerName]?.status ?: BrokerAuthStatus.DISCONNECTED
    }

    fun disconnectBroker(brokerName: String) {
        when (brokerName) {
            "Dhan" -> {
                sessionManager.clearDhanSession()
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Upstox" -> {
                brokerManager.upstoxMarketDataService.disconnect()
                sessionManager.clearUpstoxSession()
                updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Fyers" -> {
                brokerManager.fyersMarketDataService.disconnect()
                sessionManager.clearFyersSession()
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Angel One" -> {
                angelMarketDataService.disconnect()
                sessionManager.clearAngelSessionTokens()
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected • Credentials Saved")
            }
            "m.Stock" -> {
                mStockMarketDataService.disconnect()
                sessionManager.clearMStockSessionTokens()
                updateStatus("m.Stock", "Fallback #3 Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected • Credentials Saved")
            }
        }
    }

    fun removeAccount(brokerName: String) {
        when (brokerName) {
            "Dhan" -> {
                sessionManager.clearDhanCredentials()
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.NOT_CONFIGURED, "Account Removed")
            }
            "Upstox" -> {
                brokerManager.upstoxMarketDataService.disconnect()
                sessionManager.clearUpstoxCredentials()
                updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.NOT_CONFIGURED, "Account Removed")
            }
            "Fyers" -> {
                brokerManager.fyersMarketDataService.disconnect()
                sessionManager.clearFyersSession()
                sessionManager.fyersAppId = ""
                sessionManager.fyersSecretId = ""
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.NOT_CONFIGURED, "Account Removed")
            }
            "Angel One" -> {
                angelMarketDataService.disconnect()
                sessionManager.clearAngelOneCredentials()
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.NOT_CONFIGURED, "Account Removed")
            }
            "m.Stock" -> {
                mStockMarketDataService.disconnect()
                sessionManager.clearMStockCredentials()
                updateStatus("m.Stock", "Fallback #3 Market Data", BrokerAuthStatus.NOT_CONFIGURED, "Account Removed")
            }
        }
    }

    fun logoutBroker(brokerName: String) {
        disconnectBroker(brokerName)
    }

    suspend fun reconnectBroker(brokerName: String): Result<Boolean> {
        return when (brokerName) {
            "Dhan" -> refreshDhan()
            "Upstox" -> {
                if (sessionManager.hasUpstoxSession()) {
                    brokerManager.upstoxMarketDataService.connect()
                    Result.success(true)
                } else {
                    Result.failure(Exception("Upstox reconnect failed. Login required."))
                }
            }
            "Fyers" -> {
                val fyersAuth = brokerManager.fyersAuthManager
                val refreshRes = fyersAuth.refreshSession()
                if (refreshRes.isSuccess) {
                    brokerManager.fyersMarketDataService.connect()
                    Result.success(true)
                } else {
                    Result.failure(Exception(refreshRes.exceptionOrNull()?.message ?: "Fyers refresh failed"))
                }
            }
            "Angel One" -> {
                // 1. Try refresh token first if present
                if (!sessionManager.angelRefreshToken.isNullOrBlank()) {
                    val refreshRes = refreshAngelOne()
                    if (refreshRes.isSuccess && refreshRes.getOrThrow()) {
                        angelMarketDataService.connect()
                        return refreshRes
                    }
                }
                // 2. Fallback to saved credentials + auto-generated TOTP
                if (sessionManager.angelClientId.isNotBlank() && sessionManager.angelMpin.isNotBlank() && sessionManager.angelTotpSecret.isNotBlank()) {
                    Log.d(TAG, "Reconnecting Angel One using saved credentials & auto-generated TOTP...")
                    val autoRes = connectAngelOne(
                        clientCode = sessionManager.angelClientId,
                        mpin = sessionManager.angelMpin,
                        totpSecret = sessionManager.angelTotpSecret,
                        apiKey = sessionManager.angelApiKey
                    )
                    if (autoRes.isSuccess) {
                        return autoRes
                    }
                }
                Result.failure(Exception("Angel One reconnect failed. Credentials or session missing."))
            }
            "m.Stock" -> {
                // 1. Try refresh token first if present
                if (!sessionManager.mstockRefreshToken.isNullOrBlank()) {
                    val refreshRes = refreshMStock()
                    if (refreshRes.isSuccess && refreshRes.getOrThrow()) {
                        mStockMarketDataService.connect()
                        return refreshRes
                    }
                }
                // 2. Fallback to saved credentials + auto-generated TOTP
                if (sessionManager.mstockClientId.isNotBlank() && sessionManager.mstockApiKey.isNotBlank() && sessionManager.mstockTotpSecret.isNotBlank()) {
                    Log.d(TAG, "Reconnecting m.Stock using saved credentials & auto-generated TOTP...")
                    val autoRes = connectMStock(
                        clientCode = sessionManager.mstockClientId,
                        apiKey = sessionManager.mstockApiKey,
                        totpSecret = sessionManager.mstockTotpSecret
                    )
                    if (autoRes.isSuccess) {
                        return autoRes
                    }
                }
                Result.failure(Exception("m.Stock reconnect failed. Credentials or session missing."))
            }
            else -> Result.failure(Exception("Unknown broker: $brokerName"))
        }
    }

    fun handleAuthenticationError(brokerName: String, error: Throwable) {
        val msg = error.message ?: "Authentication error"
        Log.e(TAG, "Auth error for $brokerName: $msg", error)

        val status = BrokerAuthStatus.ERROR

        val role = getBrokerRole(brokerName)
        updateStatus(brokerName, role, status, msg)
    }

    fun getBrokerRole(brokerName: String): String {
        return when (brokerName) {
            "Dhan" -> "Primary Order Execution"
            "Upstox" -> "Primary Market Data"
            "Fyers" -> "Fallback #1 Market Data"
            "Angel One" -> "Fallback #2 Market Data"
            "m.Stock" -> "Fallback #3 Market Data"
            else -> "Market Provider"
        }
    }

    fun switchMarketDataProvider(providerName: String) {
        val target = when {
            providerName.contains("Upstox", ignoreCase = true) -> "Upstox"
            providerName.contains("Fyers", ignoreCase = true) -> "Fyers"
            providerName.contains("m.Stock", ignoreCase = true) -> "m.Stock"
            else -> "Angel One"
        }
        brokerManager.marketDataEngine.setPrimaryMarketDataProvider(target)
        refreshStatuses()
    }

    private fun refreshStatuses() {
        val current = _statuses.value.toMutableMap()
        for ((key, value) in current) {
            val newRole = getBrokerRole(key)
            current[key] = value.copy(role = newRole)
        }
        _statuses.value = current
    }

    fun updateStatus(brokerName: String, role: String, status: BrokerAuthStatus, message: String) {
        val current = _statuses.value.toMutableMap()
        val effectiveRole = getBrokerRole(brokerName)
        current[brokerName] = BrokerConnectionState(
            brokerName = brokerName,
            role = effectiveRole,
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
