import re

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    content = f.read()

# Replace the onSuccess block for AngelAuthHelper.exchangeToken
target = r"exchangeResult\.onSuccess \{ \(jwt, refresh\) ->\s+sessionManager\.angelJwtToken = jwt\s+sessionManager\.angelRefreshToken = refresh"
replacement = """exchangeResult.onSuccess { tokens ->
                        sessionManager.angelJwtToken = tokens.jwtToken
                        sessionManager.angelRefreshToken = tokens.refreshToken
                        sessionManager.angelFeedToken = tokens.feedToken
                        val urlFeedToken = uri.getQueryParameter("feed_token")
                        if (!urlFeedToken.isNullOrBlank() && tokens.feedToken.isBlank()) {
                            sessionManager.angelFeedToken = urlFeedToken
                        }"""

new_content = re.sub(target, replacement, content, flags=re.MULTILINE)

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'w') as f:
    f.write(new_content)
