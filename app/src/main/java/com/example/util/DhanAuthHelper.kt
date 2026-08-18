package com.example.util

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object DhanAuthHelper {
    private const val TAG = "DhanAuth"

    suspend fun generateConsent(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "OAuth step started: Generating consent URL")

            val rawApiKey = BrokerConfig.dhanApiKey.trim()
            val rawClientId = BrokerConfig.dhanClientId.trim()
            val rawClientSecret = BrokerConfig.dhanClientSecret.trim()
            val rawRedirectUri = BrokerConfig.dhanRedirectUri.trim()

            val clientId = rawClientId
            val clientSecret = rawClientSecret
            val appId = if (rawApiKey.isNotBlank()) rawApiKey else rawClientId
            val redirectUri = if (rawRedirectUri.isNotBlank()) rawRedirectUri else "kingkhan://oauth/callback"
            val responseType = "code"
            val state = "kingkhan_oauth_state"

            Log.d(TAG, "  app_id present: ${appId.isNotBlank()}")
            Log.d(TAG, "  client_id present: ${clientId.isNotBlank()}")
            Log.d(TAG, "  client_secret present: ${clientSecret.isNotBlank()}")
            Log.d(TAG, "  redirect_uri: $redirectUri")

            if (clientId.isBlank()) {
                throw Exception("DHAN_CLIENT_ID missing in configuration.")
            }

            // Official Dhan OAuth generate-consent endpoint URL
            val urlBuilder = okhttp3.HttpUrl.Builder()
                .scheme("https")
                .host("auth.dhan.co")
                .addPathSegment("app")
                .addPathSegment("generate-consent")
                .addQueryParameter("client_id", clientId)

            val url = urlBuilder.build().toString()

            val jsonBody = JSONObject().apply {
                put("client_id", clientId)
                put("client_secret", clientSecret)
                put("app_id", appId)
                put("app_secret", clientSecret)
                put("redirect_uri", redirectUri)
                put("redirectUri", redirectUri)
                put("redirect_url", redirectUri)
                put("redirectUrl", redirectUri)
                put("response_type", responseType)
                put("state", state)
            }.toString()

            val reqBody = jsonBody.toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url(url)
                .post(reqBody)
                .addHeader("client_id", clientId)
                .addHeader("app_id", appId)
                .addHeader("app_secret", clientSecret)
                .addHeader("client_secret", clientSecret)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")

            val request = requestBuilder.build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            Log.d(TAG, "Dhan HTTP Response Code: ${response.code}")

            if (!response.isSuccessful) {
                throw Exception("Dhan generate-consent failed with HTTP ${response.code}")
            }

            val json = runCatching { JSONObject(respBody) }.getOrNull()
            val consentAppId = json?.optString("consentAppId")?.takeIf { it.isNotBlank() }
                ?: json?.optString("consentId")?.takeIf { it.isNotBlank() }

            if (consentAppId.isNullOrBlank()) {
                throw Exception("Dhan generate-consent succeeded but returned no consentAppId.")
            }

            val finalConsentUrl = "https://auth.dhan.co/login/consentApp-login?consentAppId=$consentAppId"
            Log.d(TAG, "Consent URL generated successfully")

            finalConsentUrl
        }
    }

    suspend fun exchangeToken(code: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val appId = BrokerConfig.dhanApiKey.trim()
            val appSecret = BrokerConfig.dhanClientSecret.trim()
            val clientId = BrokerConfig.dhanClientId.trim()
            
            Log.d(TAG, "Token exchange started")
            
            val url = okhttp3.HttpUrl.Builder()
                .scheme("https")
                .host("auth.dhan.co")
                .addPathSegment("app")
                .addPathSegment("consumeApp-consent")
                .addQueryParameter("tokenId", code)
                .build()
                .toString()

            val jsonBody = JSONObject().apply {
                put("tokenId", code)
            }.toString()
            
            val reqBody = jsonBody.toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url(url)
                .post(reqBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")

            if (clientId.isNotBlank()) {
                requestBuilder.addHeader("client_id", clientId)
            }
            if (appId.isNotBlank()) {
                requestBuilder.addHeader("app_id", appId)
            }
            if (appSecret.isNotBlank()) {
                requestBuilder.addHeader("app_secret", appSecret)
            }
                
            val client = OkHttpClient()
            val response = client.newCall(requestBuilder.build()).execute()
            
            val respBody = response.body?.string() ?: ""
            Log.d(TAG, "exchangeToken Response Code: ${response.code}")
            
            if (response.isSuccessful && respBody.isNotBlank()) {
                val json = JSONObject(respBody)
                val token = json.optString("accessToken").ifBlank {
                    json.optString("access_token")
                }.ifBlank {
                    json.optString("token")
                }
                if (token.isBlank()) {
                    Log.e(TAG, "token exchange success/failure: FAILURE (Access Token missing in response)")
                    throw Exception("Access Token missing in response")
                }
                Log.d(TAG, "token exchange success/failure: SUCCESS")
                token
            } else {
                Log.e(TAG, "token exchange success/failure: FAILURE (HTTP ${response.code})")
                throw Exception("Failed to exchange token. Code: ${response.code}")
            }
        }
    }
}


