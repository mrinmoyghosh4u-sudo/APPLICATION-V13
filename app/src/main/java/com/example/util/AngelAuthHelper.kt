package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AngelAuthHelper {
    private const val TAG = "AngelAuth"

    data class AngelTokens(val jwtToken: String, val refreshToken: String, val feedToken: String)

    suspend fun generateSession(
        clientCode: String,
        mpin: String,
        totpOrSecret: String,
        customApiKey: String = ""
    ): Result<AngelTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = customApiKey.ifBlank { BrokerConfig.angelApiKey }
            
            Log.d(TAG, "Attempting Angel One login for client: ${clientCode.take(2)}***${clientCode.takeLast(2)}")
            
            if (apiKey.isBlank()) {
                throw Exception("Angel One API Key is missing. Please configure your API Key.")
            }

            val sanitizedTotpInput = totpOrSecret.trim()
            val effectiveTotp = if (sanitizedTotpInput.length == 6 && sanitizedTotpInput.all { it.isDigit() }) {
                sanitizedTotpInput
            } else {
                val generated = TotpUtil.generateTotp(sanitizedTotpInput)
                if (generated.isBlank()) {
                    throw Exception("Failed to generate valid 6-digit TOTP from provided secret. Verify Base32 TOTP secret.")
                }
                generated
            }
            
            val url = "https://apiconnect.angelone.in/rest/auth/angelbroking/user/v1/loginByPassword"
            val reqBody = JSONObject().apply {
                put("clientcode", clientCode.trim())
                put("password", mpin.trim())
                put("totp", effectiveTotp)
            }.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(reqBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("X-PrivateKey", apiKey.trim())
                .addHeader("X-UserType", "USER")
                .addHeader("X-SourceID", "WEB")
                .addHeader("X-ClientLocalIP", "127.0.0.1")
                .addHeader("X-ClientPublicIP", "127.0.0.1")
                .addHeader("X-MACAddress", "00:00:00:00:00:00")
                .build()
            
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            
            val respBody = response.body?.string() ?: ""
            Log.d(TAG, "Angel Login HTTP status: ${response.code}")
            
            val json = runCatching { JSONObject(respBody) }.getOrNull()
            
            if (response.isSuccessful && json?.optBoolean("status") == true) {
                val data = json.optJSONObject("data") ?: json
                val jwtToken = data.optString("jwtToken")
                val refreshToken = data.optString("refreshToken")
                val feedToken = data.optString("feedToken")
                
                if (jwtToken.isBlank() || refreshToken.isBlank() || feedToken.isBlank()) {
                    val missing = mutableListOf<String>()
                    if (jwtToken.isBlank()) missing.add("jwtToken")
                    if (refreshToken.isBlank()) missing.add("refreshToken")
                    if (feedToken.isBlank()) missing.add("feedToken")
                    throw Exception("Angel One Login Failed\nError Code: DATA_INCOMPLETE\nMessage: Missing token data: ${missing.joinToString(", ")}")
                }
                Log.d(TAG, "Angel One authentication successful")
                AngelTokens(jwtToken, refreshToken, feedToken)
            } else {
                val errCode = json?.optString("errorcode")?.ifBlank { json?.optString("errorCode") }?.ifBlank { "HTTP_${response.code}" } ?: "HTTP_${response.code}"
                val msg = json?.optString("message")?.ifBlank { json?.optString("error") }?.ifBlank { "Authentication failed" } ?: "Authentication failed (HTTP ${response.code})"
                throw Exception("Angel One Login Failed\nError Code: $errCode\nMessage: $msg")
            }
        }
    }

    suspend fun renewSession(refreshToken: String, customApiKey: String = ""): Result<AngelTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = customApiKey.ifBlank { BrokerConfig.angelApiKey }
            if (apiKey.isBlank()) {
                throw Exception("Angel One API Key is missing.")
            }
            val url = "https://apiconnect.angelone.in/rest/auth/angelbroking/jwt/v1/generateTokens"
            val reqBody = JSONObject().apply {
                put("refreshToken", refreshToken)
            }.toString().toRequestBody("application/json".toMediaType())

            val bearerToken = if (refreshToken.startsWith("Bearer ", ignoreCase = true)) refreshToken else "Bearer $refreshToken"

            val request = Request.Builder()
                .url(url)
                .post(reqBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("X-PrivateKey", apiKey)
                .addHeader("X-UserType", "USER")
                .addHeader("X-SourceID", "WEB")
                .addHeader("X-ClientLocalIP", "127.0.0.1")
                .addHeader("X-ClientPublicIP", "127.0.0.1")
                .addHeader("X-MACAddress", "00:00:00:00:00:00")
                .addHeader("Authorization", bearerToken)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            Log.d(TAG, "Angel Token Refresh HTTP status: ${response.code}")

            val json = runCatching { JSONObject(respBody) }.getOrNull()

            if (response.isSuccessful && json?.optBoolean("status") == true) {
                val data = json.optJSONObject("data") ?: json
                val jwtToken = data.optString("jwtToken")
                val newRefreshToken = data.optString("refreshToken").ifBlank { refreshToken }
                val feedToken = data.optString("feedToken")

                if (jwtToken.isBlank()) {
                    throw Exception("Angel One Token Refresh Failed\nError Code: JWT_MISSING\nMessage: JWT Token missing in refresh response")
                }
                Log.d(TAG, "Angel One token refresh successful")
                AngelTokens(jwtToken, newRefreshToken, feedToken)
            } else {
                val errCode = json?.optString("errorcode")?.ifBlank { json?.optString("errorCode") }?.ifBlank { "HTTP_${response.code}" } ?: "HTTP_${response.code}"
                val msg = json?.optString("message")?.ifBlank { json?.optString("error") }?.ifBlank { "Token refresh failed" } ?: "Token refresh failed (HTTP ${response.code})"
                throw Exception("Angel One Token Refresh Failed\nError Code: $errCode\nMessage: $msg")
            }
        }
    }
}
