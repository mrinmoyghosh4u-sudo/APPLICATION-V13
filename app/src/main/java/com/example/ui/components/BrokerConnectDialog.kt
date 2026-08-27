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
    onDhanLogin: ((String, String) -> Unit)? = null,
    onStartUpstoxOAuth: ((String, String) -> Unit)? = null,
    onStartFyersOAuth: ((String, String) -> Unit)? = null,
    onStartDhanOAuth: ((String, String, String) -> Unit)? = null
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
    
    var dhanClientIdInput by remember { mutableStateOf(sessionManager.dhanClientId.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanClientId) }
    var dhanApiKeyInput by remember { mutableStateOf(sessionManager.dhanApiKey.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanApiKey) }
    var dhanClientSecretInput by remember { mutableStateOf(sessionManager.dhanClientSecret.takeIf { it.isNotBlank() } ?: BrokerConfig.dhanClientSecret) }
    var dhanAccessTokenInput by remember { mutableStateOf(sessionManager.dhanAccessToken ?: "") }
    var showDhanCreds by remember { mutableStateOf(dhanClientIdInput.isBlank() || dhanClientSecretInput.isBlank()) }
    var showDhanDirectToken by remember { mutableStateOf(false) }

    var mstockClientId by remember { mutableStateOf(sessionManager.mstockClientId) }
    var mstockApiKey by remember { mutableStateOf(sessionManager.mstockApiKey) }
    var mstockTotpSecret by remember { mutableStateOf(sessionManager.mstockTotpSecret) }

    var fyersAppId by remember { mutableStateOf(sessionManager.fyersAppId ?: "") }
    var fyersSecretId by remember { mutableStateOf(sessionManager.fyersSecretId ?: "") }
    var fyersRedirectUri by remember { mutableStateOf(sessionManager.fyersRedirectUri) }
    var fyersAuthCode by remember { mutableStateOf("") }

    var upstoxApiKey by remember { mutableStateOf(sessionManager.upstoxApiKey ?: "") }
    var upstoxApiSecret by remember { mutableStateOf(sessionManager.upstoxApiSecret ?: "") }
    var upstoxRedirectUri by remember { mutableStateOf(sessionManager.upstoxRedirectUri) }
    var upstoxAuthCode by remember { mutableStateOf("") }
    var showManualUpstox by remember { mutableStateOf(false) }
    var showManualFyers by remember { mutableStateOf(false) }

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
            upstoxRedirectUri = sessionManager.upstoxRedirectUri
        } else if (selectedBroker == "Fyers") {
            if (fyersAppId.isBlank()) fyersAppId = sessionManager.fyersAppId ?: ""
            if (fyersSecretId.isBlank()) fyersSecretId = sessionManager.fyersSecretId ?: ""
            fyersRedirectUri = sessionManager.fyersRedirectUri
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF673AB7), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("U", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Upstox (Primary Market Data)",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = upstoxApiKey,
                            onValueChange = { upstoxApiKey = it },
                            label = { Text("API Key / Client ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = Color(0xFF673AB7),
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = Color(0xFF673AB7),
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = upstoxApiSecret,
                            onValueChange = { upstoxApiSecret = it },
                            label = { Text("API Secret") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = Color(0xFF673AB7),
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = Color(0xFF673AB7),
                                unfocusedLabelColor = TextGray,
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = upstoxRedirectUri,
                            onValueChange = { upstoxRedirectUri = it },
                            label = { Text("Redirect URI") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = Color(0xFF673AB7),
                                unfocusedBorderColor = DarkCardBorder,
                                focusedLabelColor = Color(0xFF673AB7),
                                unfocusedLabelColor = TextGray,
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val keyToUse = upstoxApiKey.trim().takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.upstoxApiKey
                                val secretToUse = upstoxApiSecret.trim().takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.upstoxApiSecret
                                val redirectUriToUse = upstoxRedirectUri.trim().takeIf { it.isNotBlank() } ?: "https://application-beige-psi.vercel.app/oauth"
                                
                                if (keyToUse.isBlank() || secretToUse.isBlank()) {
                                    localErrorMsg = "Upstox API Key & Secret are required."
                                } else {
                                    localErrorMsg = null
                                    sessionManager.saveUpstoxCredentials(keyToUse, secretToUse, redirectUriToUse)
                                    
                                    if (onStartUpstoxOAuth != null) {
                                        onStartUpstoxOAuth(keyToUse, secretToUse)
                                    } else {
                                        val randomState = com.example.util.UpstoxAuthHelper.generateSecureState()
                                        sessionManager.pendingOAuthState = randomState
                                        sessionManager.pendingOAuthBroker = "Upstox"
                                        sessionManager.pendingOAuthSession = com.example.data.network.SessionManager.PendingOAuthSession(
                                            provider = "UPSTOX",
                                            state = randomState,
                                            createdAt = System.currentTimeMillis(),
                                            redirectUri = redirectUriToUse,
                                            consumed = false
                                        )
                                        val loginUrl = com.example.util.UpstoxAuthHelper.buildLoginUrl(keyToUse, redirectUriToUse, state = randomState)
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))
                                            context.startActivity(intent)
                                            onDismiss()
                                        } catch (e: Exception) {
                                            localErrorMsg = "Failed to open browser: ${e.message}"
                                        }
                                    }
                                }
                            },
                            enabled = !isAuthInProgress,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                        ) {
                            if (isAuthInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ONE-CLICK AUTO LOGIN (UPSTOX)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Automatic Zero-Touch: Authenticate in browser and the app connects automatically.",
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showManualUpstox = !showManualUpstox },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showManualUpstox) "Hide Manual Entry ▲" else "Advanced: Manual Code / Token Entry ▼",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }

                        if (showManualUpstox) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = upstoxAuthCode,
                                onValueChange = { upstoxAuthCode = it },
                                label = { Text("Auth Code / Token") },
                                placeholder = { Text("Paste code or token", color = TextGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = Color(0xFF673AB7),
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = Color(0xFF673AB7),
                                    unfocusedLabelColor = TextGray,
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val codeOrToken = upstoxAuthCode.trim()
                                    val keyToUse = upstoxApiKey.trim().takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.upstoxApiKey
                                    val secretToUse = upstoxApiSecret.trim().takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.upstoxApiSecret
                                    val redirectUriToUse = upstoxRedirectUri.trim().takeIf { it.isNotBlank() } ?: "https://application-beige-psi.vercel.app/oauth"
                                    
                                    if (codeOrToken.isBlank()) {
                                        localErrorMsg = "Please enter an Auth Code, Callback URL, or Access Token."
                                    } else {
                                        localErrorMsg = null
                                        sessionManager.saveUpstoxCredentials(keyToUse, secretToUse, redirectUriToUse)
                                        onUpstoxLogin?.invoke(keyToUse, secretToUse, codeOrToken)
                                    }
                                },
                                enabled = !isAuthInProgress && upstoxAuthCode.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB39DDB)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF673AB7))
                            ) {
                                Text("CONNECT MANUALLY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Registered Redirect URI: https://application-beige-psi.vercel.app/oauth",
                            color = TextGray.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ProfitGreen, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("F", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Fyers (Alternative Market Data)",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = fyersAppId,
                            onValueChange = { fyersAppId = it },
                            label = { Text("App ID / Client ID") },
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
                            value = fyersSecretId,
                            onValueChange = { fyersSecretId = it },
                            label = { Text("Secret ID") },
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
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fyersRedirectUri,
                            onValueChange = { fyersRedirectUri = it },
                            label = { Text("Redirect URI") },
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val appToUse = fyersAppId.trim().takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.fyersAppId
                                val secretToUse = fyersSecretId.trim().takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.fyersSecretId
                                val redirectUriToUse = fyersRedirectUri.trim().takeIf { it.isNotBlank() } ?: com.example.util.FyersAuthHelper.DEFAULT_REDIRECT_URI
                                
                                if (appToUse.isBlank()) {
                                    localErrorMsg = "Fyers App ID is required."
                                } else {
                                    localErrorMsg = null
                                    sessionManager.saveFyersCredentials(appToUse, secretToUse, redirectUriToUse)
                                    
                                    if (onStartFyersOAuth != null) {
                                        onStartFyersOAuth(appToUse, secretToUse)
                                    } else {
                                        val randomState = com.example.util.FyersAuthHelper.generateSecureState()
                                        sessionManager.pendingOAuthState = randomState
                                        sessionManager.pendingOAuthBroker = "Fyers"
                                        sessionManager.pendingOAuthSession = com.example.data.network.SessionManager.PendingOAuthSession(
                                            provider = "FYERS",
                                            state = randomState,
                                            createdAt = System.currentTimeMillis(),
                                            redirectUri = redirectUriToUse,
                                            consumed = false
                                        )
                                        val loginUrl = com.example.util.FyersAuthHelper.buildLoginUrl(appToUse, redirectUriToUse, state = randomState)
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))
                                            context.startActivity(intent)
                                            onDismiss()
                                        } catch (e: Exception) {
                                            localErrorMsg = "Failed to open browser: ${e.message}"
                                        }
                                    }
                                }
                            },
                            enabled = !isAuthInProgress,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                        ) {
                            if (isAuthInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ONE-CLICK AUTO LOGIN (FYERS)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Automatic Zero-Touch: Authenticate in browser and the app connects automatically.",
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showManualFyers = !showManualFyers },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showManualFyers) "Hide Manual Entry ▲" else "Advanced: Manual Code / Token Entry ▼",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }

                        if (showManualFyers) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fyersAuthCode,
                                onValueChange = { fyersAuthCode = it },
                                label = { Text("Auth Code / Token") },
                                placeholder = { Text("Paste code or token", color = TextGray) },
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
                            OutlinedButton(
                                onClick = {
                                    val codeOrToken = fyersAuthCode.trim()
                                    val appToUse = fyersAppId.trim().takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.fyersAppId
                                    val secretToUse = fyersSecretId.trim().takeIf { it.isNotBlank() } ?: com.example.util.BrokerConfig.fyersSecretId
                                    val redirectUriToUse = fyersRedirectUri.trim().takeIf { it.isNotBlank() } ?: com.example.util.FyersAuthHelper.DEFAULT_REDIRECT_URI
                                    
                                    if (codeOrToken.isBlank()) {
                                        localErrorMsg = "Please enter an Auth Code, Callback URL, or Access Token."
                                    } else {
                                        localErrorMsg = null
                                        sessionManager.saveFyersCredentials(appToUse, secretToUse, redirectUriToUse)
                                        onFyersLogin?.invoke(appToUse, secretToUse, codeOrToken)
                                    }
                                },
                                enabled = !isAuthInProgress && fyersAuthCode.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfitGreen),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen)
                            ) {
                                Text("CONNECT MANUALLY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Registered Redirect URI: https://application-beige-psi.vercel.app/oauth",
                            color = TextGray.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
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
                        // Dhan Official OAuth Flow (Primary) + Direct Token (Alternative)
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Official Login Button
                        Button(
                            onClick = {
                                val clientId = dhanClientIdInput.trim().ifBlank { BrokerConfig.dhanClientId }
                                val apiKey = dhanApiKeyInput.trim().ifBlank { BrokerConfig.dhanApiKey }
                                val clientSecret = dhanClientSecretInput.trim().ifBlank { BrokerConfig.dhanClientSecret }

                                if (clientId.isBlank() || clientSecret.isBlank()) {
                                    showDhanCreds = true
                                    localErrorMsg = "Please enter your Dhan Client ID and Client Secret below."
                                } else {
                                    localErrorMsg = null
                                    sessionManager.dhanClientId = clientId
                                    sessionManager.dhanApiKey = apiKey
                                    sessionManager.dhanClientSecret = clientSecret

                                    val redirectUri = BrokerConfig.dhanRedirectUri.ifBlank { "kingkhan://oauth/callback" }
                                    val dhanState = com.example.util.DhanAuthHelper.generateSecureState()
                                    sessionManager.pendingOAuthBroker = "Dhan"
                                    sessionManager.pendingOAuthSession = com.example.data.network.SessionManager.PendingOAuthSession(
                                        provider = "DHAN",
                                        state = dhanState,
                                        createdAt = System.currentTimeMillis(),
                                        redirectUri = redirectUri,
                                        consumed = false
                                    )
                                    if (onStartDhanOAuth != null) {
                                        onStartDhanOAuth(clientId, apiKey, clientSecret)
                                    } else {
                                        isDhanConsentLoading = true
                                        coroutineScope.launch {
                                            val consentRes = com.example.util.DhanAuthHelper.generateConsent(clientId, apiKey, clientSecret, state = dhanState)
                                            isDhanConsentLoading = false
                                            consentRes.onSuccess { url ->
                                                android.util.Log.d("DhanAuth", "Opening browser for Dhan authorization")
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    localErrorMsg = "Unable to open browser: ${e.localizedMessage}"
                                                }
                                            }.onFailure { err ->
                                                localErrorMsg = err.localizedMessage ?: "Failed to generate Dhan OAuth consent URL"
                                            }
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LOGIN WITH DHAN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // API Credentials section toggle
                        TextButton(
                            onClick = { showDhanCreds = !showDhanCreds },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showDhanCreds) "Hide API Credentials ▲" else "Configure API Credentials ▼",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }

                        if (showDhanCreds) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dhanClientIdInput,
                                onValueChange = { dhanClientIdInput = it },
                                label = { Text("Client ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dhanApiKeyInput,
                                onValueChange = { dhanApiKeyInput = it },
                                label = { Text("App Key / API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dhanClientSecretInput,
                                onValueChange = { dhanClientSecretInput = it },
                                label = { Text("Client Secret") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Direct Access Token option toggle
                        TextButton(
                            onClick = { showDhanDirectToken = !showDhanDirectToken },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showDhanDirectToken) "Hide Direct Access Token ▲" else "Direct Access Token (Alternative) ▼",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }

                        if (showDhanDirectToken) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dhanAccessTokenInput,
                                onValueChange = { dhanAccessTokenInput = it },
                                label = { Text("DhanHQ Access Token") },
                                placeholder = { Text("Paste token from web.dhan.co", color = TextGray) },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = Color(0xFF00BFA5),
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = Color(0xFF00BFA5),
                                    unfocusedLabelColor = TextGray
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val clientId = dhanClientIdInput.trim()
                                    val token = dhanAccessTokenInput.trim()
                                    if (token.isBlank()) {
                                        localErrorMsg = "Please enter your Dhan Access Token."
                                    } else {
                                        localErrorMsg = null
                                        sessionManager.dhanClientId = clientId
                                        sessionManager.dhanAccessToken = token
                                        onDhanLogin?.invoke(clientId, token)
                                    }
                                },
                                enabled = !isAuthInProgress,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00BFA5)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00BFA5))
                            ) {
                                Text("CONNECT WITH ACCESS TOKEN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
