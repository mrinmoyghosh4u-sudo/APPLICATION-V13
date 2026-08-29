package com.example.util.diagnostic

enum class HealthState {
    HEALTHY,
    DEGRADED,
    STALE,
    OFFLINE,
    AUTH_FAILED,
    TOKEN_EXPIRED,
    WEBSOCKET_FAILED,
    SUBSCRIPTION_FAILED,
    NO_TICK,
    INVALID_INSTRUMENT,
    OPTION_CHAIN_UNAVAILABLE,
    ORDER_BLOCKED,
    REPAIRING,
    RECOVERED,
    UNRESOLVED
}

data class DiagnosticReport(
    val feature: String,
    val module: String,
    val broker: String,
    val stage: String,
    val error: String,
    val lastSuccessfulStage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dataSource: String,
    val possibleCause: String,
    val repairAction: String,
    val status: HealthState
)

data class AutoRecoveryLog(
    val timestamp: Long,
    val component: String,
    val problem: String,
    val detectedCause: String,
    val action: String,
    val verification: String,
    val result: HealthState
)

data class ComponentHealth(
    val name: String,
    val status: HealthState,
    val lastUpdate: Long = System.currentTimeMillis(),
    val details: Map<String, Any> = emptyMap()
)
