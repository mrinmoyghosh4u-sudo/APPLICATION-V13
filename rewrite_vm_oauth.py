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

new_processOAuth = """fun processOAuthCallback(fullUrl: String) {
        viewModelScope.launch {
            val codeMatch = Regex(\"\"\"[?&#]code=([^&#]+)\"\"\", RegexOption.IGNORE_CASE).find(fullUrl)
            val authCodeMatch = Regex(\"\"\"[?&#]auth_code=([^&#]+)\"\"\", RegexOption.IGNORE_CASE).find(fullUrl)
            val code = codeMatch?.groupValues?.get(1) ?: authCodeMatch?.groupValues?.get(1) ?: return@launch
            
            // Try Fyers if pending
            if (sessionManager.fyersAppId.isNotBlank() && sessionManager.fyersSecretId.isNotBlank()) {
                connectFyers(sessionManager.fyersAppId, sessionManager.fyersSecretId, code)
            } else if (sessionManager.upstoxApiKey.isNotBlank() && sessionManager.upstoxApiSecret.isNotBlank()) {
                connectUpstox(sessionManager.upstoxApiKey, sessionManager.upstoxApiSecret, code)
            }
        }
    }"""
content = replace_func(content, "processOAuthCallback(", new_processOAuth)

with open(filepath, "w") as f:
    f.write(content)
