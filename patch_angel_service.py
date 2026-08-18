import re

with open('/app/applet/app/src/main/java/com/example/data/network/AngelOneBrokerService.kt', 'r') as f:
    content = f.read()

if "import com.example.data.model.OptionChainInstrumentMaster" not in content:
    content = content.replace("import com.example.data.model.OptionStrikeItem", "import com.example.data.model.OptionStrikeItem\nimport com.example.data.model.OptionChainInstrumentMaster")

pattern = r'    override suspend fun getOptionChain\(symbol: String, expiry: String\): Result<List<OptionStrikeItem>> \{\s*return runCatching \{\s*val spotPrice = getMarketQuotes\(listOf\(symbol\)\)\.getOrNull\(\)\?\.firstOrNull\(\)\?\.ltp \?\: 24850\.40\s*emptyList\(\)\s*\}\s*\}'

replacement = """    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> {
        return runCatching {
            val instrument = OptionChainInstrumentMaster.getInstrument(symbol)
            if (instrument == null) throw Exception("Option Chain unavailable for this instrument.")
            
            val request = AngelOptionChainRequest(
                exchange = if (instrument.exchange == "MCX") "MCX" else "NFO", // typical for derivatives
                symboltoken = instrument.angelToken,
                expirydate = expiry
            )
            val response = api.getOptionChain(request)
            if (response.isSuccessful && response.body()?.status == true) {
                response.body()?.data?.map { item ->
                    OptionStrikeItem(
                        strikePrice = item.strikePrice?.toDoubleOrNull() ?: 0.0,
                        callOi = "${item.callOi ?: 0.0}",
                        callChgOi = "${item.callChgOi ?: 0.0}",
                        callIv = 0.0,
                        callLtp = item.callLtp ?: 0.0,
                        callDelta = 0.0,
                        putDelta = 0.0,
                        putLtp = item.putLtp ?: 0.0,
                        putIv = 0.0,
                        putChgOi = "${item.putChgOi ?: 0.0}",
                        putOi = "${item.putOi ?: 0.0}",
                        callVolume = "${item.callVolume ?: 0.0}",
                        putVolume = "${item.putVolume ?: 0.0}"
                    )
                } ?: emptyList()
            } else {
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }"""

new_content = re.sub(pattern, replacement, content)

if new_content != content:
    with open('/app/applet/app/src/main/java/com/example/data/network/AngelOneBrokerService.kt', 'w') as f:
        f.write(new_content)
    print("AngelOneBrokerService Patched!")
else:
    print("AngelOneBrokerService Patch failed!")

