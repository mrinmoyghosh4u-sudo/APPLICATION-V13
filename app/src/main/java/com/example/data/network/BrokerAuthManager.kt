package com.example.data.network

import android.util.Log
import com.example.util.AngelAuthHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        
        // Active API validation: verify Dhan session with live profile/funds call
        val profRes = dhanService.getProfile()
        if (profRes.isSuccess) {
            sessionManager.isDhanConnected = true
            updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONNECTED, "Active for Order Execution")
        } else {
            sessionManager.isDhanConnected = false
            val errorMsg = profRes.exceptionOrNull()?.message ?: "Session Expired"
            if (errorMsg.contains("expired", ignoreCase = true) || 
                errorMsg.contains("invalid", ignoreCase = true) || 
                errorMsg.contains("401") || 
                errorMsg.contains("403")) {
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Token Expired. Login Required.")
            } else {
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
                Log.w(TAG, "[ANGEL_SESSION_VALIDATION_FAILED] Token invalid: ${profRes.exceptionOrNull()?.message}")
                sessionManager.isAngelConnected = false
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
        val hasRefreshToken = !sessionManager.fyersRefreshToken.isNullOrBlank()
        val hasAppId = sessionManager.fyersAppId.isNotBlank() || com.example.util.BrokerConfig.fyersAppId.isNotBlank()
        
        if (!hasSession && !hasRefreshToken && !hasAppId) {
            sessionManager.isFyersConnected = false
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return@withContext
        }
        
        if (hasSession) {
            val isValid = brokerManager.fyersAuthManager.validateSession()
            if (isValid) {
                sessionManager.isFyersConnected = true
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active")
                return@withContext
            }
        }
        
        if (hasRefreshToken) {
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.STANDBY, "Restoring Session...")
            val result = brokerManager.fyersAuthManager.refreshSession()
            if (result.isSuccess) {
                sessionManager.isFyersConnected = true
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active (Restored)")
                return@withContext
            }
        }

        sessionManager.isFyersConnected = false
        val status = if (hasAppId) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.CONFIGURE
        val msg = if (hasAppId) "Session Expired. Login Required." else "Credentials not configured"
        updateStatus("Fyers", "Fallback #1 Market Data", status, msg)
    }
    
    suspend fun connectAngelOne(clientCode: String, pin: String, apiKey: String, totpSecret: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val res = AngelAuthHelper.generateSession(clientCode, pin, totpSecret, apiKey)
            if (res.isSuccess) {
                val tokens = res.getOrThrow()
                sessionManager.angelAuthToken = tokens.jwtToken
                sessionManager.angelFeedToken = tokens.feedToken
                sessionManager.angelRefreshToken = tokens.refreshToken
                sessionManager.angelApiKey = apiKey
                sessionManager.angelClientCode = clientCode
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
                val fyersAuth = brokerManager.fyersAuthManager
                val refreshRes = fyersAuth.refreshSession()
                if (refreshRes.isSuccess) {
                    sessionManager.isFyersConnected = true
                    updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active (Restored)")
                    Result.success(true)
                } else {
                    sessionManager.isFyersConnected = false
                    updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Session Expired. Login Required.")
                    Result.failure(Exception(refreshRes.exceptionOrNull()?.message ?: "Fyers refresh failed"))
                }
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
                sessionManager.clearDhanCredentials()
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Angel One" -> {
                sessionManager.clearAngelSessionTokens()
                angelMarketDataService.disconnect()
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Upstox" -> {
                sessionManager.clearUpstoxSession()
                brokerManager.upstoxMarketDataService.disconnect()
                updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Fyers" -> {
                sessionManager.clearFyersSession()
                brokerManager.fyersMarketDataService.disconnect()
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected")
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
                sessionManager.clearUpstoxSession()
                sessionManager.upstoxApiKey = ""
                sessionManager.upstoxApiSecret = ""
                brokerManager.upstoxMarketDataService.disconnect()
                updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Credentials removed")
            }
            "Fyers" -> {
                sessionManager.clearFyersSession()
                sessionManager.fyersAppId = ""
                sessionManager.fyersSecretId = ""
                brokerManager.fyersMarketDataService.disconnect()
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials removed")
            }
        }
    }
}
