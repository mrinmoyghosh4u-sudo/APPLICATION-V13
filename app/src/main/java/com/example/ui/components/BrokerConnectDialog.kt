package com.example.ui.components

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import com.example.util.BrokerConfig
import kotlinx.coroutines.launch

@Composable
fun BrokerConnectDialog(
    initialBroker: String,
    isAuthInProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAngelLogin: ((String, String, String, String) -> Unit)? = null,
    onMStockLogin: ((String, String, String) -> Unit)? = null,
    onFyersLogin: ((String, String, String) -> Unit)? = null,
    onUpstoxLogin: ((String, String, String) -> Unit)? = null,
    onStartUpstoxOAuth: ((String, String) -> Unit)? = null,
    onStartFyersOAuth: ((String, String) -> Unit)? = null
) {
    var selectedBroker by remember { mutableStateOf(if (initialBroker.isBlank()) "Dhan" else initialBroker) }
    var localErrorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val sessionManager = remember { com.example.data.network.SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var showUpstoxCreds by remember { mutableStateOf(sessionManager.upstoxApiKey.isBlank() || sessionManager.upstoxApiSecret.isBlank()) }
    var showFyersCreds by remember { mutableStateOf(sessionManager.fyersAppId.isBlank() || sessionManager.fyersSecretId.isBlank()) }

    var angelClientCode by remember { mutableStateOf(sessionManager.angelClientId) }
    var angelMpin by remember { mutableStateOf(sessionManager.angelMpin) }
    var angelApiKey by remember { mutableStateOf(sessionManager.angelApiKey) }
    var angelTotpSecret by remember { mutableStateOf(sessionManager.angelTotpSecret) }
    var isDhanConsentLoading by remember { mutableStateOf(false) }

    var mstockClientId by remember { mutableStateOf(sessionManager.mstockClientId) }
    var mstockApiKey by remember { mutableStateOf(sessionManager.mstockApiKey) }
    var mstockTotpSecret by remember { mutableStateOf(sessionManager.mstockTotpSecret) }

    var fyersAppId by remember { mutableStateOf(sessionManager.fyersAppId ?: "") }
    var fyersSecretId by remember { mutableStateOf(sessionManager.fyersSecretId ?: "") }
    var fyersAuthCode by remember { mutableStateOf("") }

    var upstoxApiKey by remember { mutableStateOf(sessionManager.upstoxApiKey ?: "") }
    var upstoxApiSecret by remember { mutableStateOf(sessionManager.upstoxApiSecret ?: "") }
    var upstoxAuthCode by remember { mutableStateOf("") }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            localErrorMsg = errorMessage
        }
    }

    LaunchedEffect(selectedBroker) {
        if (selectedBroker == "Angel One") {
            if (angelClientCode.isBlank()) angelClientCode = sessionManager.angelClientId
            if (angelMpin.isBlank()) angelMpin = sessionManager.angelMpin
            if (angelApiKey.isBlank()) angelApiKey = sessionManager.angelApiKey
            if (angelTotpSecret.isBlank()) angelTotpSecret = sessionManager.angelTotpSecret
        } else if (selectedBroker == "m.Stock") {
            if (mstockClientId.isBlank()) mstockClientId = sessionManager.mstockClientId
            if (mstockApiKey.isBlank()) mstockApiKey = sessionManager.mstockApiKey
            if (mstockTotpSecret.isBlank()) mstockTotpSecret = sessionManager.mstockTotpSecret
        } else if (selectedBroker == "Upstox") {
            if (upstoxApiKey.isBlank()) upstoxApiKey = sessionManager.upstoxApiKey ?: ""
            if (upstoxApiSecret.isBlank()) upstoxApiSecret = sessionManager.upstoxApiSecret ?: ""
        }
    }

    Dialog(
        onDismissRequest = { if (!isAuthInProgress) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isAuthInProgress,
            dismissOnClickOutside = !isAuthInProgress,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(DarkCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Connect Broker Account",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 5 Broker Tabs
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BrokerTab(
                        name = "Dhan",
                        logoRes = R.drawable.ic_dhan_logo,
                        isSelected = selectedBroker == "Dhan",
                        onClick = { 
                            selectedBroker = "Dhan" 
                            localErrorMsg = null
                        }
                    )
                    BrokerTab(
                        name = "Upstox",
                        letter = "U",
                        isSelected = selectedBroker == "Upstox",
                        onClick = { 
                            selectedBroker = "Upstox"
                            localErrorMsg = null
                        }
                    )
                    BrokerTab(
                        name = "Fyers",
                        logoRes = R.drawable.ic_dhan_logo, // fallback logo
                        isSelected = selectedBroker == "Fyers",
                        onClick = { 
                            selectedBroker = "Fyers"
                            localErrorMsg = null
                        }
                    )
                    BrokerTab(
                        name = "Angel",
                        logoRes = R.drawable.ic_angel_one_logo,
                        isSelected = selectedBroker == "Angel One",
                        onClick = { 
                            selectedBroker = "Angel One"
                            localErrorMsg = null
                        }
                    )
                    BrokerTab(
                        name = "m.Stock",
                        letter = "m",
                        isSelected = selectedBroker == "m.Stock",
                        onClick = { 
                            selectedBroker = "m.Stock"
                            localErrorMsg = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (selectedBroker) {
                    "Upstox" -> {
                        Text("Connect Upstox (Primary Market Data)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Provides Live Quotes and Historical Data (API V2/V3)", color = TextGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (showUpstoxCreds) {
                            OutlinedTextField(
                                value = upstoxApiKey,
                                onValueChange = { upstoxApiKey = it },
                                label = { Text("API Key / Client ID", color = TextGray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF673AB7),
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = upstoxApiSecret,
                                onValueChange = { upstoxApiSecret = it },
                                label = { Text("API Secret", color = TextGray) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF673AB7),
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (sessionManager.upstoxApiKey.isNotBlank() && sessionManager.upstoxApiSecret.isNotBlank()) {
                                TextButton(
                                    onClick = { showUpstoxCreds = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Use Configured Credentials", color = Color(0xFFBB86FC), fontSize = 12.sp)
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF241C35)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("API Credentials", color = TextGray, fontSize = 11.sp)
                                        Text("CONFIGURED", color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { showUpstoxCreds = true }) {
                                        Text("Change", color = Color(0xFFBB86FC), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val cleanKey = upstoxApiKey.trim()
                                val cleanSecret = upstoxApiSecret.trim()
                                if (cleanKey.isBlank() || cleanSecret.isBlank()) {
                                    localErrorMsg = "API Key and API Secret are required"
                                    return@Button
                                }
                                localErrorMsg = null
                                if (onStartUpstoxOAuth != null) {
                                    onStartUpstoxOAuth(cleanKey, cleanSecret)
                                } else {
                                    sessionManager.upstoxApiKey = cleanKey
                                    sessionManager.upstoxApiSecret = cleanSecret
                                    val redirectUri = sessionManager.upstoxRedirectUri.takeIf { it.isNotBlank() } ?: "https://application-beige-psi.vercel.app/oauth"
                                    val randomState = "upstox_" + java.util.UUID.randomUUID().toString()
                                    sessionManager.pendingOAuthState = randomState
                                    sessionManager.pendingOAuthBroker = "Upstox"
                                    android.util.Log.i("UpstoxAuth", "[UPSTOX_OAUTH_STARTED] Initialized Upstox OAuth with state=$randomState")
                                    android.util.Log.i("UpstoxAuth", "[UPSTOX_BROWSER_OPENED] Opening browser for Upstox authentication")
                                    android.util.Log.i("UpstoxAuth", "[UPSTOX_WAITING_FOR_CALLBACK] Waiting for redirect callback...")

                                    val loginUrl = com.example.util.UpstoxAuthHelper.buildLoginUrl(cleanKey, redirectUri, state = randomState)
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))
                                        context.startActivity(intent)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        localErrorMsg = "Failed to open browser: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !isAuthInProgress,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                        ) {
                            Text("LOGIN WITH UPSTOX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (!localErrorMsg.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(localErrorMsg!!, color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    "m.Stock" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE53935), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("m", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "m.Stock (Fallback #2 Market Data)",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = mstockClientId,
                            onValueChange = { mstockClientId = it },
                            label = { Text("Client Code / User ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = Color(0xFFE53935),
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mstockApiKey,
                            onValueChange = { mstockApiKey = it },
                            label = { Text("API Key / App Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = Color(0xFFE53935),
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mstockTotpSecret,
                            onValueChange = { mstockTotpSecret = it },
                            label = { Text("TOTP Secret (Base32 Key for Auto TOTP)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = Color(0xFFE53935),
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (mstockClientId.isBlank() || mstockApiKey.isBlank() || mstockTotpSecret.isBlank()) {
                                    localErrorMsg = "Please enter Client Code, API Key, and TOTP Secret."
                                } else {
                                    localErrorMsg = null
                                    sessionManager.saveMStockCredentials(
                                        clientCode = mstockClientId,
                                        apiKey = mstockApiKey,
                                        totpSecret = mstockTotpSecret
                                    )
                                    onMStockLogin?.invoke(mstockClientId, mstockApiKey, mstockTotpSecret)
                                }
                            },
                            enabled = !isAuthInProgress,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            if (isAuthInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color.White, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("m", color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LOGIN WITH m.STOCK", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    "Fyers" -> {
                        Text("Connect Fyers API V3", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Provides Live Quotes and Historical Data", color = TextGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (showFyersCreds) {
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
                            Spacer(modifier = Modifier.height(8.dp))
                            if (sessionManager.fyersAppId.isNotBlank() && sessionManager.fyersSecretId.isNotBlank()) {
                                TextButton(
                                    onClick = { showFyersCreds = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Use Configured Credentials", color = ProfitGreen, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF13271A)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("API Credentials", color = TextGray, fontSize = 11.sp)
                                        Text("CONFIGURED", color = ProfitGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { showFyersCreds = true }) {
                                        Text("Change", color = ProfitGreen, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val cleanAppId = fyersAppId.trim()
                                val cleanSecretId = fyersSecretId.trim()
                                if (cleanAppId.isBlank() || cleanSecretId.isBlank()) {
                                    localErrorMsg = "App ID and Secret ID are required"
                                    return@Button
                                }
                                localErrorMsg = null
                                if (onStartFyersOAuth != null) {
                                    onStartFyersOAuth(cleanAppId, cleanSecretId)
                                } else {
                                    sessionManager.fyersAppId = cleanAppId
                                    sessionManager.fyersSecretId = cleanSecretId
                                    val redirectUri = sessionManager.fyersRedirectUri.takeIf { it.isNotBlank() } ?: com.example.util.FyersAuthHelper.DEFAULT_REDIRECT_URI
                                    val randomState = "fyers_" + java.util.UUID.randomUUID().toString()
                                    sessionManager.pendingOAuthState = randomState
                                    sessionManager.pendingOAuthBroker = "Fyers"
                                    android.util.Log.i("FyersAuth", "[FYERS_OAUTH_STARTED] Initialized Fyers OAuth with state=$randomState")
                                    android.util.Log.i("FyersAuth", "[FYERS_BROWSER_OPENED] Opening browser for Fyers authentication")
                                    android.util.Log.i("FyersAuth", "[FYERS_WAITING_FOR_CALLBACK] Waiting for redirect callback...")

                                    val loginUrl = com.example.util.FyersAuthHelper.buildLoginUrl(cleanAppId, redirectUri, state = randomState)
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))
                                        context.startActivity(intent)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        localErrorMsg = "Failed to open browser: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !isAuthInProgress,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                        ) {
                            Text("LOGIN WITH FYERS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (!localErrorMsg.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(localErrorMsg!!, color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    "Angel One" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_angel_one_logo),
                                contentDescription = "Angel One Logo",
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Angel One (Fallback #1 Market Data)",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = angelClientCode,
                            onValueChange = { angelClientCode = it },
                            label = { Text("Client ID / Client Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = ProfitGreen,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = ProfitGreen,
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = angelMpin,
                            onValueChange = { angelMpin = it },
                            label = { Text("MPIN / Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = ProfitGreen,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = ProfitGreen,
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = angelApiKey,
                            onValueChange = { angelApiKey = it },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = ProfitGreen,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = ProfitGreen,
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = angelTotpSecret,
                            onValueChange = { angelTotpSecret = it },
                            label = { Text("TOTP Secret (Base32 Key)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = ProfitGreen,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = ProfitGreen,
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (angelClientCode.isBlank()) {
                                    localErrorMsg = "Please enter your Angel One Client ID."
                                } else if (angelMpin.isBlank()) {
                                    localErrorMsg = "Please enter your Angel One MPIN."
                                } else if (angelApiKey.isBlank() && BrokerConfig.angelApiKey.isBlank()) {
                                    localErrorMsg = "Please enter your Angel One API Key."
                                } else if (angelTotpSecret.isBlank()) {
                                    localErrorMsg = "Please enter your Angel One TOTP Secret for automated TOTP generation."
                                } else {
                                    localErrorMsg = null
                                    sessionManager.saveAngelOneCredentials(
                                        clientCode = angelClientCode,
                                        mpin = angelMpin,
                                        apiKey = angelApiKey,
                                        totpSecret = angelTotpSecret
                                    )
                                    onAngelLogin?.invoke(angelClientCode, angelMpin, angelApiKey, angelTotpSecret)
                                }
                            },
                            enabled = !isAuthInProgress,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003882))
                        ) {
                            if (isAuthInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_angel_one_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LOGIN WITH ANGEL ONE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                    else -> {
                        // Dhan OAuth Flow
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_dhan_logo),
                                contentDescription = "Dhan Logo",
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Dhan (Primary Order Execution)",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "You will be redirected to Dhan's official login page to authorize order execution securely.",
                            color = TextGray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val clientId = BrokerConfig.dhanClientId
                                if (clientId.isBlank()) {
                                    localErrorMsg = "Missing Dhan Client ID. Please configure it securely."
                                } else {
                                    localErrorMsg = null
                                    isDhanConsentLoading = true
                                    coroutineScope.launch {
                                        val consentRes = com.example.util.DhanAuthHelper.generateConsent()
                                        isDhanConsentLoading = false
                                        consentRes.onSuccess { url ->
                                            android.util.Log.d("DhanAuth", "Opening browser with Complete Consent URL: $url")
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                context.startActivity(intent)
                                    android.util.Log.d("UpstoxAuth", "[3] Browser opened: PASS")
                                            } catch (e: Exception) {
                                                android.util.Log.e("DhanAuth", "Failed to launch browser: ${e.message}", e)
                                                localErrorMsg = "Unable to open browser: ${e.localizedMessage}"
                                            }
                                        }.onFailure { err ->
                                            localErrorMsg = err.localizedMessage ?: "Failed to generate Dhan OAuth consent URL"
                                        }
                                    }
                                }
                            },
                            enabled = !isAuthInProgress && !isDhanConsentLoading,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                        ) {
                            if (isDhanConsentLoading || isAuthInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_dhan_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LOGIN WITH DHAN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                localErrorMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF381B1B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "⚠️ ",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg,
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                if (isAuthInProgress) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Waiting for authentication...",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Cancel", color = TextGray)
                }
            }
        }
    }
}

@Composable
private fun BrokerTab(

    name: String,
    logoRes: Int = 0,
    letter: String = "",
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen) else null,
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            if (logoRes != 0) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = name,
                    modifier = Modifier.size(18.dp)
                )
            } else if (letter.isNotBlank()) {
                val color = if (letter == "m") Color(0xFFE53935) else Color(0xFF0288D1)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(color, RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(letter, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                name,
                color = if (isSelected) TextWhite else TextGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        }
    }

}
