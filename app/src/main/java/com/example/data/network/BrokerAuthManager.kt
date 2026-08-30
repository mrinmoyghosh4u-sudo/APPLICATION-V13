package com.example.data.network

import android.util.Log
import com.example.util.AngelAuthHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    
    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()
    
    fun updateStatus(name: String, role: String, status: BrokerAuthStatus, message: String) {
        val current = _statuses.value.toMutableMap()
        current[name] = BrokerConnectionState(name, role, status, message)
        _statuses.value = current
    }

    suspend fun initialize() {
        _isInitializing.value = true
        try {
            validateDhanSession()
            validateAngelSession()
            validateUpstoxSession()
            validateFyersSession()
        } finally {
            _isInitializing.value = false
        }
    }
    
    private suspend fun validateDhanSession() {
        val hasCreds = sessionManager.dhanClientId.isNotBlank() && 
                       (sessionManager.dhanClientSecret.isNotBlank() || com.example.util.BrokerConfig.dhanClientSecret.isNotBlank())
        
        if (!hasCreds) {
            updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }
        
        if (sessionManager.dhanAccessToken.isNotBlank()) {
            updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONNECTED, "Active for Order Execution")
        } else {
            updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Login Required")
        }
    }
    
    private suspend fun validateAngelSession() {
        val hasCreds = sessionManager.angelApiKey.isNotBlank() || com.example.util.BrokerConfig.angelApiKey.isNotBlank()
        
        if (!hasCreds) {
            updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }
        
        if (sessionManager.angelAuthToken.isNotBlank()) {
            updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #2 Active")
            angelMarketDataService.connect()
        } else {
            updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Login Required")
        }
    }
    
    private suspend fun validateUpstoxSession() {
        if (!sessionManager.upstoxAccessToken.isNullOrBlank()) {
            updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active")
            brokerManager.upstoxMarketDataService.connect()
        } else {
            updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Login Required")
        }
    }
    
    private suspend fun validateFyersSession() {
        val hasSession = !sessionManager.fyersAccessToken.isNullOrBlank()
        val hasRefreshToken = !sessionManager.fyersRefreshToken.isNullOrBlank()
        
        if (!hasSession && !hasRefreshToken) {
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }
        
        val timestamp = sessionManager.fyersTokenTimestamp
        val isExpired = (System.currentTimeMillis() - timestamp) > 20 * 60 * 60 * 1000L
        
        if (hasSession && !isExpired) {
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active")
            brokerManager.fyersMarketDataService.connect()
            return
        }
        
        if (hasRefreshToken) {
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.STANDBY, "Restoring Session...")
            val result = reconnectBroker("Fyers")
            if (result.isSuccess) {
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #1 Active (Restored)")
            } else {
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Session Expired. Login Required.")
            }
        } else {
            updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Session Expired. Login Required.")
        }
    }
    
    suspend fun connectAngelOne(clientCode: String, pin: String, apiKey: String, totpSecret: String): Result<Boolean> {
        return try {
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
                sessionManager.isAngelConnected = true
                
                angelMarketDataService.connect()
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONNECTED, "Fallback #2 Active")
                Result.success(true)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Angel One Login Failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun reconnectBroker(brokerName: String): Result<Boolean> {
        return when (brokerName) {
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
                val clientCode = sessionManager.angelClientCode
                val pin = sessionManager.angelClientPin
                val apiKey = sessionManager.angelApiKey
                val totpSecret = sessionManager.angelTotpSecret
                if (clientCode.isNotBlank() && pin.isNotBlank() && apiKey.isNotBlank()) {
                    connectAngelOne(clientCode, pin, apiKey, totpSecret)
                } else {
                    Result.failure(Exception("Missing credentials for auto-reconnect"))
                }
            }
            else -> Result.failure(Exception("$brokerName auto-reconnect not supported. Please re-login."))
        }
    }
    
    fun disconnectBroker(name: String) {
        when (name) {
            "Dhan" -> {
                sessionManager.dhanAccessToken = ""
                sessionManager.isDhanConnected = false
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Angel One" -> {
                sessionManager.angelAuthToken = ""
                sessionManager.isAngelConnected = false
                angelMarketDataService.disconnect()
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Upstox" -> {
                sessionManager.upstoxAccessToken = ""
                sessionManager.isUpstoxConnected = false
                brokerManager.upstoxMarketDataService.disconnect()
                updateStatus("Upstox", "Primary Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
            "Fyers" -> {
                sessionManager.fyersAccessToken = ""
                sessionManager.fyersRefreshToken = ""
                sessionManager.isFyersConnected = false
                brokerManager.fyersMarketDataService.disconnect()
                updateStatus("Fyers", "Fallback #1 Market Data", BrokerAuthStatus.DISCONNECTED, "Disconnected")
            }
        }
    }
    
    fun removeAccount(name: String) {
        disconnectBroker(name)
        when (name) {
            "Angel One" -> {
                sessionManager.angelApiKey = ""
                sessionManager.angelClientCode = ""
                sessionManager.angelClientPin = ""
                sessionManager.angelTotpSecret = ""
                updateStatus("Angel One", "Fallback #2 Market Data", BrokerAuthStatus.CONFIGURE, "Credentials removed")
            }
            "Dhan" -> {
                sessionManager.dhanClientId = ""
                sessionManager.dhanClientSecret = ""
                updateStatus("Dhan", "Primary Order Execution", BrokerAuthStatus.CONFIGURE, "Credentials removed")
            }
        }
    }
}
