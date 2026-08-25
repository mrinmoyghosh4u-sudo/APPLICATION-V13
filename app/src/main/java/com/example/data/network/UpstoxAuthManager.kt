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
    private val exchangeMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun exchangeAuthCode(authCode: String): Result<String> = withContext(Dispatchers.IO) {
        exchangeMutex.lock()
        try {
            runCatching {
                // If already authenticated and token valid, return existing token
                val existingToken = sessionManager.upstoxAccessToken
                if (!existingToken.isNullOrBlank() && sessionManager.isUpstoxConnected) {
                    val age = System.currentTimeMillis() - sessionManager.upstoxTokenTimestamp
                    if (age < 18 * 60 * 60 * 1000L) {
                        _authStatus.value = BrokerAuthStatus.CONNECTED
                        return@runCatching existingToken
                    }
                }

                val apiKey = sessionManager.upstoxApiKey.takeIf { it.isNotBlank() }
                    ?: com.example.util.BrokerConfig.upstoxApiKey.takeIf { it.isNotBlank() }
                    ?: throw Exception("Upstox API Key (client_id) is missing")
                val secret = sessionManager.upstoxApiSecret.takeIf { it.isNotBlank() }
                    ?: com.example.util.BrokerConfig.upstoxApiSecret.takeIf { it.isNotBlank() }
                val redirectUri = sessionManager.pendingOAuthSession?.redirectUri?.takeIf { it.isNotBlank() }
                    ?: sessionManager.upstoxRedirectUri.takeIf { it.isNotBlank() }
                    ?: UpstoxAuthHelper.DEFAULT_REDIRECT_URI

                Log.i(TAG, "[TOKEN_EXCHANGE_STARTED] Initiating Upstox authorization code exchange...")
                Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE] Initiating Upstox authorization code exchange...")

                var cleanCode = authCode.trim()
                if (cleanCode.contains("code=")) {
                    cleanCode = cleanCode.substringAfter("code=").substringBefore("&")
                }
                if (cleanCode.contains("auth_code=")) {
                    cleanCode = cleanCode.substringAfter("auth_code=").substringBefore("&")
                }

                var tokenBody: UpstoxTokenResponse? = null
                var directExchangeError: String? = null

                if (!secret.isNullOrBlank()) {
                    // Direct official Upstox OAuth token exchange (POST https://api.upstox.com/v2/login/authorization/token)
                    Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE] Exchanging code directly via official Upstox API endpoint...")
                    val directRes = try {
                        upstoxApi.getAccessToken(
                            code = cleanCode,
                            clientId = apiKey,
                            clientSecret = secret,
                            redirectUri = redirectUri
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "[UPSTOX_TOKEN_EXCHANGE_WARN] Direct token exchange exception: ${e.localizedMessage}")
                        null
                    }

                    if (directRes != null && directRes.isSuccessful && directRes.body()?.accessToken?.isNotBlank() == true) {
                        tokenBody = directRes.body()
                    } else if (directRes != null) {
                        val err = directRes.errorBody()?.string() ?: "HTTP ${directRes.code()}"
                        directExchangeError = err.take(150).replace("\n", " ")
                        Log.w(TAG, "[UPSTOX_TOKEN_EXCHANGE_WARN] Direct Upstox exchange returned error: $directExchangeError")
                    }
                }

                // Fallback to secure backend endpoint if direct failed or secret missing
                if (tokenBody == null) {
                    val backendBase = redirectUri.substringBefore("/oauth")
                    val tokenExchangeUrl = "$backendBase/api/token-exchange"
                    Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE] Exchanging code via secure backend endpoint: $tokenExchangeUrl...")
                    val response = try {
                        upstoxApi.exchangeTokenSecurely(
                            url = tokenExchangeUrl,
                            code = cleanCode,
                            redirectUri = redirectUri,
                            clientId = apiKey
                        )
                    } catch (e: Exception) {
                        null
                    }

                    if (response != null && response.isSuccessful && response.body()?.accessToken?.isNotBlank() == true) {
                        tokenBody = response.body()
                    } else {
                        val rawErr = response?.errorBody()?.string() ?: directExchangeError ?: "Token exchange failed"
                        val sanitizedErr = rawErr.take(150).replace("\n", " ")
                        _authStatus.value = BrokerAuthStatus.ERROR
                        Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Token exchange failed: $sanitizedErr")
                        throw Exception("TOKEN_EXCHANGE_FAILED: $sanitizedErr")
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
        } finally {
            exchangeMutex.unlock()
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
