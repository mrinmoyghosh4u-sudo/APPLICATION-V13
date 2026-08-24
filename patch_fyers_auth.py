import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

old_exchange = """            if (body.s == "ok" && !body.access_token.isNullOrBlank()) {
                sessionManager.fyersAccessToken = body.access_token
                sessionManager.isFyersConnected = true
                _authStatus.value = BrokerAuthStatus.CONNECTED
                body.access_token"""

new_exchange = """            if (body.s == "ok" && !body.access_token.isNullOrBlank()) {
                sessionManager.fyersAccessToken = body.access_token
                sessionManager.fyersRefreshToken = body.refresh_token
                sessionManager.fyersTokenTimestamp = System.currentTimeMillis()
                sessionManager.isFyersConnected = true
                _authStatus.value = BrokerAuthStatus.CONNECTED
                body.access_token"""

content = content.replace(old_exchange, new_exchange)

# Now append a refresh function
refresh_fn = """
    suspend fun refreshSession(pin: String = "1234"): Result<String> = withContext(Dispatchers.IO) {
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
                pin = pin
            )

            val response = fyersApi.validateRefreshToken(request)
            if (!response.isSuccessful) {
                clearSession()
                throw Exception("HTTP ${response.code()}")
            }

            val body = response.body() ?: throw Exception("Empty response body")
            if (body.s == "ok" && !body.access_token.isNullOrBlank()) {
                sessionManager.fyersAccessToken = body.access_token
                sessionManager.fyersTokenTimestamp = System.currentTimeMillis()
                sessionManager.isFyersConnected = true
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
"""

content = content[:-1] + refresh_fn + "}"

with open(filepath, "w") as f:
    f.write(content)
