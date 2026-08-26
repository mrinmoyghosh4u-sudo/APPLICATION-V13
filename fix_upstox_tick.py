import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

new_content = """                                if (receivedTick) {
                                    if (!hasFirstTick) {
                                        android.util.Log.i(TAG, "[UPSTOX_FIRST_REAL_TICK] First valid Upstox real tick received!")
                                    }
                                    hasFirstTick = true"""

content = content.replace(
    '                                if (receivedTick) {\n                                    hasFirstTick = true',
    new_content
)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
