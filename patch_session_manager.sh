sed -i '/var activeBroker: String/i \
    var pendingOAuthBroker: String\
        get() = prefs.getString("pending_oauth_broker", "") ?: ""\
        set(value) = prefs.edit().putString("pending_oauth_broker", value).apply()\
' app/src/main/java/com/example/data/network/SessionManager.kt
