import re

filepath = "app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content[:content.rfind("}")] + """
    var lastProcessedOAuthCode: String? = null
    var lastProcessedOAuthTime: Long = 0L
    var fyersRefreshToken: String? = null
    var fyersTokenTimestamp: Long = 0L
    var fyersPin: String? = null

    fun clearFyersSession() {
        fyersAccessToken = null
        fyersRefreshToken = null
        isFyersConnected = false
        fyersTokenTimestamp = 0L
    }
""" + "}"
with open(filepath, "w") as f:
    f.write(content)

