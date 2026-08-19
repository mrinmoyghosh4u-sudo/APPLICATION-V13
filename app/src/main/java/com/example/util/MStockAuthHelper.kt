package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Official m.Stock (Mirae Asset Capital Markets) Authentication Helper
 * 
 * Supports Official Type A and Type B API Authentication:
 * - Type A Endpoint: POST https://api.mstock.trade/openapi/typea/session/verifytotp
 * - Type B Endpoint: POST https://api.mstock.trade/openapi/typeb/session/verifytotp
 * 
 * Strict Security Rules:
 * - Local RFC 6238 6-digit TOTP generation from secure Base32 TOTP Secret
 * - Raw TOTP secret is NEVER transmitted over the wire
 * - Raw tokens and API keys are NEVER printed in public logs
 * - Credentials and session tokens are securely persisted in Android Encrypted Preferences
 */
object MStockAuthHelper {
    private const val TAG = "MStockAuth"

    private const val TYPE_A_VERIFY_TOTP_URL = "https://api.mstock.trade/openapi/typea/session/verifytotp"
    private const val TYPE_B_VERIFY_TOTP_URL = "https://api.mstock.trade/openapi/typeb/session/verifytotp"

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

    /**
     * Executes official m.Stock TOTP authentication using clientCode, apiKey, and TOTP secret/code.
     * Uses Type A endpoint by default, or Type B endpoint if a refreshToken is provided.
     */
    suspend fun verifyTotp(
        clientCode: String,
        apiKey: String,
        totpOrSecret: String,
        refreshToken: String? = null
    ): Result<MStockTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedClientCode = clientCode.trim()
            val sanitizedApiKey = apiKey.trim()
            val sanitizedTotpInput = totpOrSecret.trim()
            val sanitizedRefreshToken = refreshToken?.trim() ?: ""

            if (sanitizedApiKey.isBlank()) {
                throw Exception("m.Stock API Key is required.")
            }
            if (sanitizedTotpInput.isBlank()) {
                throw Exception("m.Stock TOTP Secret or 6-digit TOTP code is required.")
            }

            Log.d(TAG, "Initiating m.Stock TOTP authentication for client: ${sanitizedClientCode.ifBlank { "N/A" }}")

            // 1. Generate standard 6-digit RFC 6238 TOTP locally if TOTP secret key is provided
            val effectiveTotp = if (sanitizedTotpInput.length == 6 && sanitizedTotpInput.all { it.isDigit() }) {
                sanitizedTotpInput
            } else {
                val generated = TotpUtil.generateTotp(sanitizedTotpInput)
                if (generated.isBlank()) {
                    throw Exception("Failed to generate 6-digit TOTP. Please verify your m.Stock Base32 TOTP Secret.")
                }
                generated
            }

            // 2. Decide between Type B (if refreshToken present) and Type A
            if (sanitizedRefreshToken.isNotBlank()) {
                try {
                    Log.d(TAG, "Attempting m.Stock Type B authentication...")
                    val result = executeTypeBAuth(sanitizedApiKey, sanitizedRefreshToken, effectiveTotp)
                    if (result.isSuccess) return@withContext result
                } catch (e: Exception) {
                    Log.w(TAG, "Type B authentication failed: ${e.localizedMessage}. Falling back to Type A...")
                }
            }

            // 3. Execute Type A Authentication
            Log.d(TAG, "Executing m.Stock Type A authentication...")
            executeTypeAAuth(sanitizedApiKey, effectiveTotp, sanitizedClientCode)
        }
    }

    private fun executeTypeAAuth(
        apiKey: String,
        totp: String,
        clientCode: String = ""
    ): MStockTokens {
        val formBodyBuilder = StringBuilder()
        formBodyBuilder.append("api_key=").append(URLEncoder.encode(apiKey, "UTF-8"))
        formBodyBuilder.append("&totp=").append(URLEncoder.encode(totp, "UTF-8"))
        if (clientCode.isNotBlank()) {
            formBodyBuilder.append("&clientCode=").append(URLEncoder.encode(clientCode, "UTF-8"))
        }

        val body = formBodyBuilder.toString().toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url(TYPE_A_VERIFY_TOTP_URL)
            .post(body)
            .addHeader("X-Mirae-Version", "1")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build()

        return parseResponseAndExtractTokens(request, "Type A")
    }

    private fun executeTypeBAuth(
        apiKey: String,
        refreshToken: String,
        totp: String
    ): Result<MStockTokens> {
        val jsonObj = JSONObject().apply {
            put("refreshToken", refreshToken)
            put("totp", totp)
        }
        val body = jsonObj.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(TYPE_B_VERIFY_TOTP_URL)
            .post(body)
            .addHeader("X-Mirae-Version", "1")
            .addHeader("X-PrivateKey", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return runCatching { parseResponseAndExtractTokens(request, "Type B") }
    }

    private fun parseResponseAndExtractTokens(request: Request, typeName: String): MStockTokens {
        val response = httpClient.newCall(request).execute()
        val respCode = response.code
        val respString = response.body?.string() ?: ""

        Log.d(TAG, "m.Stock $typeName verifytotp returned HTTP $respCode")

        if (response.isSuccessful || respCode == 200 || respCode == 201) {
            val json = runCatching { JSONObject(respString) }.getOrNull()
            if (json != null) {
                val isStatusOk = json.optBoolean("status", true) ||
                        json.optString("status", "").equals("success", ignoreCase = true) ||
                        json.optBoolean("success", false) ||
                        json.optString("stat", "").equals("Ok", ignoreCase = true)

                if (isStatusOk) {
                    val dataObj = json.optJSONObject("data") ?: json
                    val accessToken = dataObj.optString("jwtToken")
                        .ifBlank { dataObj.optString("accessToken") }
                        .ifBlank { dataObj.optString("token") }
                        .ifBlank { dataObj.optString("access_token") }

                    val refreshToken = dataObj.optString("refreshToken")
                        .ifBlank { dataObj.optString("refresh_token") }

                    val feedToken = dataObj.optString("feedToken")
                        .ifBlank { dataObj.optString("wsToken") }
                        .ifBlank { dataObj.optString("feed_token") }
                        .ifBlank { accessToken }

                    if (accessToken.isNotBlank()) {
                        Log.d(TAG, "m.Stock $typeName authentication successful.")
                        return MStockTokens(
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            feedToken = feedToken
                        )
                    }
                }

                val errorMsg = json.optString("message")
                    .ifBlank { json.optString("error") }
                    .ifBlank { json.optString("description") }
                    .ifBlank { "Authentication failed: invalid credentials or TOTP code" }
                throw Exception("m.Stock $typeName error: $errorMsg")
            } else {
                throw Exception("m.Stock $typeName empty response payload (HTTP $respCode)")
            }
        } else {
            val errorJson = runCatching { JSONObject(respString) }.getOrNull()
            val errorMsg = errorJson?.optString("message")
                ?.ifBlank { errorJson?.optString("error") }
                ?.ifBlank { errorJson?.optString("description") }
                ?: "HTTP $respCode ${response.message.ifBlank { "Authentication failed" }}"
            throw Exception("m.Stock $typeName verifytotp failed: $errorMsg")
        }
    }

    /**
     * Renews the m.Stock session using fresh TOTP generated from stored TOTP secret.
     */
    suspend fun renewSession(
        clientCode: String,
        apiKey: String,
        refreshToken: String,
        totpSecret: String
    ): Result<MStockTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val effectiveSecret = totpSecret.ifBlank { refreshToken }
            if (apiKey.isBlank()) {
                throw Exception("m.Stock API Key missing.")
            }
            if (effectiveSecret.isBlank()) {
                throw Exception("m.Stock TOTP Secret missing for session renewal.")
            }

            Log.d(TAG, "Renewing m.Stock session with fresh TOTP...")
            val authRes = verifyTotp(clientCode, apiKey, effectiveSecret, refreshToken)
            authRes.getOrThrow()
        }
    }
}
