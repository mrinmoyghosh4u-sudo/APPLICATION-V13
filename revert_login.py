import re

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "r") as f:
    content = f.read()

# Remove the inserted BrokerDiagnosticPanel calls from sed
pattern = r"Spacer\(modifier = Modifier\.height\(12\.dp\)\)\s*BrokerDiagnosticPanel\(selectedBroker, brokerStatuses\[selectedBroker\], providerHealth\[selectedBroker\], \{ onDisconnect\?\.invoke\(selectedBroker\) \}, \{ onReconnect\?\.invoke\(selectedBroker\) \}\)\s*Spacer\(modifier = Modifier\.height\(12\.dp\)\)"
content = re.sub(pattern, "Spacer(modifier = Modifier.height(12.dp))", content)

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "w") as f:
    f.write(content)
