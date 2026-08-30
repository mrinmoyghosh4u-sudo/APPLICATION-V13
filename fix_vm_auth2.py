import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

def replace_func(content, func_name, new_body):
    start = content.find("fun " + func_name)
    if start == -1: return content
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

new_connectUpstox = """fun connectUpstox(apiKey: String, apiSecret: String, authCode: String = "") {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            try {
                sessionManager.upstoxApiKey = apiKey
                sessionManager.upstoxApiSecret = apiSecret
                if (authCode.isNotBlank()) {
                    val res = brokerManager.upstoxAuthManager.exchangeAuthCode(authCode)
                    if (res.isFailure) {
                        _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Upstox login failed"
                    }
                }
            } catch (e: Exception) {
                _authErrorMessage.value = e.message
            }
            _isAuthInProgress.value = false
        }
    }"""

new_connectFyers = """fun connectFyers(appId: String, secretId: String, authCode: String = "") {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authErrorMessage.value = null
            try {
                sessionManager.fyersAppId = appId
                sessionManager.fyersSecretId = secretId
                if (authCode.isNotBlank()) {
                    val res = brokerManager.fyersAuthManager.exchangeAuthCode(authCode)
                    if (res.isFailure) {
                        _authErrorMessage.value = res.exceptionOrNull()?.message ?: "Fyers login failed"
                    }
                }
            } catch (e: Exception) {
                _authErrorMessage.value = e.message
            }
            _isAuthInProgress.value = false
        }
    }"""

content = replace_func(content, "connectUpstox(", new_connectUpstox)
content = replace_func(content, "connectFyers(", new_connectFyers)

# Fix collectLatest Pair ambiguity
old_collect = "}.collectLatest { (combinedList, changed) ->"
new_collect = "}.collectLatest { pair: Pair<List<com.example.data.model.WatchlistItem>, Boolean> ->\n                val combinedList = pair.first\n                val changed = pair.second"
content = content.replace(old_collect, new_collect)

with open(filepath, "w") as f:
    f.write(content)

