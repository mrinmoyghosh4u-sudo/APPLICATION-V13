import os

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

fyers_method = """
    fun connectFyers(appId: String, secretId: String, authCode: String) {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            
            sessionManager.fyersAppId = appId
            sessionManager.fyersSecretId = secretId
            
            val res = brokerManager.fyersAuthManager.exchangeAuthCode(authCode)
            _isAuthInProgress.value = false
            
            if (res.isSuccess) {
                _brokerSwitchStatus.value = "Broker Connected • Fyers (Market Data)"
                _authSuccessEvent.value = true
                
                // Immediately connect market data
                brokerManager.fyersMarketDataService.connect()
                
            } else {
                _authErrorMessage.value = "Fyers Authentication Failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }
"""

if "fun connectFyers" not in content:
    content = content.replace("fun connectTradeSmart(", fyers_method + "\n    fun connectTradeSmart(")
    with open(filepath, "w") as f:
        f.write(content)
    print("Added connectFyers")
