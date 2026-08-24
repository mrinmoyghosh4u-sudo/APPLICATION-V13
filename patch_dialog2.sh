sed -i '/val loginUrl = com.example.util.UpstoxAuthHelper.buildLoginUrl/a \
                                sessionManager.pendingOAuthBroker = "Upstox"
' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt

sed -i '/val loginUrl = com.example.util.FyersAuthHelper.buildLoginUrl/a \
                                sessionManager.pendingOAuthBroker = "Fyers"
' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt
