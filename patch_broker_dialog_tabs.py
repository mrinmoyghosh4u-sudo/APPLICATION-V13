import os

filepath = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
with open(filepath, "r") as f:
    content = f.read()

fyers_tab = """                    BrokerTab(
                        name = "Fyers",
                        logoRes = R.drawable.ic_dhan_logo, // fallback logo
                        isSelected = selectedBroker == "Fyers",
                        onClick = { 
                            selectedBroker = "Fyers"
                            localErrorMsg = null
                        }
                    )"""

fyers_form = """
                    "Fyers" -> {
                        Text("Connect Fyers API V3", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Provides Live Quotes and Historical Data", color = TextGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = fyersAppId,
                            onValueChange = { fyersAppId = it },
                            label = { Text("App ID", color = TextGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ProfitGreen,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fyersSecretId,
                            onValueChange = { fyersSecretId = it },
                            label = { Text("Secret ID", color = TextGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ProfitGreen,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Fyers Web Login Button
                        Button(
                            onClick = {
                                if (fyersAppId.isBlank()) {
                                    localErrorMsg = "App ID is required"
                                    return@Button
                                }
                                sessionManager.fyersAppId = fyersAppId
                                sessionManager.fyersSecretId = fyersSecretId
                                
                                val redirectUri = "https://trade.fyers.in/api-login/redirect-uri/index.html"
                                val url = "https://api-t1.fyers.in/api/v3/generate-authcode?client_id=$fyersAppId&redirect_uri=$redirectUri&response_type=code&state=sample_state"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                        ) {
                            Text("1. LOGIN TO FYERS (WEB)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = fyersAuthCode,
                            onValueChange = { fyersAuthCode = it },
                            label = { Text("Paste Auth Code here", color = TextGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ProfitGreen,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (fyersAppId.isBlank() || fyersSecretId.isBlank() || fyersAuthCode.isBlank()) {
                                    localErrorMsg = "All fields required"
                                    return@Button
                                }
                                onFyersLogin?.invoke(fyersAppId, fyersSecretId, fyersAuthCode)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAuthInProgress,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            if (isAuthInProgress) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                            } else {
                                Text("2. CONNECT MARKET DATA", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }"""


content = content.replace("                    BrokerTab(\n                        name = \"TradeSmart\",", fyers_tab + "\n                    BrokerTab(\n                        name = \"TradeSmart\",")
content = content.replace("                    \"TradeSmart\" -> {", fyers_form + "\n                    \"TradeSmart\" -> {")

# We also need to change the arrangement in the row to horizontally scroll if they don't fit, but 5 tabs is probably fine.
content = content.replace("horizontalArrangement = Arrangement.SpaceBetween", "horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())")
content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.horizontalScroll\nimport androidx.compose.foundation.rememberScrollState")

with open(filepath, "w") as f:
    f.write(content)
