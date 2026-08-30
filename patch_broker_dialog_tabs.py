import re

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "r") as f:
    content = f.read()

diagnostic_composable = """
@Composable
fun BrokerDiagnosticPanel(
    brokerName: String,
    brokerState: com.example.data.network.BrokerConnectionState?,
    healthState: com.example.data.network.ProviderHealthState?,
    onDisconnect: (() -> Unit)?,
    onReconnect: (() -> Unit)?
) {
    val isConnected = brokerState?.status == com.example.data.network.BrokerAuthStatus.CONNECTED || brokerState?.status == com.example.data.network.BrokerAuthStatus.LIVE
    
    val connectionStatus = brokerState?.status?.name ?: "DISCONNECTED"
    val authStatus = if (healthState?.authenticated == true || isConnected) "PASS" else if (brokerState?.status == com.example.data.network.BrokerAuthStatus.AUTHENTICATING) "PENDING" else "FAIL"
    val tokenStatus = if (healthState?.authenticated == true || isConnected) "PASS" else "FAIL"
    val accountStatus = if (isConnected) "VERIFIED" else "PENDING"
    
    val isDhan = brokerName == "Dhan"
    
    val marketDataStatus = if (isDhan) "ORDER ONLY" else (healthState?.status ?: "NONE")
    val webSocketStatus = if (isDhan) "N/A" else (healthState?.webSocketState ?: "DISCONNECTED")
    val lastError = if (isDhan) {
        if (brokerState?.status == com.example.data.network.BrokerAuthStatus.ERROR) brokerState.message else "None"
    } else {
        healthState?.lastError?.takeIf { it.isNotBlank() } ?: (if (brokerState?.status == com.example.data.network.BrokerAuthStatus.ERROR) brokerState.message else "None")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(com.example.ui.theme.DarkBackground, RoundedCornerShape(8.dp))
            .border(1.dp, com.example.ui.theme.DarkCardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text("Diagnostic Status", color = com.example.ui.theme.PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        DiagnosticRow("Connection Status", connectionStatus, if (isConnected) com.example.ui.theme.ProfitGreen else com.example.ui.theme.LossRed)
        DiagnosticRow("Authentication Status", authStatus, if (authStatus == "PASS") com.example.ui.theme.ProfitGreen else com.example.ui.theme.LossRed)
        DiagnosticRow("Token Status", tokenStatus, if (tokenStatus == "PASS") com.example.ui.theme.ProfitGreen else com.example.ui.theme.LossRed)
        DiagnosticRow("Account Status", accountStatus, if (accountStatus == "VERIFIED") com.example.ui.theme.ProfitGreen else com.example.ui.theme.LossRed)
        DiagnosticRow("Market Data Status", marketDataStatus, if (marketDataStatus == "LIVE" || marketDataStatus == "ORDER ONLY") com.example.ui.theme.ProfitGreen else com.example.ui.theme.TextGray)
        DiagnosticRow("WebSocket Status", webSocketStatus, if (webSocketStatus.contains("CONNECTED")) com.example.ui.theme.ProfitGreen else com.example.ui.theme.TextGray)
        DiagnosticRow("Last Error", lastError, if (lastError == "None") com.example.ui.theme.TextGray else com.example.ui.theme.LossRed)
        
        if (isConnected || brokerState?.status == com.example.data.network.BrokerAuthStatus.ERROR) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (onReconnect != null) {
                    OutlinedButton(
                        onClick = onReconnect,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.PrimaryGold),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PrimaryGold)
                    ) {
                        Text("RECONNECT", fontSize = 12.sp)
                    }
                }
                if (onDisconnect != null) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.LossRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.LossRed)
                    ) {
                        Text("DISCONNECT", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = com.example.ui.theme.TextGray, fontSize = 12.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
"""

if "BrokerDiagnosticPanel" not in content:
    content += "\n" + diagnostic_composable

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "w") as f:
    f.write(content)
