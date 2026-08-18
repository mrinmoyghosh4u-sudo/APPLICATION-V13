package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.formatRelativeTimestamp
import com.example.ui.theme.*

@Composable
fun NotificationCenterDialog(
    userProfile: UserProfileEntity,
    notifications: List<NotificationEntity> = emptyList(),
    onClearAll: () -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val brokerName = userProfile.connectedBroker.ifEmpty { "Broker" }
    
    val displayNotifications = remember(notifications) {
        notifications.sortedByDescending { it.timestampMillis }
    }
    val unreadCount = remember(displayNotifications) {
        displayNotifications.count { !it.isRead }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notification Center", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ProfitGreen, RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("$unreadCount New", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${displayNotifications.size} Total Alerts", fontSize = 11.sp, color = TextGray)
                    Row {
                        if (unreadCount > 0) {
                            TextButton(onClick = onMarkAllRead, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text("MARK READ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                            }
                        }
                        TextButton(onClick = onClearAll, contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text("CLEAR ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (displayNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No new notifications.", fontSize = 12.sp, color = TextGray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        items(displayNotifications) { item ->
                            val timeText = formatRelativeTimestamp(item.timestampMillis)
                            Surface(
                                color = DarkCardSecondary,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = when (item.type) {
                                            "SUCCESS" -> Icons.Default.CheckCircle
                                            "ALERT" -> Icons.Default.Notifications
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = when (item.type) {
                                            "SUCCESS" -> ProfitGreen
                                            "ALERT" -> PrimaryGold
                                            else -> SecondaryGold
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                            Text(timeText, fontSize = 9.sp, color = TextGray)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(item.message, fontSize = 11.sp, color = TextGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
            }
        }
    )
}
