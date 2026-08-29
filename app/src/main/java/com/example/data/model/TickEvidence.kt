package com.example.data.model

data class TickEvidence(
    val broker: String,
    val exchange: String,
    val instrumentToken: String,
    val symbol: String,
    val timestamp: Long,
    val ltp: Double,
    val volume: Long,
    val receivedAt: Long,
    val source: String
) {
    // isRealTick is derived directly from the presence of valid fields indicating a genuine parsed broker packet
    val isRealTick: Boolean
        get() = broker.isNotBlank() &&
                exchange.isNotBlank() &&
                instrumentToken.isNotBlank() &&
                symbol.isNotBlank() &&
                timestamp > 0L &&
                ltp > 0.0 &&
                receivedAt > 0L &&
                source == "WEBSOCKET" // Must be from WebSocket stream, not REST or synthetic

    val tickAgeMs: Long
        get() = System.currentTimeMillis() - receivedAt
}
