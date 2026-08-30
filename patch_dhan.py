import re

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "r") as f:
    content = f.read()

content = content.replace('Text(\n                                "Dhan (Primary Order Execution)",\n                                color = TextWhite,\n                                fontSize = 15.sp,\n                                fontWeight = FontWeight.Bold\n                            )\n                        }\n                        Spacer(modifier = Modifier.height(16.dp))',
'Text(\n                                "Dhan (Primary Order Execution)",\n                                color = TextWhite,\n                                fontSize = 15.sp,\n                                fontWeight = FontWeight.Bold\n                            )\n                        }\n                        Spacer(modifier = Modifier.height(16.dp))\n                        BrokerDiagnosticPanel(selectedBroker, brokerStatuses[selectedBroker], providerHealth[selectedBroker], { onDisconnect?.invoke(selectedBroker) }, { onReconnect?.invoke(selectedBroker) })\n                        Spacer(modifier = Modifier.height(16.dp))')

with open("app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt", "w") as f:
    f.write(content)
