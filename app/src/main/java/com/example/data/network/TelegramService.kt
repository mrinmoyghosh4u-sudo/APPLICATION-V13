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
        val eventId = "${alertType}_${symbol}_${details.hashCode()}"
        val formatted = when (alertType) {
            "BUY Signal", "BUY CE Signal" -> {
                TelegramMessageFormatter.formatAiSignal(
                    actionType = "BUY CE",
                    index = symbol,
                    strike = "$symbol 24850 CE",
                    expiry = "WEEKLY",
                    entry = "150.00", sl = "120.00", t1 = "180.00", t2 = "210.00", t3 = "240.00", t4 = "270.00",
                    trailingSl = "135.00", timeframe = "5 MIN", marketBias = "BULLISH",
                    confirmations = listOf("EMA", "VWAP", "RSI", "Supertrend", "OI", "Volume"), confidence = 88
                )
            }
            "BUY PE Signal" -> {
                TelegramMessageFormatter.formatAiSignal(
                    actionType = "BUY PE",
                    index = symbol,
                    strike = "$symbol 52400 PE",
                    expiry = "WEEKLY",
                    entry = "210.00", sl = "175.00", t1 = "240.00", t2 = "270.00", t3 = "300.00", t4 = "340.00",
                    trailingSl = "190.00", timeframe = "5 MIN", marketBias = "BEARISH",
                    confirmations = listOf("EMA", "VWAP", "RSI", "Supertrend", "OI", "Volume"), confidence = 82
                )
            }
            "Order Executed", "Order Placed" -> {
                TelegramMessageFormatter.formatLiveOrderPlaced(
                    broker = "Dhan", actionType = "BUY CE", index = symbol, strike = symbol, expiry = "WEEKLY",
                    quantity = "65", orderPrice = "150.00", orderId = "ORD_${System.currentTimeMillis()}", status = "EXECUTED"
                )
            }
            "Order Rejected" -> {
                TelegramMessageFormatter.formatOrderRejected("Dhan", "BUY CE", symbol, symbol, details)
            }
            "Stop Loss" -> {
                TelegramMessageFormatter.formatStopLossHit("BUY CE", symbol, symbol, "150.00", "120.00", "-1950.00")
            }
            "Target 1" -> TelegramMessageFormatter.formatTargetHit(1, "BUY CE", symbol, symbol, "150.00", "180.00", "+1950.00")
            "Target 2" -> TelegramMessageFormatter.formatTargetHit(2, "BUY CE", symbol, symbol, "150.00", "210.00", "+3900.00")
            "Target 3" -> TelegramMessageFormatter.formatTargetHit(3, "BUY CE", symbol, symbol, "150.00", "240.00", "+5850.00")
            "Target 4" -> TelegramMessageFormatter.formatTargetHit(4, "BUY CE", symbol, symbol, "150.00", "270.00", "+7800.00")
            "Trailing Stop Loss" -> TelegramMessageFormatter.formatTrailingSlHit("BUY CE", symbol, symbol, "150.00", "165.00", "+975.00")
            "Broker Connected" -> TelegramMessageFormatter.formatDhanConnected()
            "Session Expired" -> TelegramMessageFormatter.formatSessionExpired("Dhan")
            else -> {
                TelegramMessageFormatter.build("<b>$alertType</b>\n\nInstrument: $symbol\nDetails: $details")
            }
        }
        return sendFormattedEvent(eventId, formatted)
    }
}

