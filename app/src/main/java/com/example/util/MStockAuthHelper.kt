package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Official m.Stock (Mirae Asset Capital Markets) Authentication Helper
 * 
 * Handles:
 * - Local RFC 6238 6-digit TOTP generation from secure Base32 TOTP Secret
 * - Official m.Stock verifytotp authentication API
 * - Real JWT/Access Token, Refresh Token, and Feed Token extraction
 * - Automatic session renewal on token expiry
 * - Strict credential redaction (No sensitive secrets logged)
 */
object MStockAuthHelper {
    private const val TAG = "MStockAuth"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    data class MStockTokens(
        val accessToken: String,
        val refreshToken: String = "",
        val feedToken: String = ""
    )

    private val VERIFY_TOTP_URLS = listOf(
        "https://api.mstock.trade/v1/auth/verifytotp",
        "https://tradeapi.mstock.com/open-api/v1/verifytotp",
        "https://api.mstock.com/v1/auth/verifytotp"
    )

    private val RENEW_TOKEN_URLS = listOf(
        "https://api.mstock.trade/v1/auth/renew",
        "https://tradeapi.mstock.com/open-api/v1/renew",
        "https://api.mstock.com/v1/auth/renew"
    )

    /**
     * Executes official m.Stock TOTP authentication using clientCode, apiKey, and TOTP secret/code
     */
    suspend fun verifyTotp(
        clientCode: String,
        apiKey: String,
        totpOrSecret: String
    ): Result<MStockTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedClientCode = clientCode.trim()
            val sanitizedApiKey = apiKey.trim()
            val sanitizedTotpInput = totpOrSecret.trim()

            if (sanitizedClientCode.isBlank()) {
                throw Exception("m.Stock Client Code is required.")
            }
            if (sanitizedApiKey.isBlank()) {
                throw Exception("m.Stock API Key is required.")
            }
            if (sanitizedTotpInput.isBlank()) {
                throw Exception("m.Stock TOTP Secret is required.")
            }

            Log.d(TAG, "Initiating m.Stock TOTP authentication for client: $sanitizedClientCode")

            // 1. Generate standard 6-digit RFC 6238 TOTP locally if secret is provided
            val effectiveTotp = if (sanitizedTotpInput.length == 6 && sanitizedTotpInput.all { it.isDigit() }) {
                sanitizedTotpInput
            } else {
                val generated = TotpUtil.generateTotp(sanitizedTotpInput)
                if (generated.isBlank()) {
                    throw Exception("Failed to generate 6-digit TOTP. Please verify your m.Stock Base32 TOTP Secret.")
                }
                generated
            }

            val reqPayload = JSONObject().apply {
                put("clientCode", sanitizedClientCode)
                put("clientId", sanitizedClientCode)
                put("totp", effectiveTotp)
            }.toString().toRequestBody("application/json".toMediaType())

            var lastException: Exception? = null

            for (url in VERIFY_TOTP_URLS) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .post(reqPayload)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .addHeader("x-api-key", sanitizedApiKey)
                        .addHeader("X-UserType", "USER")
                        .addHeader("X-SourceID", "MOBILE")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val respCode = response.code
                    val respString = response.body?.string() ?: ""

                    Log.d(TAG, "m.Stock verifytotp endpoint: $url returned HTTP $respCode")

                    if (response.isSuccessful || respCode == 200 || respCode == 201) {
                        val json = runCatching { JSONObject(respString) }.getOrNull()
                        if (json != null) {
                            val isStatusOk = json.optBoolean("status", true) ||
                                    json.optString("status", "").equals("success", ignoreCase = true) ||
                                    json.optBoolean("success", false)

                            if (isStatusOk) {
                                val dataObj = json.optJSONObject("data") ?: json
                                val accessToken = dataObj.optString("accessToken")
                                    .ifBlank { dataObj.optString("jwtToken") }
                                    .ifBlank { dataObj.optString("token") }
                                    .ifBlank { dataObj.optString("access_token") }

                                val refreshToken = dataObj.optString("refreshToken")
                                    .ifBlank { dataObj.optString("refresh_token") }

                                val feedToken = dataObj.optString("feedToken")
                                    .ifBlank { dataObj.optString("wsToken") }
                                    .ifBlank { dataObj.optString("feed_token") }
                                    .ifBlank { accessToken }

                                if (accessToken.isNotBlank()) {
                                    Log.d(TAG, "m.Stock authentication successful for client: $sanitizedClientCode")
                                    return@withContext Result.success(
                                        MStockTokens(
                                            accessToken = accessToken,
                                            refreshToken = refreshToken,
                                            feedToken = feedToken
                                        )
                                    )
                                }
                            }

                            val errorMsg = json.optString("message", "Authentication failed: invalid response")
                            lastException = Exception("m.Stock error: $errorMsg")
                        } else {
                            lastException = Exception("m.Stock empty JSON response (HTTP $respCode)")
                        }
                    } else {
                        val json = runCatching { JSONObject(respString) }.getOrNull()
                        val errorMsg = json?.optString("message") ?: "HTTP $respCode"
                        lastException = Exception("m.Stock verifytotp failed: $errorMsg")
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }

            throw lastException ?: Exception("Failed to connect to m.Stock authentication server.")
        }
    }

    /**
     * Renews the m.Stock session using refresh token, or automatically re-authenticates
     * with fresh RFC 6238 TOTP generated from the stored TOTP secret.
     */
    suspend fun renewSession(
        clientCode: String,
        apiKey: String,
        refreshToken: String,
        totpSecret: String
    ): Result<MStockTokens> = withContext(Dispatchers.IO) {
        runCatching {
            // First attempt: Renew via refresh token if available
            if (refreshToken.isNotBlank() && apiKey.isNotBlank()) {
                val reqBody = JSONObject().apply {
                    put("refreshToken", refreshToken)
                    put("clientCode", clientCode)
                }.toString().toRequestBody("application/json".toMediaType())

                for (url in RENEW_TOKEN_URLS) {
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .post(reqBody)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "application/json")
                            .addHeader("x-api-key", apiKey.trim())
                            .build()

                        val response = httpClient.newCall(request).execute()
                        val respBody = response.body?.string() ?: ""

                        if (response.isSuccessful) {
                            val json = runCatching { JSONObject(respBody) }.getOrNull()
                            if (json != null) {
                                val dataObj = json.optJSONObject("data") ?: json
                                val newAccess = dataObj.optString("accessToken")
                                    .ifBlank { dataObj.optString("jwtToken") }
                                    .ifBlank { dataObj.optString("token") }
                                val newRefresh = dataObj.optString("refreshToken").ifBlank { refreshToken }
                                val newFeed = dataObj.optString("feedToken")

                                if (newAccess.isNotBlank()) {
                                    Log.d(TAG, "m.Stock session successfully renewed via refresh token")
                                    return@withContext Result.success(
                                        MStockTokens(newAccess, newRefresh, newFeed)
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Continue to next endpoint or auto-reauth
                    }
                }
            }

            // Fallback attempt: Generate fresh TOTP from TOTP secret and re-authenticate
            if (totpSecret.isNotBlank() && clientCode.isNotBlank() && apiKey.isNotBlank()) {
                Log.d(TAG, "Attempting m.Stock auto re-authentication using TOTP secret...")
                return@withContext verifyTotp(clientCode, apiKey, totpSecret)
            }

            throw Exception("Unable to renew m.Stock session. Missing TOTP Secret or Refresh Token.")
        }
    }
}
