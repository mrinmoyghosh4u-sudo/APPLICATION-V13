sed -i '/return@launch/!b;n;c\
            } else if (isFyers \&\& \!code.isNullOrBlank()) {\
                val fyersAppId = sessionManager.fyersAppId ?: ""\
                val fyersSecretId = sessionManager.fyersSecretId ?: ""\
                connectFyers(fyersAppId, fyersSecretId, code)\
                return@launch\
            } else {\
                _authErrorMessage.value = "Failed to determine broker or extract code from URL: $fullUrl"\
                _isAuthInProgress.value = false\
            }\
' app/src/main/java/com/example/viewmodel/MainViewModel.kt
