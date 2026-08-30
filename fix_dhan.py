import re

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "r") as f:
    content = f.read()

pattern = r"Spacer\(modifier = Modifier\.height\(16\.dp\)\)\s*BrokerDiagnosticPanel\(selectedBroker, brokerStatuses\[selectedBroker\], providerHealth\[selectedBroker\], \{ onDisconnect\?\.invoke\(selectedBroker\) \}, \{ onReconnect\?\.invoke\(selectedBroker\) \}\)\s*Spacer\(modifier = Modifier\.height\(16\.dp\)\)\s*BrokerDiagnosticPanel\(selectedBroker, brokerStatuses\[selectedBroker\], providerHealth\[selectedBroker\], \{ onDisconnect\?\.invoke\(selectedBroker\) \}, \{ onReconnect\?\.invoke\(selectedBroker\) \}\)\s*Spacer\(modifier = Modifier\.height\(16\.dp\)\)"
replacement = r"Spacer(modifier = Modifier.height(16.dp))\n                        BrokerDiagnosticPanel(selectedBroker, brokerStatuses[selectedBroker], providerHealth[selectedBroker], { onDisconnect?.invoke(selectedBroker) }, { onReconnect?.invoke(selectedBroker) })\n                        Spacer(modifier = Modifier.height(16.dp))"

content = re.sub(pattern, replacement, content)

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "w") as f:
    f.write(content)
