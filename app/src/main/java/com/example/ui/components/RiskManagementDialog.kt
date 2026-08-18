package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.util.AppPreferences
import com.example.util.RiskSettings

@Composable
fun RiskManagementDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
    onSave: (RiskSettings) -> Unit
) {
    val currentSettings = remember { appPreferences.getRiskSettings() }

    var maxDailyLossStr by remember { mutableStateOf(currentSettings.maxDailyLoss.toInt().toString()) }
    var maxDailyTradesStr by remember { mutableStateOf(currentSettings.maxDailyTrades.toString()) }
    var maxPositionSizeStr by remember { mutableStateOf(currentSettings.maxPositionSize.toString()) }
    var maxOrderValueStr by remember { mutableStateOf(currentSettings.maxOrderValue.toInt().toString()) }
    var stopLossRequired by remember { mutableStateOf(currentSettings.stopLossRequired) }
    var riskPerTradeStr by remember { mutableStateOf(currentSettings.riskPerTradePercent.toString()) }
    var dailyLossWarningEnabled by remember { mutableStateOf(currentSettings.dailyLossWarningEnabled) }
    var enforceDailyLoss by remember { mutableStateOf(currentSettings.enforceDailyLoss) }
    var enforceMaxTrades by remember { mutableStateOf(currentSettings.enforceMaxTrades) }
    var enforceMaxOrderValue by remember { mutableStateOf(currentSettings.enforceMaxOrderValue) }

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
                            .background(LossRedBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = LossRed)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Risk Management & Safety Rules", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Protect Capital • Automatic Loss Enforcer", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Max Daily Loss
                RiskInputRow(
                    label = "Max Daily Loss Limit (₹)",
                    value = maxDailyLossStr,
                    onValueChange = { maxDailyLossStr = it },
                    keyboardType = KeyboardType.Number,
                    description = "Blocks new trade entries when today's loss hits this threshold"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enforce Daily Loss Block", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = enforceDailyLoss,
                        onCheckedChange = { enforceDailyLoss = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Max Daily Trades
                RiskInputRow(
                    label = "Max Daily Orders Count",
                    value = maxDailyTradesStr,
                    onValueChange = { maxDailyTradesStr = it },
                    keyboardType = KeyboardType.Number,
                    description = "Prevents over-trading after reaching trade limit"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enforce Max Order Count", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = enforceMaxTrades,
                        onCheckedChange = { enforceMaxTrades = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Max Order Value
                RiskInputRow(
                    label = "Max Order Value (₹)",
                    value = maxOrderValueStr,
                    onValueChange = { maxOrderValueStr = it },
                    keyboardType = KeyboardType.Number,
                    description = "Maximum permissible capital per single order"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enforce Max Order Value", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = enforceMaxOrderValue,
                        onCheckedChange = { enforceMaxOrderValue = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Max Position Size
                RiskInputRow(
                    label = "Max Position Qty / Lots",
                    value = maxPositionSizeStr,
                    onValueChange = { maxPositionSizeStr = it },
                    keyboardType = KeyboardType.Number,
                    description = "Cap total open quantity across contracts"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stop loss required
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mandatory Stop-Loss", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Require Stop-Loss price on every order placement", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = stopLossRequired,
                        onCheckedChange = { stopLossRequired = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Loss Warning Banner", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Show warning card when today's loss > 50% of limit", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = dailyLossWarningEnabled,
                        onCheckedChange = { dailyLossWarningEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Warning callout about Square Off
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Safety Rule: Risk enforcement blocks NEW position entries, but NEVER blocks exit or square-off orders.",
                            fontSize = 10.sp,
                            color = TextWhite,
                            lineHeight = 14.sp
                        )
                    }
                }

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
                            val newSettings = RiskSettings(
                                maxDailyLoss = maxDailyLossStr.toDoubleOrNull() ?: 10000.0,
                                maxDailyTrades = maxDailyTradesStr.toIntOrNull() ?: 20,
                                maxPositionSize = maxPositionSizeStr.toIntOrNull() ?: 1000,
                                maxOrderValue = maxOrderValueStr.toDoubleOrNull() ?: 200000.0,
                                stopLossRequired = stopLossRequired,
                                riskPerTradePercent = riskPerTradeStr.toDoubleOrNull() ?: 2.0,
                                dailyLossWarningEnabled = dailyLossWarningEnabled,
                                enforceDailyLoss = enforceDailyLoss,
                                enforceMaxTrades = enforceMaxTrades,
                                enforceMaxOrderValue = enforceMaxOrderValue
                            )
                            appPreferences.saveRiskSettings(newSettings)
                            onSave(newSettings)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("SAVE RULES", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    description: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                modifier = Modifier.width(130.dp).height(48.dp)
            )
        }
        Text(description, fontSize = 9.sp, color = TextGray)
    }
}
