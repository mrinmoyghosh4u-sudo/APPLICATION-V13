import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

old_fyers = """    // FYERS
    var fyersAppId: String?
        get() = prefs.getString("fyers_app_id", "")
        set(value) = prefs.edit().putString("fyers_app_id", value).apply()

    var fyersSecretId: String?
        get() = prefs.getString("fyers_secret_id", "")
        set(value) = prefs.edit().putString("fyers_secret_id", value).apply()

    var fyersAccessToken: String?
        get() = prefs.getString("fyers_access_token", null)
        set(value) = prefs.edit().putString("fyers_access_token", value).apply()

    var isFyersConnected: Boolean
        get() = prefs.getBoolean("is_fyers_connected", false) && !fyersAccessToken.isNullOrBlank()
        set(value) = prefs.edit().putBoolean("is_fyers_connected", value).apply()

    fun clearFyersSession() {
        prefs.edit().apply {
            remove("fyers_access_token")
            putBoolean("is_fyers_connected", false)
        }.apply()
    }"""

new_fyers = """    // FYERS
    var fyersAppId: String
        get() = safeGetToken("fyers_app_id_enc") ?: ""
        set(value) {
            safeSetToken("fyers_app_id_enc", value)
            prefs.edit().putString("fyers_app_id_enc", value).apply() // Keep compatibility if needed, but safeSetToken is better
        }

    var fyersSecretId: String
        get() = safeGetToken("fyers_secret_id_enc") ?: ""
        set(value) {
            safeSetToken("fyers_secret_id_enc", value)
            prefs.edit().putString("fyers_secret_id_enc", value).apply()
        }

    var fyersAccessToken: String?
        get() = safeGetToken("fyers_access_token_enc")
        set(value) = safeSetToken("fyers_access_token_enc", value)

    var fyersRefreshToken: String?
        get() = safeGetToken("fyers_refresh_token_enc")
        set(value) = safeSetToken("fyers_refresh_token_enc", value)
        
    var fyersTokenTimestamp: Long
        get() = prefs.getLong("fyers_token_time", 0L)
        set(value) = prefs.edit().putLong("fyers_token_time", value).apply()

    var isFyersConnected: Boolean
        get() = prefs.getBoolean("is_fyers_connected", false) && !fyersAccessToken.isNullOrBlank()
        set(value) = prefs.edit().putBoolean("is_fyers_connected", value).apply()

    fun clearFyersSession() {
        prefs.edit().apply {
            remove("fyers_token_time")
            putBoolean("is_fyers_connected", false)
        }.apply()
        safeSetToken("fyers_access_token_enc", null)
        safeSetToken("fyers_refresh_token_enc", null)
    }"""

content = content.replace(old_fyers, new_fyers)

with open(filepath, "w") as f:
    f.write(content)
