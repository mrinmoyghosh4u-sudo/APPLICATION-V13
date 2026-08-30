package com.example.data.network

/**
 * KING KHAN AI TRADE - Strict Broker Architecture Definition
 *
 * Strict Hierarchy & Roles:
 * 1. UPSTOX    -> PRIMARY REAL MARKET DATA FEED (API V2/V3)
 * 2. FYERS     -> FALLBACK #1 MARKET DATA FEED (API V3)
 * 3. ANGEL_ONE -> FALLBACK #2 MARKET DATA FEED (SmartAPI)
  * 5. DHAN      -> PRIMARY ORDER EXECUTION ONLY (Zero market data feeds)
 */
enum class BrokerType(
    val id: String,
    val displayName: String,
    val roleDescription: String,
    val isMarketDataProvider: Boolean,
    val isOrderBroker: Boolean
) {
    DHAN(
        id = "Dhan",
        displayName = "Dhan",
        roleDescription = "Order Execution Only",
        isMarketDataProvider = false,
        isOrderBroker = true
    ),
    UPSTOX(
        id = "Upstox",
        displayName = "Upstox",
        roleDescription = "Primary Market Data",
        isMarketDataProvider = true,
        isOrderBroker = false
    ),
    FYERS(
        id = "Fyers",
        displayName = "Fyers",
        roleDescription = "Fallback #1 Market Data",
        isMarketDataProvider = true,
        isOrderBroker = false
    ),
    ANGEL_ONE(
        id = "Angel One",
        displayName = "Angel One",
        roleDescription = "Fallback #2 Market Data",
        isMarketDataProvider = true,
        isOrderBroker = false
    ),
    ;

    companion object {
        fun fromString(name: String?): BrokerType? {
            if (name.isNullOrBlank()) return null
            val trimmed = name.trim()
            return when {
                trimmed.equals("Dhan", ignoreCase = true) -> DHAN
                trimmed.equals("Upstox", ignoreCase = true) -> UPSTOX
                trimmed.equals("Fyers", ignoreCase = true) || trimmed.equals("FYERS", ignoreCase = true) -> FYERS
                trimmed.equals("Angel One", ignoreCase = true) || trimmed.equals("Angel", ignoreCase = true) || trimmed.equals("AngelOne", ignoreCase = true) -> ANGEL_ONE
                                else -> null
            }
        }
    }
}
