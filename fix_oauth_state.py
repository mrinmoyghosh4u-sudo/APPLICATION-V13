import re

with open("app/src/main/java/com/example/viewmodel/MainViewModel.kt", "r") as f:
    content = f.read()

new_content = """            val state = uri.getQueryParameter("state") ?: ""
            val callbackState = state.trim()

            var pendingSession = sessionManager.pendingOAuthSession
            
            // STRICT VALIDATION
            if (pendingSession == null) {
                _isAuthInProgress.value = false
                _authErrorMessage.value = "OAuth Error: No pending session found. Please try again."
                return@launch
            }
            
            if (pendingSession.consumed) {
                _isAuthInProgress.value = false
                _authErrorMessage.value = "OAuth Error: This callback has already been processed (duplicate)."
                return@launch
            }
            
            if (callbackState.isBlank() || pendingSession.state != callbackState) {
                _isAuthInProgress.value = false
                _authErrorMessage.value = "OAuth Error: State mismatch. Possible CSRF attack."
                return@launch
            }"""

content = re.sub(
    r'            val state = uri\.getQueryParameter\("state"\) \?: "".*?sessionManager\.pendingOAuthSession = pendingSession\s*\}?\s*\}',
    new_content,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/viewmodel/MainViewModel.kt", "w") as f:
    f.write(content)

