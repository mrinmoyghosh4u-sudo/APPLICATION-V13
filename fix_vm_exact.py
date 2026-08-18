with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "val angelOneService = AngelOneBrokerService(networkClient.angelOneApi, sessionManager)" in line:
        lines[i] = "    val instrumentMasterService = com.example.data.network.InstrumentMasterService()\n    val angelOneService = com.example.data.network.AngelOneBrokerService(networkClient.angelOneApi, sessionManager, instrumentMasterService)\n"
    elif "val brokerManager = BrokerManager(sessionManager, angelOneService, dhanService)" in line:
        lines[i] = "    val brokerManager = com.example.data.network.BrokerManager(sessionManager, angelOneService, dhanService, instrumentMasterService)\n"

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'w') as f:
    f.writelines(lines)
