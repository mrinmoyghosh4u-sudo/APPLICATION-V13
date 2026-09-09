package com.example.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom

object DhanAuthHelper {
    private const val TAG = "DhanAuth"

    private val httpClient = OkHttpClient.Builder().build()
    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure, unpredictable random state token for Dhan OAuth.
     */
    fun generateSecureState(): String {
        val randomBytes = ByteArray(24)
        secureRandom.nextBytes(randomBytes)
        return "dhan_" + Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Generates official Dhan OAuth consent URL.
     * Uses ONLY the parameters required by the official Dhan OAuth specification.
     */
    suspend fun generateConsent(
        clientIdOverride: String? = null,
        apiKeyOverride: String? = null,
        clientSecretOverride: String? = null,
        state: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "OAuth step started: Generating Dhan consent URL")

            val rawApiKey = apiKeyOverride?.trim()?.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanApiKey.trim()
            val rawClientId = clientIdOverride?.trim()?.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanClientId.trim()
            val rawClientSecret = clientSecretOverride?.trim()?.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanClientSecret.trim()
            val rawRedirectUri = BrokerConfig.dhanRedirectUri.trim()

            val clientId = rawClientId
            val appId = if (rawApiKey.isNotBlank()) rawApiKey else rawClientId
            val redirectUri = if (rawRedirectUri.isNotBlank()) rawRedirectUri else "kingkhan://oauth/callback"
            val responseType = "code"
            val effectiveState = state.trim().ifBlank { generateSecureState() }

            if (clientId.isBlank()) {
                throw Exception("DHAN_CLIENT_ID missing in configuration.")
            }

            // Using proxy to secure the client_secret and app_secret
            val url = "https://application-beige-psi.vercel.app/dhan-token-exchange"

            val jsonBody = JSONObject().apply {
                put("action", "generate-consent")
                put("redirect_uri", redirectUri)
                put("state", effectiveState)
            }.toString()

            val reqBody = jsonBody.toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url(url)
                .post(reqBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            Log.d(TAG, "Dhan generate-consent proxy HTTP Response Code: ${response.code}")

            if (!response.isSuccessful) {
                val errorJson = runCatching { JSONObject(respBody) }.getOrNull()
                val errorMsg = errorJson?.optString("errorMessage")?.takeIf { it.isNotBlank() }
                    ?: errorJson?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "HTTP ${response.code}"
                throw Exception("Dhan generate-consent failed ($errorMsg)")
            }

            val json = runCatching { JSONObject(respBody) }.getOrNull()
            val consentAppId = json?.optString("consentAppId")?.takeIf { it.isNotBlank() }
                ?: json?.optString("consentId")?.takeIf { it.isNotBlank() }

            if (consentAppId.isNullOrBlank()) {
                throw Exception("Dhan generate-consent succeeded but returned no consentAppId.")
            }

            val finalConsentUrl = "https://auth.dhan.co/login/consentApp-login?consentAppId=$consentAppId"
            Log.d(TAG, "Dhan consent URL generated successfully")

            finalConsentUrl
        }
    }

    /**
     * Exchanges Dhan tokenId for accessToken using official Dhan consumeApp-consent endpoint.
     */
    suspend fun exchangeToken(
        code: String,
        clientIdOverride: String? = null,
        apiKeyOverride: String? = null,
        clientSecretOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val appId = apiKeyOverride?.trim()?.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanApiKey.trim()
            val appSecret = clientSecretOverride?.trim()?.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanClientSecret.trim()
            val clientId = clientIdOverride?.trim()?.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanClientId.trim()

            if (code.isBlank()) {
                throw Exception("Dhan tokenId missing for token exchange.")
            }

            Log.i(TAG, "[DHAN_TOKEN_EXCHANGE_STARTED] Executing token exchange...")

            val url = "https://application-beige-psi.vercel.app/dhan-token-exchange"

            val jsonBody = JSONObject().apply {
                put("action", "consume-consent")
                put("tokenId", code.trim())
            }.toString()

            val reqBody = jsonBody.toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url(url)
                .post(reqBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val respBody = response.body?.string() ?: ""
            Log.d(TAG, "Dhan consumeApp-consent proxy HTTP Response Code: ${response.code}")

            if (response.isSuccessful && respBody.isNotBlank()) {
                val json = JSONObject(respBody)
                val accessToken = json.optString("accessToken").ifBlank {
                    json.optString("token")
                }.trim()

                if (accessToken.isBlank()) {
                    Log.e(TAG, "Dhan token exchange failed: Access token missing in response payload")
                    throw Exception("Access Token missing in Dhan response payload")
                }
                Log.i(TAG, "[DHAN_TOKEN_EXCHANGE_SUCCESS] Token exchange completed successfully")
                accessToken
            } else {
                val errorJson = runCatching { JSONObject(respBody) }.getOrNull()
                val errorMsg = errorJson?.optString("errorMessage")?.takeIf { it.isNotBlank() }
                    ?: errorJson?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "HTTP ${response.code}"
                Log.e(TAG, "Dhan token exchange failed (HTTP ${response.code})")
                throw Exception("Failed to exchange Dhan token: $errorMsg")
            }
        }
    }
}


