sed -i 's/val response = upstoxApi.getAccessToken/android.util.Log.d("UpstoxAuth", "[7] Token exchange initiated...")\n            val response = upstoxApi.getAccessToken/g' app/src/main/java/com/example/data/network/UpstoxAuthManager.kt

sed -i 's/val accessToken = body.accessToken/android.util.Log.d("UpstoxAuth", "[8] Token validated: PASS")\n            val accessToken = body.accessToken/g' app/src/main/java/com/example/data/network/UpstoxAuthManager.kt

sed -i 's/_authStatus.value = BrokerAuthStatus.CONNECTED/android.util.Log.d("UpstoxAuth", "[9] Account verified: PASS")\n                android.util.Log.d("UpstoxAuth", "[10] Authentication SUCCESS: PASS")\n                _authStatus.value = BrokerAuthStatus.CONNECTED/g' app/src/main/java/com/example/data/network/UpstoxAuthManager.kt
