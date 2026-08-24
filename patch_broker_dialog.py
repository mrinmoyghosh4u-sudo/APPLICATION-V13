import os

filepath = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add WebView imports if not present
if "import android.webkit.WebView" not in content:
    content = content.replace("import android.content.Intent", "import android.content.Intent\nimport android.webkit.WebView\nimport android.webkit.WebViewClient\nimport android.webkit.WebResourceRequest\nimport androidx.compose.ui.viewinterop.AndroidView")

# Add state for WebView
if "var showFyersWebView by remember { mutableStateOf(false) }" not in content:
    content = content.replace("var fyersAuthCode by remember { mutableStateOf(\"\") }", "var fyersAuthCode by remember { mutableStateOf(\"\") }\n    var showFyersWebView by remember { mutableStateOf(false) }")

# Update the UI for Fyers
old_fyers_ui = """                        // Fyers Web Login Button
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
                        }"""

new_fyers_ui = """                        Button(
                            onClick = {
                                if (fyersAppId.isBlank() || fyersSecretId.isBlank()) {
                                    localErrorMsg = "App ID and Secret ID are required"
                                    return@Button
                                }
                                sessionManager.fyersAppId = fyersAppId
                                sessionManager.fyersSecretId = fyersSecretId
                                showFyersWebView = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAuthInProgress,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                        ) {
                            if (isAuthInProgress) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                            } else {
                                Text("LOGIN & CONNECT", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }"""

content = content.replace(old_fyers_ui, new_fyers_ui)

webview_dialog = """
    if (showFyersWebView) {
        Dialog(
            onDismissRequest = { showFyersWebView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(DarkBackground).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fyers Secure Login", color = TextWhite, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showFyersWebView = false }) {
                            Text("Close", color = ProfitGreen)
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url.toString()
                                        if (url.startsWith("https://trade.fyers.in/api-login/redirect-uri/index.html")) {
                                            val authCode = request?.url?.getQueryParameter("auth_code")
                                            if (!authCode.isNullOrBlank()) {
                                                showFyersWebView = false
                                                onFyersLogin?.invoke(fyersAppId, fyersSecretId, authCode)
                                            }
                                            return true
                                        }
                                        return super.shouldOverrideUrlLoading(view, request)
                                    }
                                }
                                val redirectUri = "https://trade.fyers.in/api-login/redirect-uri/index.html"
                                val loginUrl = "https://api-t1.fyers.in/api/v3/generate-authcode?client_id=$fyersAppId&redirect_uri=$redirectUri&response_type=code&state=sample_state"
                                loadUrl(loginUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
"""

# inject webview_dialog before the final closing brace of BrokerConnectDialog function
if "if (showFyersWebView)" not in content:
    # Find the last closing brace in the file
    last_brace_idx = content.rfind("}")
    content = content[:last_brace_idx] + webview_dialog + "\n}"

with open(filepath, "w") as f:
    f.write(content)
