package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*
import com.example.util.BrokerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokerConnectDialog(
    initialBroker: String,
    isAuthInProgress: Boolean,
    errorMessage: String?,
    brokerStatuses: Map<String, com.example.data.network.BrokerConnectionState> = emptyMap(),
    providerHealth: Map<String, com.example.data.network.ProviderHealthState> = emptyMap(),
    sessionManager: com.example.data.network.SessionManager? = null,
    onDisconnect: ((String) -> Unit)? = null,
    onReconnect: ((String) -> Unit)? = null,
    onRemoveAccount: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onAngelLogin: ((String, String, String, String) -> Unit)? = null,
    onFyersLogin: ((String, String, String) -> Unit)? = null,
    onUpstoxLogin: ((String, String, String) -> Unit)? = null,
    onDhanLogin: ((String, String) -> Unit)? = null,
    onOpenUpstoxLogin: ((String, String) -> Unit)? = null,
    onOpenFyersLogin: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val brokerList = listOf("Dhan", "Angel One", "Upstox", "Fyers")
    val initialIndex = brokerList.indexOfFirst { it.equals(initialBroker, ignoreCase = true) }.coerceAtLeast(0)
    var selectedBroker by remember { mutableStateOf(brokerList[initialIndex]) }

    // Dhan Form State
    var dhanClientId by remember(selectedBroker) {
        mutableStateOf(sessionManager?.dhanClientId?.ifBlank { BrokerConfig.dhanClientId } ?: BrokerConfig.dhanClientId)
    }
    var dhanAccessToken by remember(selectedBroker) {
        mutableStateOf(sessionManager?.dhanAccessToken?.ifBlank { BrokerConfig.dhanApiKey } ?: BrokerConfig.dhanApiKey)
    }
    var showDhanToken by remember { mutableStateOf(false) }

    // Angel One Form State
    var angelClientCode by remember(selectedBroker) {
        mutableStateOf(sessionManager?.angelClientCode ?: "")
    }
    var angelMpin by remember(selectedBroker) {
        mutableStateOf(sessionManager?.angelClientPin ?: "")
    }
    var angelApiKey by remember(selectedBroker) {
        mutableStateOf(sessionManager?.angelApiKey?.ifBlank { BrokerConfig.angelApiKey } ?: BrokerConfig.angelApiKey)
    }
    var angelTotpSecret by remember(selectedBroker) {
        mutableStateOf(sessionManager?.angelTotpSecret ?: "")
    }
    var showAngelMpin by remember { mutableStateOf(false) }
    var showAngelTotp by remember { mutableStateOf(false) }

    // Upstox Form State
    var upstoxApiKey by remember(selectedBroker) {
        mutableStateOf(sessionManager?.upstoxApiKey?.ifBlank { BrokerConfig.upstoxApiKey } ?: BrokerConfig.upstoxApiKey)
    }
    var upstoxApiSecret by remember(selectedBroker) {
        mutableStateOf(sessionManager?.upstoxApiSecret?.ifBlank { BrokerConfig.upstoxApiSecret } ?: BrokerConfig.upstoxApiSecret)
    }
    var upstoxAuthCode by remember(selectedBroker) {
        mutableStateOf(sessionManager?.upstoxAccessToken ?: "")
    }

    // Fyers Form State
    var fyersAppId by remember(selectedBroker) {
        mutableStateOf(sessionManager?.fyersAppId?.ifBlank { BrokerConfig.fyersAppId } ?: BrokerConfig.fyersAppId)
    }
    var fyersSecretId by remember(selectedBroker) {
        mutableStateOf(sessionManager?.fyersSecretId?.ifBlank { BrokerConfig.fyersSecretId } ?: BrokerConfig.fyersSecretId)
    }
    var fyersAuthCode by remember(selectedBroker) {
        mutableStateOf(sessionManager?.fyersAccessToken ?: "")
    }

    val clipboardManager = LocalClipboardManager.current
    val currentStatus = brokerStatuses[selectedBroker]?.status ?: com.example.data.network.BrokerAuthStatus.DISCONNECTED
    val isConnected = currentStatus == com.example.data.network.BrokerAuthStatus.CONNECTED
    val isConfigured = sessionManager?.isBrokerConfigured(selectedBroker) == true || currentStatus == com.example.data.network.BrokerAuthStatus.AUTHENTICATION_REQUIRED || isConnected

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = DarkBackground,
            border = BorderStroke(1.2.dp, PrimaryGold.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CrownLogo(size = 28.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "CONNECT BROKER",
                                color = PrimaryGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "API Keys & Real-Time Trading Feeds",
                                color = TextGray,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF222222), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Error Banner if present
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = LossRedBg,
                        border = BorderStroke(1.dp, LossRed.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LossRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = LossRed,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Broker Selector Tabs
                ScrollableTabRow(
                    selectedTabIndex = brokerList.indexOf(selectedBroker).coerceAtLeast(0),
                    containerColor = DarkCardSecondary,
                    contentColor = PrimaryGold,
                    edgePadding = 12.dp,
                    divider = { HorizontalDivider(color = DarkCardBorder) }
                ) {
                    brokerList.forEach { broker ->
                        val isSel = selectedBroker == broker
                        val bStatus = brokerStatuses[broker]?.status ?: com.example.data.network.BrokerAuthStatus.DISCONNECTED
                        val isBConn = bStatus == com.example.data.network.BrokerAuthStatus.CONNECTED

                        Tab(
                            selected = isSel,
                            onClick = { selectedBroker = broker },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (isBConn) ProfitGreen else Color(0xFF666666),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        broker,
                                        color = if (isSel) PrimaryGold else TextGray,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                    }
                }

                // Scrollable Form Content Body
                val formScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(formScrollState)
                        .padding(16.dp)
                ) {
                    // Broker Status Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isConnected) ProfitGreen.copy(alpha = 0.5f) else DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val logoRes = when (selectedBroker) {
                                    "Angel One" -> R.drawable.ic_angel_one_logo
                                    "Upstox" -> R.drawable.ic_upstox_logo
                                    "Fyers" -> R.drawable.ic_fyers_logo
                                    else -> R.drawable.ic_dhan_logo
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = logoRes),
                                        contentDescription = "$selectedBroker Logo",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        selectedBroker,
                                        color = TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        when (selectedBroker) {
                                            "Dhan" -> "Primary Order Execution (DhanHQ V2)"
                                            "Angel One" -> "Primary Market Data & SmartAPI Feed"
                                            "Upstox" -> "Secondary Feed & Multi-Broker Engine"
                                            "Fyers" -> "API V3 Realtime Data & Trading"
                                            else -> "Trading Broker"
                                        },
                                        color = TextGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Connection Badge
                            Surface(
                                color = if (isConnected) ProfitGreenBg else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isConnected) ProfitGreen else Color(0xFF555555))
                            ) {
                                Text(
                                    text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                    color = if (isConnected) ProfitGreen else TextGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Forms Per Broker
                    when (selectedBroker) {
                        "Dhan" -> {
                            Text(
                                "Dhan HQ Credentials",
                                color = PrimaryGold,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Client ID
                            OutlinedTextField(
                                value = dhanClientId,
                                onValueChange = { dhanClientId = it },
                                label = { Text("Dhan Client ID (e.g. 1000000001)") },
                                placeholder = { Text("Enter your 10-digit Client ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Access Token
                            OutlinedTextField(
                                value = dhanAccessToken,
                                onValueChange = { dhanAccessToken = it },
                                label = { Text("Dhan Access Token (JWT)") },
                                placeholder = { Text("Paste token generated from web.dhan.co") },
                                visualTransformation = if (showDhanToken) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            clipboardManager.getText()?.text?.let { dhanAccessToken = it }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = PrimaryGold, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { showDhanToken = !showDhanToken }) {
                                            Icon(
                                                imageVector = if (showDhanToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = TextGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Guide Box
                            Surface(
                                color = Color(0xFF14171E),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF222B38)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("📌 How to get Dhan Access Token:", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text("1. Log in to web.dhan.co on your browser\n2. Go to Profile -> Access Token\n3. Click 'Generate Token' (valid for 30 days)\n4. Copy and paste it here", color = TextGray, fontSize = 10.sp, lineHeight = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onDhanLogin?.invoke(dhanClientId.trim(), dhanAccessToken.trim()) },
                                enabled = !isAuthInProgress && dhanClientId.isNotBlank() && dhanAccessToken.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGold,
                                    contentColor = Color.Black
                                )
                            ) {
                                if (isAuthInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CONNECTING DHAN...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SAVE & CONNECT DHAN", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }

                        "Angel One" -> {
                            Text(
                                "Angel One SmartAPI Login",
                                color = PrimaryGold,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Client Code
                            OutlinedTextField(
                                value = angelClientCode,
                                onValueChange = { angelClientCode = it.uppercase() },
                                label = { Text("Client Code (e.g. A123456)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // MPIN
                            OutlinedTextField(
                                value = angelMpin,
                                onValueChange = { if (it.length <= 6) angelMpin = it },
                                label = { Text("4-Digit MPIN") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = if (showAngelMpin) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showAngelMpin = !showAngelMpin }) {
                                        Icon(
                                            imageVector = if (showAngelMpin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // API Key
                            OutlinedTextField(
                                value = angelApiKey,
                                onValueChange = { angelApiKey = it },
                                label = { Text("SmartAPI API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // TOTP Secret
                            OutlinedTextField(
                                value = angelTotpSecret,
                                onValueChange = { angelTotpSecret = it },
                                label = { Text("TOTP Secret Key / Key from SmartAPI") },
                                singleLine = true,
                                visualTransformation = if (showAngelTotp) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            clipboardManager.getText()?.text?.let { angelTotpSecret = it }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = PrimaryGold, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { showAngelTotp = !showAngelTotp }) {
                                            Icon(
                                                imageVector = if (showAngelTotp) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = TextGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    onAngelLogin?.invoke(
                                        angelClientCode.trim(),
                                        angelMpin.trim(),
                                        angelApiKey.trim(),
                                        angelTotpSecret.trim()
                                    )
                                },
                                enabled = !isAuthInProgress && angelClientCode.isNotBlank() && angelMpin.isNotBlank() && angelApiKey.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E88E5),
                                    contentColor = Color.White
                                )
                            ) {
                                if (isAuthInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AUTHENTICATING ANGEL ONE...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LOGIN WITH ANGEL ONE", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }

                        "Upstox" -> {
                            Text(
                                "Upstox API & Market Streamer",
                                color = PrimaryGold,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = upstoxApiKey,
                                onValueChange = { upstoxApiKey = it },
                                label = { Text("API Key (Client ID)") },
                                placeholder = { Text("Enter your Upstox API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = upstoxApiSecret,
                                onValueChange = { upstoxApiSecret = it },
                                label = { Text("API Secret (Secret Key)") },
                                placeholder = { Text("Enter your Upstox API Secret") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // OAuth Login URL button
                            OutlinedButton(
                                onClick = {
                                    if (upstoxApiKey.isNotBlank()) {
                                        if (onOpenUpstoxLogin != null) {
                                            onOpenUpstoxLogin.invoke(upstoxApiKey.trim(), upstoxApiSecret.trim())
                                        } else {
                                            val url = com.example.util.UpstoxAuthHelper.buildLoginUrl(upstoxApiKey.trim())
                                            try {
                                                val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder().setShowTitle(true).build()
                                                customTabsIntent.launchUrl(context, Uri.parse(url))
                                            } catch (_: Exception) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                },
                                enabled = upstoxApiKey.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF673AB7)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB388FF))
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OPEN UPSTOX LOGIN IN BROWSER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Auth Code or Direct Access Token
                            OutlinedTextField(
                                value = upstoxAuthCode,
                                onValueChange = { upstoxAuthCode = it },
                                label = { Text("Auth Code / Redirect URL / Access Token") },
                                placeholder = { Text("Paste redirect URL or token from Upstox") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        clipboardManager.getText()?.text?.let { upstoxAuthCode = it }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = PrimaryGold, modifier = Modifier.size(18.dp))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { onUpstoxLogin?.invoke(upstoxApiKey.trim(), upstoxApiSecret.trim(), upstoxAuthCode.trim()) },
                                enabled = !isAuthInProgress && upstoxApiKey.isNotBlank() && (upstoxApiSecret.isNotBlank() || upstoxAuthCode.isNotBlank()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF673AB7),
                                    contentColor = Color.White
                                )
                            ) {
                                if (isAuthInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CONNECTING UPSTOX...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CONNECT UPSTOX", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }

                        "Fyers" -> {
                            Text(
                                "Fyers API V3 & Market Data",
                                color = PrimaryGold,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = fyersAppId,
                                onValueChange = { fyersAppId = it },
                                label = { Text("Fyers App ID (e.g. XC12345-100)") },
                                placeholder = { Text("Enter your Fyers App ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = fyersSecretId,
                                onValueChange = { fyersSecretId = it },
                                label = { Text("Secret ID / Key") },
                                placeholder = { Text("Enter your Fyers Secret ID") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // OAuth Login URL button
                            OutlinedButton(
                                onClick = {
                                    if (fyersAppId.isNotBlank()) {
                                        if (onOpenFyersLogin != null) {
                                            onOpenFyersLogin.invoke(fyersAppId.trim(), fyersSecretId.trim())
                                        } else {
                                            val url = com.example.util.FyersAuthHelper.buildLoginUrl(fyersAppId.trim())
                                            try {
                                                val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder().setShowTitle(true).build()
                                                customTabsIntent.launchUrl(context, Uri.parse(url))
                                            } catch (_: Exception) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                },
                                enabled = fyersAppId.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF00897B)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80CBC4))
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OPEN FYERS LOGIN IN BROWSER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Auth Code or Direct Access Token
                            OutlinedTextField(
                                value = fyersAuthCode,
                                onValueChange = { fyersAuthCode = it },
                                label = { Text("Auth Code / Redirect URL / Access Token") },
                                placeholder = { Text("Paste redirect URL or token from Fyers") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        clipboardManager.getText()?.text?.let { fyersAuthCode = it }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = PrimaryGold, modifier = Modifier.size(18.dp))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { onFyersLogin?.invoke(fyersAppId.trim(), fyersSecretId.trim(), fyersAuthCode.trim()) },
                                enabled = !isAuthInProgress && fyersAppId.isNotBlank() && (fyersSecretId.isNotBlank() || fyersAuthCode.isNotBlank()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00897B),
                                    contentColor = Color.White
                                )
                            ) {
                                if (isAuthInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CONNECTING FYERS...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CONNECT FYERS", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // If already configured or connected, offer Reconnect, Disconnect & Clear Saved Data Actions
                    if (isConnected || isConfigured) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DarkCardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onReconnect?.invoke(selectedBroker) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, PrimaryGold)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("RECONNECT", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onDisconnect?.invoke(selectedBroker) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, LossRed)
                                ) {
                                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LOGOUT", color = LossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (onRemoveAccount != null) {
                                TextButton(
                                    onClick = { onRemoveAccount.invoke(selectedBroker) },
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CLEAR ALL SAVED CONFIGURATION FOR $selectedBroker", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

