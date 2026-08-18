import re

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    content = f.read()

deps = """
    val instrumentMasterService = com.example.data.network.InstrumentMasterService()
    val angelOneService = AngelOneBrokerService(networkClient.angelOneApi, sessionManager, instrumentMasterService)
    val dhanService = DhanBrokerService(networkClient.dhanApi, sessionManager)
    val brokerManager = BrokerManager(sessionManager, angelOneService, dhanService)
    
    init {
        brokerManager.angelMarketDataService = com.example.data.network.AngelOneMarketDataService(angelOneService, sessionManager, instrumentMasterService)
    }
"""

content = re.sub(r'val angelOneService = AngelOneBrokerService\(networkClient\.angelOneApi, sessionManager\)\s+val dhanService = DhanBrokerService\(networkClient\.dhanApi, sessionManager\)\s+val brokerManager = BrokerManager\(sessionManager, angelOneService, dhanService\)',
"""
    val instrumentMasterService = com.example.data.network.InstrumentMasterService()
    val angelOneService = com.example.data.network.AngelOneBrokerService(networkClient.angelOneApi, sessionManager, instrumentMasterService)
    val dhanService = com.example.data.network.DhanBrokerService(networkClient.dhanApi, sessionManager)
    val brokerManager = com.example.data.network.BrokerManager(sessionManager, angelOneService, dhanService)
""", content)

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'w') as f:
    f.write(content)
