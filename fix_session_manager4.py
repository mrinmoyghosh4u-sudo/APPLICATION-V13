import re

filepath = "app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
    fun hasUpstoxSession(): Boolean = isUpstoxConnected
    fun hasFyersSession(): Boolean = isFyersConnected
    fun hasAngelSession(): Boolean = isAngelConnected
    
    var angelTokenTimestamp: Long
        get() = prefs.getLong("angel_token_timestamp", 0L)
        set(value) = prefs.edit().putLong("angel_token_timestamp", value).apply()
        
    var angelClientId: String
        get() = prefs.getString("angel_client_id", "") ?: ""
        set(value) = prefs.edit().putString("angel_client_id", value).apply()
"""

idx = content.rfind("}")
if idx != -1:
    content = content[:idx] + props + content[idx:]
    with open(filepath, "w") as f:
        f.write(content)
