import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix plaintext storage of Fyers App ID and Secret
content = content.replace('prefs.edit().putString("fyers_app_id_enc", value).apply() // Keep compatibility if needed, but safeSetToken is better', "")
content = content.replace('prefs.edit().putString("fyers_secret_id_enc", value).apply()', "")

# Add PIN property
pin_prop = """
    var fyersPin: String
        get() = safeGetToken("fyers_pin_enc") ?: ""
        set(value) = safeSetToken("fyers_pin_enc", value)
"""

content = content.replace("var fyersAccessToken: String?", pin_prop + "\n    var fyersAccessToken: String?")

with open(filepath, "w") as f:
    f.write(content)

