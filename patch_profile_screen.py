import os

filepath = "app/src/main/java/com/example/ui/screens/ProfileScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

fyers_row = """
                // Fyers Row
                val fyersInfo = brokerStatuses["Fyers"]
                val fyersStatus = fyersInfo?.status ?: com.example.data.network.BrokerAuthStatus.OFFLINE
                BrokerConnectionRow(
                    name = "Fyers",
                    role = "Primary Market Data",
                    status = fyersStatus,
                    statusMessage = fyersInfo?.message ?: "Market Data Only",
                    onConnect = { onSwitchBroker("Fyers") },
                    onReconnect = { onReconnectBroker("Fyers") },
                    onDisconnect = { onDisconnectBroker("Fyers") },
                    onRemoveAccount = { onRemoveAccountBroker("Fyers") }
                )
                Spacer(modifier = Modifier.height(12.dp))
"""

if "name = \"Fyers\"" not in content:
    content = content.replace("                // 2. Angel One Row", fyers_row + "                // 2. Angel One Row")
    with open(filepath, "w") as f:
        f.write(content)
    print("Added Fyers row to ProfileScreen")
