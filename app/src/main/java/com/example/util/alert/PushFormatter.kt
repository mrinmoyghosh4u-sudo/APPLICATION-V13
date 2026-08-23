package com.example.util.alert

data class PushNotificationPayload(
    val title: String,
    val message: String,
    val type: String = "INFO" // INFO, SUCCESS, WARNING, ERROR
)

object PushFormatter {

    fun formatAiBuyCe(contract: String, entry: String, sl: String, t1: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "🟢 BUY CE — $contract",
            message = "Entry ₹$entry | SL ₹$sl | Target 1 ₹$t1",
            type = "SUCCESS"
        )
    }

    fun formatAiBuyPe(contract: String, entry: String, sl: String, t1: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "🔴 BUY PE — $contract",
            message = "Entry ₹$entry | SL ₹$sl | Target 1 ₹$t1",
            type = "WARNING"
        )
    }

    fun formatEntryPosition(contract: String, entry: String, quantity: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "🎯 POSITION OPENED — $contract",
            message = "Entered at ₹$entry | Qty: $quantity",
            type = "SUCCESS"
        )
    }

    fun formatTargetHit(targetNumber: Int, contract: String, price: String, profit: String, returnPercent: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "🎯 TARGET $targetNumber HIT — $contract",
            message = "Exit/LTP ₹$price | Profit ₹$profit (+$returnPercent%)",
            type = "SUCCESS"
        )
    }

    fun formatStopLossHit(contract: String, exit: String, loss: String, returnPercent: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "🛑 STOP LOSS HIT — $contract",
            message = "Exit ₹$exit | Loss ₹$loss (-$returnPercent%)",
            type = "ERROR"
        )
    }

    fun formatTrailingSlHit(contract: String, current: String, newSL: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "📈 TRAILING SL UPDATED — $contract",
            message = "Current ₹$current | New SL locked at ₹$newSL",
            type = "INFO"
        )
    }

    fun formatOrderExecuted(contract: String, side: String, quantity: String, price: String, orderId: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "✅ ORDER EXECUTED — $contract",
            message = "$side $quantity @ ₹$price (ID: $orderId)",
            type = "SUCCESS"
        )
    }

    fun formatOrderRejected(contract: String, reason: String, orderId: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "❌ ORDER REJECTED — $contract",
            message = "Reason: $reason (ID: $orderId)",
            type = "ERROR"
        )
    }

    fun formatBrokerConnected(broker: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "🔗 BROKER CONNECTED",
            message = "Connected to $broker successfully",
            type = "SUCCESS"
        )
    }

    fun formatBrokerDisconnected(broker: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "🔴 BROKER DISCONNECTED",
            message = "$broker disconnected. Automatic reconnect initiated.",
            type = "WARNING"
        )
    }

    fun formatAlgoStarted(strategy: String, symbol: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "▶️ ALGO STARTED",
            message = "Strategy $strategy running on $symbol",
            type = "INFO"
        )
    }

    fun formatAlgoStopped(strategy: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "⏹️ ALGO STOPPED",
            message = "Strategy $strategy has been stopped",
            type = "WARNING"
        )
    }

    fun formatRiskLimitReached(pnl: String, maxLoss: String): PushNotificationPayload {
        return PushNotificationPayload(
            title = "⚠️ RISK LIMIT REACHED",
            message = "Daily P&L ₹$pnl reached Max Limit ₹$maxLoss. Trades Disabled.",
            type = "ERROR"
        )
    }
}
