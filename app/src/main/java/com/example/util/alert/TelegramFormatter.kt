package com.example.util.alert

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramFormatter {

    const val APP_HEADER = "💥 KING KHAN AI TRADE 💥\n   👑 Trade Like a King 👑\n\n━━━━━━━━━━━━━━━━━━"
    const val APP_FOOTER = "━━━━━━━━━━━━━━━━━━\nThanks,\nKK 👑"

    fun getCurrentTime(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    fun getCurrentDateTime(): String {
        return SimpleDateFormat("hh:mm:ss a dd-MMM-yyyy", Locale.getDefault()).format(Date())
    }

    private fun wrap(body: String): String {
        return "$APP_HEADER\n\n$body\n\n$APP_FOOTER"
    }

    fun formatTestMessage(): String {
        val body = """
            ✅ TELEGRAM TEST MESSAGE

            System: King Khan AI Trade Alert Engine
            Status: Active & Operational

            ⏰ Time: ${getCurrentTime()}
        """.trimIndent()
        return wrap(body)
    }

    fun formatBrokerConnected(broker: String, time: String = getCurrentTime()): String {
        val body = """
            🔗 BROKER CONNECTED

            🏦 Broker: $broker
            🟢 Status: Connected

            ⏰ Time: $time
        """.trimIndent()
        return wrap(body)
    }

    fun formatBrokerDisconnected(broker: String, time: String = getCurrentTime()): String {
        val body = """
            🔴 BROKER DISCONNECTED

            🏦 Broker: $broker
            ❌ Status: Disconnected

            ⚠️ Automatic reconnect initiated.

            ⏰ Time: $time
        """.trimIndent()
        return wrap(body)
    }

    fun formatAiBuyCe(
        symbol: String,
        contract: String,
        entry: String,
        sl: String,
        t1: String,
        t2: String,
        t3: String,
        t4: String,
        confidence: Int,
        strength: String = if (confidence >= 80) "STRONG" else "MODERATE",
        oiChange: String = "+18.4%",
        volume: String = "HIGH",
        time: String = getCurrentTime()
    ): String {
        val body = """
            🟢 AI BUY CE SIGNAL

            📊 Symbol: $symbol
            🎯 Contract: $contract

            💰 Entry: ₹$entry
            🛑 Stop Loss: ₹$sl

            🎯 Target 1: ₹$t1
            🎯 Target 2: ₹$t2
            🎯 Target 3: ₹$t3
            🎯 Target 4: ₹$t4

            📈 AI Confidence: $confidence%
            🔥 Signal Strength: $strength
            📊 OI: $oiChange
            📊 Volume: $volume
            ⚡ Momentum: BULLISH

            ⏰ Time: $time
        """.trimIndent()
        return wrap(body)
    }

    fun formatAiBuyPe(
        symbol: String,
        contract: String,
        entry: String,
        sl: String,
        t1: String,
        t2: String,
        t3: String,
        t4: String,
        confidence: Int,
        strength: String = if (confidence >= 80) "STRONG" else "MODERATE",
        oiChange: String = "+18.4%",
        volume: String = "HIGH",
        time: String = getCurrentTime()
    ): String {
        val body = """
            🔴 AI BUY PE SIGNAL

            📊 Symbol: $symbol
            🎯 Contract: $contract

            💰 Entry: ₹$entry
            🛑 Stop Loss: ₹$sl

            🎯 Target 1: ₹$t1
            🎯 Target 2: ₹$t2
            🎯 Target 3: ₹$t3
            🎯 Target 4: ₹$t4

            📈 AI Confidence: $confidence%
            🔥 Signal Strength: $strength
            📊 OI: $oiChange
            📊 Volume: $volume
            ⚡ Momentum: BEARISH

            ⏰ Time: $time
        """.trimIndent()
        return wrap(body)
    }

    fun formatEntryPositionOpened(
        symbol: String,
        contract: String,
        entry: String,
        quantity: String,
        sl: String,
        t1: String,
        t2: String,
        t3: String,
        t4: String,
        time: String = getCurrentTime()
    ): String {
        val body = """
            🎯 ENTRY — POSITION OPENED

            📊 Symbol: $symbol
            🎯 Contract: $contract

            💰 Entry: ₹$entry
            📦 Quantity: $quantity

            🛑 Stop Loss: ₹$sl

            🎯 Target 1: ₹$t1
            🎯 Target 2: ₹$t2
            🎯 Target 3: ₹$t3
            🎯 Target 4: ₹$t4

            🕐 Time: $time
        """.trimIndent()
        return wrap(body)
    }

    fun formatTargetHit(
        targetNumber: Int,
        symbol: String,
        contract: String,
        entry: String,
        price: String,
        profit: String,
        returnPercent: String,
        t1Status: String = if (targetNumber >= 1) "HIT ✅" else "PENDING ⏳",
        t2Status: String = if (targetNumber >= 2) "HIT ✅" else "PENDING ⏳",
        t3Status: String = if (targetNumber >= 3) "HIT ✅" else "PENDING ⏳",
        t4Status: String = if (targetNumber >= 4) "HIT ✅" else "PENDING ⏳"
    ): String {
        val body = """
            🎯 TARGET $targetNumber HIT

            📊 $symbol
            🎯 $contract

            💰 Entry: ₹$entry
            📈 Exit/LTP: ₹$price

            💵 Profit: ₹$profit
            📈 Return: $returnPercent%

            🎯 T1: $t1Status
            🎯 T2: $t2Status
            🎯 T3: $t3Status
            🎯 T4: $t4Status
        """.trimIndent()
        return wrap(body)
    }

    fun formatStopLossHit(
        symbol: String,
        contract: String,
        entry: String,
        exit: String,
        loss: String,
        returnPercent: String,
        reason: String = "Stop Loss Level Hit"
    ): String {
        val body = """
            🛑 STOP LOSS HIT

            📊 $symbol
            🎯 $contract

            💰 Entry: ₹$entry
            ❌ Exit: ₹$exit

            📉 Loss: ₹$loss
            📉 Return: $returnPercent%

            ⚠️ Reason: $reason
        """.trimIndent()
        return wrap(body)
    }

    fun formatTrailingSlUpdated(
        symbol: String,
        contract: String,
        entry: String,
        current: String,
        oldSL: String,
        newSL: String,
        nextTarget: String,
        pnl: String
    ): String {
        val body = """
            📈 TRAILING STOP LOSS UPDATED

            📊 $symbol
            🎯 $contract

            💰 Entry: ₹$entry
            📍 Current: ₹$current

            🔒 Previous SL: ₹$oldSL
            🔒 New SL: ₹$newSL

            🎯 Next Target: ₹$nextTarget

            📈 Current P&L: ₹$pnl
        """.trimIndent()
        return wrap(body)
    }

    fun formatOrderExecuted(
        symbol: String,
        contract: String,
        side: String,
        price: String,
        quantity: String,
        orderId: String,
        time: String = getCurrentTime()
    ): String {
        val body = """
            ✅ ORDER EXECUTED

            📊 $symbol
            🎯 $contract

            📌 Side: $side
            💰 Price: ₹$price
            📦 Quantity: $quantity

            🆔 Order ID: $orderId

            ⏰ Time: $time
        """.trimIndent()
        return wrap(body)
    }

    fun formatOrderRejected(
        symbol: String,
        contract: String,
        side: String,
        quantity: String,
        rejectionReason: String,
        orderId: String
    ): String {
        val body = """
            ❌ ORDER REJECTED

            📊 $symbol
            🎯 $contract

            📌 Side: $side
            📦 Quantity: $quantity

            ⚠️ Reason:
            $rejectionReason

            🆔 Order ID: $orderId
        """.trimIndent()
        return wrap(body)
    }

    fun formatAlgoStarted(
        strategy: String,
        symbol: String,
        time: String = getCurrentTime()
    ): String {
        val body = """
            ▶️ ALGO STARTED

            🤖 Strategy: $strategy
            📊 Symbol: $symbol

            🟢 Status: ACTIVE

            ⏰ Time: $time
        """.trimIndent()
        return wrap(body)
    }

    fun formatAlgoStopped(
        strategy: String,
        time: String = getCurrentTime()
    ): String {
        val body = """
            ⏹️ ALGO STOPPED

            🤖 Strategy: $strategy

            🔴 Status: STOPPED

            ⏰ Time: $time
        """.trimIndent()
        return wrap(body)
    }

    fun formatRiskLimitReached(
        pnl: String,
        maxLoss: String,
        time: String = getCurrentTime()
    ): String {
        val body = """
            ⚠️ RISK LIMIT REACHED

            📊 Daily P&L: ₹$pnl
            🛑 Maximum Loss: ₹$maxLoss

            🚫 New trades have been disabled.

            ⏰ Time: $time
        """.trimIndent()
        return wrap(body)
    }
}
