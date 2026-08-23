package com.example.util.alert

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.example.util.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Enterprise SMS Delivery Service for King Khan AI Trading Alerts.
 * 
 * Supports on-device cellular SMS dispatch via SmsManager
 * as well as remote Web SMS Gateway / Webhook transmission.
 */
class SmsService(
    private val context: Context,
    private val appPreferences: AppPreferences
) {
    companion object {
        private const val TAG = "SmsService"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Dispatches an SMS alert to the user's configured destination phone number
     * or via custom Web SMS Gateway if provided.
     */
    suspend fun sendAlertSms(
        message: String,
        targetPhone: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val phone = if (targetPhone.isNotBlank()) targetPhone else appPreferences.getSmsAlertPhone()
        val isEnabled = appPreferences.isSmsAlertsEnabled()

        if (!isEnabled && targetPhone.isBlank()) {
            Log.d(TAG, "SMS alerts are currently disabled in user preferences.")
            return@withContext Result.success(false)
        }

        if (phone.isBlank() && appPreferences.getSmsGatewayUrl().isBlank()) {
            Log.w(TAG, "No SMS recipient phone number or Gateway URL configured.")
            return@withContext Result.failure(IllegalStateException("No phone number or gateway URL configured for SMS alerts"))
        }

        // 1. Try Custom Web SMS Gateway API if configured
        val gatewayUrl = appPreferences.getSmsGatewayUrl()
        if (gatewayUrl.isNotBlank()) {
            try {
                val apiKey = appPreferences.getSmsApiKey()
                val jsonPayload = JSONObject().apply {
                    put("phone", phone)
                    put("message", message)
                    put("sender", "KINGKHAN")
                    put("timestamp", System.currentTimeMillis())
                }

                val reqBuilder = Request.Builder()
                    .url(gatewayUrl)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))

                if (apiKey.isNotBlank()) {
                    reqBuilder.header("Authorization", "Bearer $apiKey")
                    reqBuilder.header("x-api-key", apiKey)
                }

                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(TAG, "SMS alert sent successfully via Web Gateway to $phone")
                        return@withContext Result.success(true)
                    } else {
                        Log.w(TAG, "Web SMS Gateway responded with code ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching SMS via Web Gateway: ${e.message}")
            }
        }

        // 2. Try on-device Android SmsManager
        if (phone.isNotBlank()) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                if (smsManager != null) {
                    val parts = smsManager.divideMessage(message)
                    if (parts.size > 1) {
                        smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(phone, null, message, null, null)
                    }
                    Log.i(TAG, "SMS alert successfully queued to $phone via SmsManager")
                    return@withContext Result.success(true)
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "SEND_SMS permission not granted on device: ${e.message}")
                // Return success false without failing applet
                return@withContext Result.success(false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS via SmsManager: ${e.message}")
                return@withContext Result.failure(e)
            }
        }

        return@withContext Result.success(true)
    }
}
