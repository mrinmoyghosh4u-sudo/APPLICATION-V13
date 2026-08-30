import re

filepath = "app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

new_props = """
    var angelAuthToken: String
        get() = prefs.getString(KEY_ANGEL_JWT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_JWT, value).apply()
        
    var angelFeedToken: String
        get() = prefs.getString(KEY_ANGEL_FEED, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_FEED, value).apply()
        
    var angelRefreshToken: String
        get() = prefs.getString(KEY_ANGEL_REFRESH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_REFRESH, value).apply()

    var angelApiKey: String
        get() = prefs.getString(KEY_ANGEL_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_API_KEY, value).apply()
        
    var angelClientCode: String
        get() = prefs.getString(KEY_ANGEL_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_CLIENT_ID, value).apply()

    var angelClientPin: String
        get() = prefs.getString("angel_mpin_enc", "") ?: ""
        set(value) = prefs.edit().putString("angel_mpin_enc", value).apply()

    var angelTotpSecret: String
        get() = prefs.getString(KEY_ANGEL_TOTP_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_TOTP_SECRET, value).apply()

"""

# Insert these properties before "var upstoxRefreshToken"
content = content.replace("    var upstoxRefreshToken: String? = null", new_props + "    var upstoxRefreshToken: String? = null")

with open(filepath, "w") as f:
    f.write(content)
