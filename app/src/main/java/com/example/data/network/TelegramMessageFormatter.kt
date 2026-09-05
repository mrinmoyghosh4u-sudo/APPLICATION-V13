package com.example.data.network

import com.example.util.alert.TelegramFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramMessageFormatter {

    fun currentTime(): String = TelegramFormatter.getCurrentTime()

    fun build(body: String): String {
        return "${TelegramFormatter.APP_HEADER}\n\n$body\n\n${TelegramFormatter.APP_FOOTER}"
    }

    fun formatTestMessage(): String = TelegramFormatter.formatTestMessage()

    fun formatDhanConnected(): String = TelegramFormatter.formatBrokerConnected("Dhan")

    fun formatAngelConnected(): String = TelegramFormatter.formatBrokerConnected("Angel One")

    fun formatBrokerConnected(broker: String): String = TelegramFormatter.formatBrokerConnected(broker)

    fun formatBrokerDisconnected(broker: String, reason: String = "Session Disconnected"): String = 
        TelegramFormatter.formatBrokerDisconnected(broker)

    fun formatSessionExpired(broker: String): String = 
        TelegramFormatter.formatBrokerDisconnected(broker)

    fun formatAlgoStarted(strategyName: String, index: String, timeframe: String = "", mode: String = ""): String =
        TelegramFormatter.formatAlgoStarted(strategyName, index)

    fun formatAlgoStopped(strategyName: String, reason: String = ""): String =
        TelegramFormatter.formatAlgoStopped(strategyName)

    fun formatAiSignal(
        actionType: String,
        index: String,
        strike: String,
        expiry: String = "WEEKLY",
        entry: String,
        sl: String,
        t1: String,
        t2: String,
        t3: String,
        t4: String,
        trailingSl: String = "",
        timeframe: String = "5 MIN",
        marketBias: String = "BULLISH",
        confirmations: List<String> = emptyList(),
        confidence: Int = 85
    ): String {
        return if (actionType.contains("PE")) {
            TelegramFormatter.formatAiBuyPe(
                symbol = index,
                contract = strike,
                entry = entry,
                sl = sl,
                t1 = t1,
                t2 = t2,
                t3 = t3,
                t4 = t4,
                confidence = confidence
            )
        } else {
            TelegramFormatter.formatAiBuyCe(
                symbol = index,
                contract = strike,
                entry = entry,
                sl = sl,
                t1 = t1,
                t2 = t2,
                t3 = t3,
                t4 = t4,
                confidence = confidence
            )
        }
    }

    fun formatPaperTradeOpened(
        actionType: String,
        index: String,
        strike: String,
        expiry: String = "WEEKLY",
        entryPrice: String,
        quantity: String,
        sl: String,
        t1: String,
        t2: String = "",
        t3: String = "",
        t4: String = ""
    ): String {
        return TelegramFormatter.formatEntryPositionOpened(
            symbol = index,
            contract = strike,
            entry = entryPrice,
            quantity = quantity,
            sl = sl,
            t1 = t1,
            t2 = t2.ifBlank {
                val ep = entryPrice.toDoubleOrNull()
                if (ep != null && ep > 0.0) String.format(Locale.getDefault(), "%.2f", ep * 1.15) else "N/A"
            },
            t3 = t3.ifBlank {
                val ep = entryPrice.toDoubleOrNull()
                if (ep != null && ep > 0.0) String.format(Locale.getDefault(), "%.2f", ep * 1.25) else "N/A"
            },
            t4 = t4.ifBlank {
                val ep = entryPrice.toDoubleOrNull()
                if (ep != null && ep > 0.0) String.format(Locale.getDefault(), "%.2f", ep * 1.35) else "N/A"
            }
        )
    }

    fun formatLiveOrderPlaced(
        broker: String,
        actionType: String,
        index: String,
        strike: String,
        expiry: String,
        quantity: String,
        orderPrice: String,
        orderId: String,
        status: String
    ): String {
        return TelegramFormatter.formatOrderExecuted(
            symbol = index,
            contract = strike,
            side = actionType,
            price = orderPrice,
            quantity = quantity,
            orderId = orderId
        )
    }

    fun formatOrderRejected(
        broker: String,
        actionType: String,
        index: String,
        strike: String,
        reason: String,
        orderId: String = "ORD_${System.currentTimeMillis()}"
    ): String {
        return TelegramFormatter.formatOrderRejected(
            symbol = index,
            contract = strike,
            side = actionType,
            quantity = "1 LOT",
            rejectionReason = reason,
            orderId = orderId
        )
    }

    fun formatStopLossHit(
        actionType: String,
        index: String,
        strike: String,
        entry: String,
        exit: String,
        pnl: String
    ): String {
        val entryVal = entry.toDoubleOrNull()
        val exitVal = exit.toDoubleOrNull()
        val retPct = if (entryVal != null && entryVal > 0.0 && exitVal != null) {
            String.format(Locale.getDefault(), "%.1f", ((exitVal - entryVal) / entryVal) * 100)
        } else {
            "N/A"
        }
        return TelegramFormatter.formatStopLossHit(
            symbol = index,
            contract = strike,
            entry = entry,
            exit = exit,
            loss = pnl.replace("-", ""),
            returnPercent = retPct
        )
    }

    fun formatTargetHit(
        targetNumber: Int,
        actionType: String,
        index: String,
        strike: String,
        entry: String,
        currentLtp: String,
        pnl: String
    ): String {
        val entryVal = entry.toDoubleOrNull()
        val ltpVal = currentLtp.toDoubleOrNull()
        val retPct = if (entryVal != null && entryVal > 0.0 && ltpVal != null) {
            String.format(Locale.getDefault(), "%.1f", ((ltpVal - entryVal) / entryVal) * 100)
        } else {
            "N/A"
        }
        return TelegramFormatter.formatTargetHit(
            targetNumber = targetNumber,
            symbol = index,
            contract = strike,
            entry = entry,
            price = currentLtp,
            profit = pnl.replace("+", ""),
            returnPercent = retPct
        )
    }

    fun formatTrailingSlHit(
        actionType: String,
        index: String,
        strike: String,
        entry: String,
        exit: String,
        pnl: String
    ): String {
        return TelegramFormatter.formatTrailingSlUpdated(
            symbol = index,
            contract = strike,
            entry = entry,
            current = exit,
            oldSL = entry,
            newSL = exit,
            nextTarget = "TARGET 2",
            pnl = pnl
        )
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
        val pnlVal = pnl.toDoubleOrNull() ?: 0.0
        return if (pnlVal >= 0) {
            formatTargetHit(1, actionType, index, strike, entry, exit, pnl)
        } else {
            formatStopLossHit(actionType, index, strike, entry, exit, pnl)
        }
    }

    fun formatRiskLimitReached(
        reason: String,
        dailyPnl: String
    ): String {
        return TelegramFormatter.formatRiskLimitReached(
            pnl = dailyPnl,
            maxLoss = "10,000.00"
        )
    }
}
