package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
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

import com.example.util.AiSignalSettings
import com.example.util.AppPreferences

@Composable
fun AiSignalSettingsDialog(
    appPreferences: AppPreferences? = null,
    onDismiss: () -> Unit
) {
    val initialSettings = remember { appPreferences?.getAiSignalSettings() ?: AiSignalSettings() }

    var selectedIndex by remember { mutableStateOf(initialSettings.selectedIndex) }
    var selectedTimeframe by remember { mutableStateOf(initialSettings.selectedTimeframe) }

    var emaEnabled by remember { mutableStateOf(initialSettings.emaEnabled) }
    var vwapEnabled by remember { mutableStateOf(initialSettings.vwapEnabled) }
    var rsiEnabled by remember { mutableStateOf(initialSettings.rsiEnabled) }
    var supertrendEnabled by remember { mutableStateOf(initialSettings.supertrendEnabled) }
    var oiEnabled by remember { mutableStateOf(initialSettings.oiEnabled) }
    var volumeEnabled by remember { mutableStateOf(initialSettings.volumeEnabled) }

    var confidenceThreshold by remember { mutableFloatStateOf(initialSettings.confidenceThreshold) }

    val indices = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX")
    val timeframes = listOf("1 MIN", "3 MIN", "5 MIN", "15 MIN")

    var expandedIndexDropdown by remember { mutableStateOf(false) }
    var expandedTfDropdown by remember { mutableStateOf(false) }

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
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryGold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("AI SIGNAL SETTINGS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Configure Indicators & Model Confidence", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Select Index
                Text("Select Index", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedIndexDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedIndex, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("▼", fontSize = 10.sp, color = SecondaryGold)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedIndexDropdown,
                        onDismissRequest = { expandedIndexDropdown = false },
                        modifier = Modifier.background(DarkCard)
                    ) {
                        indices.forEach { idx ->
                            DropdownMenuItem(
                                text = { Text(idx, color = TextWhite) },
                                onClick = {
                                    selectedIndex = idx
                                    expandedIndexDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Timeframe
                Text("Timeframe", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedTfDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedTimeframe, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("▼", fontSize = 10.sp, color = SecondaryGold)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedTfDropdown,
                        onDismissRequest = { expandedTfDropdown = false },
                        modifier = Modifier.background(DarkCard)
                    ) {
                        timeframes.forEach { tf ->
                            DropdownMenuItem(
                                text = { Text(tf, color = TextWhite) },
                                onClick = {
                                    selectedTimeframe = tf
                                    expandedTfDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("AI Confirmation Indicators", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                Spacer(modifier = Modifier.height(8.dp))

                IndicatorToggleRow("EMA", "📈", emaEnabled) { emaEnabled = it }
                IndicatorToggleRow("VWAP", "📊", vwapEnabled) { vwapEnabled = it }
                IndicatorToggleRow("RSI", "📉", rsiEnabled) { rsiEnabled = it }
                IndicatorToggleRow("Supertrend", "⚡", supertrendEnabled) { supertrendEnabled = it }
                IndicatorToggleRow("OI (Open Interest)", "🎯", oiEnabled) { oiEnabled = it }
                IndicatorToggleRow("Volume", "📦", volumeEnabled) { volumeEnabled = it }

                Spacer(modifier = Modifier.height(16.dp))

                // Confidence Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confidence Threshold", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("${(confidenceThreshold * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                }

                Slider(
                    value = confidenceThreshold,
                    onValueChange = { confidenceThreshold = it },
                    valueRange = 0.50f..0.95f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryGold,
                        activeTrackColor = PrimaryGold,
                        inactiveTrackColor = DarkCardSecondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Notice Box
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
                        Icon(Icons.Default.Info, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "If confidence is below threshold, AI will not generate any signals (NO TRADE).",
                            fontSize = 10.sp,
                            color = TextWhite,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        appPreferences?.saveAiSignalSettings(
                            AiSignalSettings(
                                selectedIndex = selectedIndex,
                                selectedTimeframe = selectedTimeframe,
                                emaEnabled = emaEnabled,
                                vwapEnabled = vwapEnabled,
                                rsiEnabled = rsiEnabled,
                                supertrendEnabled = supertrendEnabled,
                                oiEnabled = oiEnabled,
                                volumeEnabled = volumeEnabled,
                                confidenceThreshold = confidenceThreshold
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("SAVE SETTINGS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun IndicatorToggleRow(
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
