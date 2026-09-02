package com.example.data.network

import android.util.Log
import com.example.data.model.MarketDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

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
    private val healthMap = ConcurrentHashMap<String, ProviderHealthState>()
    private val _providerHealth = MutableStateFlow<Map<String, ProviderHealthState>>(emptyMap())
    val providerHealth: StateFlow<Map<String, ProviderHealthState>> = _providerHealth.asStateFlow()

    init {
        for (provider in listOf(PROVIDER_UPSTOX, PROVIDER_FYERS, PROVIDER_ANGEL_ONE, PROVIDER_DHAN)) {
            val norm = normalizeKey(provider)
            healthMap[norm] = ProviderHealthState(provider = provider)
        }
        _providerHealth.value = healthMap.toMap()

        MarketDataStore.onTickReceivedListener = { provider: String, timestamp: Long ->
            reportTickReceived(provider, timestamp)
        }
    }

    companion object {
        const val PROVIDER_DHAN = "DHAN"
        const val PROVIDER_UPSTOX = "Upstox"
        const val PROVIDER_FYERS = "Fyers"
        const val PROVIDER_ANGEL_ONE = "Angel One"

        const val STALE_TIMEOUT_MS = 15000L // 15 seconds

        const val STATE_CREDENTIALS_MISSING = "CREDENTIALS_MISSING"
        const val STATE_STATE_MISMATCH = "STATE_MISMATCH"
        const val STATE_AUTH_CODE_MISSING = "AUTH_CODE_MISSING"
        const val STATE_TOKEN_EXCHANGE_FAILED = "TOKEN_EXCHANGE_FAILED"
        const val STATE_TOKEN_INVALID = "TOKEN_INVALID"
        const val STATE_AUTH_CANCELLED = "AUTH_CANCELLED"
    }

    private fun normalizeKey(provider: String): String {
        return com.example.data.model.MarketDataProviders.normalize(provider)
    }

    private fun updateState(provider: String, transform: (ProviderHealthState) -> ProviderHealthState) {
        val norm = normalizeKey(provider)
        val current = healthMap[norm] ?: ProviderHealthState(provider = provider)
        val updated = transform(current)
        healthMap[norm] = updated
        _providerHealth.value = healthMap.toMap()
    }

    fun getHealthState(provider: String = PROVIDER_UPSTOX): ProviderHealthState {
        val norm = normalizeKey(provider)
        return healthMap[norm] ?: ProviderHealthState(provider = provider)
    }

    fun isProviderHealthy(provider: String): Boolean {
        return getHealthState(provider).healthy
    }

    fun reportConfigured(provider: String, configured: Boolean) {
        updateState(provider) {
            it.copy(
                status = if (configured) "CONFIGURED" else "NOT_CONFIGURED",
                authenticationState = if (configured) "CONFIGURED" else "NOT_CONFIGURED",
                healthy = false
            )
        }
    }

    fun reportAuthenticating(provider: String = PROVIDER_UPSTOX) {
        updateState(provider) {
            it.copy(
                status = "AUTHENTICATING",
                authenticationState = "AUTHENTICATING",
                healthy = false
            )
        }
    }

    fun reportWaitingForCallback(provider: String = PROVIDER_UPSTOX, state: String = "") {
        updateState(provider) {
            it.copy(
                status = "WAITING_FOR_CALLBACK",
                authenticationState = "WAITING_FOR_CALLBACK"
            )
        }
    }

    fun reportCallbackReceived(provider: String = PROVIDER_UPSTOX, state: String = "") {
        updateState(provider) {
            it.copy(
                status = "CALLBACK_RECEIVED",
                authenticationState = "CALLBACK_RECEIVED"
            )
        }
    }

    fun reportValidatingState(provider: String = PROVIDER_UPSTOX) {
        updateState(provider) {
            it.copy(
                status = "VALIDATING_STATE",
                authenticationState = "VALIDATING_STATE"
            )
        }
    }

    fun reportAuthCodeReceived(provider: String = PROVIDER_UPSTOX, code: String = "") {
        updateState(provider) {
            it.copy(
                status = "AUTH_CODE_RECEIVED",
                authenticationState = "AUTH_CODE_RECEIVED"
            )
        }
    }

    fun reportTokenExchange(provider: String = PROVIDER_UPSTOX) {
        updateState(provider) {
            it.copy(
                status = "TOKEN_EXCHANGE",
                authenticationState = "TOKEN_EXCHANGE"
            )
        }
    }

    fun reportTokenValidated(provider: String = PROVIDER_UPSTOX) {
        updateState(provider) {
            it.copy(
                status = "TOKEN_VALIDATED",
                authenticationState = "TOKEN_VALIDATED"
            )
        }
    }

    fun reportAuthentication(provider: String = PROVIDER_UPSTOX, success: Boolean, errorMessage: String = "") {
        updateState(provider) {
            if (success) {
                it.copy(
                    status = "AUTHENTICATED",
                    authenticationState = "AUTHENTICATED",
                    authenticated = true,
                    lastError = "",
                    healthy = false
                )
            } else {
                it.copy(
                    status = "AUTH_FAILED",
                    authenticationState = "AUTH_FAILED",
                    authenticated = false,
                    lastError = errorMessage,
                    healthy = false
                )
            }
        }
    }

    fun reportAuthFailure(provider: String, failureType: String, message: String) {
        val mappedAuthState = when (failureType) {
            STATE_STATE_MISMATCH -> "STATE_VALIDATION_FAILED"
            STATE_AUTH_CODE_MISSING -> "AUTH_CODE_MISSING"
            STATE_TOKEN_EXCHANGE_FAILED -> "TOKEN_EXCHANGE_FAILED"
            STATE_TOKEN_INVALID -> "TOKEN_INVALID"
            STATE_AUTH_CANCELLED -> "AUTH_CANCELLED"
            else -> "AUTH_FAILED"
        }
        updateState(provider) {
            it.copy(
                status = "AUTH_FAILED",
                authenticationState = mappedAuthState,
                authenticated = false,
                lastError = message,
                healthy = false
            )
        }
    }

    fun reportConnecting(provider: String = PROVIDER_UPSTOX) {
        updateState(provider) {
            it.copy(
                status = "CONNECTING",
                webSocketState = "CONNECTING",
                healthy = false
            )
        }
    }

    fun reportConnection(provider: String = PROVIDER_UPSTOX, connected: Boolean) {
        updateState(provider) {
            if (connected) {
                it.copy(
                    status = "CONNECTED",
                    webSocketState = "CONNECTED",
                    connected = true,
                    healthy = false
                )
            } else {
                it.copy(
                    status = "DISCONNECTED",
                    webSocketState = "DISCONNECTED",
                    connected = false,
                    healthy = false
                )
            }
        }
    }

    fun reportDisconnected(provider: String = PROVIDER_UPSTOX) {
        reportConnection(provider, false)
    }

    fun reportSubscribing(provider: String = PROVIDER_UPSTOX) {
        updateState(provider) {
            it.copy(
                status = "SUBSCRIBING",
                subscriptionState = "SUBSCRIBING",
                healthy = false
            )
        }
    }

    fun reportSubscribed(provider: String = PROVIDER_UPSTOX, count: Int) {
        updateState(provider) {
            it.copy(
                status = "WAITING_FOR_FIRST_TICK",
                subscriptionState = "SUBSCRIBED",
                activeSubscriptionCount = count,
                healthy = false
            )
        }
    }

    fun reportTickReceived(provider: String, timestamp: Long) {
        updateState(provider) {
            it.copy(
                status = "LIVE",
                firstTickReceived = true,
                lastTickTimestamp = timestamp,
                tickAgeMs = 0L,
                stale = false,
                healthy = true,
                connected = true,
                authenticated = true,
                lastError = ""
            )
        }
    }

    fun checkAndEvaluateStaleness(now: Long = System.currentTimeMillis()) {
        for ((provider, state) in healthMap) {
            if (state.lastTickTimestamp > 0L) {
                val age = now - state.lastTickTimestamp
                if (age > STALE_TIMEOUT_MS) {
                    updateState(provider) {
                        it.copy(
                            status = "STALE",
                            tickAgeMs = age,
                            stale = true,
                            healthy = false
                        )
                    }
                }
            }
        }
    }
}
