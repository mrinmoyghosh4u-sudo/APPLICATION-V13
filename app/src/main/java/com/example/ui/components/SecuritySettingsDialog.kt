package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.util.AppPreferences
import com.example.util.BiometricHelper

@Composable
fun SecuritySettingsDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
    onSecurityUpdated: () -> Unit
) {
    val context = LocalContext.current
    var isBiometricEnabled by remember { mutableStateOf(appPreferences.isBiometricEnabled()) }
    var hasPasscode by remember { mutableStateOf(appPreferences.hasPasscode()) }
    var isAppLockEnabled by remember { mutableStateOf(appPreferences.isAppLockEnabled()) }

    var showPinSetup by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var oldPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val biometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
    val biometricStatusMsg = remember { BiometricHelper.getBiometricStatusMessage(context) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkCardSecondary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SecondaryGold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Security & Biometric Lock", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Configure Passcode PIN & Biometric Access", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Status Overview Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("SECURITY STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Passcode:", fontSize = 11.sp, color = TextGray)
                                Text(
                                    if (hasPasscode) "Set" else "Not Set",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasPasscode) ProfitGreen else LossRed
                                )
                            }
                            Column {
                                Text("Biometric:", fontSize = 11.sp, color = TextGray)
                                Text(
                                    if (isBiometricEnabled && biometricAvailable) "Enabled"
                                    else if (!biometricAvailable) "Unavailable"
                                    else "Disabled",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBiometricEnabled && biometricAvailable) ProfitGreen else TextGray
                                )
                            }
                            Column {
                                Text("App Lock:", fontSize = 11.sp, color = TextGray)
                                Text(
                                    if (isAppLockEnabled) "Enabled" else "Disabled",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAppLockEnabled) ProfitGreen else TextGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                errorMessage?.let {
                    Text(it, color = LossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                successMessage?.let {
                    Text(it, color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // App Lock Toggle Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SecondaryGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("App Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text(
                                    if (isAppLockEnabled) "Lock screen shown on app launch & resume" else "App lock disabled",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasPasscode && !isBiometricEnabled) {
                                    errorMessage = "Please set a Passcode first to enable App Lock."
                                } else {
                                    isAppLockEnabled = enabled
                                    appPreferences.setAppLockEnabled(enabled)
                                    errorMessage = null
                                    successMessage = if (enabled) "App Lock enabled!" else "App Lock disabled."
                                    onSecurityUpdated()
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Biometric Toggle Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SecondaryGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Use Biometric Unlock", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text(
                                        if (biometricAvailable) {
                                            if (isBiometricEnabled) "Fingerprint / Face unlock enabled" else "Fingerprint / Face unlock available"
                                        } else {
                                            "Biometric authentication is not available on this device."
                                        },
                                        fontSize = 10.sp,
                                        color = if (biometricAvailable) TextGray else LossRed
                                    )
                                }
                            }

                            Switch(
                                checked = isBiometricEnabled && biometricAvailable,
                                enabled = biometricAvailable,
                                onCheckedChange = { enabled ->
                                    if (enabled && !hasPasscode) {
                                        errorMessage = "Please set up an App Passcode first as fallback."
                                    } else {
                                        isBiometricEnabled = enabled
                                        appPreferences.setBiometricEnabled(enabled)
                                        isAppLockEnabled = appPreferences.isAppLockEnabled()
                                        errorMessage = null
                                        successMessage = if (enabled) "Biometric unlock enabled!" else "Biometric unlock disabled."
                                        onSecurityUpdated()
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Passcode Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Passcode PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text(
                                    if (hasPasscode) "4-6 digit PIN active & hashed" else "No passcode set",
                                    fontSize = 10.sp,
                                    color = if (hasPasscode) ProfitGreen else TextGray
                                )
                            }

                            Button(
                                onClick = {
                                    showPinSetup = !showPinSetup
                                    pinInput = ""
                                    pinConfirmInput = ""
                                    oldPinInput = ""
                                    errorMessage = null
                                    successMessage = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    if (hasPasscode) "CHANGE PASSCODE" else "SET PASSCODE",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (showPinSetup) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DarkCardBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            if (hasPasscode) {
                                Text("Current Passcode", fontSize = 11.sp, color = TextWhite)
                                OutlinedTextField(
                                    value = oldPinInput,
                                    onValueChange = { if (it.length <= 6) oldPinInput = it },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryGold,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(if (hasPasscode) "New Passcode (4 to 6 digits)" else "Enter Passcode (4 to 6 digits)", fontSize = 11.sp, color = TextWhite)
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 6) pinInput = it },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Confirm Passcode", fontSize = 11.sp, color = TextWhite)
                            OutlinedTextField(
                                value = pinConfirmInput,
                                onValueChange = { if (it.length <= 6) pinConfirmInput = it },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (hasPasscode) {
                                    OutlinedButton(
                                        onClick = {
                                            if (!appPreferences.verifyPasscode(oldPinInput)) {
                                                errorMessage = "Current passcode is incorrect."
                                            } else {
                                                appPreferences.disablePasscode()
                                                hasPasscode = false
                                                isBiometricEnabled = false
                                                isAppLockEnabled = false
                                                showPinSetup = false
                                                errorMessage = null
                                                successMessage = "Passcode disabled successfully."
                                                onSecurityUpdated()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, LossRed)
                                    ) {
                                        Text("DISABLE PASSCODE", color = LossRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (hasPasscode && !appPreferences.verifyPasscode(oldPinInput)) {
                                            errorMessage = "Current passcode is incorrect."
                                            return@Button
                                        }
                                        if (pinInput.length < 4 || pinInput.length > 6) {
                                            errorMessage = "Passcode must be 4 to 6 digits."
                                            return@Button
                                        }
                                        if (pinInput != pinConfirmInput) {
                                            errorMessage = "Passcodes do not match."
                                            return@Button
                                        }

                                        appPreferences.setPasscode(pinInput)
                                        hasPasscode = true
                                        isAppLockEnabled = true
                                        showPinSetup = false
                                        errorMessage = null
                                        successMessage = "Passcode saved successfully!"
                                        onSecurityUpdated()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                                ) {
                                    Text("SAVE PASSCODE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
