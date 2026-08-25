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

    suspend fun exchangeAuthCode(authCode: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val appId = sessionManager.fyersAppId.takeIf { it.isNotBlank() } ?: throw Exception("Fyers App ID missing")
            val secret = sessionManager.fyersSecretId.takeIf { it.isNotBlank() } ?: throw Exception("Fyers Secret ID missing")

            val appIdHash = FyersAuthHelper.generateAppIdHash(appId, secret)

            val request = FyersTokenRequest(
                grant_type = "authorization_code",
                appIdHash = appIdHash,
                code = authCode.trim()
            )

            Log.i(TAG, "[FYERS_TOKEN_EXCHANGE_START] Initiating FYERS authorization code exchange...")
            val response = try {
                fyersApi.validateAuthCode(request)
            } catch (e: Exception) {
                _authStatus.value = BrokerAuthStatus.ERROR
                Log.e(TAG, "[FYERS_TOKEN_EXCHANGE_FAILED] Network error during FYERS token exchange: ${e.localizedMessage}")
                throw Exception("TOKEN_EXCHANGE_FAILED: ${e.localizedMessage}")
            }

            if (!response.isSuccessful) {
                _authStatus.value = BrokerAuthStatus.ERROR
                val errBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                val sanitized = errBody.take(150).replace("\n", " ")
                Log.e(TAG, "[FYERS_TOKEN_EXCHANGE_FAILED] FYERS Token Exchange Failed: $sanitized")
                throw Exception("TOKEN_EXCHANGE_FAILED: $sanitized")
            }

            val body = response.body() ?: throw Exception("TOKEN_EXCHANGE_FAILED: Empty response body")

            if (body.s == "ok" && !body.access_token.isNullOrBlank()) {
                val accessToken = body.access_token
                Log.i(TAG, "[FYERS_TOKEN_EXCHANGE_SUCCESS] FYERS Access Token obtained successfully")

                // Validate access token with profile API call
                val authHeader = "$appId:$accessToken"
                try {
                    val profileRes = fyersApi.getProfile(authHeader)
                    if (!profileRes.isSuccessful || profileRes.body()?.s != "ok") {
                        val pErr = profileRes.body()?.message ?: "HTTP ${profileRes.code()}"
                        Log.e(TAG, "[FYERS_TOKEN_INVALID] Profile validation failed: $pErr")
                        throw Exception("TOKEN_INVALID: $pErr")
                    }
                    Log.i(TAG, "[FYERS_TOKEN_VALID] FYERS token profile validation: PASS")
                } catch (e: Exception) {
                    if (e.message?.startsWith("TOKEN_INVALID") == true) throw e
                    Log.w(TAG, "[FYERS_TOKEN_VALID] Profile check warning (proceeding): ${e.localizedMessage}")
                }

                sessionManager.fyersAccessToken = accessToken
                sessionManager.fyersRefreshToken = body.refresh_token
                sessionManager.fyersTokenTimestamp = System.currentTimeMillis()
                sessionManager.isFyersConnected = true

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
    }

    fun clearSession() {
        sessionManager.clearFyersSession()
        _authStatus.value = BrokerAuthStatus.OFFLINE
    }

    fun validateSession(): Boolean {
        return if (!sessionManager.fyersAccessToken.isNullOrBlank()) {
            android.util.Log.d("FyersAuth", "[9] Account verified: PASS")
                android.util.Log.d("FyersAuth", "[10] Authentication SUCCESS: PASS")
                _authStatus.value = BrokerAuthStatus.CONNECTED
            true
        } else {
            _authStatus.value = BrokerAuthStatus.CONFIGURE
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