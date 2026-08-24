import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/BrokerAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

# 1. Update validateFyersSession to attempt refresh if needed
old_fyers_validate = """    private suspend fun validateFyersSession() {
        val hasSession = sessionManager.isFyersConnected && !sessionManager.fyersAccessToken.isNullOrBlank()
        if (!hasSession) {
            updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }
        
        // Fyers session doesn't expire quickly or we just assume it's valid if we have it, 
        // until a data request fails. But we can set to CONNECTED.
        updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Active for Market Data")
    }"""

new_fyers_validate = """    private suspend fun validateFyersSession() {
        val hasSession = !sessionManager.fyersAccessToken.isNullOrBlank()
        val hasRefreshToken = !sessionManager.fyersRefreshToken.isNullOrBlank()
        
        if (!hasSession && !hasRefreshToken) {
            updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.CONFIGURE, "Credentials not configured")
            return
        }
        
        // Check if token is older than 20 hours (expires daily)
        val timestamp = sessionManager.fyersTokenTimestamp
        val isExpired = (System.currentTimeMillis() - timestamp) > 20 * 60 * 60 * 1000L
        
        if (hasSession && !isExpired) {
            updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active")
            // Reconnect WebSocket
            brokerManager.fyersMarketDataService.connect()
            return
        }
        
        if (hasRefreshToken) {
            updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.STANDBY, "Restoring Session...")
            val result = reconnectBroker("Fyers")
            if (result.isSuccess) {
                updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.CONNECTED, "Live Market Data Active (Restored)")
            } else {
                updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Session Expired. Login Required.")
            }
        } else {
            updateStatus("Fyers", "Primary Market Data", BrokerAuthStatus.AUTHENTICATION_REQUIRED, "Session Expired. Login Required.")
        }
    }"""

content = content.replace(old_fyers_validate, new_fyers_validate)

# 2. Update reconnectBroker to handle Fyers refresh instead of error
old_fyers_reconnect = """            "Fyers" -> Result.failure(Exception("Fyers auto-reconnect not supported. Please re-login."))"""

new_fyers_reconnect = """            "Fyers" -> {
                val fyersAuth = brokerManager.fyersAuthManager
                val refreshRes = fyersAuth.refreshSession()
                if (refreshRes.isSuccess) {
                    brokerManager.fyersMarketDataService.connect()
                    Result.success(true)
                } else {
                    Result.failure(Exception(refreshRes.exceptionOrNull()?.message ?: "Fyers refresh failed"))
                }
            }"""

content = content.replace(old_fyers_reconnect, new_fyers_reconnect)

with open(filepath, "w") as f:
    f.write(content)
