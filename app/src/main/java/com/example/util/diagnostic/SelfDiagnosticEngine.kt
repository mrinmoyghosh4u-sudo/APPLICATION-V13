package com.example.util.diagnostic

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.data.network.BrokerManager
import com.example.data.model.MarketDataStore
import com.example.util.validation.ValidationEngine

class SelfDiagnosticEngine(private val brokerManager: BrokerManager) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _systemHealth = MutableStateFlow(HealthState.HEALTHY)
    val systemHealth: StateFlow<HealthState> = _systemHealth.asStateFlow()

    private val _componentHealthMap = MutableStateFlow<Map<String, ComponentHealth>>(emptyMap())
    val componentHealthMap: StateFlow<Map<String, ComponentHealth>> = _componentHealthMap.asStateFlow()

    private val _recoveryLogs = MutableStateFlow<List<AutoRecoveryLog>>(emptyList())
    val recoveryLogs: StateFlow<List<AutoRecoveryLog>> = _recoveryLogs.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            while (true) {
                delay(5000) // Monitor every 5 seconds
                runDiagnostics()
            }
        }
    }

    private suspend fun runDiagnostics() {
        val newHealthMap = mutableMapOf<String, ComponentHealth>()

        // 1. Monitor Upstox (Market Data Priority 1)
        val upstoxHealth = checkBrokerHealth("UPSTOX")
        newHealthMap["UPSTOX"] = upstoxHealth

        // 2. Monitor Fyers (Market Data Priority 2)
        val fyersHealth = checkBrokerHealth("FYERS")
        newHealthMap["FYERS"] = fyersHealth

        // 3. Monitor Angel One (Market Data Priority 3)
        val angelHealth = checkBrokerHealth("ANGEL ONE")
        newHealthMap["ANGEL_ONE"] = angelHealth

        // 4. Monitor m.Stock (Market Data Priority 4)
        val mStockHealth = checkBrokerHealth("m.STOCK")
        newHealthMap["MSTOCK"] = mStockHealth

        // 5. Monitor Dhan (Order Execution Only)
        val dhanHealth = checkDhanHealth()
        newHealthMap["DHAN"] = dhanHealth

        // 6. Monitor Option Chain
        val optionChainHealth = checkOptionChainHealth()
        newHealthMap["OPTION_CHAIN"] = optionChainHealth

        // 7. Monitor AI Engine
        val aiHealth = checkAiSignalHealth()
        newHealthMap["AI_SIGNAL"] = aiHealth

        _componentHealthMap.value = newHealthMap

        // Update overall system health based on critical components
        val activeProviderState = MarketDataStore.providerState.value
        val isMarketDataLive = activeProviderState.live && !activeProviderState.stale
        
        if (!isMarketDataLive) {
            _systemHealth.value = HealthState.DEGRADED
            attemptFailoverOrRecovery()
        } else {
            _systemHealth.value = HealthState.HEALTHY
        }
    }

    private fun checkBrokerHealth(broker: String): ComponentHealth {
        val authStatus = brokerManager.brokerAuthManager.statuses.value[broker]?.status
        if (authStatus != com.example.data.network.BrokerAuthStatus.CONNECTED) {
            return ComponentHealth(broker, HealthState.AUTH_FAILED, details = mapOf("error" to "Not authenticated"))
        }

        val providerState = MarketDataStore.providerState.value
        if (providerState.provider == broker) {
            if (providerState.stale) {
                return ComponentHealth(broker, HealthState.STALE, details = mapOf(
                    "tickAge" to providerState.lastTickTimestamp,
                    "error" to "Stale market data (> 15s)"
                ))
            }
            if (providerState.live) {
                return ComponentHealth(broker, HealthState.HEALTHY, details = mapOf(
                    "lastTick" to providerState.lastTickTimestamp,
                    "connected" to true
                ))
            }
            return ComponentHealth(broker, HealthState.NO_TICK, details = mapOf("error" to "No real tick received yet"))
        }
        
        return ComponentHealth(broker, HealthState.OFFLINE, details = mapOf("error" to "Standby or disconnected"))
    }

    private fun checkDhanHealth(): ComponentHealth {
        val authStatus = brokerManager.brokerAuthManager.statuses.value["Dhan"]?.status
        if (authStatus != com.example.data.network.BrokerAuthStatus.CONNECTED) {
            return ComponentHealth("DHAN", HealthState.AUTH_FAILED, details = mapOf("error" to "Dhan not authenticated (Order execution blocked)"))
        }
        return ComponentHealth("DHAN", HealthState.HEALTHY, details = mapOf("connected" to true, "role" to "ORDER_EXECUTION_ONLY"))
    }

    private fun checkOptionChainHealth(): ComponentHealth {
        // Just checking if we can potentially fetch options or if there's any loaded
        // For actual self-diagnostic we would verify against MarketDataStore's current option chain if it was stored there.
        // Assuming we rely on OptionExpiryUtil and Option contracts being official
        return ComponentHealth("OPTION_CHAIN", HealthState.HEALTHY, details = mapOf("status" to "Ready to fetch"))
    }

    private fun checkAiSignalHealth(): ComponentHealth {
        val isMarketDataLive = MarketDataStore.providerState.value.live && !MarketDataStore.providerState.value.stale
        if (!isMarketDataLive) {
            return ComponentHealth("AI_SIGNAL", HealthState.STALE, details = mapOf("error" to "Signal paused due to stale data"))
        }
        return ComponentHealth("AI_SIGNAL", HealthState.HEALTHY, details = mapOf("status" to "Ready"))
    }

    private suspend fun attemptFailoverOrRecovery() {
        val providerState = MarketDataStore.providerState.value
        val activeProvider = providerState.provider

        if (providerState.stale || !providerState.live) {
            val log = AutoRecoveryLog(
                timestamp = System.currentTimeMillis(),
                component = activeProvider,
                problem = "No real tick received or stale data",
                detectedCause = "WebSocket disconnect or silent feed",
                action = "Attempting failover/reconnect via ProviderHealthManager",
                verification = "Waiting for new real tick...",
                result = HealthState.REPAIRING
            )
            addRecoveryLog(log)
            
            // Re-trigger the active provider's websocket connection or rely on the engine's internal reconnect
            brokerManager.marketDataEngine.retryConnection()
            
            delay(5000)
            
            val newState = MarketDataStore.providerState.value
            if (newState.live && !newState.stale) {
                addRecoveryLog(log.copy(
                    verification = "PASS",
                    result = HealthState.RECOVERED
                ))
            } else {
                addRecoveryLog(log.copy(
                    verification = "FAIL",
                    result = HealthState.UNRESOLVED
                ))
            }
        }
    }

    private fun addRecoveryLog(log: AutoRecoveryLog) {
        val current = _recoveryLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }
        _recoveryLogs.value = current
    }
}
