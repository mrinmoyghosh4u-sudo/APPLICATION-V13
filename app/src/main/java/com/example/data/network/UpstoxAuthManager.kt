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
            val redirectUri = sessionManager.upstoxRedirectUri.takeIf { it.isNotBlank() }
                ?: UpstoxAuthHelper.DEFAULT_REDIRECT_URI

            Log.i(TAG, "[TOKEN_EXCHANGE_STARTED] Initiating Upstox authorization code exchange via backend...")
            Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE] Initiating Upstox authorization code exchange via backend...")

            val backendBase = UpstoxAuthHelper.DEFAULT_REDIRECT_URI.substringBefore("/oauth")
            val tokenExchangeUrl = "$backendBase/api/token-exchange"

            var tokenBody: UpstoxTokenResponse? = null
            var lastErrorString: String? = null

            // 1. Try secure backend token exchange first
            try {
                val response = upstoxApi.exchangeTokenSecurely(
                    url = tokenExchangeUrl,
                    code = authCode.trim(),
                    redirectUri = redirectUri,
                    clientId = apiKey
                )
                if (response.isSuccessful && response.body()?.accessToken?.isNotBlank() == true) {
                    tokenBody = response.body()
                } else {
                    val rawErr = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                    lastErrorString = rawErr.take(200).replace("\n", " ")
                    Log.w(TAG, "[UPSTOX_TOKEN_EXCHANGE] Backend exchange returned $lastErrorString, trying direct official API fallback...")
                }
            } catch (e: Exception) {
                lastErrorString = e.localizedMessage
                Log.w(TAG, "[UPSTOX_TOKEN_EXCHANGE] Backend exchange error: ${e.message}, trying direct official API fallback...")
            }

            // 2. Fallback to direct official Upstox token exchange endpoint if backend was 404 or unavailable
            if (tokenBody == null) {
                val secret = sessionManager.upstoxApiSecret.takeIf { it.isNotBlank() }
                    ?: com.example.util.BrokerConfig.upstoxApiSecret.takeIf { it.isNotBlank() }
                if (!secret.isNullOrBlank()) {
                    Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE] Exchanging code directly with official Upstox OAuth API...")
                    val directRes = try {
                        upstoxApi.getAccessToken(
                            code = authCode.trim(),
                            clientId = apiKey,
                            clientSecret = secret,
                            redirectUri = redirectUri
                        )
                    } catch (e: Exception) {
                        _authStatus.value = BrokerAuthStatus.ERROR
                        Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Direct Upstox exchange failed: ${e.localizedMessage}")
                        throw Exception("TOKEN_EXCHANGE_FAILED: ${e.localizedMessage}")
                    }

                    if (directRes.isSuccessful && directRes.body()?.accessToken?.isNotBlank() == true) {
                        tokenBody = directRes.body()
                    } else {
                        val err = directRes.errorBody()?.string() ?: "HTTP ${directRes.code()}"
                        val sanitizedErr = err.take(150).replace("\n", " ")
                        _authStatus.value = BrokerAuthStatus.ERROR
                        Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Direct Upstox exchange error: $sanitizedErr")
                        throw Exception("TOKEN_EXCHANGE_FAILED: $sanitizedErr")
                    }
                } else {
                    _authStatus.value = BrokerAuthStatus.ERROR
                    Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Token exchange failed and Upstox API secret not found for direct fallback: $lastErrorString")
                    throw Exception("TOKEN_EXCHANGE_FAILED: $lastErrorString")
                }
            }

            val body = tokenBody ?: throw Exception("TOKEN_EXCHANGE_FAILED: Empty response body")

            val accessToken = body.accessToken
            if (accessToken.isNullOrBlank()) {
                _authStatus.value = BrokerAuthStatus.ERROR
                Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Access Token is empty in response")
                throw Exception("TOKEN_EXCHANGE_FAILED: Access token is empty")
            }

            Log.i(TAG, "[TOKEN_EXCHANGE_SUCCESS] Upstox Access Token received successfully")
            Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE_SUCCESS] Upstox Access Token received successfully")

            // Validate token with profile endpoint before marking authenticated
            try {
                validateUserProfile(accessToken)
            } catch (e: Exception) {
                _authStatus.value = BrokerAuthStatus.ERROR
                Log.e(TAG, "[UPSTOX_PROFILE_VALIDATION_FAILED] Token validation failed: ${e.message}")
                throw Exception("PROFILE_VALIDATION_FAILED: ${e.message}")
            }

            Log.i(TAG, "[PROFILE_VALIDATED] Upstox token profile validation passed")
            Log.i(TAG, "[UPSTOX_TOKEN_VALIDATED] Upstox token profile validation: PASS")

            // Securely store credentials and tokens in encrypted storage
            sessionManager.upstoxAccessToken = accessToken
            if (!body.refreshToken.isNullOrBlank()) {
                sessionManager.upstoxRefreshToken = body.refreshToken
            }
            sessionManager.upstoxTokenTimestamp = System.currentTimeMillis()
            sessionManager.isUpstoxConnected = true

            Log.i(TAG, "[BROKER_CONNECTED] Upstox OAuth session successfully connected and authenticated")
            Log.i(TAG, "[UPSTOX_AUTHENTICATED] Upstox OAuth session successfully authenticated")
            _authStatus.value = BrokerAuthStatus.CONNECTED
            accessToken
        }
    }

    private suspend fun validateUserProfile(accessToken: String) {
        val authHeader = if (accessToken.startsWith("Bearer ", ignoreCase = true)) accessToken else "Bearer $accessToken"
        val profileRes = upstoxApi.getUserProfile(authHeader)
        if (profileRes.isSuccessful) {
            val body = profileRes.body()
            if (body == null || !body.status.equals("success", ignoreCase = true)) {
                val statusMsg = body?.status ?: "null"
                throw Exception("Upstox Profile Validation Failed: response status is '$statusMsg'")
            }
            Log.d(TAG, "Upstox profile validated: user=${body.data?.userName ?: ""}")
        } else {
            val errBody = profileRes.errorBody()?.string() ?: "HTTP ${profileRes.code()}"
            throw Exception("Upstox Profile Validation Failed (${profileRes.code()}): $errBody")
        }
    }

    suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        val token = sessionManager.upstoxAccessToken
        if (token.isNullOrBlank()) {
            sessionManager.isUpstoxConnected = false
            _authStatus.value = if (sessionManager.upstoxApiKey.isNotBlank()) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.CONFIGURE
            return@withContext false
        }

        // Upstox tokens expire daily at 3:30 AM (approx 20 hours lifetime)
        val tokenTime = sessionManager.upstoxTokenTimestamp
        val isExpired = tokenTime > 0L && (System.currentTimeMillis() - tokenTime > 20 * 60 * 60 * 1000L)

        if (isExpired) {
            Log.w(TAG, "Upstox access token expired. Re-authentication required.")
            sessionManager.isUpstoxConnected = false
            _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
            return@withContext false
        }

        try {
            val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
            val profileRes = upstoxApi.getUserProfile(authHeader)
            if (profileRes.isSuccessful && profileRes.body()?.status?.equals("success", ignoreCase = true) == true) {
                android.util.Log.d("UpstoxAuth", "[9] Account verified: PASS")
                android.util.Log.d("UpstoxAuth", "[10] Authentication SUCCESS: PASS")
                sessionManager.isUpstoxConnected = true
                _authStatus.value = BrokerAuthStatus.CONNECTED
                true
            } else {
                sessionManager.isUpstoxConnected = false
                val code = profileRes.code()
                if (code == 401 || code == 403) {
                    _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
                } else {
                    _authStatus.value = BrokerAuthStatus.ERROR
                }
                false
            }
        } catch (e: Exception) {
            sessionManager.isUpstoxConnected = false
            _authStatus.value = BrokerAuthStatus.ERROR
            false
        }
    }

    fun clearSession() {
        sessionManager.clearUpstoxSession()
        _authStatus.value = BrokerAuthStatus.OFFLINE
    }
}
