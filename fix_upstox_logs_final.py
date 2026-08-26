import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace(
    '[UPSTOX_FIRST_REAL_TICK] First valid Upstox real tick received',
    '[UPSTOX_FIRST_REAL_TICK] / UPSTOX_LIVE First valid Upstox real tick received'
)
content = content.replace(
    '[UPSTOX_FIRST_REAL_TICK] First valid Upstox V3 real tick received',
    '[UPSTOX_FIRST_REAL_TICK] / UPSTOX_LIVE First valid Upstox V3 real tick received'
)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
