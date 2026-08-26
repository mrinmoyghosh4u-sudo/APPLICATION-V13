import re

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    content = f.read()

content = content.replace(
    'android.util.Log.i(TAG, "First valid Upstox V3 real tick received!")',
    'android.util.Log.i(TAG, "[UPSTOX_FIRST_REAL_TICK] First valid Upstox V3 real tick received!")'
)
content = content.replace(
    'android.util.Log.i(TAG, "[UPSTOX_LIVE] Feed is now live!")',
    ''
) # just in case it was there

# Let's search for hasFirstTick = true and add the log before it
def replace_has_first_tick(match):
    return """                if (!hasFirstTick) {
                    try { android.util.Log.i(TAG, "[UPSTOX_FIRST_REAL_TICK] / UPSTOX_LIVE First valid Upstox V3 real tick received!") } catch (_: Throwable) {}
                }
                hasFirstTick = true"""

if "[UPSTOX_FIRST_REAL_TICK]" not in content:
    content = re.sub(r'\bhasFirstTick = true\b', replace_has_first_tick, content, count=1)

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(content)
