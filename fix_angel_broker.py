import re

with open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt', 'r') as f:
    content = f.read()

content = content.replace("class AngelOneBrokerService(\n    private val api: AngelOneApi,\n    private val sessionManager: SessionManager\n)", 
"""class AngelOneBrokerService(
    private val api: AngelOneApi,
    private val sessionManager: SessionManager,
    private val instrumentMaster: InstrumentMasterService
)""")

# Update getOptionChain and getOptionExpiries
content = content.replace("val instrument = OptionChainInstrumentMaster.getInstrument(symbol)", 
"""val instrument = instrumentMaster.resolveIndexToken(symbol)""")
content = content.replace("symboltoken = instrument.angelToken,", "symboltoken = instrument.token,")

# Wait, in getOptionChain we have:
# val request = AngelOptionChainRequest(
#    exchange = if (instrument.exchange == "MCX") "MCX" else "NFO", 
#    symboltoken = instrument.token,
#    expirydate = expiry
# )
# 'instrument.exchange' is not valid for 'Instrument', it is 'exch_seg'.
content = content.replace("exchange = if (instrument.exchange == \"MCX\") \"MCX\" else \"NFO\",", "exchange = if (instrument.exch_seg == \"MCX\") \"MCX\" else \"NFO\",")
content = content.replace("exchange = if (instrument.exchange == \"MCX\") \"MCX\" else \"NFO\"", "exchange = if (instrument.exch_seg == \"MCX\") \"MCX\" else \"NFO\"")


with open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt', 'w') as f:
    f.write(content)
