package com.example.data.network

import android.util.Log
import com.example.util.UpstoxAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UpstoxAuthManager(
    private val sessionManager: SessionManager,
    private val upstoxApi: UpstoxApi,
    private val healthManager: ProviderHealthManager? = null
) {
    companion object {
        private const val TAG = "UpstoxAuthManager"
        // Backend token-exchange endpoint (server must host this)
        private const val TOKEN_EXCHANGE_ENDPOINT = "https://application-beige-psi.vercel.app/api/token-exchange"
    }

    private val _authStatus = MutableStateFlow<BrokerAuthStatus>(BrokerAuthStatus.DISCONNECTED)
    val authStatus: StateFlow<BrokerAuthStatus> = _authStatus.asStateFlow()
    private val exchangeMutex = kotlinx.coroutines.sync.Mutex()
    var onConnectedCallback: (() -> Unit)? = null

    suspend fun exchangeAuthCode(authCode: String, state: String? = null): Result<String> = withContext(Dispatchers.IO) {
        exchangeMutex.lock()
        try {
            runCatching {
                var cleanCode = authCode.trim()

                // If state provided, validate client-side but fail-closed if invalid.
                if (!state.isNullOrBlank()) {
                    val stateValid = UpstoxAuthHelper.validateAndConsumeState(state)
                    if (!stateValid) {
                        Log.e(TAG, "[UPSTOX_OAUTH_STATE_FAILCLOSED] Provided state is invalid/expired — aborting exchange")
                        throw Exception("Invalid or expired OAuth state")
                    } else {
                        Log.i(TAG, "[UPSTOX_OAUTH_STATE_VALID] State validated client-side")
                    }
                } else {
                    // Prefer server-side state; if client has no state, still allow backend to validate if present there.
                    Log.w(TAG, "[UPSTOX_OAUTH_STATE_MISSING] No state provided by client — backend will validate and may reject")
                }

                // If already authenticated and token valid, return existing token
                val existingToken = sessionManager.upstoxAccessToken
                if (!existingToken.isNullOrBlank() && sessionManager.isUpstoxConnected) {
                    val age = System.currentTimeMillis() - sessionManager.upstoxTokenTimestamp
                    if (age < 18 * 60 * 60 * 1000L) {
                        Log.i(TAG, "[UPSTOX_TOKEN_VALID] Active Upstox session token is still valid (${age / 3600000}h old)")
                        _authStatus.value = BrokerAuthStatus.CONNECTED
                        healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
                        onConnectedCallback?.invoke()
                        return@runCatching existingToken
                    }
                }

                // Authorization code single-use guard (client-side best-effort). Server will enforce single-use persistently.
                if (!UpstoxAuthHelper.validateAndConsumeAuthCode(cleanCode)) {
                    Log.w(TAG, "[UPSTOX_CODE_REPLAY_WARN] Auth code already consumed on client-side; proceeding to server check")
                    // don't fail outright because server may have a cache; server will enforce.
                }

                healthManager?.reportAuthCodeReceived(ProviderHealthManager.PROVIDER_UPSTOX, "[REDACTED]")
                healthManager?.reportTokenExchange(ProviderHealthManager.PROVIDER_UPSTOX)
                Log.i(TAG, "[UPSTOX_TOKEN_EXCHANGE] Requesting server-side token exchange via backend endpoint")

                // Call backend token-exchange endpoint (server holds client secret)
                val url = URL(TOKEN_EXCHANGE_ENDPOINT)
                val params = StringBuilder()
                params.append("code=").append(java.net.URLEncoder.encode(cleanCode, "UTF-8"))
                if (!state.isNullOrBlank()) {
                    params.append("&state=").append(java.net.URLEncoder.encode(state, "UTF-8"))
                }

                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                BufferedOutputStream(conn.outputStream).use { out ->
                    out.write(params.toString().toByteArray(Charsets.UTF_8))
                    out.flush()
                }

                val responseCode = conn.responseCode
                val responseBody = StringBuilder()
                BufferedReader(InputStreamReader(if (responseCode in 200..299) conn.inputStream else conn.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        responseBody.append(line)
                    }
                }

                if (responseCode !in 200..299) {
                    Log.e(TAG, "[UPSTOX_BACKEND_EXCHANGE_FAILED] HTTP $responseCode: ${responseBody}")
                    throw Exception("Backend token exchange failed: HTTP $responseCode")
                }

                val json = JSONObject(responseBody.toString())
                // Support common field variants
                val accessToken = when {
                    json.has("access_token") -> json.optString("access_token")
                    json.has("effectiveAccessToken") -> json.optString("effectiveAccessToken")
                    json.has("data") && json.getJSONObject("data").has("access_token") -> json.getJSONObject("data").optString("access_token")
                    else -> ""
                }

                if (accessToken.isNullOrBlank()) {
                    Log.e(TAG, "[UPSTOX_BACKEND_INVALID_RESPONSE] Token missing in backend response: ${json.optString("error", "no-error")}")
                    throw Exception("Backend did not return access token")
                }

                // Validate token with profile endpoint before marking authenticated
                try {
                    validateUserProfile(accessToken)
                } catch (e: Exception) {
                    sessionManager.isUpstoxConnected = false
                    sessionManager.upstoxAccessToken = null
                    _authStatus.value = BrokerAuthStatus.ERROR
                    healthManager?.reportAuthFailure(ProviderHealthManager.PROVIDER_UPSTOX, ProviderHealthManager.STATE_TOKEN_INVALID, e.message ?: "Profile validation failed")
                    Log.e(TAG, "[UPSTOX_PROFILE_VALIDATION_FAILED] Token validation failed: ${e.message}")
                    throw Exception("PROFILE_VALIDATION_FAILED: ${e.message}")
                }

                // Securely store credentials and tokens in encrypted storage (do NOT store client secret)
                sessionManager.upstoxAccessToken = accessToken
                sessionManager.upstoxTokenTimestamp = System.currentTimeMillis()
                sessionManager.isUpstoxConnected = true

                Log.i(TAG, "[UPSTOX_AUTHENTICATED] Upstox session successfully connected and authenticated (backend-exchanged)")
                _authStatus.value = BrokerAuthStatus.CONNECTED
                healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
                onConnectedCallback?.invoke()
                accessToken
            }
        } finally {
            exchangeMutex.unlock()
        }
    }

    suspend fun authenticateWithToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Direct token authentication is disabled in production. Only allow in DEBUG builds.
            if (!com.example.BuildConfig.DEBUG) {
                Log.w(TAG, "[UPSTOX_DIRECT_AUTH_BLOCKED] Direct access-token authentication is disabled in production")
                throw Exception("Direct token authentication disabled in production")
            }

            val cleanToken = token.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
            if (cleanToken.isBlank()) {
                throw Exception("Upstox Access Token cannot be blank")
            }

            Log.i(TAG, "[UPSTOX_DIRECT_AUTH_DEBUG] Authenticating directly with Upstox Access Token (DEBUG)...")
            validateUserProfile(cleanToken)

            sessionManager.upstoxAccessToken = cleanToken
            sessionManager.upstoxTokenTimestamp = System.currentTimeMillis()
            sessionManager.isUpstoxConnected = true

            Log.i(TAG, "[BROKER_CONNECTED] Upstox session successfully connected and authenticated via token")
            _authStatus.value = BrokerAuthStatus.CONNECTED
            onConnectedCallback?.invoke()
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
            _authStatus.value = if (sessionManager.upstoxApiKey.isNotBlank()) BrokerAuthStatus.ERROR else BrokerAuthStatus.NOT_CONFIGURED
            return@withContext false
        }

        // Upstox tokens expire daily at 3:30 AM (approx 20 hours lifetime)
        val tokenTime = sessionManager.upstoxTokenTimestamp
        val isExpired = tokenTime > 0L && (System.currentTimeMillis() - tokenTime > 20 * 60 * 60 * 1000L)

        if (isExpired) {
            Log.w(TAG, "Upstox access token expired. Re-authentication required.")
            sessionManager.isUpstoxConnected = false
            _authStatus.value = BrokerAuthStatus.ERROR
            return@withContext false
        }

        try {
            val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
            val profileRes = upstoxApi.getUserProfile(authHeader)
            if (profileRes.isSuccessful && (profileRes.body()?.status?.equals("success", ignoreCase = true) == true || profileRes.body()?.data != null)) {
                android.util.Log.d("UpstoxAuth", "[9] Account verified: PASS")
                android.util.Log.d("UpstoxAuth", "[10] Authentication SUCCESS: PASS")
                sessionManager.isUpstoxConnected = true
                _authStatus.value = BrokerAuthStatus.CONNECTED
                healthManager?.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
                true
            } else {
                val code = profileRes.code()
                if (code == 401 || code == 403) {
                    sessionManager.isUpstoxConnected = false
                    _authStatus.value = BrokerAuthStatus.ERROR
                    false
                } else {
                    // Non-auth HTTP error (e.g. 500, 503) - if token is fresh, keep session
                    if (tokenTime > 0L && System.currentTimeMillis() - tokenTime < 18 * 60 * 60 * 1000L) {
                        Log.w(TAG, "Upstox profile HTTP $code, but token timestamp is fresh (<18h). Preserving session.")
                        sessionManager.isUpstoxConnected = true
                        _authStatus.value = BrokerAuthStatus.CONNECTED
                        true
                    } else {
                        sessionManager.isUpstoxConnected = false
                        _authStatus.value = BrokerAuthStatus.ERROR
                        false
                    }
                }
            }
        } catch (e: Exception) {
            if (tokenTime > 0L && System.currentTimeMillis() - tokenTime < 18 * 60 * 60 * 1000L) {
                Log.w(TAG, "Upstox validateSession network exception: ${e.message}, but token timestamp is fresh (<18h). Preserving session.")
                sessionManager.isUpstoxConnected = true
                _authStatus.value = BrokerAuthStatus.CONNECTED
                true
            } else {
                sessionManager.isUpstoxConnected = false
                _authStatus.value = BrokerAuthStatus.ERROR
                false
            }
        }
    }

    fun clearSession() {
        sessionManager.clearUpstoxSession()
        _authStatus.value = BrokerAuthStatus.DISCONNECTED
    }
}
