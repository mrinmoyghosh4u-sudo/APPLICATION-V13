import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

# Replace the wsUrl logic with 302 redirect resolver
new_logic = """
                var wsUrl = authRes.body()?.data?.authorized_redirect_uri ?: authRes.body()?.data?.authorizedRedirectUri
                if (wsUrl.isNullOrBlank()) {
                    _connectionState.value = "AUTH_FAILED"
                    return@launch
                }
                
                // Resolve HTTP 302 Redirect for Upstox WebSockets if it points to api.upstox.com
                try {
                    val httpUrl = wsUrl.replace("wss://", "https://").replace("ws://", "http://")
                    val redirectRequest = okhttp3.Request.Builder()
                        .url(httpUrl)
                        .header("Authorization", authHeader)
                        .build()
                    val redirectClient = client.newBuilder().followRedirects(false).build()
                    val redirectResponse = redirectClient.newCall(redirectRequest).execute()
                    if (redirectResponse.isRedirect) {
                        val location = redirectResponse.header("Location")
                        if (!location.isNullOrBlank()) {
                            wsUrl = location.replace("https://", "wss://").replace("http://", "ws://")
                        }
                    }
                    redirectResponse.close()
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Failed to resolve Upstox WS redirect: ${e.message}")
                }
"""

content = re.sub(
    r'val wsUrl = authRes\.body\(\)\?\.data\?\.authorizedRedirectUri\s+if \(wsUrl\.isNullOrBlank\(\)\) \{\s+_connectionState\.value = "AUTH_FAILED"\s+return@launch\s+\}',
    new_logic.strip(),
    content
)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
