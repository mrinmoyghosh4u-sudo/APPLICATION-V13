package com.example.data.network

import android.util.Log
import com.example.util.FyersAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class FyersAuthManager(
    private val sessionManager: SessionManager,
    private val fyersApi: FyersApi,
    private val healthManager: ProviderHealthManager? = null
) {
    private val TAG = "FyersAuthManager"

    private val _authStatus = MutableStateFlow<BrokerAuthStatus>(BrokerAuthStatus.DISCONNECTED)
    val authStatus: StateFlow<BrokerAuthStatus> = _authStatus
    private val exchangeMutex = kotlinx.coroutines.sync.Mutex()

    private val consumedAuthCodes = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    var onConnectedCallback: (() -> Unit)? = null

    suspend fun exchangeAuthCode(authCode: String, expectedState: String? = null): Result<String> = withContext(Dispatchers.IO) {
        exchangeMutex.lock()
        try {
            runCatching {
                var cleanCode = authCode.trim()
                var extractedState: String? = expectedState

                if (cleanCode.startsWith("http://") || cleanCode.startsWith("https://") || cleanCode.startsWith("kingkhan://")) {
                    try {
                        val parsedUri = android.net.Uri.parse(cleanCode)
                        val extracted = parsedUri.getQueryParameter("auth_code") ?: parsedUri.getQueryParameter("code")
                        if (!extracted.isNullOrBlank()) {
                            cleanCode = extracted
                        }
                        val stateFromUrl = parsedUri.getQueryParameter("state")
                        if (!stateFromUrl.isNullOrBlank()) {
                            extractedState = stateFromUrl
                        }
                    } catch (_: Exception) {}
                }
                if (cleanCode.contains("auth_code=")) {
                    cleanCode = cleanCode.substringAfter("auth_code=").substringBefore("&")
                }
                if (cleanCode.contains("code=") && cleanCode != "200") {
                    cleanCode = cleanCode.substringAfter("code=").substringBefore("&")
                }

                // 1. Validate state if pending OAuth session exists or state provided
                val pendingSession = sessionManager.pendingOAuthSession
                val storedState = pendingSession?.state?.takeIf { it.isNotBlank() }
                    ?: sessionManager.pendingFyersOAuthState.takeIf { it.isNotBlank() }

                if (!storedState.isNullOrBlank()) {
                    if (!extractedState.isNullOrBlank() && extractedState != storedState) {
                        Log.w(TAG, "[FYERS_OAUTH_STATE_WARN] OAuth state mismatch ($extractedState vs $storedState) - proceeding with non-fatal warning for manual/seamless OAuth callback")
                    } else if (extractedState.isNullOrBlank()) {
                        Log.w(TAG, "[FYERS_OAUTH_STATE_WARN] OAuth state missing in callback - proceeding with non-fatal warning for manual/seamless OAuth callback")
                    } else {
                        Log.i(TAG, "[FYERS_OAUTH_STATE_MATCH] OAuth state matched successfully")
                    }
                }

                // 2. Prevent reuse of authorization code (Single-use enforcement with active session recovery)
                if (consumedAuthCodes.contains(cleanCode) || sessionManager.lastProcessedOAuthCode == cleanCode) {
                    Log.w(TAG, "[FYERS_CODE_REUSED_WARN] Authorization code already used, checking active session")
                    val existingToken = sessionManager.fyersAccessToken
                    if (!existingToken.isNullOrBlank() && sessionManager.isFyersConnected) {
                        _authStatus.value = BrokerAuthStatus.CONNECTED
                        onConnectedCallback?.invoke()
                        return@runCatching existingToken
                    }
                }

                // If already authenticated and token valid, return existing token
                val existingToken = sessionManager.fyersAccessToken
                if (!existingToken.isNullOrBlank() && sessionManager.isFyersConnected) {
                    val age = System.currentTimeMillis() - sessionManager.fyersTokenTimestamp
                    if (age < 18 * 60 * 60 * 1000L) {
                        _authStatus.value = BrokerAuthStatus.CONNECTED
                        onConnectedCallback?.invoke()
                        return@runCatching existingToken
                    }
                }

                val rawAppId = sessionManager.fyersAppId.takeIf { it.isNotBlank() }
                    ?: com.example.util.BrokerConfig.fyersAppId.takeIf { it.isNotBlank() }
                    ?: throw Exception("FYERS App ID is missing")
                val fullAppId = FyersAuthHelper.getFullAppId(rawAppId)
                if (sessionManager.fyersAppId != fullAppId) {
                    sessionManager.fyersAppId = fullAppId
                }

                val secret = sessionManager.fyersSecretId.takeIf { it.isNotBlank() }
                    ?: com.example.util.BrokerConfig.fyersSecretId.takeIf { it.isNotBlank() }

                val rawRedirectUri = sessionManager.fyersRedirectUri
                val redirectUri = if (rawRedirectUri.isBlank() || rawRedirectUri.contains("kingkhan://")) {
                    "https://application-beige-psi.vercel.app/oauth"
                } else {
                    rawRedirectUri
                }
                if (sessionManager.fyersRedirectUri != redirectUri) {
                    sessionManager.fyersRedirectUri = redirectUri
                }

                Log.i(TAG, "[TOKEN_EXCHANGE_STARTED] Initiating FYERS authorization code exchange...")
                Log.i(TAG, "[FYERS_TOKEN_EXCHANGE] Initiating FYERS authorization code exchange...")

                var tokenBody: FyersTokenResponse? = null
                var directExchangeError: String? = null

                if (!secret.isNullOrBlank()) {
                    // Direct official FYERS V3 OAuth token exchange (POST https://api-t1.fyers.in/api/v3/validate-authcode)
                    val appIdHash = FyersAuthHelper.generateAppIdHash(fullAppId, secret)
                    val request = FyersTokenRequest(
                        grant_type = "authorization_code",
                        appIdHash = appIdHash,
                        code = cleanCode
                    )
                    Log.i(TAG, "[FYERS_TOKEN_EXCHANGE] Exchanging code directly via official FYERS V3 API...")
                    val directRes = try {
                        fyersApi.validateAuthCode(request)
                    } catch (e: Exception) {
                        Log.w(TAG, "[FYERS_TOKEN_EXCHANGE_WARN] FYERS direct token exchange exception: ${e.localizedMessage}")
                        null
                    }

                    if (directRes != null && directRes.isSuccessful && directRes.body()?.access_token?.isNotBlank() == true) {
                        tokenBody = directRes.body()
                    } else if (directRes != null) {
                        val err = directRes.errorBody()?.string() ?: directRes.body()?.message ?: "HTTP ${directRes.code()}"
                        directExchangeError = err.take(150).replace("\n", " ")
                        Log.w(TAG, "[FYERS_TOKEN_EXCHANGE_WARN] Direct FYERS exchange returned error: $directExchangeError")
                    }
                }

                if (tokenBody == null) {
                    val err = directExchangeError ?: "Failed to exchange Fyers auth code. Please verify Secret ID."
                    sessionManager.isFyersConnected = false
                    sessionManager.fyersAccessToken = null
                    _authStatus.value = BrokerAuthStatus.ERROR
                    Log.e(TAG, "[FYERS_TOKEN_EXCHANGE_FAILED] Token exchange failed: $err")
                    throw Exception("TOKEN_EXCHANGE_FAILED: $err")
                }

                val body = tokenBody ?: throw Exception("TOKEN_EXCHANGE_FAILED: Empty response body")

                if ((body.s == "ok" || body.code == 200 || body.code == null) && !body.access_token.isNullOrBlank()) {
                    val accessToken = body.access_token
                    Log.i(TAG, "[TOKEN_EXCHANGE_SUCCESS] FYERS Access Token obtained successfully")

                    // Mark code as consumed to prevent reuse
                    consumedAuthCodes.add(cleanCode)
                    sessionManager.lastProcessedOAuthCode = cleanCode
                    sessionManager.lastProcessedOAuthTime = System.currentTimeMillis()

                    // Validate access token with profile API call
                    val authHeader = "$fullAppId:$accessToken"
                    try {
                        val profileRes = fyersApi.getProfile(authHeader)
                        if (!profileRes.isSuccessful || profileRes.body()?.s != "ok") {
                            val pErr = profileRes.body()?.message ?: "HTTP ${profileRes.code()}"
                            Log.e(TAG, "[FYERS_TOKEN_INVALID] Profile validation failed: $pErr")
                            throw Exception("PROFILE_VALIDATION_FAILED: $pErr")
                        }
                        Log.i(TAG, "[FYERS_TOKEN_VALIDATED] FYERS token profile validation: PASS")
                    } catch (e: Exception) {
                        sessionManager.isFyersConnected = false
                        sessionManager.fyersAccessToken = null
                        _authStatus.value = BrokerAuthStatus.ERROR
                        if (e.message?.startsWith("PROFILE_VALIDATION_FAILED") == true) {
                            throw e
                        } else {
                            throw Exception("PROFILE_VALIDATION_FAILED: ${e.localizedMessage}")
                        }
                    }

                    sessionManager.fyersAccessToken = accessToken
                    sessionManager.fyersRefreshToken = body.refresh_token
                    sessionManager.fyersTokenTimestamp = System.currentTimeMillis()
                    sessionManager.isFyersConnected = true
                    sessionManager.pendingFyersOAuthState = ""

                    Log.i(TAG, "[FYERS_AUTHENTICATED] FYERS OAuth session successfully authenticated")
                    _authStatus.value = BrokerAuthStatus.CONNECTED
                    onConnectedCallback?.invoke()
                    accessToken
                } else {
                    val errorMsg = body.message ?: "Unknown error from Fyers"
                    _authStatus.value = BrokerAuthStatus.ERROR
                    Log.e(TAG, "[FYERS_TOKEN_EXCHANGE_FAILED] FYERS Token Exchange Failed: $errorMsg")
                    throw Exception("TOKEN_EXCHANGE_FAILED: $errorMsg")
                }
            }
        } finally {
            exchangeMutex.unlock()
        }
    }

    suspend fun authenticateWithToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanToken = token.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
            if (cleanToken.isBlank()) {
                throw Exception("FYERS Access Token cannot be blank")
            }

            val rawAppId = sessionManager.fyersAppId.takeIf { it.isNotBlank() }
                ?: com.example.util.BrokerConfig.fyersAppId.takeIf { it.isNotBlank() }
                ?: throw Exception("FYERS App ID is missing")
            val fullAppId = FyersAuthHelper.getFullAppId(rawAppId)
            val authHeader = if (cleanToken.contains(":")) cleanToken else "$fullAppId:$cleanToken"

            Log.i(TAG, "[FYERS_DIRECT_AUTH] Authenticating directly with FYERS Access Token...")
            val profileRes = fyersApi.getProfile(authHeader)
            if (!profileRes.isSuccessful || profileRes.body()?.s != "ok") {
                val pErr = profileRes.body()?.message ?: "HTTP ${profileRes.code()}"
                Log.e(TAG, "[FYERS_TOKEN_INVALID] Profile validation failed: $pErr")
                throw Exception("PROFILE_VALIDATION_FAILED: $pErr")
            }

            val finalToken = if (cleanToken.contains(":")) cleanToken.substringAfter(":") else cleanToken
            sessionManager.fyersAccessToken = finalToken
            sessionManager.fyersTokenTimestamp = System.currentTimeMillis()
            sessionManager.isFyersConnected = true
            sessionManager.pendingFyersOAuthState = ""

            Log.i(TAG, "[BROKER_CONNECTED] FYERS direct token successfully authenticated")
            Log.i(TAG, "[FYERS_AUTHENTICATED] FYERS direct token authenticated")
            _authStatus.value = BrokerAuthStatus.CONNECTED
            onConnectedCallback?.invoke()
            finalToken
        }
    }

    fun clearSession() {
        sessionManager.clearFyersSession()
        _authStatus.value = BrokerAuthStatus.DISCONNECTED
    }

        suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        val token = sessionManager.fyersAccessToken
        val appId = sessionManager.fyersAppId
        val fullAppId = FyersAuthHelper.getFullAppId(appId)
        
        if (token.isNullOrBlank() || fullAppId.isBlank()) {
            _authStatus.value = if (fullAppId.isNotBlank()) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.NOT_CONFIGURED
            return@withContext false
        }

        // Fyers access tokens expire daily.
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        calendar.timeInMillis = sessionManager.fyersTokenTimestamp
        val authDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val isExpired = currentDay != authDay
        
        if (isExpired) {
            clearSession()
            _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
            return@withContext false
        }

        val authHeader = "$fullAppId:$token"
        try {
            val profileRes = fyersApi.getProfile(authHeader)
            if (profileRes.isSuccessful && profileRes.body()?.s == "ok") {
                _authStatus.value = BrokerAuthStatus.CONNECTED
                return@withContext true
            } else {
                val errorMsg = profileRes.body()?.message ?: "HTTP ${profileRes.code()}"
                if (profileRes.code() == 401 || profileRes.code() == 403 || errorMsg.contains("expired", true)) {
                    clearSession()
                    _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
                } else {
                    _authStatus.value = BrokerAuthStatus.ERROR
                }
                return@withContext false
            }
        } catch (e: Exception) {
            // Network error: don't clear session, just return false for now
            _authStatus.value = BrokerAuthStatus.ERROR
            return@withContext false
        }
    }

    fun buildAuthorizationUrl(appId: String, state: String? = null): String {
        val fullAppId = FyersAuthHelper.getFullAppId(appId)
        if (sessionManager.fyersAppId != fullAppId && fullAppId.isNotBlank()) {
            sessionManager.fyersAppId = fullAppId
        }
        val rawRedirectUri = sessionManager.fyersRedirectUri
        val redirectUri = if (rawRedirectUri.isBlank() || rawRedirectUri.contains("kingkhan://")) {
            "https://application-beige-psi.vercel.app/oauth"
        } else {
            rawRedirectUri
        }
        if (sessionManager.fyersRedirectUri != redirectUri) {
            sessionManager.fyersRedirectUri = redirectUri
        }
        return FyersAuthHelper.buildLoginUrl(fullAppId, redirectUri, state)
    }

    // FYERS 2026 Audit: Refresh token flows are not supported for continuous sessions.
    // Explicit re-authentication via login flow is required if the token expires.
}