import re

with open('app/src/main/java/com/example/data/network/BrokerManager.kt', 'r') as f:
    content = f.read()

content = content.replace("val dhanService: DhanBrokerService", "val dhanService: DhanBrokerService,\n    val instrumentMasterService: InstrumentMasterService")
content = content.replace("val instrumentMasterService = InstrumentMasterService()\n    ", "")

with open('app/src/main/java/com/example/data/network/BrokerManager.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val brokerManager = com.example.data.network.BrokerManager(sessionManager, angelOneService, dhanService)", "val brokerManager = com.example.data.network.BrokerManager(sessionManager, angelOneService, dhanService, instrumentMasterService)")

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'w') as f:
    f.write(content)

