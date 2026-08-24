import os

filepath = "app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

fyers_props = """
    // FYERS
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
    }
}
"""
if "fyersAppId" not in content:
    # replace the very last brace
    idx = content.rfind("}")
    if idx != -1:
        content = content[:idx] + fyers_props
        with open(filepath, "w") as f:
            f.write(content)

