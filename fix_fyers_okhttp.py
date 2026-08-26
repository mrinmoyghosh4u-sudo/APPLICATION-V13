import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

# Add User-Agent and query parameters to the OkHttp request
new_logic = """
        val url = "wss://socket.fyers.in/hsm/v1-5/prod?access_token=$token"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Origin", "https://myapi.fyers.in")
            .build()
"""

content = re.sub(
    r'val url = "wss://socket\.fyers\.in/hsm/v1-5/prod"\s+val request = okhttp3\.Request\.Builder\(\)\s+\.url\(url\)\s+\.build\(\)',
    new_logic.strip(),
    content
)

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
