import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersApi.kt"
with open(filepath, "r") as f:
    content = f.read()

models = """
data class FyersOptionChainResponse(
    val s: String?,
    val data: FyersOptionChainData?
)

data class FyersOptionChainData(
    val expiryData: List<FyersExpiryData>?
)

data class FyersExpiryData(
    val expiry: String?,
    val optionChain: List<FyersOptionContract>?
)

data class FyersOptionContract(
    val strike_price: Double?,
    val symbol: String?,
    val ltp: Double?,
    val oi: Double?,
    val volume: Double?,
    val option_type: String?,
    val ch: Double?,
    val chp: Double?,
    val bid: Double?,
    val ask: Double?
)
"""

content = content.replace("interface FyersApi {", models + "\ninterface FyersApi {")

endpoint = """
    @GET("data/options-chain-v3")
    suspend fun getOptionChain(
        @Header("Authorization") auth: String,
        @Query("symbol") symbol: String,
        @Query("strikecount") strikecount: Int = 10,
        @Query("timestamp") timestamp: String = ""
    ): Response<FyersOptionChainResponse>
"""

content = content.replace("interface FyersApi {", "interface FyersApi {" + endpoint)

with open(filepath, "w") as f:
    f.write(content)

