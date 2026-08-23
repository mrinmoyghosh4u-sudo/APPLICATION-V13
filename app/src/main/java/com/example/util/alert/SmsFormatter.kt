package com.example.util.alert

object SmsFormatter {

    const val HEADER = "👑 KK AI TRADE"

    fun formatAiBuyCe(
        contract: String,
        entry: String,
        sl: String,
        t1: String,
        t2: String,
        t3: String,
        t4: String,
        confidence: Int
    ): String {
        return """
            $HEADER
            🟢 BUY CE
            $contract
            Entry ₹$entry | SL ₹$sl
            T1 ₹$t1 | T2 ₹$t2
            T3 ₹$t3 | T4 ₹$t4
            Conf $confidence%
        """.trimIndent()
    }

    fun formatAiBuyPe(
        contract: String,
        entry: String,
        sl: String,
        t1: String,
        t2: String,
        t3: String,
        t4: String,
        confidence: Int
    ): String {
        return """
            $HEADER
            🔴 BUY PE
            $contract
            Entry ₹$entry | SL ₹$sl
            T1 ₹$t1 | T2 ₹$t2
            T3 ₹$t3 | T4 ₹$t4
            Conf $confidence%
        """.trimIndent()
    }

    fun formatEntryPosition(
        contract: String,
        entry: String,
        quantity: String,
        sl: String,
        t1: String
    ): String {
        return """
            $HEADER
            🎯 ENTRY OPENED
            $contract
            Entry ₹$entry | Qty $quantity
            SL ₹$sl | T1 ₹$t1
        """.trimIndent()
    }

    fun formatTargetHit(
        targetNumber: Int,
        contract: String,
        entry: String,
        price: String,
        profit: String,
        returnPercent: String
    ): String {
        return """
            $HEADER
            🎯 T$targetNumber HIT
            $contract
            Entry ₹$entry
            LTP ₹$price
            Profit ₹$profit (+$returnPercent%)
        """.trimIndent()
    }

    fun formatStopLossHit(
        contract: String,
        entry: String,
        exit: String,
        loss: String,
        returnPercent: String
    ): String {
        return """
            $HEADER
            🛑 SL HIT
            $contract
            Entry ₹$entry
            Exit ₹$exit
            Loss ₹$loss (-$returnPercent%)
        """.trimIndent()
    }

    fun formatTrailingSlHit(
        contract: String,
        entry: String,
        current: String,
        newSL: String,
        pnl: String
    ): String {
        return """
            $HEADER
            📈 TRAILING SL
            $contract
            Current ₹$current | New SL ₹$newSL
            P&L ₹$pnl
        """.trimIndent()
    }

    fun formatOrderExecuted(
        contract: String,
        side: String,
        quantity: String,
        price: String,
        orderId: String
    ): String {
        return """
            $HEADER
            ✅ ORDER EXECUTED
            $contract
            $side $quantity @ ₹$price
            ID: $orderId
        """.trimIndent()
    }

    fun formatOrderRejected(
        contract: String,
        side: String,
        quantity: String,
        reason: String,
        orderId: String
    ): String {
        return """
            $HEADER
            ❌ ORDER REJECTED
            $contract | $side $quantity
            Reason: $reason
            ID: $orderId
        """.trimIndent()
    }

    fun formatBrokerConnected(broker: String): String {
        return """
            $HEADER
            🔗 BROKER CONNECTED
            $broker - Connected
        """.trimIndent()
    }

    fun formatBrokerDisconnected(broker: String): String {
        return """
            $HEADER
            🔴 BROKER DISCONNECTED
            $broker - Disconnected
        """.trimIndent()
    }

    fun formatAlgoStarted(strategy: String, symbol: String): String {
        return """
            $HEADER
            ▶️ ALGO ACTIVE
            $strategy ($symbol)
        """.trimIndent()
    }

    fun formatAlgoStopped(strategy: String): String {
        return """
            $HEADER
            ⏹️ ALGO STOPPED
            $strategy
        """.trimIndent()
    }

    fun formatRiskLimitReached(pnl: String, maxLoss: String): String {
        return """
            $HEADER
            ⚠️ RISK LIMIT HIT
            P&L: ₹$pnl | Max: ₹$maxLoss
            Trades Disabled
        """.trimIndent()
    }
}
