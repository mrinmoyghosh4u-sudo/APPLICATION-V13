cat << 'INNER' > app/src/main/java/com/example/util/AngelAuthHelper.kt
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

    suspend fun exchangeToken(code: String): Result<AngelTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = BrokerConfig.angelApiKey
            
            Log.d(TAG, "Loaded configuration - API Key: ${apiKey.take(4)}...")
            Log.d(TAG, "Token Exchange - Refresh Token (Code): $code")
            
            if (apiKey.isBlank()) {
                throw Exception("ANGEL_ONE_API_KEY is missing. Cannot exchange code.")
            }
            
            val url = "https://apiconnect.angelone.in/rest/auth/angelbroking/jwt/v1/generateTokens"
            val reqBody = JSONObject().apply {
                put("refreshToken", code)
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
            Log.d(TAG, "Token exchange response: $respBody")
            
            val json = runCatching { JSONObject(respBody) }.getOrNull()
            
            if (response.isSuccessful && json?.optBoolean("status") == true) {
                val data = json.optJSONObject("data") ?: json
                val jwtToken = data.optString("jwtToken")
                val refreshToken = data.optString("refreshToken")
                val feedToken = data.optString("feedToken")
                
                if (jwtToken.isBlank()) {
                    throw Exception("JWT Token missing in response")
                }
                AngelTokens(jwtToken, refreshToken, feedToken)
            } else {
                val msg = json?.optString("message") ?: "Unknown error"
                val errorCode = json?.optString("errorcode") ?: ""
                
                if (msg.contains("Invalid Token", ignoreCase = true) || errorCode == "AB1041" || msg.contains("API key", ignoreCase = true)) {
                    throw Exception("Failed to verify token. Make sure you are using a Publisher API Key (not a Trading API Key). Error: $msg")
                }
                throw Exception("Angel Token exchange failed: $msg (Code: ${response.code})")
            }
        }
    }
}
INNER
