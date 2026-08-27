package com.example.data.network

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Provider Health Monitor
 * 
 * Tracks internal health metrics for all market data providers:
 * - Connection state
 * - Authentication state
 * - Last successful request & tick timestamps
 * - Latency & error count
 * - Stale threshold detection
 * 
 * Strict Rules:
 * - Provider info is NEVER exposed to normal trading UI.
 * - Safe internal diagnostic logging only.
 * - Sensitive secrets (API keys, JWT, TOTP, tokens) are NEVER logged.
 */
data class ProviderHealthState(
    val provider: String,
    val status: String = "NOT_CONFIGURED",
    val exchange: String = "ALL",
    val connected: Boolean = false,
    val authenticated: Boolean = false,
    val authenticationState: String = "NOT_CONFIGURED",
    val webSocketState: String = "DISCONNECTED",
    val subscriptionState: String = "UNSUBSCRIBED",
    val firstTickReceived: Boolean = false,
    val lastSuccessfulRequest: Long = 0L,
    val lastTickTimestamp: Long = 0L,
    val tickAgeMs: Long = 0L,
    val latency: Long = 0L,
    val errorCount: Int = 0,
    val activeInstrumentCount: Int = 0,
    val activeSubscriptionCount: Int = 0,
    val lastError: String = "",
    val dataSource: String = provider,
    val stale: Boolean = false,
    val healthy: Boolean = false
)

class ProviderHealthManager {
    companion object {
        private const val TAG = "DataEngineDiagnostics"
        const val STALE_TIMEOUT_MS = 15000L // 15 seconds stale timeout
        
        // Canonical Provider Keys
        const val PROVIDER_UPSTOX = "Upstox"
        const val PROVIDER_FYERS = "Fyers"
        const val PROVIDER_ANGEL_ONE = "Angel One"
        const val PROVIDER_MSTOCK = "m.Stock"
        const val PROVIDER_NONE = "NONE"

        // Required Phase 1 States
        const val STATE_NOT_CONFIGURED = "NOT_CONFIGURED"
        const val STATE_CONFIGURED = "CONFIGURED"
        const val STATE_READY = "READY"
        const val STATE_AUTHENTICATING = "AUTHENTICATING"
        const val STATE_WAITING_FOR_CALLBACK = "WAITING_FOR_CALLBACK"
        const val STATE_CALLBACK_RECEIVED = "CALLBACK_RECEIVED"
        const val STATE_VALIDATING_STATE = "VALIDATING_STATE"
        const val STATE_AUTH_CODE_RECEIVED = "AUTH_CODE_RECEIVED"
        const val STATE_TOKEN_EXCHANGE = "TOKEN_EXCHANGE"
        const val STATE_TOKEN_VALIDATED = "TOKEN_VALIDATED"
        const val STATE_AUTHENTICATED = "AUTHENTICATED"

        // Failure States
        const val STATE_CREDENTIALS_MISSING = "CREDENTIALS_MISSING"
        const val STATE_AUTHORIZATION_STARTED = "AUTHORIZATION_STARTED"
        const val STATE_AUTH_FAILED = "AUTH_FAILED"
        const val STATE_CALLBACK_FAILED = "CALLBACK_FAILED"
        const val STATE_STATE_MISMATCH = "STATE_VALIDATION_FAILED"
        const val STATE_AUTH_CODE_MISSING = "AUTH_CODE_MISSING"
        const val STATE_TOKEN_EXCHANGE_FAILED = "TOKEN_EXCHANGE_FAILED"
        const val STATE_TOKEN_INVALID = "TOKEN_INVALID"
        const val STATE_AUTH_CANCELLED = "AUTH_CANCELLED"

        const val STATE_CONNECTING = "WEBSOCKET_CONNECTING"
        const val STATE_CONNECTED = "WEBSOCKET_CONNECTED"
        const val STATE_SUBSCRIBING = "SUBSCRIBING"
        const val STATE_SUBSCRIBED = "SUBSCRIBED"
        const val STATE_WAITING_FOR_FIRST_TICK = "WAITING_FOR_FIRST_TICK"
        const val STATE_LIVE = "LIVE"
        const val STATE_STALE = "STALE"
        const val STATE_MARKET_CLOSED = "MARKET_CLOSED"
        const val STATE_DISCONNECTED = "DISCONNECTED"
        const val STATE_ERROR = "ERROR"
    }

    private val healthMap = ConcurrentHashMap<String, ProviderHealthState>()
    private val _providerHealthFlow = MutableStateFlow<Map<String, ProviderHealthState>>(emptyMap())
    val providerHealthFlow: StateFlow<Map<String, ProviderHealthState>> = _providerHealthFlow.asStateFlow()

    init {
        // Initialize default health state
        listOf(PROVIDER_UPSTOX, PROVIDER_FYERS, PROVIDER_ANGEL_ONE, PROVIDER_MSTOCK).forEach { name ->
            healthMap[name] = ProviderHealthState(provider = name)
        }
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportConfigured(provider: String, isConfigured: Boolean) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val newStatus = if (isConfigured) STATE_CONFIGURED else STATE_NOT_CONFIGURED
        val updated = current.copy(
            status = newStatus,
            authenticationState = if (isConfigured) STATE_CONFIGURED else STATE_NOT_CONFIGURED
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportAuthenticating(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            status = STATE_AUTHENTICATING,
            authenticationState = STATE_AUTHENTICATING
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportWaitingForCallback(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            status = STATE_WAITING_FOR_CALLBACK,
            authenticationState = STATE_WAITING_FOR_CALLBACK
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportCallbackReceived(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            status = STATE_CALLBACK_RECEIVED,
            authenticationState = STATE_CALLBACK_RECEIVED
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportValidatingState(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            status = STATE_VALIDATING_STATE,
            authenticationState = STATE_VALIDATING_STATE
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportAuthCodeReceived(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            status = STATE_AUTH_CODE_RECEIVED,
            authenticationState = STATE_AUTH_CODE_RECEIVED
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportTokenExchange(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            status = STATE_TOKEN_EXCHANGE,
            authenticationState = STATE_TOKEN_EXCHANGE
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportTokenValidated(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            status = STATE_TOKEN_VALIDATED,
            authenticationState = STATE_TOKEN_VALIDATED
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportAuthFailure(provider: String, failureState: String, errorMessage: String = "") {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            authenticated = false,
            authenticationState = failureState,
            status = failureState,
            lastError = if (errorMessage.isNotBlank()) errorMessage else current.lastError,
            healthy = false
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportAuthentication(provider: String, isAuthenticated: Boolean, errorMessage: String = "") {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val authState = if (isAuthenticated) STATE_AUTHENTICATED else STATE_AUTH_FAILED
        val newStatus = if (isAuthenticated) {
            if (current.firstTickReceived) STATE_LIVE else STATE_AUTHENTICATED
        } else {
            STATE_AUTH_FAILED
        }
        val updated = current.copy(
            authenticated = isAuthenticated,
            authenticationState = authState,
            status = newStatus,
            lastError = if (isAuthenticated) "" else (if (errorMessage.isNotBlank()) errorMessage else current.lastError),
            healthy = current.connected && isAuthenticated && current.firstTickReceived && !current.stale
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportConnecting(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            status = STATE_CONNECTING,
            webSocketState = STATE_CONNECTING
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportConnection(provider: String, isConnected: Boolean) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val wsState = if (isConnected) STATE_CONNECTED else STATE_DISCONNECTED
        val newStatus = if (isConnected) {
            if (current.firstTickReceived) STATE_LIVE else STATE_CONNECTED
        } else {
            STATE_DISCONNECTED
        }
        val updated = current.copy(
            connected = isConnected,
            webSocketState = wsState,
            status = newStatus,
            lastError = if (isConnected) "" else current.lastError,
            errorCount = if (isConnected) 0 else current.errorCount,
            healthy = isConnected && current.authenticated && current.firstTickReceived && !current.stale
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun clearError(provider: String) {
        val current = healthMap[provider] ?: return
        val updated = current.copy(
            lastError = "",
            errorCount = 0
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportSubscribing(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            subscriptionState = STATE_SUBSCRIBING,
            status = STATE_SUBSCRIBING
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportSubscribed(provider: String, activeCount: Int = 0) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val newStatus = if (current.firstTickReceived) STATE_LIVE else STATE_WAITING_FOR_FIRST_TICK
        val updated = current.copy(
            subscriptionState = STATE_SUBSCRIBED,
            activeSubscriptionCount = activeCount,
            status = newStatus,
            lastError = ""
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportWaitingForTick(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val newStatus = if (current.firstTickReceived) STATE_LIVE else STATE_WAITING_FOR_FIRST_TICK
        val updated = current.copy(status = newStatus)
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportTickReceived(provider: String, timestamp: Long = System.currentTimeMillis(), latencyMs: Long = 0L) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val wasHealthy = current.healthy
        val now = System.currentTimeMillis()
        val age = (now - timestamp).coerceAtLeast(0L)
        val updated = current.copy(
            connected = true,
            authenticated = true,
            authenticationState = STATE_AUTHENTICATED,
            webSocketState = STATE_CONNECTED,
            subscriptionState = STATE_SUBSCRIBED,
            firstTickReceived = true,
            lastTickTimestamp = timestamp,
            lastSuccessfulRequest = timestamp,
            tickAgeMs = age,
            latency = if (latencyMs > 0) latencyMs else current.latency,
            stale = false,
            errorCount = 0,
            lastError = "",
            status = STATE_LIVE,
            healthy = true
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)

        if (!wasHealthy && updated.healthy) {
            try { Log.i(TAG, "DATA PROVIDER: $provider → HEALTHY (FIRST REAL TICK RECEIVED)") } catch (_: Throwable) { println("DATA PROVIDER: $provider → HEALTHY") }
        }
    }

    fun reportDisconnected(provider: String) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            connected = false,
            webSocketState = STATE_DISCONNECTED,
            status = STATE_DISCONNECTED,
            healthy = false
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportSuccessfulRequest(provider: String, latencyMs: Long = 0L) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val now = System.currentTimeMillis()
        val updated = current.copy(
            lastSuccessfulRequest = now,
            latency = if (latencyMs > 0) latencyMs else current.latency,
            errorCount = 0
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportError(provider: String, errorMessage: String = "") {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val newErrors = current.errorCount + 1
        val isHealthy = if (newErrors >= 3) false else current.healthy
        val updated = current.copy(
            errorCount = newErrors,
            lastError = if (errorMessage.isNotBlank()) errorMessage else current.lastError,
            status = STATE_ERROR,
            healthy = isHealthy
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
        Log.w(TAG, "DATA PROVIDER ERROR: $provider (error count=$newErrors, msg=$errorMessage)")
    }

    fun isMarketOpen(now: Long = System.currentTimeMillis()): Boolean {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        cal.timeInMillis = now
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
            return false
        }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute
        // Regular NSE/BSE Market Hours: 09:15 to 15:30 IST
        return timeInMinutes in (9 * 60 + 15)..(15 * 60 + 30)
    }

    fun checkAndEvaluateStaleness(now: Long = System.currentTimeMillis()) {
        val marketOpen = isMarketOpen(now)
        healthMap.forEach { (provider, state) ->
            if (state.lastTickTimestamp > 0 && (now - state.lastTickTimestamp > STALE_TIMEOUT_MS)) {
                val newStatus = if (!marketOpen && state.connected && state.authenticated) {
                    STATE_MARKET_CLOSED
                } else {
                    STATE_STALE
                }
                val updated = state.copy(
                    stale = true,
                    status = newStatus,
                    healthy = false
                )
                healthMap[provider] = updated
                if (newStatus == STATE_MARKET_CLOSED) {
                    try { Log.i(TAG, "DATA PROVIDER: $provider → MARKET_CLOSED (Connection healthy, market closed)") } catch (_: Throwable) { println("DATA PROVIDER: $provider → MARKET_CLOSED") }
                } else {
                    try { Log.w(TAG, "DATA PROVIDER: $provider → STALE (no ticks for >15s)") } catch (_: Throwable) { println("DATA PROVIDER: $provider → STALE") }
                }
            }
        }
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun isProviderHealthy(provider: String): Boolean {
        val state = healthMap[provider] ?: return false
        val now = System.currentTimeMillis()
        val isRecent = state.lastTickTimestamp > 0 && (now - state.lastTickTimestamp <= STALE_TIMEOUT_MS)
        return state.connected && state.authenticated && !state.stale && isRecent
    }

    fun isProviderAvailableForRest(provider: String): Boolean {
        val state = healthMap[provider] ?: return false
        return state.errorCount < 3 && (state.authenticated)
    }

    fun getHealthState(provider: String): ProviderHealthState {
        return healthMap[provider] ?: ProviderHealthState(provider = provider)
    }

    fun logFailover(fromProvider: String, toProvider: String) {
        try { Log.i(TAG, "DATA FAILOVER: $fromProvider → $toProvider") } catch (_: Throwable) { println("DATA FAILOVER: $fromProvider → $toProvider") }
    }

    fun logRestored(fallbackProvider: String, primaryProvider: String) {
        try { Log.i(TAG, "DATA RESTORED: $fallbackProvider → $primaryProvider") } catch (_: Throwable) { println("DATA RESTORED: $fallbackProvider → $primaryProvider") }
    }
}
