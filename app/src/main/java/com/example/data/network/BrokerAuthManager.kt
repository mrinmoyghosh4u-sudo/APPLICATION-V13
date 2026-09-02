package com.example.data.network

import android.util.Log
import com.example.util.AngelAuthHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

enum class BrokerAuthStatus {
    NOT_CONFIGURED,
    DISCONNECTED,
    AUTHENTICATION_REQUIRED,
    CONFIGURE,
    STANDBY,
    CONNECTED,
    ERROR
}

data class BrokerConnectionState(
    val brokerName: String,
    val roleDescription: String,
    val status: BrokerAuthStatus,
    val message: String
)

class BrokerAuthManager(
    private val sessionManager: SessionManager,
    private val dhanService: DhanBrokerService,
    private val angelOneService: AngelOneBrokerService,
    private val angelMarketDataService: AngelOneMarketDataService,
    private val brokerManager: BrokerManager
) {
    private val TAG = "BrokerAuthManager"
    
    private val _statuses = MutableStateFlow<Map<String, BrokerConnectionState>>(emptyMap())
    val statuses: StateFlow<Map<String, BrokerConnectionState>> = _statuses.asStateFlow()
    val providerHealth: StateFlow<Map<String, ProviderHealthState>>
        get() = brokerManager.healthManager.providerHealth
    
    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            brokerManager.healthManager.providerHealth.collect { healthMap ->
                val current = _statuses.value.toMutableMap()
                var changed = false
                healthMap.forEach { (key, healthState) ->
                    val existingName = when(key) {
                        "UPSTOX" -> "Upstox"
                        "FYERS" -> "Fyers"
                        "ANGEL_ONE" -> "Angel One"
                        "DHAN" -> "Dhan"
                        else -> return@forEach
                    }
                    val existing = current[existingName] ?: return@forEach
                    val newStatus = when {
                        healthState.status == "CONNECTED" || healthState.status == "SUBSCRIBED" || healthState.status == "LIVE" -> BrokerAuthStatus.CONNECTED
                        healthState.status == "CONNECTING" || healthState.status == "AUTHENTICATING" -> BrokerAuthStatus.STANDBY
                        healthState.status == "AUTH_FAILED" || healthState.status == "ERROR" -> BrokerAuthStatus.ERROR
                        healthState.status == "DISCONNECTED" -> BrokerAuthStatus.DISCONNECTED
                        else -> existing.status
                    }
                    if (existing.status != newStatus) {
                        current[existingName] = existing.copy(status = newStatus, message = healthState.status)
                        changed = true
                    }
                }
                if (changed) {
                    _statuses.value = current
                }
            }
        }
    }

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()
    
    fun updateStatus(name: String, role: String, status: BrokerAuthStatus, message: String) {
        val current = _statuses.value.toMutableMap()
        current[name] = BrokerConnectionState(name, role, status, message)
        _statuses.value = current
    }

    fun hasAnyConnectedBroker(): Boolean {
        return sessionManager.isDhanConnected || 
               sessionManager.isAngelConnected || 
               sessionManager.isUpstoxConnected || 
               sessionManager.isFyersConnected
    }

    fun hasActiveMarketDataProvider(): Boolean {
        return sessionManager.isUpstoxConnected || 
               sessionManager.isFyersConnected || 
               sessionManager.isAngelConnected
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        _isInitializing.value = true
        try {
            Log.i(TAG, "[SESSION_RESTORE_START] Commencing independent broker session validation...")
            validateDhanSession()
            validateUpstoxSession()
            validateFyersSession()
            validateAngelSession()
            Log.i(TAG, "[SESSION_RESTORE_END] Broker session validation completed. Connected brokers: Dhan=${sessionManager.isDhanConnected}, Upstox=${sessionManager.isUpstoxConnected}, Fyers=${sessionManager.isFyersConnected}, Angel=${sessionManager.isAngelConnected}")
        } finally {
            _isInitializing.value = false
        }
    }
    
    suspend fun validateDhanSession() = withContext(Dispatchers.IO) {
        val token = sessionManager.dhanAccessToken.trim()
        val clientId = sessionManager.dhanClientId.trim()
        
        if (token.isBlank()) {
            val hasCreds = clientId.isNotBlank() && 
                           (sessionManager.dhanClientSecret.isNotBlank() || com.example.util.BrokerConfig.dhanClientSecret.isNotBlank())
            val status = if (hasCreds) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.CONFIGURE
            val msg = if (hasCreds) "Login Required" else "Credentials not configured"
            sessionManager.isDhanConnected = false
            updateStatus("Dhan", "Primary Order Execution", status, msg)
            return@withContext
        }
        
        // Auto-renew Dhan token if older than 12 hours
        dhanService.checkAndRenewTokenIfNeeded()

        // Active API validation: verify Dhan session with live profile/funds call
        val profRes = dhanService.getProfile()
        if (profRes.isSuccess) {
            sessionManager.isDhanConnected = true
            updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONNECTED, "Active for Order Execution")
        } else {
            val errorMsg = profRes.exceptionOrNull()?.message ?: "Session Expired"
            if (errorMsg.contains("expired", ignoreCase = true) || 
                errorMsg.contains("invalid", ignoreCase = true) || 
                errorMsg.contains("401") || 
                errorMsg.contains("403")) {
                sessionManager.isDhanConnected = false
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Token Expired. Login Required.")
            } else {
                // Network error or other transient error: do not disconnect, preserve last state.
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.ERROR, errorMsg)
            }
        }
    }
    
    suspend fun validateAngelSession() = withContext(Dispatchers.IO) {
        val hasCreds = (sessionManager.angelApiKey.isNotBlank() || com.example.util.BrokerConfig.angelApiKey.isNotBlank()) &&
                       sessionManager.angelClientCode.isNotBlank()
        val hasAuthToken = sessionManager.angelAuthToken.isNotBlank()
        
        if (!hasCreds && !hasAuthToken) {
            sessionManager.isAngelConnected = false
            updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return@withContext
        }
        
        if (hasAuthToken) {
            val profRes = angelOneService.getProfile()
            if (profRes.isSuccess) {
                sessionManager.isAngelConnected = true
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #2 Active")
                return@withContext
            } else {
                val errorMsg = profRes.exceptionOrNull()?.message ?: "Session Expired"
                if (errorMsg.contains("expired", ignoreCase = true) || 
                    errorMsg.contains("invalid", ignoreCase = true) || 
                    errorMsg.contains("401") || 
                    errorMsg.contains("403")) {
                    Log.w(TAG, "[ANGEL_SESSION_VALIDATION_FAILED] Token invalid: ${errorMsg}")
                    sessionManager.isAngelConnected = false
                } else {
                    // Network error: preserve state
                    updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.ERROR, errorMsg)
                    return@withContext
                }
            }
        }

        // Token missing or expired: attempt auto-reconnect if credentials exist
        val clientCode = sessionManager.angelClientCode
        val pin = sessionManager.angelClientPin
        val apiKey = sessionManager.angelApiKey.takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.angelApiKey
        val totpSecret = sessionManager.angelTotpSecret
        
        if (clientCode.isNotBlank() && pin.isNotBlank() && apiKey.isNotBlank() && totpSecret.isNotBlank()) {
            updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.STANDBY, "Restoring Session...")
            val reconnectRes = connectAngelOne(clientCode, pin, apiKey, totpSecret)
            if (reconnectRes.isSuccess) {
                sessionManager.isAngelConnected = true
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #2 Active (Auto-Restored)")
                return@withContext
            }
        }

        sessionManager.isAngelConnected = false
        val status = if (hasCreds) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.CONFIGURE
        val msg = if (hasCreds) "Login Required" else "Credentials not configured"
        updateStatus("Angel One", "Fallback #2 Market Data", status, msg)
    }
    
    suspend fun validateUpstoxSession() = withContext(Dispatchers.IO) {
        val hasToken = !sessionManager.upstoxAccessToken.isNullOrBlank()
        val hasApiKey = sessionManager.upstoxApiKey.isNotBlank() || com.example.util.BrokerConfig.upstoxApiKey.isNotBlank()

        if (!hasToken && !hasApiKey) {
            sessionManager.isUpstoxConnected = false
            updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return@withContext
        }

        if (hasToken) {
            val isValid = brokerManager.upstoxAuthManager.validateSession()
            if (isValid) {
                sessionManager.isUpstoxConnected = true
                updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active")
                return@withContext
            }
        }

        sessionManager.isUpstoxConnected = false
        val status = if (hasApiKey) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.CONFIGURE
        val msg = if (hasApiKey) "Token Expired. Login Required." else "Credentials not configured"
        updateStatus("Upstox", "Primary Market Data", status, msg)
    }
    
    suspend fun validateFyersSession() = withContext(Dispatchers.IO) {
        val hasSession = !sessionManager.fyersAccessToken.isNullOrBlank()
        val hasAppId = sessionManager.fyersAppId.isNotBlank() || com.example.util.BrokerConfig.fyersAppId.isNotBlank()
        
        if (!hasSession && !hasAppId) {
            sessionManager.isFyersConnected = false
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return@withContext
        }
        
        // Enforce Daily Authentication
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        calendar.timeInMillis = sessionManager.fyersTokenTimestamp
        val authDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        
        if (hasSession && currentDay == authDay) {
            val isValid = brokerManager.fyersAuthManager.validateSession()
            if (isValid) {
                sessionManager.isFyersConnected = true
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active")
                return@withContext
            } else {
                val currentStatus = brokerManager.fyersAuthManager.authStatus.value
                if (currentStatus == BrokerAuthStatus.ERROR) {
                    sessionManager.isFyersConnected = true // Preserve state on network error
                    updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.ERROR, "Network or API Error")
                    return@withContext
                }
            }
        }
        
        sessionManager.isFyersConnected = false
        val status = if (hasAppId) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.CONFIGURE
        val msg = if (hasAppId) "Daily Login Required." else "Credentials not configured"
        updateStatus("Fyers", "Fallback #1 Market Data", status, msg)
    }
    
    suspend fun connectAngelOne(clientCode: String, pin: String, apiKey: String, totpSecret: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val res = AngelAuthHelper.generateSession(clientCode, pin, totpSecret, apiKey)
            if (res.isSuccess) {
                val tokens = res.getOrThrow()
                sessionManager.angelAuthToken = tokens.jwtToken
                sessionManager.angelJwtToken = tokens.jwtToken
                sessionManager.angelClientCode = clientCode
                sessionManager.angelClientId = clientCode
                sessionManager.angelFeedToken = tokens.feedToken
                sessionManager.angelRefreshToken = tokens.refreshToken
                sessionManager.angelApiKey = apiKey
                sessionManager.angelClientPin = pin
                sessionManager.angelTotpSecret = totpSecret
                sessionManager.angelTokenTimestamp = System.currentTimeMillis()
                sessionManager.isAngelConnected = true
                
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #2 Active")
                Result.success(true)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Angel One Login Failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun reconnectBroker(brokerName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        when (brokerName) {
            "Fyers" -> {
                sessionManager.isFyersConnected = false
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Daily Login Required.")
                Result.failure(Exception("Daily authentication required for Fyers"))
            }
            "Angel One" -> {
                val clientCode = sessionManager.angelClientCode
                val pin = sessionManager.angelClientPin
                val apiKey = sessionManager.angelApiKey.takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.angelApiKey
                val totpSecret = sessionManager.angelTotpSecret
                if (clientCode.isNotBlank() && pin.isNotBlank() && apiKey.isNotBlank() && totpSecret.isNotBlank()) {
                    connectAngelOne(clientCode, pin, apiKey, totpSecret)
                } else {
                    Result.failure(Exception("Missing credentials for auto-reconnect"))
                }
            }
            "Dhan" -> {
                validateDhanSession()
                if (sessionManager.isDhanConnected) Result.success(true) else Result.failure(Exception("Dhan session invalid"))
            }
            "Upstox" -> {
                validateUpstoxSession()
                if (sessionManager.isUpstoxConnected) Result.success(true) else Result.failure(Exception("Upstox session invalid"))
            }
            else -> Result.failure(Exception("$brokerName auto-reconnect not supported. Please re-login."))
        }
    }
    
    fun disconnectBroker(name: String) {
        when (name) {
            "Dhan" -> {
                sessionManager.clearDhanSessionTokens()
                val status = if (sessionManager.dhanClientId.isNotBlank()) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.DISCONNECTED
                val msg = if (sessionManager.dhanClientId.isNotBlank()) "Disconnected. Login Required." else "Disconnected"
                updateStatus("Dhan", "Primary Order Execution", status, msg)
            }
            "Angel One" -> {
                sessionManager.clearAngelSessionTokens()
                angelMarketDataService.disconnect()
                val isConfigured = sessionManager.angelClientId.isNotBlank()
                val status = if (isConfigured) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.DISCONNECTED
                val msg = if (isConfigured) "Disconnected. Login Required." else "Disconnected"
                updateStatus("Angel One", "Fallback #2 Market Data", status, msg)
            }
            "Upstox" -> {
                sessionManager.clearUpstoxSession()
                brokerManager.upstoxMarketDataService.disconnect()
                val isConfigured = sessionManager.upstoxApiKey.isNotBlank()
                val status = if (isConfigured) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.DISCONNECTED
                val msg = if (isConfigured) "Disconnected. Login Required." else "Disconnected"
                updateStatus("Upstox", "Primary Market Data", status, msg)
            }
            "Fyers" -> {
                sessionManager.clearFyersSession()
                brokerManager.fyersMarketDataService.disconnect()
                val isConfigured = sessionManager.fyersAppId.isNotBlank()
                val status = if (isConfigured) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.DISCONNECTED
                val msg = if (isConfigured) "Disconnected. Login Required." else "Disconnected"
                updateStatus("Fyers", "Fallback #1 Market Data", status, msg)
            }
        }
    }
    
    fun removeAccount(name: String) {
        when (name) {
            "Angel One" -> {
                sessionManager.clearAngelOneCredentials()
                angelMarketDataService.disconnect()
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials removed")
            }
            "Dhan" -> {
                sessionManager.clearDhanCredentials()
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONFIGURE, "Credentials removed")
            }
            "Upstox" -> {
                sessionManager.clearUpstoxAllData()
                brokerManager.upstoxMarketDataService.disconnect()
                updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Credentials removed")
            }
            "Fyers" -> {
                sessionManager.clearFyersAllData()
                brokerManager.fyersMarketDataService.disconnect()
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials removed")
            }
        }
    }
}
