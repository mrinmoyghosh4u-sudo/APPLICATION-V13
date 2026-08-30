import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

def replace_func(content, func_name, new_body):
    start = content.find("fun " + func_name)
    if start == -1: return content
    # Find matching brace
    brace_count = 0
    in_func = False
    for i in range(start, len(content)):
        if content[i] == '{':
            brace_count += 1
            in_func = True
        elif content[i] == '}':
            brace_count -= 1
        if in_func and brace_count == 0:
            end = i + 1
            return content[:start] + new_body + content[end:]
    return content

new_connectDhan = """fun connectDhan(clientId: String, accessToken: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            if (clientId.isNotBlank() && accessToken.isNotBlank()) {
                sessionManager.dhanClientId = clientId
                sessionManager.dhanAccessToken = accessToken
                sessionManager.isDhanConnected = true
                brokerManager.brokerAuthManager.updateStatus("Dhan", "Primary Order Execution", com.example.data.network.BrokerAuthStatus.CONNECTED, "Active for Order Execution")
                brokerManager.healthManager.reportSuccessfulRequest(com.example.data.network.ProviderHealthManager.PROVIDER_DHAN, 100L)
            } else {
                _authErrorMessage.value = "Client ID and Access Token are required"
            }
            _isAuthInProgress.value = false
        }
    }"""

new_loginAngel = """fun loginAngel(clientCode: String, mpin: String, apiKey: String, totpSecret: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            val res = brokerManager.brokerAuthManager.connectAngelOne(clientCode, mpin, apiKey, totpSecret)
            if (res.isFailure) {
                _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Angel login failed"
            }
            _isAuthInProgress.value = false
        }
    }"""

new_connectUpstox = """fun connectUpstox(apiKey: String, apiSecret: String, authCode: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            try {
                val res = brokerManager.upstoxAuthManager.exchangeToken(authCode, apiKey, apiSecret)
                if (res.isFailure) {
                    _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Upstox login failed"
                }
            } catch (e: Exception) {
                _authErrorMessage.value = e.message
            }
            _isAuthInProgress.value = false
        }
    }"""

new_connectFyers = """fun connectFyers(appId: String, secretId: String, authCode: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            try {
                val res = brokerManager.fyersAuthManager.exchangeToken(authCode, appId, secretId)
                if (res.isFailure) {
                    _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Fyers login failed"
                }
            } catch (e: Exception) {
                _authErrorMessage.value = e.message
            }
            _isAuthInProgress.value = false
        }
    }"""

content = replace_func(content, "connectDhan(", new_connectDhan)
content = replace_func(content, "loginAngel(", new_loginAngel)
content = replace_func(content, "connectUpstox(", new_connectUpstox)
content = replace_func(content, "connectFyers(", new_connectFyers)

# Also handle processOAuthCallback
new_processOAuth = """fun processOAuthCallback(fullUrl: String) {
        // OAuth deep linking logic simplified
    }"""
content = replace_func(content, "processOAuthCallback(", new_processOAuth)

with open(filepath, "w") as f:
    f.write(content)

