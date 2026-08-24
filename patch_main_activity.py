import os

filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
    "onTradeSmartLogin = { apiKey, clientId, token -> viewModel.connectTradeSmart(apiKey, clientId, token) }",
    "onTradeSmartLogin = { apiKey, clientId, token -> viewModel.connectTradeSmart(apiKey, clientId, token) },\n                                onFyersLogin = { appId, secretId, authCode -> viewModel.connectFyers(appId, secretId, authCode) }"
)

with open(filepath, "w") as f:
    f.write(content)
