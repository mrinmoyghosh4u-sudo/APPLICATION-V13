import re

filepath = "app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

# Remove the appended block
content = content.replace("""
    var upstoxRefreshToken: String? = null
    var upstoxTokenTimestamp: Long = 0L

    fun clearUpstoxSession() {
        upstoxAccessToken = null
        upstoxRefreshToken = null
        isUpstoxConnected = false
        upstoxTokenTimestamp = 0L
    }
""", "")

# Insert it before the last closing brace
content = content[:content.rfind("}")] + """
    var upstoxRefreshToken: String? = null
    var upstoxTokenTimestamp: Long = 0L

    fun clearUpstoxSession() {
        upstoxAccessToken = null
        upstoxRefreshToken = null
        isUpstoxConnected = false
        upstoxTokenTimestamp = 0L
    }
""" + "}"

with open(filepath, "w") as f:
    f.write(content)
