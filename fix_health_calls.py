import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix reportWaitingForCallback
content = content.replace("brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX)", 'brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_UPSTOX, randomState)')
content = content.replace("brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS)", 'brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_FYERS, randomState)')
content = content.replace("brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_DHAN)", 'brokerManager.healthManager.reportWaitingForCallback(com.example.data.network.ProviderHealthManager.PROVIDER_DHAN, randomState)')

# Fix reportCallbackReceived
content = content.replace("brokerManager.healthManager.reportCallbackReceived(providerName)", 'brokerManager.healthManager.reportCallbackReceived(providerName, "")')

# Fix reportAuthCodeReceived
content = content.replace("brokerManager.healthManager.reportAuthCodeReceived(providerName)", 'brokerManager.healthManager.reportAuthCodeReceived(providerName, cleanedCode)')

with open(filepath, "w") as f:
    f.write(content)

