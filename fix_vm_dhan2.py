with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("val dhanService = com.example.data.network.DhanBrokerService(networkClient.dhanApi, sessionManager)", "val dhanService = com.example.data.network.DhanBrokerService(api = networkClient.dhanApi, sessionManager = sessionManager)")

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'w') as f:
    f.write(text)
