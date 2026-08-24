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
            val appId = sessionManager.fyersAppId ?: throw Exception("App ID missing")
            val secret = sessionManager.fyersSecretId ?: throw Exception("Secret missing")

            val appIdHash = FyersAuthHelper.generateAppIdHash(appId, secret)

            val request = FyersTokenRequest(
                grant_type = "authorization_code",
                appIdHash = appIdHash,
                code = authCode
            )

            val response = fyersApi.validateAuthCode(request)
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }

            val body = response.body() ?: throw Exception("Empty response body")
            if (body.s == "ok" && !body.access_token.isNullOrBlank()) {
                sessionManager.fyersAccessToken = body.access_token
                sessionManager.isFyersConnected = true
                _authStatus.value = BrokerAuthStatus.CONNECTED
                body.access_token
            } else {
                val errorMsg = body.message ?: "Unknown error from Fyers"
                _authStatus.value = BrokerAuthStatus.ERROR
                throw Exception(errorMsg)
            }
        }
    }

    fun clearSession() {
        sessionManager.clearFyersSession()
        _authStatus.value = BrokerAuthStatus.OFFLINE
    }

    fun validateSession(): Boolean {
        return if (!sessionManager.fyersAccessToken.isNullOrBlank()) {
            _authStatus.value = BrokerAuthStatus.CONNECTED
            true
        } else {
            _authStatus.value = BrokerAuthStatus.CONFIGURE
            false
        }
    }
}
