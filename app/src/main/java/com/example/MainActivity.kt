*** Begin Patch
*** Update File: app/src/main/java/com/example/MainActivity.kt
@@
-        val fullUrl = uri.toString()
-        android.util.Log.d("OAuthCallback", "MainActivity handleIntent deep link received: $fullUrl")
+        // Redact full URL logging to avoid leaking tokens/codes in logs
+        val fullUrl = uri.toString()
+        android.util.Log.d("OAuthCallback", "MainActivity handleIntent deep link received: <REDACTED>")
@@
-            val upstoxKey = viewModel.sessionManager.upstoxApiKey.takeIf { it.isNotBlank() }
-                ?: com.example.util.BrokerConfig.upstoxApiKey
-            val upstoxSecret = viewModel.sessionManager.upstoxApiSecret.takeIf { it.isNotBlank() }
-                ?: com.example.util.BrokerConfig.upstoxApiSecret
-            viewModel.connectUpstox(upstoxKey, upstoxSecret, code)
+            val upstoxKey = viewModel.sessionManager.upstoxApiKey.takeIf { it.isNotBlank() }
+                ?: com.example.util.BrokerConfig.upstoxApiKey
+            // Never send client secret from the Android client. Token exchange must be performed by backend only.
+            viewModel.connectUpstox(upstoxKey, "", code)
*** End Patch
