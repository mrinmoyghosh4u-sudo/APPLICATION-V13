with open('app/src/main/java/com/example/data/network/BrokerManager.kt', 'r') as f:
    content = f.read()

content = content.replace("val angelMarketDataService = AngelOneMarketDataService(angelOneService, sessionManager)", "val instrumentMasterService = InstrumentMasterService()\n    val angelMarketDataService = AngelOneMarketDataService(angelOneService, sessionManager, instrumentMasterService)")

with open('app/src/main/java/com/example/data/network/BrokerManager.kt', 'w') as f:
    f.write(content)
