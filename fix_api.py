import re

content = open("app/src/main/java/com/example/data/network/AngelOneApi.kt").read()

api_func = """
    @POST("rest/secure/angelbroking/historical/v1/getCandleData")
    suspend fun getHistoricalData(
        @Body request: AngelHistoricalRequest
    ): Response<AngelOneResponse<List<List<Any>>>>
}"""

content = re.sub(r'\}\s*$', api_func, content)
open("app/src/main/java/com/example/data/network/AngelOneApi.kt", "w").write(content)
