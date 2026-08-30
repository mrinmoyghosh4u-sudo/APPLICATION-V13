import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix Upstox
content = content.replace("sessionManager.upstoxRedirectUri", '"https://application-beige-psi.vercel.app/oauth"')
content = content.replace(",\n            redirectUri = redirectUri", "")
content = content.replace("redirectUri = redirectUri", "")

# Fix Fyers
content = content.replace("state = randomState", "")
content = content.replace("com.example.util.UpstoxAuthHelper.buildLoginUrl(cleanKey, redirectUri, state = randomState)", "com.example.util.UpstoxAuthHelper.buildLoginUrl(cleanKey, redirectUri)")
content = content.replace("com.example.util.FyersAuthHelper.buildLoginUrl(cleanAppId, redirectUri, state = randomState)", "com.example.util.FyersAuthHelper.buildLoginUrl(cleanAppId, redirectUri)")
content = content.replace("com.example.util.DhanAuthHelper.buildLoginUrl(cleanClientId, redirectUri, state = randomState)", "com.example.util.DhanAuthHelper.buildLoginUrl(cleanClientId, redirectUri)")
content = content.replace("brokerManager.healthManager.reportSuccessfulRequest(com.example.data.network.ProviderHealthManager.PROVIDER_DHAN, 100L)", "")

with open(filepath, "w") as f:
    f.write(content)

