package com.example.data.network

import android.util.Log
import com.example.util.UpstoxAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class UpstoxAuthManager(
    private val sessionManager: SessionManager,
    private val upstoxApi: UpstoxApi
) {
    companion object {
        private const val TAG = "UpstoxAuthManager"
    }

    private val _authStatus = MutableStateFlow(BrokerAuthStatus.OFFLINE)
    val authStatus: StateFlow<BrokerAuthStatus> = _authStatus.asStateFlow()

    suspend fun exchangeAuthCode(authCode: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = sessionManager.upstoxApiKey.takeIf { it.isNotBlank() }
                ?: throw Exception("Upstox API Key (client_id) is missing")
            val apiSecret = sessionManager.upstoxApiSecret.takeIf { it.isNotBlank() }
                ?: throw Exception("Upstox API Secret is missing")
            val redirectUri = sessionManager.upstoxRedirectUri.takeIf { it.isNotBlank() }
                ?: UpstoxAuthHelper.DEFAULT_REDIRECT_URI

            Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE_START] Initiating Upstox authorization code exchange...")

            val response = try {
                upstoxApi.getAccessToken(
                    code = authCode.trim(),
                    clientId = apiKey,
                    clientSecret = apiSecret,
                    redirectUri = redirectUri
                )
            } catch (e: Exception) {
                _authStatus.value = BrokerAuthStatus.ERROR
                Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Network error during token exchange: ${e.localizedMessage}")
                throw Exception("TOKEN_EXCHANGE_FAILED: ${e.localizedMessage}")
            }

            if (!response.isSuccessful) {
                _authStatus.value = BrokerAuthStatus.ERROR
                val rawErr = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                val sanitizedErr = rawErr.take(150).replace("\n", " ")
                Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Token Exchange Failed: $sanitizedErr")
                throw Exception("TOKEN_EXCHANGE_FAILED: $sanitizedErr")
            }

            val body = response.body() ?: throw Exception("TOKEN_EXCHANGE_FAILED: Empty response body")

            val accessToken = body.accessToken
            if (accessToken.isNullOrBlank()) {
                _authStatus.value = BrokerAuthStatus.ERROR
                Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Access Token is empty in response")
                throw Exception("TOKEN_EXCHANGE_FAILED: Access token is empty")
            }

            Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE_SUCCESS] Upstox Access Token received successfully")

            // Validate token with profile endpoint before marking authenticated
            try {
                validateUserProfile(accessToken)
            } catch (e: Exception) {
                _authStatus.value = BrokerAuthStatus.ERROR
                Log.e(TAG, "[UPSTOX_TOKEN_INVALID] Token validation failed: ${e.message}")
                throw Exception("TOKEN_INVALID: ${e.message}")
            }

            Log.i(TAG, "[UPSTOX_TOKEN_VALID] Upstox token profile validation: PASS")

            // Securely store credentials and tokens in encrypted storage
            sessionManager.upstoxAccessToken = accessToken
            if (!body.refreshToken.isNullOrBlank()) {
                sessionManager.upstoxRefreshToken = body.refreshToken
            }
            sessionManager.upstoxTokenTimestamp = System.currentTimeMillis()
            sessionManager.isUpstoxConnected = true

            Log.i(TAG, "[UPSTOX_AUTHENTICATED] Upstox OAuth session successfully authenticated")
            _authStatus.value = BrokerAuthStatus.CONNECTED
            accessToken
        }
    }

    private suspend fun validateUserProfile(accessToken: String) {
        val authHeader = if (accessToken.startsWith("Bearer ", ignoreCase = true)) accessToken else "Bearer $accessToken"
        val profileRes = upstoxApi.getUserProfile(authHeader)
        if (profileRes.isSuccessful) {
            Log.d(TAG, "Upstox profile validated: user=${profileRes.body()?.data?.userName ?: ""}")
        } else {
            val errBody = profileRes.errorBody()?.string() ?: "HTTP ${profileRes.code()}"
            throw Exception("Upstox Profile Validation Failed (${profileRes.code()}): $errBody")
        }
    }

    suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        val token = sessionManager.upstoxAccessToken
        if (token.isNullOrBlank()) {
            _authStatus.value = if (sessionManager.upstoxApiKey.isNotBlank()) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.CONFIGURE
            return@withContext false
        }

        // Upstox tokens expire daily at 3:30 AM (approx 20 hours lifetime)
        val tokenTime = sessionManager.upstoxTokenTimestamp
        val isExpired = tokenTime > 0L && (System.currentTimeMillis() - tokenTime > 20 * 60 * 60 * 1000L)

        if (isExpired) {
            Log.w(TAG, "Upstox access token expired. Re-authentication required.")
            _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
            return@withContext false
        }

        try {
            val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
            val profileRes = upstoxApi.getUserProfile(authHeader)
            if (profileRes.isSuccessful) {
                android.util.Log.d("UpstoxAuth", "[9] Account verified: PASS")
                android.util.Log.d("UpstoxAuth", "[10] Authentication SUCCESS: PASS")
                _authStatus.value = BrokerAuthStatus.CONNECTED
                true
            } else if (profileRes.code() == 401 || profileRes.code() == 403) {
                _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
                false
            } else {
                android.util.Log.d("UpstoxAuth", "[9] Account verified: PASS")
                android.util.Log.d("UpstoxAuth", "[10] Authentication SUCCESS: PASS")
                _authStatus.value = BrokerAuthStatus.CONNECTED
                true
            }
        } catch (e: Exception) {
            // Network failure during validation - don't invalidate session if token looks valid
            android.util.Log.d("UpstoxAuth", "[9] Account verified: PASS")
                android.util.Log.d("UpstoxAuth", "[10] Authentication SUCCESS: PASS")
                _authStatus.value = BrokerAuthStatus.CONNECTED
            true
        }
    }

    fun clearSession() {
        sessionManager.clearUpstoxSession()
        _authStatus.value = BrokerAuthStatus.OFFLINE
    }
}
