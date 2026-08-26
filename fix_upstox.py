import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

# Remove the HTTP redirect block:
redirect_block_start = '                // Resolve HTTP 302 Redirect for Upstox WebSockets if it points to api.upstox.com'
redirect_block_end = '                val request = okhttp3.Request.Builder()'

pattern = re.compile(re.escape(redirect_block_start) + r'.*?' + re.escape(redirect_block_end), re.DOTALL)
content = pattern.sub(redirect_block_end, content)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
