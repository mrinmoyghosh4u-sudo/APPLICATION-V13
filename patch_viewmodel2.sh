sed -i '/connectUpstox(upstoxKey, upstoxSecret, code)/i \
                sessionManager.pendingOAuthBroker = ""' app/src/main/java/com/example/viewmodel/MainViewModel.kt

sed -i '/connectFyers(fyersAppId, fyersSecretId, code)/i \
                sessionManager.pendingOAuthBroker = ""' app/src/main/java/com/example/viewmodel/MainViewModel.kt
