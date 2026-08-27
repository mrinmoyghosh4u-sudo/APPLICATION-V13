package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Strict Type A ONLY API Authentication:
 * - Official Type A Endpoint: POST https://api.mstock.trade/openapi/typea/session/verifytotp
 * 
 * Strict Security Rules:
 * - Local RFC 6238 6-digit TOTP generation from secure Base32 TOTP Secret
 * - Raw TOTP secret is NEVER transmitted over the wire
 * - Raw tokens and API keys are NEVER printed in public logs
 * - Credentials and session tokens are securely persisted in Android Encrypted Preferences
 */
object MStockAuthHelper {
    private const val TAG = "MStockAuth"

    const val TYPE_A_VERIFY_TOTP_URL = "https://api.mstock.trade/openapi/typea/session/verifytotp"

    private val _lastEndpoint = MutableStateFlow(TYPE_A_VERIFY_TOTP_URL)
    val lastEndpoint: StateFlow<String> = _lastEndpoint.asStateFlow()

    private val _lastHttpStatus = MutableStateFlow("Not Executed")
    val lastHttpStatus: StateFlow<String> = _lastHttpStatus.asStateFlow()

    private val _authStage = MutableStateFlow("IDLE")
    val authStage: StateFlow<String> = _authStage.asStateFlow()

    private val _lastAuthMessage = MutableStateFlow("Idle")
    val lastAuthMessage: StateFlow<String> = _lastAuthMessage.asStateFlow()

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
     * Executes official m.Stock Type A TOTP authentication using clientCode, apiKey, and TOTP secret/code.
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

            _authStage.value = "INITIALIZING"
            _lastAuthMessage.value = "Validating input parameters"

            if (sanitizedApiKey.isBlank()) {
                _authStage.value = "FAILED"
                _lastAuthMessage.value = "m.Stock API Key is required"
                throw Exception("m.Stock API Key is required.")
            }
            if (sanitizedTotpInput.isBlank()) {
                _authStage.value = "FAILED"
                _lastAuthMessage.value = "m.Stock TOTP Secret or 6-digit TOTP code is required"
                throw Exception("m.Stock TOTP Secret or 6-digit TOTP code is required.")
            }

            Log.d(TAG, "Initiating m.Stock Type A TOTP authentication")

            // 1. Generate standard 6-digit RFC 6238 TOTP locally if TOTP secret key is provided
            _authStage.value = "GENERATING_TOTP"
            val effectiveTotp = if (sanitizedTotpInput.length == 6 && sanitizedTotpInput.all { it.isDigit() }) {
                sanitizedTotpInput
            } else {
                val generated = TotpUtil.generateTotp(sanitizedTotpInput)
                if (generated.isBlank() || generated.length != 6 || !generated.all { it.isDigit() }) {
                    _authStage.value = "FAILED"
                    _lastAuthMessage.value = "Failed to generate 6-digit TOTP from secret"
                    throw Exception("Failed to generate valid 6-digit TOTP. Please verify your m.Stock Base32 TOTP Secret.")
                }
                generated
            }

            // 2. Execute Official Type A Authentication ONLY
            _lastEndpoint.value = TYPE_A_VERIFY_TOTP_URL
            _authStage.value = "VERIFYING_TOTP_TYPE_A"
            Log.d(TAG, "Executing m.Stock Type A authentication...")
            val tokens = executeTypeAAuth(sanitizedApiKey, effectiveTotp, sanitizedClientCode)
            _authStage.value = "AUTHENTICATED"
            _lastAuthMessage.value = "Success (Type A Tokens Stored)"
            tokens
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

        val bodyString = formBodyBuilder.toString()
        _lastEndpoint.value = TYPE_A_VERIFY_TOTP_URL
        val body = bodyString.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
            .url(TYPE_A_VERIFY_TOTP_URL)
            .post(body)
            .addHeader("X-Mirae-Version", "1")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build()

        return parseResponseAndExtractTokens(request)
    }

    private fun parseResponseAndExtractTokens(request: Request): MStockTokens {
        val response = httpClient.newCall(request).execute()
        val respCode = response.code
        val respString = response.body?.string() ?: ""

        _lastHttpStatus.value = "$respCode ${response.message.ifBlank { if (response.isSuccessful) "OK" else "Error" }}"
        Log.d(TAG, "m.Stock Type A verifytotp returned HTTP $respCode")

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
                        Log.d(TAG, "m.Stock Type A authentication successful.")
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
                _authStage.value = "FAILED"
                _lastAuthMessage.value = errorMsg
                throw Exception("m.Stock Type A error: $errorMsg")
            } else {
                _authStage.value = "FAILED"
                _lastAuthMessage.value = "Empty response payload (HTTP $respCode)"
                throw Exception("m.Stock Type A empty response payload (HTTP $respCode)")
            }
        } else {
            val errorJson = runCatching { JSONObject(respString) }.getOrNull()
            val apiErrCode = errorJson?.optString("errorcode")?.ifBlank { errorJson?.optString("errorCode") } ?: "HTTP_$respCode"
            val errorMsg = errorJson?.optString("message")
                ?.ifBlank { errorJson?.optString("error") }
                ?.ifBlank { errorJson?.optString("description") }
                ?: if (respCode == 404) "Endpoint Not Found (404)" else "HTTP $respCode ${response.message.ifBlank { "Authentication failed" }}"
            _authStage.value = "FAILED"
            _lastAuthMessage.value = "$apiErrCode: $errorMsg"
            throw Exception("m.Stock Login Failed\nHTTP Status: $respCode\nError Code: $apiErrCode\nMessage: $errorMsg")
        }
    }

    /**
     * Renews the m.Stock session using fresh TOTP generated from stored TOTP secret via Type A.
     */
    suspend fun renewSession(
        clientCode: String,
        apiKey: String,
        totpSecret: String
    ): Result<MStockTokens> = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) {
                throw Exception("m.Stock API Key missing.")
            }
            if (totpSecret.isBlank()) {
                throw Exception("m.Stock TOTP Secret missing for session renewal.")
            }

            Log.d(TAG, "Renewing m.Stock Type A session with fresh TOTP...")
            val authRes = verifyTotp(clientCode, apiKey, totpSecret)
            authRes.getOrThrow()
        }
    }
}
