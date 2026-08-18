import re

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    text = f.read()

deps = """    val instrumentMasterService = com.example.data.network.InstrumentMasterService()
    val angelOneService = com.example.data.network.AngelOneBrokerService(networkClient.angelOneApi, sessionManager, instrumentMasterService)
    val dhanService = com.example.data.network.DhanBrokerService(networkClient.dhanApi, sessionManager)
    val brokerManager = com.example.data.network.BrokerManager(sessionManager, angelOneService, dhanService, instrumentMasterService)"""

text = re.sub(r'val angelOneService.*?val brokerManager.*?\n', deps + "\n", text, flags=re.DOTALL)
text = text.replace("val angelOneService = AngelOneBrokerService(networkClient.angelOneApi, sessionManager)", "val angelOneService = com.example.data.network.AngelOneBrokerService(networkClient.angelOneApi, sessionManager, instrumentMasterService)")
text = text.replace("val brokerManager = BrokerManager(sessionManager, angelOneService, dhanService)", "val brokerManager = com.example.data.network.BrokerManager(sessionManager, angelOneService, dhanService, instrumentMasterService)")


with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'w') as f:
    f.write(text)

