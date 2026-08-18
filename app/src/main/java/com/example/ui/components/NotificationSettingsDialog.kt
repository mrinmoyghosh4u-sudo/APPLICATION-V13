package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import com.example.util.AppPreferences
import com.example.util.NotificationSettings

@Composable
fun NotificationSettingsDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
    onSave: (NotificationSettings) -> Unit
) {
    val current = remember { appPreferences.getNotificationSettings() }

    var notifyOrderUpdates by remember { mutableStateOf(current.notifyOrderUpdates) }
    var notifyPositionUpdates by remember { mutableStateOf(current.notifyPositionUpdates) }
    var notifyPnlAlerts by remember { mutableStateOf(current.notifyPnlAlerts) }
    var notifyRiskAlerts by remember { mutableStateOf(current.notifyRiskAlerts) }
    var notifyMarketStatus by remember { mutableStateOf(current.notifyMarketStatus) }
    var notifyBrokerAlerts by remember { mutableStateOf(current.notifyBrokerAlerts) }

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
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = SecondaryGold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Notification & Alert Channels", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Configure push, sound & in-app alerts", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                NotifToggleRow("Order Placement & Status Updates", "Alert when order is placed, modified or cancelled", notifyOrderUpdates) { notifyOrderUpdates = it }
                NotifToggleRow("Position & Square-off Alerts", "Alert when position is closed or modified", notifyPositionUpdates) { notifyPositionUpdates = it }
                NotifToggleRow("Target & Stop-Loss P&L Alerts", "Alert when trade reaches profit or loss milestones", notifyPnlAlerts) { notifyPnlAlerts = it }
                NotifToggleRow("Risk Management & Limit Alerts", "Alert when approaching daily loss or order limits", notifyRiskAlerts) { notifyRiskAlerts = it }
                NotifToggleRow("Exchange & Market Open/Close", "Notifications for market open, close and holiday events", notifyMarketStatus) { notifyMarketStatus = it }
                NotifToggleRow("Broker API Connection Health", "Alerts for Dhan / Angel One token expiry or errors", notifyBrokerAlerts) { notifyBrokerAlerts = it }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextGray)
                    ) {
                        Text("CANCEL", color = TextWhite, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val newSettings = NotificationSettings(
                                notifyOrderUpdates = notifyOrderUpdates,
                                notifyPositionUpdates = notifyPositionUpdates,
                                notifyPnlAlerts = notifyPnlAlerts,
                                notifyRiskAlerts = notifyRiskAlerts,
                                notifyMarketStatus = notifyMarketStatus,
                                notifyBrokerAlerts = notifyBrokerAlerts
                            )
                            appPreferences.saveNotificationSettings(newSettings)
                            onSave(newSettings)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("SAVE NOTIFS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(subtitle, fontSize = 10.sp, color = TextGray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
        )
    }
}
