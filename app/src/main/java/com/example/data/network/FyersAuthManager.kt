package com.example.data.network

import android.util.Log
import com.example.util.FyersAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class FyersAuthManager(
    private val sessionManager: SessionManager,
    private val fyersApi: FyersApi
) {
    private val TAG = "FyersAuthManager"

    private val _authStatus = MutableStateFlow(BrokerAuthStatus.OFFLINE)
    val authStatus: StateFlow<BrokerAuthStatus> = _authStatus
    private val exchangeMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun exchangeAuthCode(authCode: String): Result<String> = withContext(Dispatchers.IO) {
        exchangeMutex.lock()
        try {
            runCatching {
                var cleanCode = authCode.trim()

                // Check if user passed an access token directly (e.g. JWT starts with "ey" or raw token)
                if (cleanCode.startsWith("ey", ignoreCase = true) || (cleanCode.length > 50 && !cleanCode.contains("&") && !cleanCode.contains("?") && !cleanCode.contains("="))) {
                    Log.i(TAG, "[FYERS_DIRECT_TOKEN] Input recognized as direct Access Token, validating...")
                    val tokenResult = authenticateWithToken(cleanCode)
                    return@runCatching tokenResult.getOrThrow()
                }

                // If already authenticated and token valid, return existing token
                val existingToken = sessionManager.fyersAccessToken
                if (!existingToken.isNullOrBlank() && sessionManager.isFyersConnected) {
                    val age = System.currentTimeMillis() - sessionManager.fyersTokenTimestamp
                    if (age < 18 * 60 * 60 * 1000L) {
                        _authStatus.value = BrokerAuthStatus.CONNECTED
                        return@runCatching existingToken
                    }
                }

                val rawAppId = sessionManager.fyersAppId.takeIf { it.isNotBlank() }
                    ?: com.example.util.BrokerConfig.fyersAppId.takeIf { it.isNotBlank() }
                    ?: throw Exception("FYERS App ID is missing")
                val secret = sessionManager.fyersSecretId.takeIf { it.isNotBlank() }
                    ?: com.example.util.BrokerConfig.fyersSecretId.takeIf { it.isNotBlank() }
                val redirectUri = sessionManager.pendingOAuthSession?.redirectUri?.takeIf { it.isNotBlank() }
                    ?: sessionManager.fyersRedirectUri.takeIf { it.isNotBlank() }
                    ?: FyersAuthHelper.DEFAULT_REDIRECT_URI

                val fullAppId = FyersAuthHelper.getFullAppId(rawAppId)
                if (cleanCode.startsWith("http://") || cleanCode.startsWith("https://") || cleanCode.startsWith("kingkhan://")) {
                    try {
                        val parsedUri = android.net.Uri.parse(cleanCode)
                        val extracted = parsedUri.getQueryParameter("auth_code") ?: parsedUri.getQueryParameter("code")
                        if (!extracted.isNullOrBlank()) {
                            cleanCode = extracted
                        }
                    } catch (_: Exception) {}
                }
                if (cleanCode.contains("auth_code=")) {
                    cleanCode = cleanCode.substringAfter("auth_code=").substringBefore("&")
                }
                if (cleanCode.contains("code=") && cleanCode != "200") {
                    cleanCode = cleanCode.substringAfter("code=").substringBefore("&")
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

                // Fallback to secure backend endpoint if direct failed or secret missing
                if (tokenBody == null) {
                    val backendBase = if (redirectUri.startsWith("http://") || redirectUri.startsWith("https://")) {
                        redirectUri.substringBefore("/oauth").substringBefore("/api").trimEnd('/')
                    } else {
                        "https://application-beige-psi.vercel.app"
                    }
                    val tokenExchangeUrl = "$backendBase/api/fyers-token-exchange"
                    Log.i(TAG, "[FYERS_TOKEN_EXCHANGE] Exchanging code via secure backend endpoint: $tokenExchangeUrl...")
                    val response = try {
                        fyersApi.exchangeTokenSecurely(
                            url = tokenExchangeUrl,
                            code = cleanCode,
                            redirectUri = redirectUri
                        )
                    } catch (e: Exception) {
                        null
                    }

                    if (response != null && response.isSuccessful && response.body()?.access_token?.isNotBlank() == true) {
                        tokenBody = response.body()
                    } else {
                        val errBody = response?.errorBody()?.string() ?: directExchangeError ?: "Token exchange failed"
                        val sanitizedErr = errBody.take(150).replace("\n", " ")
                        _authStatus.value = BrokerAuthStatus.ERROR
                        Log.e(TAG, "[FYERS_TOKEN_EXCHANGE_FAILED] Token exchange failed: $sanitizedErr")
                        throw Exception("TOKEN_EXCHANGE_FAILED: $sanitizedErr")
                    }
                }

            val body = tokenBody ?: throw Exception("TOKEN_EXCHANGE_FAILED: Empty response body")

            if ((body.s == "ok" || body.code == 200 || body.code == null) && !body.access_token.isNullOrBlank()) {
                val accessToken = body.access_token
                Log.i(TAG, "[TOKEN_EXCHANGE_SUCCESS] FYERS Access Token obtained successfully")
                Log.i(TAG, "[FYERS_TOKEN_EXCHANGE_SUCCESS] FYERS Access Token obtained successfully")

                // Validate access token with profile API call
                val authHeader = "$fullAppId:$accessToken"
                try {
                    val profileRes = fyersApi.getProfile(authHeader)
                    if (!profileRes.isSuccessful || profileRes.body()?.s != "ok") {
                        val pErr = profileRes.body()?.message ?: "HTTP ${profileRes.code()}"
                        Log.e(TAG, "[FYERS_TOKEN_INVALID] Profile validation failed: $pErr")
                        throw Exception("PROFILE_VALIDATION_FAILED: $pErr")
                    }
                    Log.i(TAG, "[PROFILE_VALIDATED] FYERS token profile validation passed")
                    Log.i(TAG, "[FYERS_TOKEN_VALIDATED] FYERS token profile validation: PASS")
                } catch (e: Exception) {
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

                Log.i(TAG, "[BROKER_CONNECTED] FYERS OAuth session successfully connected and authenticated")
                Log.i(TAG, "[FYERS_AUTHENTICATED] FYERS OAuth session successfully authenticated")
                _authStatus.value = BrokerAuthStatus.CONNECTED
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

            Log.i(TAG, "[BROKER_CONNECTED] FYERS direct token successfully authenticated")
            Log.i(TAG, "[FYERS_AUTHENTICATED] FYERS direct token authenticated")
            _authStatus.value = BrokerAuthStatus.CONNECTED
            finalToken
        }
    }

    fun clearSession() {
        sessionManager.clearFyersSession()
        _authStatus.value = BrokerAuthStatus.OFFLINE
    }

    suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        val token = sessionManager.fyersAccessToken
        val appId = sessionManager.fyersAppId
        if (token.isNullOrBlank() || appId.isBlank()) {
            _authStatus.value = if (appId.isNotBlank()) BrokerAuthStatus.AUTHENTICATION_REQUIRED else BrokerAuthStatus.CONFIGURE
            return@withContext false
        }

        // Check if token is older than 20 hours (expires daily)
        val timestamp = sessionManager.fyersTokenTimestamp
        val isExpired = (System.currentTimeMillis() - timestamp) > 20 * 60 * 60 * 1000L
        if (isExpired) {
            _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
            return@withContext false
        }

        val authHeader = "$appId:$token"
        try {
            val profileRes = fyersApi.getProfile(authHeader)
            if (profileRes.isSuccessful && profileRes.body()?.s == "ok") {
                android.util.Log.d("FyersAuth", "[9] Account verified: PASS")
                android.util.Log.d("FyersAuth", "[10] Authentication SUCCESS: PASS")
                _authStatus.value = BrokerAuthStatus.CONNECTED
                true
            } else {
                _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fyers validateSession profile call failed: ${e.message}")
            _authStatus.value = BrokerAuthStatus.AUTHENTICATION_REQUIRED
            false
        }
    }
    suspend fun refreshSession(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val appId = sessionManager.fyersAppId
            val secret = sessionManager.fyersSecretId
            val refreshToken = sessionManager.fyersRefreshToken
            
            if (appId.isBlank() || secret.isBlank() || refreshToken.isNullOrBlank()) {
                throw Exception("Missing credentials or refresh token")
            }

            val appIdHash = FyersAuthHelper.generateAppIdHash(appId, secret)
            
            val request = FyersRefreshTokenRequest(
                grant_type = "refresh_token",
                appIdHash = appIdHash,
                refresh_token = refreshToken,
                pin = sessionManager.fyersPin
            )

            val response = fyersApi.validateRefreshToken(request)
            if (!response.isSuccessful) {
                clearSession()
                throw Exception("HTTP ${response.code()}")
            }

            val body = response.body() ?: throw Exception("Empty response body")
            android.util.Log.d("FyersAuth", "[8] Token validated: PASS")
            if (body.s == "ok" && !body.access_token.isNullOrBlank()) {
                sessionManager.fyersAccessToken = body.access_token
                sessionManager.fyersTokenTimestamp = System.currentTimeMillis()
                sessionManager.isFyersConnected = true
                android.util.Log.d("FyersAuth", "[9] Account verified: PASS")
                android.util.Log.d("FyersAuth", "[10] Authentication SUCCESS: PASS")
                _authStatus.value = BrokerAuthStatus.CONNECTED
                body.access_token
            } else {
                clearSession()
                val errorMsg = body.message ?: "Unknown error from Fyers refresh"
                _authStatus.value = BrokerAuthStatus.ERROR
                throw Exception(errorMsg)
            }
        }
    }
}