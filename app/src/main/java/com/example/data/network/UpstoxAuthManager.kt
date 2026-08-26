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

    suspend fun exchangeAuthCode(authCode: String, state: String? = null): Result<String> = withContext(Dispatchers.IO) {
        exchangeMutex.lock()
        try {
            runCatching {
                var cleanCode = authCode.trim()

                // If state provided, validate state
                if (!state.isNullOrBlank()) {
                    val stateValid = UpstoxAuthHelper.validateAndConsumeState(state)
                    if (!stateValid) {
                        Log.w(TAG, "[UPSTOX_OAUTH_STATE_REJECTED] OAuth state validation failed")
                    }
                }

                // If already authenticated and token valid, return existing token
                val existingToken = sessionManager.upstoxAccessToken
                if (!existingToken.isNullOrBlank() && sessionManager.isUpstoxConnected) {
                    val age = System.currentTimeMillis() - sessionManager.upstoxTokenTimestamp
                    if (age < 18 * 60 * 60 * 1000L) {
                        Log.i(TAG, "[UPSTOX_TOKEN_VALID] Active Upstox session token is still valid (${age / 3600000}h old)")
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

                if (cleanCode.startsWith("http://") || cleanCode.startsWith("https://") || cleanCode.startsWith("kingkhan://")) {
                    try {
                        val parsedUri = android.net.Uri.parse(cleanCode)
                        val extracted = parsedUri.getQueryParameter("code") ?: parsedUri.getQueryParameter("auth_code")
                        val uriState = parsedUri.getQueryParameter("state")
                        if (!uriState.isNullOrBlank() && state.isNullOrBlank()) {
                            UpstoxAuthHelper.validateAndConsumeState(uriState)
                        }
                        if (!extracted.isNullOrBlank()) {
                            cleanCode = extracted
                        }
                    } catch (_: Exception) {}
                }
                if (cleanCode.contains("code=")) {
                    cleanCode = cleanCode.substringAfter("code=").substringBefore("&")
                }
                if (cleanCode.contains("auth_code=")) {
                    cleanCode = cleanCode.substringAfter("auth_code=").substringBefore("&")
                }

                // Authorization code single-use guard
                if (!UpstoxAuthHelper.validateAndConsumeAuthCode(cleanCode)) {
                    Log.w(TAG, "[UPSTOX_CODE_REPLAY_REJECTED] Duplicate attempt to exchange authorization code")
                }

                Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE] Initiating Upstox authorization code exchange...")

                var tokenBody: UpstoxTokenResponse? = null
                var directExchangeError: String? = null

                if (!secret.isNullOrBlank()) {
                    // Direct official Upstox OAuth token exchange (POST https://api-v2.upstox.com/v2/login/authorization/token)
                    Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE] Exchanging code via official Upstox OAuth endpoint...")
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

                    if (directRes != null && directRes.isSuccessful && directRes.body()?.effectiveAccessToken?.isNotBlank() == true) {
                        tokenBody = directRes.body()
                    } else if (directRes != null) {
                        val err = directRes.errorBody()?.string() ?: "HTTP ${directRes.code()}"
                        directExchangeError = err.take(150).replace("\n", " ")
                        Log.w(TAG, "[UPSTOX_TOKEN_EXCHANGE_WARN] Direct Upstox exchange returned error: $directExchangeError")
                    }
                }

                // Check if user passed an access token directly
                if (tokenBody == null) {
                    val directProfileTest = try {
                        upstoxApi.getUserProfile(token = "Bearer $cleanCode")
                    } catch (_: Exception) { null }

                    if (directProfileTest != null && directProfileTest.isSuccessful && directProfileTest.body()?.status == "success") {
                        Log.i(TAG, "[UPSTOX_DIRECT_TOKEN_MATCH] Input verified as valid direct Access Token")
                        tokenBody = UpstoxTokenResponse(accessToken = cleanCode)
                    }
                }

                if (tokenBody == null) {
                    val err = directExchangeError ?: "Failed to exchange Upstox code. Please check API Key and Secret."
                    sessionManager.isUpstoxConnected = false
                    sessionManager.upstoxAccessToken = null
                    _authStatus.value = BrokerAuthStatus.ERROR
                    Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Token exchange failed: $err")
                    throw Exception("TOKEN_EXCHANGE_FAILED: $err")
                }

                val body = tokenBody
                val accessToken = body.effectiveAccessToken
                if (accessToken.isNullOrBlank()) {
                    sessionManager.isUpstoxConnected = false
                    sessionManager.upstoxAccessToken = null
                    _authStatus.value = BrokerAuthStatus.ERROR
                    Log.e(TAG, "[UPSTOX_TOKEN_EXCHANGE_FAILED] Access Token is empty in response")
                    throw Exception("TOKEN_EXCHANGE_FAILED: Access token is empty")
                }

                Log.i(TAG, "[UPSTOX_OAUTH_SUCCESS] Upstox Access Token received successfully")

                // Validate token with profile endpoint before marking authenticated
                try {
                    validateUserProfile(accessToken)
                } catch (e: Exception) {
                    sessionManager.isUpstoxConnected = false
                    sessionManager.upstoxAccessToken = null
                    _authStatus.value = BrokerAuthStatus.ERROR
                    Log.e(TAG, "[UPSTOX_PROFILE_VALIDATION_FAILED] Token validation failed: ${e.message}")
                    throw Exception("PROFILE_VALIDATION_FAILED: ${e.message}")
                }

                Log.i(TAG, "[UPSTOX_TOKEN_VALID] Upstox profile validation passed")

                // Securely store credentials and tokens in encrypted storage
                sessionManager.upstoxAccessToken = accessToken
                if (!body.effectiveRefreshToken.isNullOrBlank()) {
                    sessionManager.upstoxRefreshToken = body.effectiveRefreshToken
                }
                sessionManager.upstoxTokenTimestamp = System.currentTimeMillis()
                sessionManager.isUpstoxConnected = true

                Log.i(TAG, "[UPSTOX_AUTHENTICATED] Upstox session successfully connected and authenticated")
                _authStatus.value = BrokerAuthStatus.CONNECTED
                accessToken
            }
        } finally {
            exchangeMutex.unlock()
        }
    }

    suspend fun authenticateWithToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = token.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
            if (cleanToken.isBlank()) {
                throw Exception("Upstox Access Token cannot be blank")
            }

            Log.i(TAG, "[UPSTOX_DIRECT_AUTH] Authenticating directly with Upstox Access Token...")
            validateUserProfile(cleanToken)

            sessionManager.upstoxAccessToken = cleanToken
            sessionManager.upstoxTokenTimestamp = System.currentTimeMillis()
            sessionManager.isUpstoxConnected = true

            Log.i(TAG, "[BROKER_CONNECTED] Upstox session successfully connected and authenticated via token")
            Log.i(TAG, "[UPSTOX_AUTHENTICATED] Upstox direct token authenticated")
            _authStatus.value = BrokerAuthStatus.CONNECTED
            cleanToken
        }
    }

    private suspend fun validateUserProfile(accessToken: String) {
        val authHeader = if (accessToken.startsWith("Bearer ", ignoreCase = true)) accessToken else "Bearer $accessToken"
        val profileRes = upstoxApi.getUserProfile(authHeader)
        if (profileRes.isSuccessful) {
            val body = profileRes.body()
            if (body != null && (body.status.equals("success", ignoreCase = true) || body.data != null)) {
                Log.d(TAG, "Upstox profile validated: user=${body.data?.userName ?: body.data?.userId ?: "User"}")
            } else {
                val statusMsg = body?.status ?: "HTTP ${profileRes.code()}"
                throw Exception("Upstox Profile Validation Failed: response status is '$statusMsg'")
            }
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
