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

            Log.d(TAG, "Exchanging Upstox authorization code...")

            val response = upstoxApi.getAccessToken(
                code = authCode.trim(),
                clientId = apiKey,
                clientSecret = apiSecret,
                redirectUri = redirectUri
            )

            if (!response.isSuccessful) {
                _authStatus.value = BrokerAuthStatus.ERROR
                val errBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                throw Exception("Upstox Token Exchange Failed: $errBody")
            }

            val body = response.body() ?: throw Exception("Empty response body from Upstox")

            val accessToken = body.accessToken
            if (accessToken.isNullOrBlank()) {
                _authStatus.value = BrokerAuthStatus.ERROR
                throw Exception("Upstox Access Token is empty in response")
            }

            // Securely store credentials and tokens in encrypted storage
            sessionManager.upstoxAccessToken = accessToken
            if (!body.refreshToken.isNullOrBlank()) {
                sessionManager.upstoxRefreshToken = body.refreshToken
            }
            sessionManager.upstoxTokenTimestamp = System.currentTimeMillis()
            sessionManager.isUpstoxConnected = true

            // Validate with user profile check
            validateUserProfile(accessToken)

            _authStatus.value = BrokerAuthStatus.CONNECTED
            Log.d(TAG, "Upstox authenticated successfully")
            accessToken
        }
    }

    private suspend fun validateUserProfile(accessToken: String) {
        try {
            val authHeader = if (accessToken.startsWith("Bearer ", ignoreCase = true)) accessToken else "Bearer $accessToken"
            val profileRes = upstoxApi.getUserProfile(authHeader)
            if (profileRes.isSuccessful) {
                Log.d(TAG, "Upstox profile validated: user=${profileRes.body()?.data?.userName ?: ""}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upstox profile validation notice: ${e.message}")
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
                _authStatus.value = BrokerAuthStatus.CONNECTED
                true
            } else if (profileRes.code() == 401 || profileRes.code() == 403) {
                _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
                false
            } else {
                _authStatus.value = BrokerAuthStatus.CONNECTED
                true
            }
        } catch (e: Exception) {
            // Network failure during validation - don't invalidate session if token looks valid
            _authStatus.value = BrokerAuthStatus.CONNECTED
            true
        }
    }

    fun clearSession() {
        sessionManager.clearUpstoxSession()
        _authStatus.value = BrokerAuthStatus.OFFLINE
    }
}
