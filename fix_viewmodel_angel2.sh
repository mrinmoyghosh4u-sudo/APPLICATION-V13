cat app/src/main/java/com/example/viewmodel/MainViewModel.kt | head -n 290 > MainViewModel_fixed.kt
cat << 'INNEREOF' >> MainViewModel_fixed.kt
            } else {
                val isCodeExchange = !code.isNullOrBlank() || !authToken.isNullOrBlank()
                val tokenToUse = authToken ?: code ?: accessToken ?: ""
                val cidToUse = clientId ?: sessionManager.angelClientId
                if (tokenToUse.isBlank()) {
                    val msg = "Angel One OAuth redirect received, but JWT token/Code is missing."
                    _authErrorMessage.value = msg
                    _apiError.value = msg
                    _isAuthInProgress.value = false
                    return@launch
                }
                if (isCodeExchange) {
                    val exchangeResult = com.example.util.AngelAuthHelper.exchangeToken(tokenToUse)
                    exchangeResult.onSuccess { (jwt, refresh) ->
                        sessionManager.angelJwtToken = jwt
                        sessionManager.angelRefreshToken = refresh
                        if (cidToUse.isNotBlank()) sessionManager.angelClientId = cidToUse
                        sessionManager.activeBroker = "Angel One"
                        val profileRes = angelOneService.getProfile()
                        profileRes.onSuccess { profile ->
                            repository.updateProfile(
                                profile.copy(
                                    connectedBroker = "Angel One",
                                    isAngelConnected = true,
                                    angelClientId = cidToUse
                                )
                            )
                            runCatching { repository.syncWithBroker() }
                            _showConnectDialog.value = false
                            _isAuthInProgress.value = false
                            _authSuccessEvent.value = true
                        }.onFailure { err ->
                            _authErrorMessage.value = err.localizedMessage ?: "Failed to fetch profile"
                            _apiError.value = err.localizedMessage
                            _isAuthInProgress.value = false
                        }
                    }.onFailure { err ->
                        val msg = err.localizedMessage ?: "Angel One Token Exchange Failed."
                        _authErrorMessage.value = msg
                        _apiError.value = msg
                        _isAuthInProgress.value = false
                    }
                } else {
                    sessionManager.angelJwtToken = tokenToUse
                    if (cidToUse.isNotBlank()) sessionManager.angelClientId = cidToUse
                    sessionManager.activeBroker = "Angel One"
                    val profileRes = angelOneService.getProfile()
                    profileRes.onSuccess { profile ->
                        repository.updateProfile(
                            profile.copy(
                                connectedBroker = "Angel One",
                                isAngelConnected = true,
                                angelClientId = cidToUse
                            )
                        )
                        runCatching { repository.syncWithBroker() }
                        _showConnectDialog.value = false
                        _isAuthInProgress.value = false
                        _authSuccessEvent.value = true
                    }.onFailure { err ->
                        _authErrorMessage.value = err.localizedMessage ?: "Failed to fetch profile"
                        _apiError.value = err.localizedMessage
                        _isAuthInProgress.value = false
                    }
                }
            }
        }
    }
INNEREOF
cat app/src/main/java/com/example/viewmodel/MainViewModel.kt | tail -n +368 >> MainViewModel_fixed.kt
mv MainViewModel_fixed.kt app/src/main/java/com/example/viewmodel/MainViewModel.kt
