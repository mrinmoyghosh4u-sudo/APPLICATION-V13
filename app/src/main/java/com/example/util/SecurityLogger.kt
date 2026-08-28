package com.example.util

import android.util.Log
import com.example.BuildConfig

object SecurityLogger {
    private const val TAG = "SecurityLogger"
    private val SENSITIVE_KEYS = listOf(
        "token", "access_token", "refresh_token", "api_key", "apiKey",
        "secret", "appSecret", "auth_code", "authorization", "client_secret",
        "password", "mpin", "totp", "feed_token"
    )

    fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, redact(message))
        }
    }

    private fun redact(input: String): String {
        var redacted = input
        for (key in SENSITIVE_KEYS) {
            // Regex to match key=value or "key":"value" and replace value
            val regex1 = Regex("($key[:=])([^,&\"\\s{}]+)")
            redacted = redacted.replace(regex1, "$1***REDACTED***")
        }
        return redacted
    }
}
