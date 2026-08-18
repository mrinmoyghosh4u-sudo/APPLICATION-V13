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

    suspend fun generateSession(clientCode: String, mpin: String, totp: String): Result<AngelTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = BrokerConfig.angelApiKey
            
            Log.d(TAG, "Attempting Angel One login for client: $clientCode")
            
            if (apiKey.isBlank()) {
                throw Exception("ANGEL_ONE_API_KEY is missing.")
            }
            
            val url = "https://apiconnect.angelone.in/rest/auth/angelbroking/user/v1/loginByPassword"
            val reqBody = JSONObject().apply {
                put("clientcode", clientCode)
                put("password", mpin)
                put("totp", totp)
            }.toString().toRequestBody("application/json".toMediaType())
            
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
                
                if (jwtToken.isBlank()) {
                    throw Exception("JWT Token missing in response")
                }
                Log.d(TAG, "Angel One authentication successful")
                AngelTokens(jwtToken, refreshToken, feedToken)
            } else {
                val msg = json?.optString("message") ?: "Unknown error"
                throw Exception("Angel Login failed: $msg (Code: ${response.code})")
            }
        }
    }

    suspend fun renewSession(refreshToken: String): Result<AngelTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = BrokerConfig.angelApiKey
            if (apiKey.isBlank()) {
                throw Exception("ANGEL_ONE_API_KEY is missing.")
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
                    throw Exception("JWT Token missing in refresh response")
                }
                Log.d(TAG, "Angel One token refresh successful")
                AngelTokens(jwtToken, newRefreshToken, feedToken)
            } else {
                val msg = json?.optString("message") ?: "Unknown error"
                throw Exception("Angel token refresh failed: $msg (Code: ${response.code})")
            }
        }
    }
}
