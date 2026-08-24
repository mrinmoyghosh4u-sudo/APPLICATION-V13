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
    val exchange: String = "ALL",
    val connected: Boolean = false,
    val authenticated: Boolean = false,
    val authenticationState: String = "UNAUTHENTICATED",
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

    fun reportConnection(provider: String, isConnected: Boolean) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            connected = isConnected,
            webSocketState = if (isConnected) "CONNECTED" else "DISCONNECTED",
            healthy = isConnected && current.authenticated && current.firstTickReceived && !current.stale
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
    }

    fun reportAuthentication(provider: String, isAuthenticated: Boolean) {
        val current = healthMap[provider] ?: ProviderHealthState(provider = provider)
        val updated = current.copy(
            authenticated = isAuthenticated,
            authenticationState = if (isAuthenticated) "AUTHENTICATED" else "AUTHENTICATION_FAILED",
            healthy = current.connected && isAuthenticated && current.firstTickReceived && !current.stale
        )
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
            authenticationState = "AUTHENTICATED",
            webSocketState = "CONNECTED",
            subscriptionState = "SUBSCRIBED",
            firstTickReceived = true,
            lastTickTimestamp = timestamp,
            lastSuccessfulRequest = timestamp,
            tickAgeMs = age,
            latency = if (latencyMs > 0) latencyMs else current.latency,
            stale = false,
            errorCount = 0,
            healthy = true
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)

        if (!wasHealthy && updated.healthy) {
            try { Log.i(TAG, "DATA PROVIDER: $provider → HEALTHY (FIRST REAL TICK RECEIVED)") } catch (_: Throwable) { println("DATA PROVIDER: $provider → HEALTHY") }
        }
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
            healthy = isHealthy
        )
        healthMap[provider] = updated
        _providerHealthFlow.value = HashMap(healthMap)
        Log.w(TAG, "DATA PROVIDER ERROR: $provider (error count=$newErrors, msg=$errorMessage)")
    }

    fun checkAndEvaluateStaleness(now: Long = System.currentTimeMillis()) {
        healthMap.forEach { (provider, state) ->
            if (state.lastTickTimestamp > 0 && (now - state.lastTickTimestamp > STALE_TIMEOUT_MS)) {
                if (!state.stale) {
                    val updated = state.copy(stale = true, healthy = false)
                    healthMap[provider] = updated
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
        return state.errorCount < 3 && (state.authenticated )
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
