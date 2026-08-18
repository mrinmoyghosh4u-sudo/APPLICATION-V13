package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

import com.example.util.AlertPreferences
import com.example.util.AppPreferences

@Composable
fun AlertPreferencesDialog(
    appPreferences: AppPreferences? = null,
    onDismiss: () -> Unit
) {
    val initialPrefs = remember { appPreferences?.getAlertPreferences() ?: AlertPreferences() }

    var brokerConnected by remember { mutableStateOf(initialPrefs.brokerConnected) }
    var brokerDisconnected by remember { mutableStateOf(initialPrefs.brokerDisconnected) }
    var buyCeSignal by remember { mutableStateOf(initialPrefs.buyCeSignal) }
    var buyPeSignal by remember { mutableStateOf(initialPrefs.buyPeSignal) }
    var entryPosition by remember { mutableStateOf(initialPrefs.entryPosition) }
    var stopLossHit by remember { mutableStateOf(initialPrefs.stopLossHit) }
    var target1Hit by remember { mutableStateOf(initialPrefs.target1Hit) }
    var target2Hit by remember { mutableStateOf(initialPrefs.target2Hit) }
    var target3Hit by remember { mutableStateOf(initialPrefs.target3Hit) }
    var target4Hit by remember { mutableStateOf(initialPrefs.target4Hit) }
    var trailingSlHit by remember { mutableStateOf(initialPrefs.trailingSlHit) }
    var orderExecuted by remember { mutableStateOf(initialPrefs.orderExecuted) }
    var orderRejected by remember { mutableStateOf(initialPrefs.orderRejected) }
    var algoStarted by remember { mutableStateOf(initialPrefs.algoStarted) }
    var algoStopped by remember { mutableStateOf(initialPrefs.algoStopped) }
    var riskLimitReached by remember { mutableStateOf(initialPrefs.riskLimitReached) }

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
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SecondaryGold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("ALERT PREFERENCES", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Customize Active Telegram & App Push Triggers", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Toggle items list
                AlertToggleRow("Broker Connected", "🔗", brokerConnected) { brokerConnected = it }
                AlertToggleRow("Broker Disconnected", "🔴", brokerDisconnected) { brokerDisconnected = it }
                AlertToggleRow("AI BUY CE Signal", "🟢", buyCeSignal) { buyCeSignal = it }
                AlertToggleRow("AI BUY PE Signal", "🔴", buyPeSignal) { buyPeSignal = it }
                AlertToggleRow("Entry (Position Opened)", "🎯", entryPosition) { entryPosition = it }
                AlertToggleRow("Stop Loss Hit", "🛑", stopLossHit) { stopLossHit = it }
                AlertToggleRow("Target 1 Hit", "🎯", target1Hit) { target1Hit = it }
                AlertToggleRow("Target 2 Hit", "🎯", target2Hit) { target2Hit = it }
                AlertToggleRow("Target 3 Hit", "🚀", target3Hit) { target3Hit = it }
                AlertToggleRow("Target 4 Hit", "🎯", target4Hit) { target4Hit = it }
                AlertToggleRow("Trailing Stop Loss Hit", "📈", trailingSlHit) { trailingSlHit = it }
                AlertToggleRow("Order Executed", "✅", orderExecuted) { orderExecuted = it }
                AlertToggleRow("Order Rejected", "❌", orderRejected) { orderRejected = it }
                AlertToggleRow("Algo Started", "▶️", algoStarted) { algoStarted = it }
                AlertToggleRow("Algo Stopped", "⏹️", algoStopped) { algoStopped = it }
                AlertToggleRow("Risk Limit Reached", "⚠️", riskLimitReached) { riskLimitReached = it }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "This is an OPTIONS BUYER ONLY application.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅ BUY CE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            Text("✅ BUY PE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            Text("🔴 SELL CE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LossRed)
                            Text("🔴 SELL PE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LossRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        appPreferences?.saveAlertPreferences(
                            AlertPreferences(
                                brokerConnected = brokerConnected,
                                brokerDisconnected = brokerDisconnected,
                                buyCeSignal = buyCeSignal,
                                buyPeSignal = buyPeSignal,
                                entryPosition = entryPosition,
                                stopLossHit = stopLossHit,
                                target1Hit = target1Hit,
                                target2Hit = target2Hit,
                                target3Hit = target3Hit,
                                target4Hit = target4Hit,
                                trailingSlHit = trailingSlHit,
                                orderExecuted = orderExecuted,
                                orderRejected = orderRejected,
                                algoStarted = algoStarted,
                                algoStopped = algoStopped,
                                riskLimitReached = riskLimitReached
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("SAVE PREFERENCES", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AlertToggleRow(
    title: String,
    iconEmoji: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(iconEmoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextWhite)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = ProfitGreen,
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = DarkCardSecondary
            )
        )
    }
}
