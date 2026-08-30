import re

filepath = "app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
    var fyersAppId: String
        get() = prefs.getString("fyers_app_id", "") ?: ""
        set(value) = prefs.edit().putString("fyers_app_id", value).apply()

    var fyersSecretId: String
        get() = prefs.getString("fyers_secret_id", "") ?: ""
        set(value) = prefs.edit().putString("fyers_secret_id", value).apply()

    var fyersRedirectUri: String
        get() = prefs.getString("fyers_redirect_uri", "kingkhan://oauth/callback") ?: "kingkhan://oauth/callback"
        set(value) = prefs.edit().putString("fyers_redirect_uri", value).apply()
        
    var upstoxApiKey: String
        get() = prefs.getString("upstox_api_key", "") ?: ""
        set(value) = prefs.edit().putString("upstox_api_key", value).apply()

    var upstoxApiSecret: String
        get() = prefs.getString("upstox_api_secret", "") ?: ""
        set(value) = prefs.edit().putString("upstox_api_secret", value).apply()

    fun isDhanTokenIdConsumed(token: String): Boolean {
        return prefs.getBoolean("dhan_token_consumed_$token", false)
    }
"""

# Override the previous markDhanTokenIdConsumed
old_mark = """    fun markDhanTokenIdConsumed(token: String) {
        // No-op for now
    }"""
new_mark = """    fun markDhanTokenIdConsumed(token: String) {
        prefs.edit().putBoolean("dhan_token_consumed_$token", true).apply()
    }"""
content = content.replace(old_mark, new_mark)

idx = content.rfind("}")
if idx != -1:
    content = content[:idx] + props + content[idx:]
    with open(filepath, "w") as f:
        f.write(content)
