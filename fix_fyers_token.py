import re

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    content = f.read()

# Fix the token logic to strip appId if present, ensuring it is a valid JWT
new_logic = """
                var actualToken = token
                if (actualToken.contains(":")) {
                    actualToken = actualToken.split(":")[1]
                }
                
                val tokenParts = actualToken.split(".")
                val hsmToken = if (tokenParts.size >= 2) {
                    val payloadBytes = android.util.Base64.decode(tokenParts[1], android.util.Base64.URL_SAFE)
                    org.json.JSONObject(String(payloadBytes)).optString("hsm_key", actualToken)
                } else actualToken
"""

content = re.sub(
    r'val tokenParts = token\.split\("\."\)\s+val hsmToken = if \(tokenParts\.size >= 2\) \{\s+val payloadBytes = android\.util\.Base64\.decode\(tokenParts\[1\], android\.util\.Base64\.URL_SAFE\)\s+org\.json\.JSONObject\(String\(payloadBytes\)\)\.optString\("hsm_key", token\)\s+\} else token',
    new_logic.strip(),
    content
)

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(content)
