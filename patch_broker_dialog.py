import re

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "r") as f:
    content = f.read()

# We need to change the function signature
signature_old = """fun BrokerConnectDialog(
    initialBroker: String,
    isAuthInProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAngelLogin: ((String, String, String, String) -> Unit)? = null,
    onMStockLogin: ((String, String, String) -> Unit)? = null,
    onFyersLogin: ((String, String, String) -> Unit)? = null,
    onUpstoxLogin: ((String, String, String) -> Unit)? = null,
    onDhanLogin: ((String, String) -> Unit)? = null,
    onStartUpstoxOAuth: ((String, String) -> Unit)? = null,
    onStartFyersOAuth: ((String, String) -> Unit)? = null,
    onStartDhanOAuth: ((String, String, String) -> Unit)? = null
)"""

signature_new = """fun BrokerConnectDialog(
    initialBroker: String,
    isAuthInProgress: Boolean,
    errorMessage: String?,
    brokerStatuses: Map<String, com.example.data.network.BrokerConnectionState> = emptyMap(),
    providerHealth: Map<String, com.example.data.network.ProviderHealthState> = emptyMap(),
    onDisconnect: ((String) -> Unit)? = null,
    onReconnect: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onAngelLogin: ((String, String, String, String) -> Unit)? = null,
    onMStockLogin: ((String, String, String) -> Unit)? = null,
    onFyersLogin: ((String, String, String) -> Unit)? = null,
    onUpstoxLogin: ((String, String, String) -> Unit)? = null,
    onDhanLogin: ((String, String) -> Unit)? = null,
    onStartUpstoxOAuth: ((String, String) -> Unit)? = null,
    onStartFyersOAuth: ((String, String) -> Unit)? = null,
    onStartDhanOAuth: ((String, String, String) -> Unit)? = null
)"""

content = content.replace(signature_old, signature_new)

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "w") as f:
    f.write(content)
