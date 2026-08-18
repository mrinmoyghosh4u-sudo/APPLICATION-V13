import re

with open('/app/applet/app/src/main/java/com/example/data/network/BrokerApiModels.kt', 'r') as f:
    content = f.read()

angel_models = """
@JsonClass(generateAdapter = true)
data class AngelOptionChainRequest(
    @Json(name = "exchange") val exchange: String,
    @Json(name = "symboltoken") val symboltoken: String,
    @Json(name = "expirydate") val expirydate: String
)

@JsonClass(generateAdapter = true)
data class AngelOptionChainResponse(
    @Json(name = "status") val status: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "errorcode") val errorcode: String,
    @Json(name = "data") val data: List<AngelOptionChainItem>?
)

@JsonClass(generateAdapter = true)
data class AngelOptionChainItem(
    @Json(name = "strikeprice") val strikePrice: String?,
    @Json(name = "ce_oi") val callOi: Double?,
    @Json(name = "ce_oichange") val callChgOi: Double?,
    @Json(name = "ce_ltp") val callLtp: Double?,
    @Json(name = "ce_netchange") val callNetChg: Double?,
    @Json(name = "pe_oi") val putOi: Double?,
    @Json(name = "pe_oichange") val putChgOi: Double?,
    @Json(name = "pe_ltp") val putLtp: Double?,
    @Json(name = "pe_netchange") val putNetChg: Double?,
    @Json(name = "ce_volume") val callVolume: Double?,
    @Json(name = "pe_volume") val putVolume: Double?
)
"""

if "AngelOptionChainRequest" not in content:
    content = content + "\n" + angel_models
    with open('/app/applet/app/src/main/java/com/example/data/network/BrokerApiModels.kt', 'w') as f:
        f.write(content)
    print("BrokerApiModels Patched!")
else:
    print("Already patched.")
