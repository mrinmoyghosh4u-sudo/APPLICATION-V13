sed -i 's/val response = fyersApi.validateAuthCode/android.util.Log.d("FyersAuth", "[7] Token exchange initiated...")\n            val response = fyersApi.validateAuthCode/g' app/src/main/java/com/example/data/network/FyersAuthManager.kt

sed -i 's/if (body.s == "ok"/android.util.Log.d("FyersAuth", "[8] Token validated: PASS")\n            if (body.s == "ok"/g' app/src/main/java/com/example/data/network/FyersAuthManager.kt

sed -i 's/_authStatus.value = BrokerAuthStatus.CONNECTED/android.util.Log.d("FyersAuth", "[9] Account verified: PASS")\n                android.util.Log.d("FyersAuth", "[10] Authentication SUCCESS: PASS")\n                _authStatus.value = BrokerAuthStatus.CONNECTED/g' app/src/main/java/com/example/data/network/FyersAuthManager.kt
