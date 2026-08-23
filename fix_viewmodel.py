import os

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, 'r') as f:
    content = f.read()

target = """            repository.userProfile.collectLatest { prof ->
                prof?.let { _userProfile.value = it }
            }"""

replacement = """            repository.userProfile.collectLatest { prof ->
                prof?.let { 
                    // Ensure isDhanConnected reflects actual auth status
                    val actualDhanStatus = brokerManager.brokerAuthManager.statuses.value["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED
                    _userProfile.value = it.copy(isDhanConnected = actualDhanStatus) 
                }
            }"""

if target in content:
    with open(filepath, 'w') as f:
        f.write(content.replace(target, replacement))
    print("Replaced collectLatest block 1")
else:
    print("Could not find collectLatest block 1")

target2 = """            _userProfile.value = updated
            repository.updateProfile(updated)"""

replacement2 = """            _userProfile.value = updated.copy(isDhanConnected = brokerManager.brokerAuthManager.statuses.value["Dhan"]?.status == com.example.data.network.BrokerAuthStatus.CONNECTED)
            repository.updateProfile(updated)"""

if target2 in content:
    with open(filepath, 'w') as f:
        f.write(content.replace(target2, replacement2))
    print("Replaced collectLatest block 2")
else:
    print("Could not find collectLatest block 2")

