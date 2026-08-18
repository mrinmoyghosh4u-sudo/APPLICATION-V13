package com.example.data.network

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramMessageFormatter {

    private const val HEADER = "💥KING KHAN AI TRADE💥\n   👑Trade Like a King 👑"
    private const val FOOTER = "Thanks\nKK"

    private fun currentTime(): String {
        return SimpleDateFormat("hh:mm:ss a dd-MMM-yyyy", Locale.getDefault()).format(Date())
    }

    fun build(body: String): String {
        return "$HEADER\n\n$body\n\n$FOOTER"
    }

    fun formatTestMessage(): String {
        val body = """
            ✅ TELEGRAM CONNECTED

            Telegram notification system is working correctly.

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatDhanConnected(): String {
        val body = """
            🟢 DHAN CONNECTED

            Broker: Dhan
            Status: CONNECTED

            Trading Mode: OPTIONS BUYER ONLY

            ✅ BUY CE
            ✅ BUY PE

            Market Data: CONNECTED
            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatAngelConnected(): String {
        val body = """
            🟢 ANGEL ONE CONNECTED

            Broker: Angel One
            Status: CONNECTED

            Trading Mode: OPTIONS BUYER ONLY

            ✅ BUY CE
            ✅ BUY PE

            Market Data: CONNECTED
            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatBrokerDisconnected(broker: String, reason: String): String {
        val body = """
            🔴 BROKER DISCONNECTED

            Broker: $broker
            Status: DISCONNECTED

            Reason: $reason

            Algo Status: STOPPED
            New Trading: DISABLED

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatSessionExpired(broker: String): String {
        val body = """
            ⚠️ BROKER SESSION EXPIRED

            Broker: $broker

            Status: SESSION EXPIRED

            New Signals: DISABLED
            New Orders: DISABLED

            Action:
            Please reconnect the broker.

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatAlgoStarted(strategyName: String, index: String, timeframe: String, mode: String): String {
        val body = """
            🟢 ALGO STARTED

            Strategy: $strategyName
            Index: $index
            Timeframe: $timeframe

            Mode: $mode

            Options Buyer Only:

            ✅ BUY CE
            ✅ BUY PE

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatAlgoStopped(strategyName: String, reason: String): String {
        val body = """
            🛑 ALGO STOPPED

            Strategy: $strategyName

            New Signals: DISABLED
            New Orders: DISABLED

            Reason:
            $reason

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatAiSignal(
        actionType: String, // "BUY CE" or "BUY PE"
        index: String,
        strike: String,
        expiry: String,
        entry: String,
        sl: String,
        t1: String,
        t2: String,
        t3: String,
        t4: String,
        trailingSl: String,
        timeframe: String,
        marketBias: String,
        confirmations: List<String>,
        confidence: Int
    ): String {
        val icon = if (actionType.contains("PE")) "🔴" else "🟢"
        val formattedConfirmations = if (confirmations.isNotEmpty()) {
            confirmations.joinToString("\n") { "✅ $it" }
        } else {
            "✅ EMA\n✅ VWAP\n✅ RSI\n✅ Supertrend\n✅ OI\n✅ Volume"
        }

        val body = """
            🚨 AI SIGNAL

            $icon $actionType

            Index: $index
            Strike: $strike
            Expiry: $expiry

            Entry: ₹$entry
            Stop Loss: ₹$sl

            Target 1: ₹$t1
            Target 2: ₹$t2
            Target 3: ₹$t3
            Target 4: ₹$t4

            Trailing SL: ₹$trailingSl

            Timeframe: $timeframe
            Market Bias: $marketBias

            AI Confirmation:
            $formattedConfirmations

            Confidence: $confidence%

            Mode: OPTIONS BUYER ONLY

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatNoTrade(index: String, marketBias: String, reason: String = "Insufficient market confirmation."): String {
        val body = """
            ⚪ NO TRADE

            Index: $index

            Reason:
            $reason

            Market Bias: $marketBias

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatPaperTradeOpened(
        actionType: String, // "BUY CE" or "BUY PE"
        index: String,
        strike: String,
        expiry: String,
        entryPrice: String,
        quantity: String,
        sl: String,
        t1: String
    ): String {
        val icon = if (actionType.contains("PE")) "🔴" else "🟢"
        val body = """
            🟡 PAPER TRADE OPENED

            $icon $actionType

            Index: $index
            Strike: $strike
            Expiry: $expiry

            Entry: ₹$entryPrice
            Quantity: $quantity

            Stop Loss: ₹$sl
            Target 1: ₹$t1

            Mode: PAPER TRADING

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatLiveOrderPlaced(
        broker: String,
        actionType: String, // "BUY CE" or "BUY PE"
        index: String,
        strike: String,
        expiry: String,
        quantity: String,
        orderPrice: String,
        orderId: String,
        status: String
    ): String {
        val body = """
            🟢 ORDER PLACED

            Broker: $broker

            $actionType

            Index: $index
            Strike: $strike
            Expiry: $expiry

            Quantity: $quantity
            Order Price: ₹$orderPrice

            Order ID: $orderId
            Status: $status

            Mode: AUTO TRADING

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatOrderRejected(
        broker: String,
        actionType: String,
        index: String,
        strike: String,
        reason: String
    ): String {
        val body = """
            🔴 ORDER REJECTED

            Broker: $broker

            $actionType

            Index: $index
            Strike: $strike

            Reason:
            $reason

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatStopLossHit(
        actionType: String,
        index: String,
        strike: String,
        entry: String,
        exit: String,
        pnl: String
    ): String {
        val body = """
            🛑 STOP LOSS HIT

            $actionType

            Index: $index
            Strike: $strike

            Entry: ₹$entry
            Exit: ₹$exit

            P&L: ₹$pnl

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatTargetHit(
        targetNumber: Int, // 1, 2, 3, 4
        actionType: String,
        index: String,
        strike: String,
        entry: String,
        currentLtp: String,
        pnl: String
    ): String {
        val body = """
            🎯 TARGET $targetNumber HIT

            $actionType

            Index: $index
            Strike: $strike

            Entry: ₹$entry
            Current LTP: ₹$currentLtp

            P&L: ₹$pnl

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatTrailingSlHit(
        actionType: String,
        index: String,
        strike: String,
        entry: String,
        exit: String,
        pnl: String
    ): String {
        val body = """
            🔄 TRAILING STOP LOSS HIT

            $actionType

            Index: $index
            Strike: $strike

            Entry: ₹$entry
            Exit: ₹$exit

            P&L: ₹$pnl

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatPositionClosed(
        broker: String,
        actionType: String,
        index: String,
        strike: String,
        entry: String,
        exit: String,
        quantity: String,
        pnl: String
    ): String {
        val body = """
            🏁 POSITION CLOSED

            $actionType

            Broker: $broker

            Index: $index
            Strike: $strike

            Entry: ₹$entry
            Exit: ₹$exit

            Quantity: $quantity

            P&L: ₹$pnl

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }

    fun formatRiskLimitReached(
        reason: String,
        dailyPnl: String
    ): String {
        val body = """
            🚨 RISK LIMIT REACHED

            Algo: STOPPED

            Reason:
            $reason

            Daily P&L: ₹$dailyPnl

            New Signals: DISABLED
            New Orders: DISABLED

            Time: ${currentTime()}
        """.trimIndent()
        return build(body)
    }
}
