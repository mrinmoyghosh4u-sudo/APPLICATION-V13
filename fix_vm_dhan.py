import re

with open('app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("val dhanService = com.example.data.network.DhanBrokerService(networkClient.dhanApi, sessionManager)", "val dhanService = com.example.data.network.DhanBrokerService(networkClient.dhanApi, sessionManager)")

# Wait, the error is: Argument type mismatch: actual type is 'DhanApi', but 'String' was expected.
# Let's check DhanBrokerService constructor in MainViewModel
