import re

filepath = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
with open(filepath, "r") as f:
    content = f.read()

# Change onDhanLogin type
content = content.replace("onDhanLogin: ((String) -> Unit)? = null", "onDhanLogin: ((String, String) -> Unit)? = null")

old_dhan_tab = """                    "Dhan" -> {
                        var clientId by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            label = { Text("Dhan Client ID", color = TextGray) },
                            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White)
                        )
                        Button(
                            onClick = { onDhanLogin?.invoke(clientId) },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                        ) {
                            Text("Connect Dhan", color = DarkBackground)
                        }
                    }"""

new_dhan_tab = """                    "Dhan" -> {
                        var clientId by remember { mutableStateOf("") }
                        var clientSecret by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            label = { Text("Dhan Client ID", color = TextGray) },
                            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = clientSecret,
                            onValueChange = { clientSecret = it },
                            label = { Text("Dhan Secret / API Key", color = TextGray) },
                            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { onDhanLogin?.invoke(clientId, clientSecret) },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect Dhan", color = DarkBackground)
                        }
                    }"""

content = content.replace(old_dhan_tab, new_dhan_tab)
with open(filepath, "w") as f:
    f.write(content)
