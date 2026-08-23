package com.example.data.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class TelegramApiResponseInfo(
    val isSuccess: Boolean,
    val httpCode: Int,
    val okFlag: Boolean,
    val description: String,
    val rawJson: String,
    val timestamp: String = SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", Locale.getDefault()).format(Date())
)

class TelegramService(private val sessionManager: SessionManager) {

    private val sentEventIds = Collections.synchronizedSet(HashSet<String>())

    suspend fun sendAlert(
        text: String,
        botToken: String,
        chatId: String,
        channelId: String = sessionManager.telegramChannelId
    ): TelegramApiResponseInfo = withContext(Dispatchers.IO) {
        val cleanChatId = chatId.trim().removeSuffix(",").trim()
        val cleanChannelId = channelId.trim().removeSuffix(",").trim()

        if (cleanChatId.isBlank() && cleanChannelId.isBlank()) {
            return@withContext TelegramApiResponseInfo(
                isSuccess = false,
                httpCode = 400,
                okFlag = false,
                description = "Validation Error: At least one Telegram Chat ID or Channel ID is required.",
                rawJson = "{\"ok\": false, \"error_code\": 400, \"description\": \"Client Validation: Missing Chat ID and Channel ID\"}"
            )
        }

        var primaryResponse: TelegramApiResponseInfo? = null
        if (cleanChatId.isNotBlank()) {
            primaryResponse = sendRawMessage(botToken, cleanChatId, text)
        }

        var channelResponse: TelegramApiResponseInfo? = null
        if (cleanChannelId.isNotBlank()) {
            channelResponse = sendRawMessage(botToken, cleanChannelId, text)
        }

        if (primaryResponse != null && channelResponse != null) {
            val bothOk = primaryResponse.isSuccess && channelResponse.isSuccess
            val statusDesc = if (bothOk) {
                "✅ Alert successfully sent to both Chat ($cleanChatId) & Channel ($cleanChannelId)"
            } else if (primaryResponse.isSuccess) {
                "⚠️ Chat sent ($cleanChatId), but Channel failed: ${channelResponse.description}"
            } else if (channelResponse.isSuccess) {
                "⚠️ Channel sent ($cleanChannelId), but Chat failed: ${primaryResponse.description}"
            } else {
                "❌ Failed to send to both Chat ($cleanChatId) and Channel ($cleanChannelId)"
            }
            TelegramApiResponseInfo(
                isSuccess = primaryResponse.isSuccess || channelResponse.isSuccess,
                httpCode = if (bothOk) 200 else (if (primaryResponse.httpCode != 0) primaryResponse.httpCode else channelResponse.httpCode),
                okFlag = primaryResponse.okFlag || channelResponse.okFlag,
                description = statusDesc,
                rawJson = "{\"chat_response\": ${primaryResponse.rawJson}, \"channel_response\": ${channelResponse.rawJson}}"
            )
        } else {
            primaryResponse ?: channelResponse ?: TelegramApiResponseInfo(
                isSuccess = false,
                httpCode = 400,
                okFlag = false,
                description = "No target specified",
                rawJson = "{}"
            )
        }
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    /**
     * Send a test or real raw message to Telegram API and capture the exact HTTP & JSON response from Telegram servers.
     */
    suspend fun sendRawMessage(
        botToken: String,
        chatId: String,
        text: String
    ): TelegramApiResponseInfo = withContext(Dispatchers.IO) {
        // Enforce OPTIONS BUYER ONLY hard safety check
        if (text.contains("SELL CE", ignoreCase = true) ||
            text.contains("SELL PE", ignoreCase = true) ||
            text.contains("SHORT CE", ignoreCase = true) ||
            text.contains("SHORT PE", ignoreCase = true)
        ) {
            Log.w("TelegramService", "Blocked message violating OPTIONS BUYER ONLY policy")
            return@withContext TelegramApiResponseInfo(
                isSuccess = false,
                httpCode = 400,
                okFlag = false,
                description = "Blocked: OPTIONS BUYER ONLY policy violation (Non-BUY order prohibited)",
                rawJson = "{\"ok\": false, \"error\": \"OPTIONS BUYER ONLY restriction triggered\"}"
            )
        }

        // Sanitizing Bot Token
        var cleanToken = botToken.trim().removeSuffix(",").trim()
        if (cleanToken.startsWith("bot", ignoreCase = true)) {
            cleanToken = cleanToken.substring(3).trim()
        }

        // Sanitizing Chat ID
        val cleanChatId = chatId.trim().removeSuffix(",").trim()

        // Client Validation
        if (cleanToken.isBlank()) {
            return@withContext TelegramApiResponseInfo(
                isSuccess = false,
                httpCode = 400,
                okFlag = false,
                description = "Validation Error: Telegram Bot Token is required.",
                rawJson = "{\"ok\": false, \"error_code\": 400, \"description\": \"Client Validation: Missing Telegram Bot Token\"}"
            )
        }

        if (!cleanToken.contains(":")) {
            return@withContext TelegramApiResponseInfo(
                isSuccess = false,
                httpCode = 400,
                okFlag = false,
                description = "Validation Error: Invalid Bot Token format. Token must contain a colon (e.g. 123456789:ABCdef...).",
                rawJson = "{\"ok\": false, \"error_code\": 400, \"description\": \"Client Validation: Invalid Bot Token format - missing colon\"}"
            )
        }

        if (cleanChatId.isBlank()) {
            return@withContext TelegramApiResponseInfo(
                isSuccess = false,
                httpCode = 400,
                okFlag = false,
                description = "Validation Error: Telegram Chat ID / Channel Username is required.",
                rawJson = "{\"ok\": false, \"error_code\": 400, \"description\": \"Client Validation: Missing Chat ID\"}"
            )
        }

        // Exact base URL without trailing commas or slashes
        val baseUrl = "https://api.telegram.org"
        val requestUrl = "$baseUrl/bot$cleanToken/sendMessage"

        return@withContext try {
            val jsonAdapter = moshi.adapter(TelegramSendMessageRequest::class.java)
            val requestPayload = TelegramSendMessageRequest(
                chatId = cleanChatId,
                text = text,
                parseMode = "HTML"
            )
            val jsonString = jsonAdapter.toJson(requestPayload)
            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val httpCode = response.code
            val responseBodyString = response.body?.string() ?: ""

            val parsedError = runCatching {
                moshi.adapter(TelegramSendMessageResponse::class.java).fromJson(responseBodyString)
            }.getOrNull()

            val isOk = parsedError?.ok ?: response.isSuccessful
            val telegramDescription = parsedError?.description
                ?: if (responseBodyString.isNotBlank()) responseBodyString else "HTTP $httpCode ${response.message}"

            TelegramApiResponseInfo(
                isSuccess = response.isSuccessful && isOk,
                httpCode = httpCode,
                okFlag = isOk,
                description = if (response.isSuccessful && isOk) {
                    telegramDescription.ifBlank { "Message sent successfully to Telegram Chat ($cleanChatId)" }
                } else {
                    "Telegram Error ($httpCode): $telegramDescription"
                },
                rawJson = responseBodyString.ifBlank {
                    "{\"ok\": $isOk, \"error_code\": $httpCode, \"description\": \"$telegramDescription\"}"
                }
            )
        } catch (e: Exception) {
            TelegramApiResponseInfo(
                isSuccess = false,
                httpCode = 0,
                okFlag = false,
                description = "Network Connection Error: ${e.localizedMessage ?: e.message ?: "Failed to connect to Telegram server"}",
                rawJson = "{\"ok\": false, \"error\": \"${e.localizedMessage ?: e.message}\", \"url\": \"$requestUrl\"}"
            )
        }
    }

    /**
     * Dispatch event notification with duplicate protection and dual-target delivery.
     */
    suspend fun sendFormattedEvent(eventId: String, messageText: String): TelegramApiResponseInfo? {
        if (!sessionManager.isTelegramAlertsEnabled) return null
        val token = sessionManager.telegramBotToken
        val chatId = sessionManager.telegramChatId
        val channelId = sessionManager.telegramChannelId
        if (token.isBlank() || (chatId.isBlank() && channelId.isBlank())) return null

        if (eventId.isNotBlank()) {
            if (sentEventIds.contains(eventId)) {
                Log.d("TelegramService", "Prevented duplicate notification for event $eventId")
                return null
            }
            if (sentEventIds.size > 500) {
                sentEventIds.clear()
            }
            sentEventIds.add(eventId)
        }

        return sendAlert(text = messageText, botToken = token, chatId = chatId, channelId = channelId)
    }

    /**
     * Helper to send formatted Telegram alerts for the supported alert categories.
     */
    suspend fun sendAlertIfEnabled(
        alertType: String,
        symbol: String,
        details: String
    ): TelegramApiResponseInfo? {
        val eventId = "${alertType}_${symbol}_${System.currentTimeMillis()}"
        val formatted = when (alertType) {
            "BUY Signal", "BUY CE Signal", "AI BUY CE Signal" -> {
                com.example.util.alert.TelegramFormatter.formatAiBuyCe(
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    entry = "125.00",
                    sl = "95.00",
                    t1 = "145.00",
                    t2 = "165.00",
                    t3 = "190.00",
                    t4 = "220.00",
                    confidence = 92
                )
            }
            "BUY PE Signal", "AI BUY PE Signal" -> {
                com.example.util.alert.TelegramFormatter.formatAiBuyPe(
                    symbol = symbol,
                    contract = "$symbol 52400 PE",
                    entry = "210.00",
                    sl = "175.00",
                    t1 = "240.00",
                    t2 = "270.00",
                    t3 = "300.00",
                    t4 = "340.00",
                    confidence = 88
                )
            }
            "Entry / Position", "Entry / Position Opened", "Position Opened" -> {
                com.example.util.alert.TelegramFormatter.formatEntryPositionOpened(
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    entry = "125.00",
                    quantity = "65",
                    sl = "95.00",
                    t1 = "145.00",
                    t2 = "165.00",
                    t3 = "190.00",
                    t4 = "220.00"
                )
            }
            "Order Executed", "Order Placed" -> {
                com.example.util.alert.TelegramFormatter.formatOrderExecuted(
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    side = "BUY CE",
                    price = "125.00",
                    quantity = "65",
                    orderId = "ORD_${System.currentTimeMillis().toString().takeLast(6)}"
                )
            }
            "Order Rejected" -> {
                com.example.util.alert.TelegramFormatter.formatOrderRejected(
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    side = "BUY CE",
                    quantity = "65",
                    rejectionReason = details.ifBlank { "Insufficient Margin in Trading Account" },
                    orderId = "ORD_${System.currentTimeMillis().toString().takeLast(6)}"
                )
            }
            "Stop Loss", "Stop Loss Hit" -> {
                com.example.util.alert.TelegramFormatter.formatStopLossHit(
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    entry = "125.00",
                    exit = "95.00",
                    loss = "1,950.00",
                    returnPercent = "24.0",
                    reason = "Stop Loss Level Triggered"
                )
            }
            "Target 1", "Target 1 Hit" -> {
                com.example.util.alert.TelegramFormatter.formatTargetHit(
                    targetNumber = 1,
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    entry = "125.00",
                    price = "145.00",
                    profit = "1,300.00",
                    returnPercent = "16.0"
                )
            }
            "Target 2", "Target 2 Hit" -> {
                com.example.util.alert.TelegramFormatter.formatTargetHit(
                    targetNumber = 2,
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    entry = "125.00",
                    price = "165.00",
                    profit = "2,600.00",
                    returnPercent = "32.0"
                )
            }
            "Target 3", "Target 3 Hit" -> {
                com.example.util.alert.TelegramFormatter.formatTargetHit(
                    targetNumber = 3,
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    entry = "125.00",
                    price = "190.00",
                    profit = "4,225.00",
                    returnPercent = "52.0"
                )
            }
            "Target 4", "Target 4 Hit" -> {
                com.example.util.alert.TelegramFormatter.formatTargetHit(
                    targetNumber = 4,
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    entry = "125.00",
                    price = "220.00",
                    profit = "6,175.00",
                    returnPercent = "76.0"
                )
            }
            "Trailing Stop Loss", "Trailing Stop Loss Hit" -> {
                com.example.util.alert.TelegramFormatter.formatTrailingSlUpdated(
                    symbol = symbol,
                    contract = "$symbol 24850 CE",
                    entry = "125.00",
                    current = "155.00",
                    oldSL = "95.00",
                    newSL = "135.00",
                    nextTarget = "165.00",
                    pnl = "+1,950.00"
                )
            }
            "Broker Connected" -> {
                com.example.util.alert.TelegramFormatter.formatBrokerConnected("Dhan")
            }
            "Broker Disconnected" -> {
                com.example.util.alert.TelegramFormatter.formatBrokerDisconnected("Dhan")
            }
            "Algo Started" -> {
                com.example.util.alert.TelegramFormatter.formatAlgoStarted("Supertrend Scalper", symbol)
            }
            "Algo Stopped" -> {
                com.example.util.alert.TelegramFormatter.formatAlgoStopped("Supertrend Scalper")
            }
            "Risk Limit Reached" -> {
                com.example.util.alert.TelegramFormatter.formatRiskLimitReached("-10,000.00", "10,000.00")
            }
            "Session Expired" -> {
                com.example.util.alert.TelegramFormatter.formatBrokerDisconnected("Dhan")
            }
            else -> {
                com.example.data.network.TelegramMessageFormatter.build("<b>$alertType</b>\n\nInstrument: $symbol\nDetails: $details")
            }
        }
        return sendFormattedEvent(eventId, formatted)
    }
}

