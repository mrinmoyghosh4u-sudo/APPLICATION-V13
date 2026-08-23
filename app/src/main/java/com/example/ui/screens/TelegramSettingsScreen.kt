package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.TelegramApiResponseInfo
import com.example.ui.components.GoldCard
import com.example.ui.theme.*
import com.example.util.AlertPreferences

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelegramSettingsScreen(
    botToken: String,
    chatId: String,
    channelId: String = "",
    isAlertsEnabled: Boolean,
    isTesting: Boolean,
    telegramResponseInfo: TelegramApiResponseInfo?,
    alertPreferences: AlertPreferences = AlertPreferences(),
    isSmsEnabled: Boolean = false,
    smsPhone: String = "",
    smsGatewayUrl: String = "",
    smsApiKey: String = "",
    isSmsTesting: Boolean = false,
    smsStatusMessage: String? = null,
    onBack: () -> Unit,
    onSaveSettings: (token: String, chatId: String, isEnabled: Boolean, channelId: String) -> Unit,
    onToggleAlertEvent: (eventType: String, enabled: Boolean) -> Unit = { _, _ -> },
    onSaveSmsSettings: (phone: String, enabled: Boolean, gatewayUrl: String, apiKey: String) -> Unit = { _, _, _, _ -> },
    onTestTelegramBot: (token: String, chatId: String, channelId: String) -> Unit,
    onTestAlert: (alertType: String, symbol: String, details: String) -> Unit,
    onTestSms: (phone: String, message: String) -> Unit = { _, _ -> },
    onClearResponse: () -> Unit,
    onClearSmsStatus: () -> Unit = {}
) {
    var tokenInput by remember(botToken) { mutableStateOf(botToken) }
    var chatIdInput by remember(chatId) { mutableStateOf(chatId) }
    var channelIdInput by remember(channelId) { mutableStateOf(channelId) }
    var alertsEnabledState by remember(isAlertsEnabled) { mutableStateOf(isAlertsEnabled) }
    var isTokenVisible by remember { mutableStateOf(false) }

    // SMS States
    var smsPhoneInput by remember(smsPhone) { mutableStateOf(smsPhone) }
    var smsEnabledState by remember(isSmsEnabled) { mutableStateOf(isSmsEnabled) }
    var smsGatewayUrlInput by remember(smsGatewayUrl) { mutableStateOf(smsGatewayUrl) }
    var smsApiKeyInput by remember(smsApiKey) { mutableStateOf(smsApiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryGold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Alerts & Telegram Engine",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Real-time AI Trading Signals, Orders & SMS",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (alertsEnabledState) ProfitGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (alertsEnabledState) ProfitGreen else LossRed
                    )
                ) {
                    Text(
                        text = if (alertsEnabledState) "TELEGRAM LIVE" else "DISABLED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alertsEnabledState) ProfitGreen else LossRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Telegram Master Switch
            GoldCard(borderColor = if (alertsEnabledState) PrimaryGold else DarkCardBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF0088CC).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF0088CC), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                tint = Color(0xFF0088CC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Enable Telegram Notifications",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Dispatch instant alerts to your Telegram chat or channel",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }

                    Switch(
                        checked = alertsEnabledState,
                        onCheckedChange = {
                            alertsEnabledState = it
                            onSaveSettings(tokenInput, chatIdInput, it, channelIdInput)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = PrimaryGold,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = DarkCardSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Telegram Bot Credentials Card
            GoldCard(borderColor = DarkCardBorder) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Telegram API Configuration",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bot Token Field
                    Text("Telegram Bot Token", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ", fontSize = 11.sp, color = TextGray) },
                        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle token visibility",
                                    tint = TextGray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkCardSecondary,
                            unfocusedContainerColor = DarkCardSecondary
                        ),
                        singleLine = true
                    )
                    Text(
                        text = "💡 Generated via Telegram's @BotFather bot",
                        fontSize = 10.sp,
                        color = TextGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chat ID Field
                    Text("Telegram Chat ID / User ID", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = chatIdInput,
                        onValueChange = { chatIdInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 987654321", fontSize = 11.sp, color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkCardSecondary,
                            unfocusedContainerColor = DarkCardSecondary
                        ),
                        singleLine = true
                    )
                    Text(
                        text = "💡 Find your Chat ID via @userinfobot (direct private alerts)",
                        fontSize = 10.sp,
                        color = TextGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Channel ID Field
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Telegram Channel ID / Username", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryGold.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, PrimaryGold)
                        ) {
                            Text(
                                "OPTIONAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = channelIdInput,
                        onValueChange = { channelIdInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. @MyTradingChannel or -1001234567890", fontSize = 11.sp, color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkCardSecondary,
                            unfocusedContainerColor = DarkCardSecondary
                        ),
                        singleLine = true
                    )
                    Text(
                        text = "📢 Broadcasts simultaneously to both Chat and Channel when configured (Add bot as Admin)",
                        fontSize = 10.sp,
                        color = TextGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Save & Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveSettings(tokenInput, chatIdInput, alertsEnabledState, channelIdInput)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SAVE SETTINGS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onTestTelegramBot(tokenInput, chatIdInput, channelIdInput)
                            },
                            enabled = !isTesting && tokenInput.isNotBlank() && (chatIdInput.isNotBlank() || channelIdInput.isNotBlank()),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0088CC)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0088CC))
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0088CC), strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF0088CC), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TEST TELEGRAM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Telegram API Response Card
            telegramResponseInfo?.let { response ->
                GoldCard(
                    borderColor = if (response.isSuccess) ProfitGreen else LossRed
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (response.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (response.isSuccess) ProfitGreen else LossRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Telegram API Server Response",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }

                            IconButton(onClick = onClearResponse, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("HTTP Status: ${response.httpCode}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                            Text("ok: ${response.okFlag}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (response.okFlag) ProfitGreen else LossRed)
                            Text("Time: ${response.timestamp}", fontSize = 10.sp, color = TextGray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DarkCardSecondary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = response.description,
                                fontSize = 11.sp,
                                color = if (response.isSuccess) TextWhite else LossRed,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. SMS ALERT SERVICES CARD
            GoldCard(borderColor = if (smsEnabledState) SecondaryGold else DarkCardBorder) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, Color(0xFF2E7D32), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("SMS Alert Transport", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text("Cellular & Web SMS Gateway Delivery", fontSize = 10.sp, color = TextGray)
                            }
                        }

                        Switch(
                            checked = smsEnabledState,
                            onCheckedChange = {
                                smsEnabledState = it
                                onSaveSmsSettings(smsPhoneInput, it, smsGatewayUrlInput, smsApiKeyInput)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = PrimaryGold,
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = DarkCardSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Recipient Phone Number (+91)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = smsPhoneInput,
                        onValueChange = { smsPhoneInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 9876543210", fontSize = 11.sp, color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkCardSecondary,
                            unfocusedContainerColor = DarkCardSecondary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Custom Web SMS Gateway URL (Optional)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = smsGatewayUrlInput,
                        onValueChange = { smsGatewayUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. https://api.sms-gateway.com/send", fontSize = 11.sp, color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkCardSecondary,
                            unfocusedContainerColor = DarkCardSecondary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveSmsSettings(smsPhoneInput, smsEnabledState, smsGatewayUrlInput, smsApiKeyInput)
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                        ) {
                            Text("SAVE SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                onTestSms(smsPhoneInput, "KK TRADE ALERT: Real-time SMS alert integration verified successfully.")
                            },
                            enabled = !isSmsTesting && (smsPhoneInput.isNotBlank() || smsGatewayUrlInput.isNotBlank()),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfitGreen)
                        ) {
                            if (isSmsTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ProfitGreen, strokeWidth = 2.dp)
                            } else {
                                Text("TEST SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    smsStatusMessage?.let { status ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DarkCardSecondary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(status, fontSize = 11.sp, color = SecondaryGold, modifier = Modifier.weight(1f))
                                IconButton(onClick = onClearSmsStatus, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextGray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. ALERT PREFERENCES (ENFORCE ON / OFF FOR ALL 16 EVENTS)
            GoldCard(borderColor = DarkCardBorder) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Enforced Alert Triggers (16 Events)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryGold
                            )
                            Text(
                                text = "Strictly controls Telegram, SMS & Push for each event",
                                fontSize = 10.sp,
                                color = TextGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val toggles = listOf(
                        Triple("brokerConnected", "🟢 Broker Connected", alertPreferences.brokerConnected),
                        Triple("brokerDisconnected", "🔴 Broker Disconnected", alertPreferences.brokerDisconnected),
                        Triple("buyCeSignal", "🟢 AI BUY CE Signal", alertPreferences.buyCeSignal),
                        Triple("buyPeSignal", "🔴 AI BUY PE Signal", alertPreferences.buyPeSignal),
                        Triple("entryPosition", "⚡ Entry / Position Opened", alertPreferences.entryPosition),
                        Triple("stopLossHit", "🛑 Stop Loss Hit", alertPreferences.stopLossHit),
                        Triple("target1Hit", "🎯 Target 1 Hit", alertPreferences.target1Hit),
                        Triple("target2Hit", "🎯 Target 2 Hit", alertPreferences.target2Hit),
                        Triple("target3Hit", "🚀 Target 3 Hit", alertPreferences.target3Hit),
                        Triple("target4Hit", "🏆 Target 4 Hit", alertPreferences.target4Hit),
                        Triple("trailingSlHit", "📈 Trailing Stop Loss Hit", alertPreferences.trailingSlHit),
                        Triple("orderExecuted", "✅ Broker Order Executed", alertPreferences.orderExecuted),
                        Triple("orderRejected", "❌ Broker Order Rejected", alertPreferences.orderRejected),
                        Triple("algoStarted", "🤖 Algo Engine Started", alertPreferences.algoStarted),
                        Triple("algoStopped", "⏹️ Algo Engine Stopped", alertPreferences.algoStopped),
                        Triple("riskLimitReached", "🚨 Risk Limit Reached", alertPreferences.riskLimitReached)
                    )

                    toggles.forEach { (key, label, isChecked) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { onToggleAlertEvent(key, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PrimaryGold,
                                    uncheckedThumbColor = TextGray,
                                    uncheckedTrackColor = DarkCardSecondary
                                )
                            )
                        }
                        HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. TEST REAL ALERT DISPATCH BUTTONS
            GoldCard(borderColor = DarkCardBorder) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Instant Alert Trigger Tests",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGold
                    )
                    Text(
                        text = "Dispatches configured alert format directly to Telegram & SMS",
                        fontSize = 10.sp,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val testList = listOf(
                        Triple("AI BUY CE Signal", "🟢", "NIFTY 24850 CE Buy Signal Triggered @ ₹125"),
                        Triple("AI BUY PE Signal", "🔴", "BANKNIFTY 52400 PE Buy Signal Triggered @ ₹210"),
                        Triple("Order Executed", "✅", "Order Executed: BUY 65 x NIFTY 24850 CE @ ₹125.00"),
                        Triple("Order Rejected", "❌", "Order Rejected: Insufficient Margin"),
                        Triple("Stop Loss Hit", "🛑", "NIFTY 24850 CE Stop Loss Hit @ ₹95.00"),
                        Triple("Target 1 Hit", "🎯", "NIFTY Target 1 Achieved @ ₹145.00"),
                        Triple("Target 2 Hit", "🎯", "NIFTY Target 2 Achieved @ ₹165.00"),
                        Triple("Risk Limit Reached", "🚨", "Daily Max Loss Limit Reached: Trading Halted")
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        testList.forEach { (type, emoji, sampleDetails) ->
                            Surface(
                                modifier = Modifier.clickable {
                                    onTestAlert(type, "NIFTY 50", sampleDetails)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = DarkCardSecondary,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emoji, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Send, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
