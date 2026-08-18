import re

with open('/app/applet/app/src/main/java/com/example/data/network/AngelOneApi.kt', 'r') as f:
    content = f.read()

new_method = """    @POST("rest/secure/angelbroking/market/v1/optionchain")
    suspend fun getOptionChain(
        @Body request: AngelOptionChainRequest
    ): Response<AngelOptionChainResponse>
"""

if "fun getOptionChain" not in content:
    content = content.replace('}', new_method + '}')
    with open('/app/applet/app/src/main/java/com/example/data/network/AngelOneApi.kt', 'w') as f:
        f.write(content)
    print("AngelOneApi Patched!")
else:
    print("Already patched.")
