package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
    onAngelLogin: ((String, String, String) -> Unit)? = null,
    onMStockLogin: ((String, String, String, String) -> Unit)? = null
) {
    var selectedBroker by remember { mutableStateOf(if (initialBroker.isBlank()) "m.Stock" else initialBroker) }
    var localErrorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("trading_prefs", android.content.Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()

    var angelClientCode by remember { mutableStateOf(prefs.getString("angel_client_id", "") ?: "") }
    var angelMpin by remember { mutableStateOf(prefs.getString("angel_mpin_enc", "") ?: "") }
    var angelTotp by remember { mutableStateOf("") }
    var isDhanConsentLoading by remember { mutableStateOf(false) }

    var mstockApiKey by remember { mutableStateOf(prefs.getString("mstock_api_key_enc", "") ?: "") }
    var mstockClientId by remember { mutableStateOf(prefs.getString("mstock_client_id", "") ?: "") }
    var mstockPasswordPin by remember { mutableStateOf(prefs.getString("mstock_password_pin_enc", "") ?: "") }
    var mstockTotp by remember { mutableStateOf("") }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            localErrorMsg = errorMessage
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
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BrokerTab(
                        name = "m.Stock",
                        logoRes = 0,
                        isSelected = selectedBroker == "m.Stock",
                        onClick = { 
                            selectedBroker = "m.Stock"
                            localErrorMsg = null
                        }
                    )
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
                        name = "Angel One",
                        logoRes = R.drawable.ic_angel_one_logo,
                        isSelected = selectedBroker == "Angel One",
                        onClick = { 
                            selectedBroker = "Angel One"
                            localErrorMsg = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedBroker == "m.Stock") {
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
                            "m.Stock (Mirae Asset)",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                        value = mstockPasswordPin,
                        onValueChange = { mstockPasswordPin = it },
                        label = { Text("Password / MPIN") },
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
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mstockTotp,
                        onValueChange = { mstockTotp = it },
                        label = { Text("TOTP / Access Token") },
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
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (mstockApiKey.isBlank() && mstockClientId.isBlank()) {
                                localErrorMsg = "Please enter API Key or Client ID."
                            } else {
                                localErrorMsg = null
                                onMStockLogin?.invoke(mstockApiKey, mstockClientId, mstockPasswordPin, mstockTotp)
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
                } else if (selectedBroker == "Angel One") {
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
                            "Angel One SmartAPI",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = angelClientCode,
                        onValueChange = { angelClientCode = it },
                        label = { Text("Client Code") },
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
                        value = angelTotp,
                        onValueChange = { angelTotp = it },
                        label = { Text("TOTP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = ProfitGreen,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedLabelColor = ProfitGreen,
                            unfocusedLabelColor = TextGray,
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val apiKey = BrokerConfig.angelApiKey
                            if (apiKey.isBlank()) {
                                localErrorMsg = "Missing Angel One API Key. Please configure it securely."
                            } else if (angelClientCode.isBlank() || angelMpin.isBlank() || angelTotp.isBlank()) {
                                localErrorMsg = "Please fill in all fields."
                            } else {
                                localErrorMsg = null
                                onAngelLogin?.invoke(angelClientCode, angelMpin, angelTotp)
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
                } else {
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
                            "Dhan HQ OAuth Portal",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "You will be redirected to Dhan's official login page to authorize this app securely.",
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
                                        println("Complete Consent URL: $url")
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
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

                localErrorMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = msg,
                        color = Color.Red.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
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
    logoRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen) else null,
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (logoRes != 0) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = name,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xFFE53935), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("m", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                name,
                color = if (isSelected) TextWhite else TextGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
    }
}

