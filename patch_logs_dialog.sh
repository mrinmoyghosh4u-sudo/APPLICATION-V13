sed -i 's/val redirectUri = sessionManager.upstoxRedirectUri.takeIf/android.util.Log.d("UpstoxAuth", "[1] Credentials: PASS")\n                                val redirectUri = sessionManager.upstoxRedirectUri.takeIf/g' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt

sed -i 's/val loginUrl = com.example.util.UpstoxAuthHelper.buildLoginUrl/val loginUrl = com.example.util.UpstoxAuthHelper.buildLoginUrl/g' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt

sed -i 's/val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))/android.util.Log.d("UpstoxAuth", "[2] Authorization URL generated: PASS")\n                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))/g' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt

sed -i 's/context.startActivity(intent)/context.startActivity(intent)\n                                    android.util.Log.d("UpstoxAuth", "[3] Browser opened: PASS")/g' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt

