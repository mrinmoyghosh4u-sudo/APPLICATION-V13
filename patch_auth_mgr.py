import os

filepath = "app/src/main/java/com/example/data/network/BrokerAuthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

if "FyersAuthManager" not in content:
    # Not used inside BrokerAuthManager, it's separated. But let's wire it to BrokerManager.
    pass
