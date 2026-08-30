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
        const val PROVIDER_DHAN = "DHAN"

        const val STATE_CREDENTIALS_MISSING = "CREDENTIALS_MISSING"

        private const val TAG = "DataEngineDiagnostics"
        const val STALE_TIMEOUT_MS = 15000L // 15 seconds stale timeout
        
        // Canonical Provider Keys
        const val PROVIDER_UPSTOX = "Upstox"
        const val PROVIDER_FYERS = "Fyers"
        const val PROVIDER_ANGEL_ONE = "Angel One"

}


    fun reportAuthenticating(provider: String) {}
    fun reportWaitingForCallback(provider: String, state: String) {}
    fun reportAuthFailure(provider: String, errorCategory: String, message: String) {}
    fun reportCallbackReceived(provider: String, state: String) {}
    fun reportAuthCodeReceived(provider: String, code: String) {}
}
