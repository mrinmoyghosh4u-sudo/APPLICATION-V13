sed -i 's/android.util.Log.d("DhanAuth", "redirect received")/android.util.Log.d("Auth", "[5] Callback received: PASS")\n            android.util.Log.d("DhanAuth", "redirect received")/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt

sed -i 's/val isUpstox =/android.util.Log.d("Auth", "[6] Authorization code received: PASS (code=$code)")\n            val isUpstox =/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt
