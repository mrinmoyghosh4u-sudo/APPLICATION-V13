with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace('"wss://api.mstock.trade/openapi/typea/ws"', '"wss://ws.mstock.trade"')
content = content.replace('val fullUrl = if (token.isNotBlank()) "$WS_URL?jwtToken=$token&key=$apiKey" else WS_URL', 'val fullUrl = if (token.isNotBlank() && apiKey.isNotBlank()) "$WS_URL?API_KEY=$apiKey&ACCESS_TOKEN=$token" else WS_URL')

with open("app/src/main/java/com/example/data/network/MStockMarketDataService.kt", "w") as f:
    f.write(content)
