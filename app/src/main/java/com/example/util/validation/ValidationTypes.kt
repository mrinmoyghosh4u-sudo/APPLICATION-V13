package com.example.util.validation

enum class ValidationStatus {
    PASS, FAIL
}

data class ValidationResult(
    val status: ValidationStatus,
    val code: String,
    val message: String
) {
    val isSuccess: Boolean get() = status == ValidationStatus.PASS
}

object ValidationEngine {
    fun validateRealTick(
        broker: String?,
        timestamp: Long,
        ltp: Double,
        tickAgeMs: Long
    ): ValidationResult {
        if (broker.isNullOrBlank()) return ValidationResult(ValidationStatus.FAIL, "MISSING_BROKER", "Broker identity missing")
        if (ltp <= 0.0) return ValidationResult(ValidationStatus.FAIL, "INVALID_LTP", "LTP must be greater than zero")
        if (timestamp <= 0L) return ValidationResult(ValidationStatus.FAIL, "INVALID_TIMESTAMP", "Invalid exchange timestamp")
        if (tickAgeMs > 15000L) return ValidationResult(ValidationStatus.FAIL, "STALE_TICK", "Tick is older than 15 seconds")
        return ValidationResult(ValidationStatus.PASS, "OK", "Real tick validated")
    }

    fun validateBrokerAuthentication(isAuthenticated: Boolean): ValidationResult {
        if (!isAuthenticated) return ValidationResult(ValidationStatus.FAIL, "AUTH_FAILED", "Broker authentication missing")
        return ValidationResult(ValidationStatus.PASS, "OK", "Broker authenticated")
    }

    fun validateWebSocket(isConnected: Boolean): ValidationResult {
        if (!isConnected) return ValidationResult(ValidationStatus.FAIL, "WS_DISCONNECTED", "WebSocket is disconnected")
        return ValidationResult(ValidationStatus.PASS, "OK", "WebSocket connected")
    }

    fun validateSubscription(isSubscribed: Boolean): ValidationResult {
        if (!isSubscribed) return ValidationResult(ValidationStatus.FAIL, "NOT_SUBSCRIBED", "Instrument not subscribed")
        return ValidationResult(ValidationStatus.PASS, "OK", "Subscription active")
    }

    fun validateInstrument(symbol: String, token: String?): ValidationResult {
        if (symbol.isBlank()) return ValidationResult(ValidationStatus.FAIL, "MISSING_SYMBOL", "Symbol is required")
        if (token.isNullOrBlank()) return ValidationResult(ValidationStatus.FAIL, "MISSING_TOKEN", "Instrument token missing")
        return ValidationResult(ValidationStatus.PASS, "OK", "Instrument valid")
    }

    fun validateOptionContract(contractFound: Boolean): ValidationResult {
        if (!contractFound) return ValidationResult(ValidationStatus.FAIL, "CONTRACT_NOT_FOUND", "Option contract unavailable from broker")
        return ValidationResult(ValidationStatus.PASS, "OK", "Option contract resolved")
    }

    fun validateExpiry(expiryStr: String): ValidationResult {
        if (expiryStr.isBlank() || expiryStr == "UNAVAILABLE") return ValidationResult(ValidationStatus.FAIL, "EXPIRY_UNAVAILABLE", "Official expiry unavailable")
        return ValidationResult(ValidationStatus.PASS, "OK", "Expiry valid")
    }

    fun validateOptionChain(chainSize: Int): ValidationResult {
        if (chainSize == 0) return ValidationResult(ValidationStatus.FAIL, "CHAIN_EMPTY", "Option chain is empty or unavailable")
        return ValidationResult(ValidationStatus.PASS, "OK", "Option chain loaded")
    }

    fun validateSignal(
        hasRealUnderlyingTick: Boolean,
        hasRealOptionTick: Boolean,
        hasOfficialContract: Boolean,
        hasOfficialExpiry: Boolean,
        isFreshData: Boolean
    ): ValidationResult {
        if (!hasRealUnderlyingTick) return ValidationResult(ValidationStatus.FAIL, "NO_UNDERLYING_TICK", "Missing real underlying tick")
        if (!hasRealOptionTick) return ValidationResult(ValidationStatus.FAIL, "NO_OPTION_TICK", "Missing real option tick")
        if (!hasOfficialContract) return ValidationResult(ValidationStatus.FAIL, "NO_CONTRACT", "Missing official option contract")
        if (!hasOfficialExpiry) return ValidationResult(ValidationStatus.FAIL, "NO_EXPIRY", "Missing official expiry")
        if (!isFreshData) return ValidationResult(ValidationStatus.FAIL, "STALE_DATA", "Data is stale (older than 15s)")
        return ValidationResult(ValidationStatus.PASS, "OK", "Signal validated")
    }

    fun validateOrder(
        isDhanAuthenticated: Boolean,
        hasValidSecurityId: Boolean,
        isMarketDataLive: Boolean,
        isDataFresh: Boolean
    ): ValidationResult {
        if (!isDhanAuthenticated) return ValidationResult(ValidationStatus.FAIL, "DHAN_UNAUTHENTICATED", "Dhan authentication required for order")
        if (!hasValidSecurityId) return ValidationResult(ValidationStatus.FAIL, "INVALID_SECURITY_ID", "Missing official Dhan Security ID")
        if (!isMarketDataLive) return ValidationResult(ValidationStatus.FAIL, "MARKET_OFFLINE", "Market data is not live")
        if (!isDataFresh) return ValidationResult(ValidationStatus.FAIL, "STALE_DATA", "Market data is stale")
        return ValidationResult(ValidationStatus.PASS, "OK", "Order validated")
    }
}
