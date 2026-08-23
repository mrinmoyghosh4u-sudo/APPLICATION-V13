package com.example.util.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.network.SessionManager
import com.example.data.network.TelegramApiResponseInfo
import com.example.data.network.TelegramService
import com.example.data.repository.TradingRepository
import com.example.util.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class AlertService(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val appPreferences: AppPreferences,
    private val telegramService: TelegramService,
    private val repository: TradingRepository? = null
) {
    val preferenceManager = AlertPreferenceManager(appPreferences)
    val duplicateGuard = DuplicateAlertGuard()

    private val notificationIdCounter = AtomicInteger(1000)
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "kk_trade_alerts_channel"
        const val CHANNEL_NAME = "💥 KING KHAN AI TRADE Alerts"
        private const val TAG = "AlertService"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Critical Real-Time AI Trading Signals, Orders and Risk Alerts"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Core unified alert dispatcher.
     * Enforces:
     * 1. Independent ON/OFF check for the given event type.
     * 2. Strict duplicate prevention via event key.
     * 3. Dispatches Telegram Alert (if enabled in preferences and Telegram configured).
     * 4. Dispatches App Push Notification (if enabled in preferences).
     * 5. Saves to in-app Notification Center (Room database).
     */
    suspend fun dispatchAlert(
        eventType: AlertEventType,
        eventKey: String,
        telegramMessage: String,
        smsMessage: String,
        pushPayload: PushNotificationPayload
    ): TelegramApiResponseInfo? = withContext(Dispatchers.IO) {
        // 1. Check ON/OFF toggle for this specific event
        if (!preferenceManager.isEventEnabled(eventType)) {
            Log.d(TAG, "Alert skipped: Event ${eventType.name} is disabled in Alert Preferences")
            return@withContext null
        }

        // 2. Prevent duplicate alerts for the same event key
        if (!duplicateGuard.tryAcquire(eventKey)) {
            Log.d(TAG, "Alert duplicate suppressed for key: $eventKey")
            return@withContext null
        }

        // 3. Save to In-App Notification Center
        repository?.let { repo ->
            try {
                repo.addNotification(
                    title = pushPayload.title,
                    message = pushPayload.message,
                    type = pushPayload.type
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving in-app notification: ${e.message}")
            }
        }

        // 4. Send Android System Push Notification
        try {
            sendSystemPushNotification(pushPayload)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting system push notification: ${e.message}")
        }

        // 5. Send Telegram Message (if Telegram master alert is enabled & credentials configured)
        if (sessionManager.isTelegramAlertsEnabled) {
            val token = sessionManager.telegramBotToken
            val chatId = sessionManager.telegramChatId
            val channelId = sessionManager.telegramChannelId
            if (token.isNotBlank() && (chatId.isNotBlank() || channelId.isNotBlank())) {
                return@withContext telegramService.sendAlert(
                    text = telegramMessage,
                    botToken = token,
                    chatId = chatId,
                    channelId = channelId
                )
            }
        }

        return@withContext null
    }

    private fun sendSystemPushNotification(payload: PushNotificationPayload) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdCounter.get(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(payload.title)
            .setContentText(payload.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationIdCounter.incrementAndGet(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Push notification permission not granted: ${e.message}")
        }
    }

    // ==========================================
    // 16 Event-Specific Dispatchers
    // ==========================================

    // 1. Broker Connected
    fun notifyBrokerConnected(broker: String, account: String = "PRIMARY") {
        serviceScope.launch {
            val eventType = AlertEventType.BROKER_CONNECTED
            val eventKey = duplicateGuard.buildEventKey(
                broker = broker,
                account = account,
                eventType = eventType,
                timestampBucket = System.currentTimeMillis() / 30000 // 30s debounce
            )
            val tg = TelegramFormatter.formatBrokerConnected(broker)
            val sms = SmsFormatter.formatBrokerConnected(broker)
            val push = PushFormatter.formatBrokerConnected(broker)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 2. Broker Disconnected
    fun notifyBrokerDisconnected(broker: String, account: String = "PRIMARY") {
        serviceScope.launch {
            val eventType = AlertEventType.BROKER_DISCONNECTED
            val eventKey = duplicateGuard.buildEventKey(
                broker = broker,
                account = account,
                eventType = eventType,
                timestampBucket = System.currentTimeMillis() / 30000 // 30s debounce
            )
            val tg = TelegramFormatter.formatBrokerDisconnected(broker)
            val sms = SmsFormatter.formatBrokerDisconnected(broker)
            val push = PushFormatter.formatBrokerDisconnected(broker)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 3. AI BUY CE Signal
    fun notifyAiBuyCeSignal(
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
        volume: String = "HIGH"
    ) {
        serviceScope.launch {
            val eventType = AlertEventType.BUY_CE_SIGNAL
            val eventKey = duplicateGuard.buildEventKey(
                orderId = contract,
                eventType = eventType,
                timestampBucket = System.currentTimeMillis() / 60000 // 1 min debounce per contract
            )
            val tg = TelegramFormatter.formatAiBuyCe(
                symbol = symbol, contract = contract, entry = entry, sl = sl,
                t1 = t1, t2 = t2, t3 = t3, t4 = t4,
                confidence = confidence, strength = strength, oiChange = oiChange, volume = volume
            )
            val sms = SmsFormatter.formatAiBuyCe(
                contract = contract, entry = entry, sl = sl,
                t1 = t1, t2 = t2, t3 = t3, t4 = t4, confidence = confidence
            )
            val push = PushFormatter.formatAiBuyCe(contract = contract, entry = entry, sl = sl, t1 = t1)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 4. AI BUY PE Signal
    fun notifyAiBuyPeSignal(
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
        volume: String = "HIGH"
    ) {
        serviceScope.launch {
            val eventType = AlertEventType.BUY_PE_SIGNAL
            val eventKey = duplicateGuard.buildEventKey(
                orderId = contract,
                eventType = eventType,
                timestampBucket = System.currentTimeMillis() / 60000 // 1 min debounce per contract
            )
            val tg = TelegramFormatter.formatAiBuyPe(
                symbol = symbol, contract = contract, entry = entry, sl = sl,
                t1 = t1, t2 = t2, t3 = t3, t4 = t4,
                confidence = confidence, strength = strength, oiChange = oiChange, volume = volume
            )
            val sms = SmsFormatter.formatAiBuyPe(
                contract = contract, entry = entry, sl = sl,
                t1 = t1, t2 = t2, t3 = t3, t4 = t4, confidence = confidence
            )
            val push = PushFormatter.formatAiBuyPe(contract = contract, entry = entry, sl = sl, t1 = t1)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 5. Entry / Position Opened
    fun notifyEntryPositionOpened(
        symbol: String,
        contract: String,
        entry: String,
        quantity: String,
        sl: String,
        t1: String,
        t2: String,
        t3: String,
        t4: String,
        positionId: String = "",
        orderId: String = ""
    ) {
        serviceScope.launch {
            val eventType = AlertEventType.ENTRY_POSITION
            val eventKey = duplicateGuard.buildEventKey(
                orderId = orderId,
                positionId = positionId.ifBlank { "${symbol}_${contract}_${entry}" },
                eventType = eventType
            )
            val tg = TelegramFormatter.formatEntryPositionOpened(
                symbol = symbol, contract = contract, entry = entry,
                quantity = quantity, sl = sl, t1 = t1, t2 = t2, t3 = t3, t4 = t4
            )
            val sms = SmsFormatter.formatEntryPosition(
                contract = contract, entry = entry, quantity = quantity, sl = sl, t1 = t1
            )
            val push = PushFormatter.formatEntryPosition(contract = contract, entry = entry, quantity = quantity)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 6. Stop Loss Hit
    fun notifyStopLossHit(
        symbol: String,
        contract: String,
        entry: String,
        exit: String,
        loss: String,
        returnPercent: String,
        positionId: String = "",
        orderId: String = "",
        reason: String = "Stop Loss Level Hit"
    ) {
        serviceScope.launch {
            val eventType = AlertEventType.STOP_LOSS_HIT
            val eventKey = duplicateGuard.buildEventKey(
                orderId = orderId,
                positionId = positionId.ifBlank { "${symbol}_${contract}" },
                eventType = eventType
            )
            val tg = TelegramFormatter.formatStopLossHit(
                symbol = symbol, contract = contract, entry = entry,
                exit = exit, loss = loss, returnPercent = returnPercent, reason = reason
            )
            val sms = SmsFormatter.formatStopLossHit(
                contract = contract, entry = entry, exit = exit,
                loss = loss, returnPercent = returnPercent
            )
            val push = PushFormatter.formatStopLossHit(
                contract = contract, exit = exit, loss = loss, returnPercent = returnPercent
            )
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 7, 8, 9, 10. Target Hit
    fun notifyTargetHit(
        targetNumber: Int,
        symbol: String,
        contract: String,
        entry: String,
        price: String,
        profit: String,
        returnPercent: String,
        positionId: String = "",
        orderId: String = ""
    ) {
        serviceScope.launch {
            val eventType = when (targetNumber) {
                1 -> AlertEventType.TARGET_1_HIT
                2 -> AlertEventType.TARGET_2_HIT
                3 -> AlertEventType.TARGET_3_HIT
                else -> AlertEventType.TARGET_4_HIT
            }
            val eventKey = duplicateGuard.buildEventKey(
                orderId = orderId,
                positionId = positionId.ifBlank { "${symbol}_${contract}" },
                eventType = eventType,
                targetNumber = targetNumber
            )
            val tg = TelegramFormatter.formatTargetHit(
                targetNumber = targetNumber, symbol = symbol, contract = contract,
                entry = entry, price = price, profit = profit, returnPercent = returnPercent
            )
            val sms = SmsFormatter.formatTargetHit(
                targetNumber = targetNumber, contract = contract, entry = entry,
                price = price, profit = profit, returnPercent = returnPercent
            )
            val push = PushFormatter.formatTargetHit(
                targetNumber = targetNumber, contract = contract, price = price,
                profit = profit, returnPercent = returnPercent
            )
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 11. Trailing Stop Loss Hit / Updated
    fun notifyTrailingSlUpdated(
        symbol: String,
        contract: String,
        entry: String,
        current: String,
        oldSL: String,
        newSL: String,
        nextTarget: String,
        pnl: String,
        positionId: String = "",
        orderId: String = ""
    ) {
        serviceScope.launch {
            val eventType = AlertEventType.TRAILING_SL_HIT
            val eventKey = duplicateGuard.buildEventKey(
                positionId = positionId.ifBlank { "${symbol}_${contract}" },
                orderId = orderId.ifBlank { newSL },
                eventType = eventType
            )
            val tg = TelegramFormatter.formatTrailingSlUpdated(
                symbol = symbol, contract = contract, entry = entry,
                current = current, oldSL = oldSL, newSL = newSL,
                nextTarget = nextTarget, pnl = pnl
            )
            val sms = SmsFormatter.formatTrailingSlHit(
                contract = contract, entry = entry, current = current,
                newSL = newSL, pnl = pnl
            )
            val push = PushFormatter.formatTrailingSlHit(contract = contract, current = current, newSL = newSL)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 12. Order Executed
    fun notifyOrderExecuted(
        broker: String = "Dhan",
        symbol: String,
        contract: String,
        side: String,
        price: String,
        quantity: String,
        orderId: String
    ) {
        serviceScope.launch {
            val eventType = AlertEventType.ORDER_EXECUTED
            val eventKey = duplicateGuard.buildEventKey(
                broker = broker,
                orderId = orderId,
                eventType = eventType
            )
            val tg = TelegramFormatter.formatOrderExecuted(
                symbol = symbol, contract = contract, side = side,
                price = price, quantity = quantity, orderId = orderId
            )
            val sms = SmsFormatter.formatOrderExecuted(
                contract = contract, side = side, quantity = quantity,
                price = price, orderId = orderId
            )
            val push = PushFormatter.formatOrderExecuted(
                contract = contract, side = side, quantity = quantity,
                price = price, orderId = orderId
            )
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 13. Order Rejected
    fun notifyOrderRejected(
        broker: String = "Dhan",
        symbol: String,
        contract: String,
        side: String,
        quantity: String,
        rejectionReason: String,
        orderId: String
    ) {
        serviceScope.launch {
            val eventType = AlertEventType.ORDER_REJECTED
            val eventKey = duplicateGuard.buildEventKey(
                broker = broker,
                orderId = orderId.ifBlank { "${symbol}_${System.currentTimeMillis()}" },
                eventType = eventType
            )
            val tg = TelegramFormatter.formatOrderRejected(
                symbol = symbol, contract = contract, side = side,
                quantity = quantity, rejectionReason = rejectionReason, orderId = orderId
            )
            val sms = SmsFormatter.formatOrderRejected(
                contract = contract, side = side, quantity = quantity,
                reason = rejectionReason, orderId = orderId
            )
            val push = PushFormatter.formatOrderRejected(
                contract = contract, reason = rejectionReason, orderId = orderId
            )
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 14. Algo Started
    fun notifyAlgoStarted(strategy: String, symbol: String) {
        serviceScope.launch {
            val eventType = AlertEventType.ALGO_STARTED
            val eventKey = duplicateGuard.buildEventKey(
                orderId = strategy,
                eventType = eventType,
                timestampBucket = System.currentTimeMillis() / 15000 // 15s debounce
            )
            val tg = TelegramFormatter.formatAlgoStarted(strategy = strategy, symbol = symbol)
            val sms = SmsFormatter.formatAlgoStarted(strategy = strategy, symbol = symbol)
            val push = PushFormatter.formatAlgoStarted(strategy = strategy, symbol = symbol)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 15. Algo Stopped
    fun notifyAlgoStopped(strategy: String) {
        serviceScope.launch {
            val eventType = AlertEventType.ALGO_STOPPED
            val eventKey = duplicateGuard.buildEventKey(
                orderId = strategy,
                eventType = eventType,
                timestampBucket = System.currentTimeMillis() / 15000 // 15s debounce
            )
            val tg = TelegramFormatter.formatAlgoStopped(strategy = strategy)
            val sms = SmsFormatter.formatAlgoStopped(strategy = strategy)
            val push = PushFormatter.formatAlgoStopped(strategy = strategy)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }

    // 16. Risk Limit Reached
    fun notifyRiskLimitReached(pnl: String, maxLoss: String) {
        serviceScope.launch {
            val eventType = AlertEventType.RISK_LIMIT_REACHED
            val eventKey = duplicateGuard.buildEventKey(
                eventType = eventType,
                timestampBucket = System.currentTimeMillis() / 300000 // 5 min debounce
            )
            val tg = TelegramFormatter.formatRiskLimitReached(pnl = pnl, maxLoss = maxLoss)
            val sms = SmsFormatter.formatRiskLimitReached(pnl = pnl, maxLoss = maxLoss)
            val push = PushFormatter.formatRiskLimitReached(pnl = pnl, maxLoss = maxLoss)
            dispatchAlert(eventType, eventKey, tg, sms, push)
        }
    }
}
