package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.CrownLogo
import com.example.ui.theme.*
import com.example.util.BrokerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onConnectBroker: (String) -> Unit,
    onDhanLogin: ((String, String) -> Unit)? = null,
    onAngelLogin: ((String, String, String, String) -> Unit)? = null,
    onSkipLogin: () -> Unit = {},
    viewModel: com.example.viewmodel.MainViewModel? = null
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    // Observe ViewModel Auth State if available
    val isAuthInProgress by viewModel?.isAuthInProgress?.collectAsStateWithLifecycle(initialValue = false)
        ?: remember { mutableStateOf(false) }
    val authErrorMessage by viewModel?.authErrorMessage?.collectAsStateWithLifecycle(initialValue = null)
        ?: remember { mutableStateOf<String?>(null) }

    // Navigation & Form State
    var mainLoginMode by remember { mutableStateOf(0) } // 0 = Direct Fast Auth, 1 = Broker Cards & OAuth
    var selectedDirectBroker by remember { mutableStateOf("Dhan") } // Dhan, Angel One, Upstox, Fyers

    // Dhan Form Fields
    var dhanClientId by remember { mutableStateOf(BrokerConfig.dhanClientId) }
    var dhanAccessToken by remember { mutableStateOf(BrokerConfig.dhanApiKey) }
    var showDhanToken by remember { mutableStateOf(false) }

    // Angel One Form Fields
    var angelClientCode by remember { mutableStateOf("") }
    var angelMpin by remember { mutableStateOf("") }
    var angelApiKey by remember { mutableStateOf(BrokerConfig.angelApiKey) }
    var angelTotpSecret by remember { mutableStateOf("") }
    var showAngelMpin by remember { mutableStateOf(false) }
    var showAngelTotp by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040406),
                        Color(0xFF0B0A03),
                        Color(0xFF020204)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // --- HEADER & LOGO BLOCK ---
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(104.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryGold.copy(alpha = 0.30f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .border(1.2.dp, PrimaryGold.copy(alpha = 0.7f), CircleShape)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CrownLogo(size = 72.dp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "KING KHAN AI TRADE",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Motto Line with gold accents
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, PrimaryGold)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Trade ", color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Like a ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("King ", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("👑", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(PrimaryGold, Color.Transparent)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Message Banner if authentication failed
                AnimatedVisibility(visible = !authErrorMessage.isNullOrBlank()) {
                    authErrorMessage?.let { errMsg ->
                        Surface(
                            color = LossRedBg,
                            border = BorderStroke(1.dp, LossRed.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LossRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errMsg,
                                    color = LossRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // --- MAIN DUAL TAB SWITCHER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(12.dp))
                        .border(1.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (mainLoginMode == 0) PrimaryGold else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { mainLoginMode = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ DIRECT FAST AUTH",
                            color = if (mainLoginMode == 0) Color.Black else TextGray,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (mainLoginMode == 1) PrimaryGold else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { mainLoginMode = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🌐 BROKER CARDS & OAUTH",
                            color = if (mainLoginMode == 1) Color.Black else TextGray,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (mainLoginMode == 0) {
                    // --- TAB 0: DIRECT CREDENTIAL LOGIN FORM ---
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "SELECT BROKER FOR DIRECT LOGIN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Broker Selection Chips Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Dhan", "Angel One", "Upstox", "Fyers").forEach { broker ->
                                    val isSel = selectedDirectBroker == broker
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedDirectBroker = broker },
                                        color = if (isSel) Color(0xFF2A220F) else Color(0xFF14171E),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSel) PrimaryGold else DarkCardBorder
                                        )
                                    ) {
                                        Text(
                                            text = broker,
                                            color = if (isSel) PrimaryGold else TextGray,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            when (selectedDirectBroker) {
                                "Dhan" -> {
                                    // DHAN DIRECT ACCESS TOKEN FORM
                                    Text(
                                        text = "Dhan HQ Direct Developer Credentials",
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Dhan Client ID
                                    OutlinedTextField(
                                        value = dhanClientId,
                                        onValueChange = { dhanClientId = it },
                                        label = { Text("Dhan Client ID (e.g. 1000000001)") },
                                        placeholder = { Text("Enter 10-digit Client ID") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dhan_client_id_input"),
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

                                    // Dhan Access Token
                                    OutlinedTextField(
                                        value = dhanAccessToken,
                                        onValueChange = { dhanAccessToken = it },
                                        label = { Text("Dhan Permanent Access Token (JWT)") },
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dhan_access_token_input"),
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

                                    // Direct Token Instructions Box
                                    Surface(
                                        color = Color(0xFF14171E),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF222B38)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("📌 Direct Token Access (Bypasses Browser Loops):", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                "1. Log in to web.dhan.co in your browser\n2. Open Profile -> Access Token / Developer HQ\n3. Click 'Generate Token' (valid for 30 days)\n4. Copy and paste Client ID & Access Token here for 24/7 direct execution!",
                                                color = TextGray,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            if (onDhanLogin != null) {
                                                onDhanLogin.invoke(dhanClientId.trim(), dhanAccessToken.trim())
                                            } else {
                                                onConnectBroker("Dhan")
                                            }
                                        },
                                        enabled = !isAuthInProgress && dhanClientId.isNotBlank() && dhanAccessToken.isNotBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("dhan_direct_connect_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryGold,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        if (isAuthInProgress) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("VALIDATING & CONNECTING...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        } else {
                                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SAVE & CONNECT DHAN DIRECT ⚡", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        }
                                    }
                                }

                                "Angel One" -> {
                                    // ANGEL ONE SMARTAPI FORM
                                    Text(
                                        text = "Angel One SmartAPI Direct Login",
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Client Code
                                    OutlinedTextField(
                                        value = angelClientCode,
                                        onValueChange = { angelClientCode = it.uppercase() },
                                        label = { Text("Angel Client Code (e.g. A123456)") },
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
                                        label = { Text("TOTP Secret Key") },
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
                                            if (onAngelLogin != null) {
                                                onAngelLogin.invoke(
                                                    angelClientCode.trim(),
                                                    angelMpin.trim(),
                                                    angelApiKey.trim(),
                                                    angelTotpSecret.trim()
                                                )
                                            } else {
                                                onConnectBroker("Angel One")
                                            }
                                        },
                                        enabled = !isAuthInProgress && angelClientCode.isNotBlank() && angelMpin.isNotBlank() && angelApiKey.isNotBlank() && angelTotpSecret.isNotBlank(),
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
                                            Text("AUTHENTICATING...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        } else {
                                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("LOGIN WITH ANGEL ONE 🔐", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        }
                                    }
                                }

                                else -> {
                                    // UPSTOX & FYERS QUICK LAUNCHER
                                    Text(
                                        text = "$selectedDirectBroker OAuth Connection",
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        color = Color(0xFF14171E),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF222B38)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("🌐 Launch $selectedDirectBroker Authentication", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Tap below to open $selectedDirectBroker connection dialog to configure API Keys and OAuth consent.",
                                                color = TextGray,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { onConnectBroker(selectedDirectBroker) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryGold,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CONNECT $selectedDirectBroker 🚀", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- TAB 1: ALL BROKER CARDS & OAUTH ---
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.8.dp, PrimaryGold.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SELECT BROKER TO CONNECT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // DHAN LOGIN CARD
                            BrokerLoginCard(
                                title = "LOGIN WITH DHAN",
                                subtitle = "⚡ Direct Token & Dhan HQ API v2",
                                containerColor = Color(0xFF003D2C),
                                borderColor = Color(0xFF00875A),
                                testTag = "dhan_login_button",
                                iconContent = {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_dhan_logo),
                                        contentDescription = "Dhan Logo",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                onClick = { onConnectBroker("Dhan") }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // ANGEL ONE LOGIN CARD
                            BrokerLoginCard(
                                title = "LOGIN WITH ANGEL ONE",
                                subtitle = "📊 Live Market Quotes & SmartAPI TOTP",
                                containerColor = Color(0xFF092B6B),
                                borderColor = Color(0xFF1E88E5),
                                testTag = "angel_login_button",
                                iconContent = {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_angel_one_logo),
                                        contentDescription = "Angel One Logo",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                onClick = { onConnectBroker("Angel One") }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // UPSTOX LOGIN CARD
                            BrokerLoginCard(
                                title = "LOGIN WITH UPSTOX",
                                subtitle = "📈 Upstox Pro API & Live Data Stream",
                                containerColor = Color(0xFF2C103D),
                                borderColor = Color(0xFF8E24AA),
                                testTag = "upstox_login_button",
                                iconContent = {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_upstox_logo),
                                        contentDescription = "Upstox Logo",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                onClick = { onConnectBroker("Upstox") }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // FYERS LOGIN CARD
                            BrokerLoginCard(
                                title = "LOGIN WITH FYERS",
                                subtitle = "🚀 Fyers API v3 OAuth & Order Gateway",
                                containerColor = Color(0xFF331C04),
                                borderColor = Color(0xFFE65100),
                                testTag = "fyers_login_button",
                                iconContent = {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_fyers_logo),
                                        contentDescription = "Fyers Logo",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                onClick = { onConnectBroker("Fyers") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security & Feature Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FeatureBadge(icon = Icons.Outlined.Lock, title = "256-BIT\nENCRYPTED")
                    FeatureBadge(icon = Icons.Outlined.BarChart, title = "REAL TIME\nQUOTES")
                    FeatureBadge(icon = Icons.Outlined.FlashOn, title = "ALGO\nEXECUTION")
                    FeatureBadge(icon = Icons.Outlined.SupportAgent, title = "24/7\nSUPPORT")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOTTOM GOLDEN LINE < SKIP TO DEMO / PAPER TRADING > GOLDEN LINE ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkipLogin() }
                    .padding(vertical = 10.dp)
                    .testTag("skip_login_button")
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, PrimaryGold.copy(alpha = 0.8f))
                            )
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF14171C), RoundedCornerShape(18.dp))
                        .border(0.8.dp, PrimaryGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SKIP TO DEMO / PAPER TRADING",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold,
                        letterSpacing = 1.0.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryGold.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun BrokerLoginCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    borderColor: Color,
    testTag: String,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Logo Avatar Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,
                        fontSize = 9.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Arrow Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(0.8.dp, PrimaryGold.copy(alpha = 0.7f), CircleShape)
                .background(DarkCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            textAlign = TextAlign.Center,
            lineHeight = 10.sp
        )
    }
}
