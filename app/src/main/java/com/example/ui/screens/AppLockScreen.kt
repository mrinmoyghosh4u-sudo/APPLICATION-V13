package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.CrownLogo
import com.example.ui.theme.*
import com.example.util.AppPreferences
import com.example.util.BiometricHelper

@Composable
fun AppLockScreen(
    appPreferences: AppPreferences,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isBiometricEnabled = remember { appPreferences.isBiometricEnabled() }
    val hasPasscode = remember { appPreferences.hasPasscode() }

    fun triggerBiometric() {
        val activity = context as? FragmentActivity
        if (activity != null && isBiometricEnabled && BiometricHelper.isBiometricAvailable(activity)) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Unlock KING KHAN AI TRADE",
                subtitle = "Biometric Security Lock",
                negativeButtonText = if (hasPasscode) "Use Passcode PIN" else "Cancel",
                onSuccess = { onUnlocked() },
                onError = { err -> errorMessage = err },
                onUsePasscode = { /* switch to PIN UI */ }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled) {
            triggerBiometric()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CrownLogo(size = 64.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text("KING KHAN AI TRADE", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextWhite)
            Text("App Locked for Security", fontSize = 12.sp, color = SecondaryGold)

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(DarkCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (hasPasscode) {
                Text("Enter Passcode PIN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { input ->
                        if (input.length <= 6) {
                            pinInput = input
                            errorMessage = null
                            if (input.length >= 4 && appPreferences.verifyPasscode(input)) {
                                onUnlocked()
                            }
                        }
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.width(220.dp).height(54.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (appPreferences.verifyPasscode(pinInput)) {
                            onUnlocked()
                        } else {
                            errorMessage = "Incorrect Passcode. Try again."
                            pinInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(220.dp).height(44.dp)
                ) {
                    Text("UNLOCK APP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isBiometricEnabled) {
                OutlinedButton(
                    onClick = { triggerBiometric() },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold),
                    modifier = Modifier.width(220.dp).height(44.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SecondaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("USE BIOMETRIC", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(err, color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}
