import os
import re

filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

# Replace refreshSession default parameter and usage
content = content.replace('suspend fun refreshSession(pin: String = "1234"): Result<String> = withContext(Dispatchers.IO) {',
                          'suspend fun refreshSession(): Result<String> = withContext(Dispatchers.IO) {')

# Find where it calls validateRefreshToken
content = content.replace('val request = FyersRefreshTokenRequest(appIdHash = appIdHash, refresh_token = refreshToken, pin = pin)',
                          'val request = FyersRefreshTokenRequest(appIdHash = appIdHash, refresh_token = refreshToken, pin = sessionManager.fyersPin)')

with open(filepath, "w") as f:
    f.write(content)

