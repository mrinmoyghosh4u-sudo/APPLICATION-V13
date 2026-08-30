import re
filepath = "app/src/main/java/com/example/data/network/FyersAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
"""                val redirectUri = sessionManager.pendingOAuthSession?.redirectUri?.takeIf { it.isNotBlank() }
                    ?: sessionManager.fyersRedirectUri.takeIf { it.isNotBlank() }
                    ?: FyersAuthHelper.DEFAULT_REDIRECT_URI""",
"""                val redirectUri = sessionManager.fyersRedirectUri.takeIf { it.isNotBlank() } ?: FyersAuthHelper.DEFAULT_REDIRECT_URI"""
)

with open(filepath, "w") as f:
    f.write(content)
