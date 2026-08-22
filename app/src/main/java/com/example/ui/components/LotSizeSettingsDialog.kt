package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import com.example.util.LotSizeSettings

@Composable
fun LotSizeSettingsDialog(
    appPreferences: AppPreferences? = null,
    onDismiss: () -> Unit,
    onSave: () -> Unit = {}
) {
    val initialSettings = remember { appPreferences?.getLotSizeSettings() ?: LotSizeSettings() }

    var niftyStr by remember { mutableStateOf(initialSettings.nifty.toString()) }
    var bankniftyStr by remember { mutableStateOf(initialSettings.banknifty.toString()) }
    var finniftyStr by remember { mutableStateOf(initialSettings.finnifty.toString()) }
    var midcpniftyStr by remember { mutableStateOf(initialSettings.midcpnifty.toString()) }

    var sensexStr by remember { mutableStateOf(initialSettings.sensex.toString()) }
    var bankexStr by remember { mutableStateOf(initialSettings.bankex.toString()) }

    var crudeoilStr by remember { mutableStateOf(initialSettings.crudeoil.toString()) }
    var crudeoilmStr by remember { mutableStateOf(initialSettings.crudeoilm.toString()) }
    var goldStr by remember { mutableStateOf(initialSettings.gold.toString()) }
    var goldmStr by remember { mutableStateOf(initialSettings.goldm.toString()) }
    var silverStr by remember { mutableStateOf(initialSettings.silver.toString()) }
    var silvermStr by remember { mutableStateOf(initialSettings.silverm.toString()) }
    var copperStr by remember { mutableStateOf(initialSettings.copper.toString()) }
    var coppermStr by remember { mutableStateOf(initialSettings.copperm.toString()) }
    var naturalgasStr by remember { mutableStateOf(initialSettings.naturalgas.toString()) }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkCardSecondary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = SecondaryGold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Lot Size Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("Exchange & Symbol Default Lot Quantities", fontSize = 11.sp, color = SecondaryGold)
                        }
                    }

                    TextButton(
                        onClick = {
                            niftyStr = "65"
                            bankniftyStr = "30"
                            finniftyStr = "60"
                            midcpniftyStr = "120"
                            sensexStr = "20"
                            bankexStr = "30"
                            crudeoilStr = "100"
                            crudeoilmStr = "10"
                            goldStr = "100"
                            goldmStr = "10"
                            silverStr = "30"
                            silvermStr = "5"
                            copperStr = "2500"
                            coppermStr = "250"
                            naturalgasStr = "1250"
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = SecondaryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: NSE
                ExchangeHeader(title = "NSE", badgeColor = ProfitGreen)
                Spacer(modifier = Modifier.height(8.dp))

                LotSizeCard(symbol = "NIFTY", lotSizeStr = niftyStr, defaultVal = "65") { niftyStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "BANKNIFTY", lotSizeStr = bankniftyStr, defaultVal = "30") { bankniftyStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "FINNIFTY", lotSizeStr = finniftyStr, defaultVal = "60") { finniftyStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "MIDCPNIFTY", lotSizeStr = midcpniftyStr, defaultVal = "120") { midcpniftyStr = it }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: BSE
                ExchangeHeader(title = "BSE", badgeColor = PrimaryGold)
                Spacer(modifier = Modifier.height(8.dp))

                LotSizeCard(symbol = "SENSEX", lotSizeStr = sensexStr, defaultVal = "20") { sensexStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "BANKEX", lotSizeStr = bankexStr, defaultVal = "30") { bankexStr = it }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: MCX
                ExchangeHeader(title = "MCX COMMODITY", badgeColor = SecondaryGold)
                Spacer(modifier = Modifier.height(8.dp))

                LotSizeCard(symbol = "CRUDEOIL", lotSizeStr = crudeoilStr, defaultVal = "100") { crudeoilStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "CRUDEOILM (Mini)", lotSizeStr = crudeoilmStr, defaultVal = "10") { crudeoilmStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "GOLD", lotSizeStr = goldStr, defaultVal = "100") { goldStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "GOLDM (Mini)", lotSizeStr = goldmStr, defaultVal = "10") { goldmStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "SILVER", lotSizeStr = silverStr, defaultVal = "30") { silverStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "SILVERM (Mini)", lotSizeStr = silvermStr, defaultVal = "5") { silvermStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "NATURALGAS", lotSizeStr = naturalgasStr, defaultVal = "1250") { naturalgasStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "COPPER", lotSizeStr = copperStr, defaultVal = "2500") { copperStr = it }
                Spacer(modifier = Modifier.height(6.dp))

                LotSizeCard(symbol = "COPPERM (Mini)", lotSizeStr = coppermStr, defaultVal = "250") { coppermStr = it }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Note
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
                            "These lot sizes are used consistently across Order Placement & Option Chain views.",
                            fontSize = 11.sp,
                            color = TextWhite,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextGray)
                    ) {
                        Text("CANCEL", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val newSettings = LotSizeSettings(
                                nifty = niftyStr.toIntOrNull() ?: 65,
                                banknifty = bankniftyStr.toIntOrNull() ?: 30,
                                finnifty = finniftyStr.toIntOrNull() ?: 60,
                                midcpnifty = midcpniftyStr.toIntOrNull() ?: 120,
                                sensex = sensexStr.toIntOrNull() ?: 20,
                                bankex = bankexStr.toIntOrNull() ?: 30,
                                crudeoil = crudeoilStr.toIntOrNull() ?: 100,
                                crudeoilm = crudeoilmStr.toIntOrNull() ?: 10,
                                gold = goldStr.toIntOrNull() ?: 100,
                                goldm = goldmStr.toIntOrNull() ?: 10,
                                silver = silverStr.toIntOrNull() ?: 30,
                                silverm = silvermStr.toIntOrNull() ?: 5,
                                copper = copperStr.toIntOrNull() ?: 2500,
                                copperm = coppermStr.toIntOrNull() ?: 250,
                                naturalgas = naturalgasStr.toIntOrNull() ?: 1250
                            )
                            appPreferences?.saveLotSizeSettings(newSettings)
                            onSave()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("SAVE SETTINGS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExchangeHeader(
    title: String,
    badgeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .border(1.dp, badgeColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = badgeColor)
        }
        Text("EXCHANGE SYMBOLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray)
    }
}

@Composable
private fun LotSizeCard(
    symbol: String,
    lotSizeStr: String,
    defaultVal: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkCardSecondary,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(symbol, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                Text("Default Lot: $defaultVal", fontSize = 10.sp, color = TextGray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = lotSizeStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            onValueChange(input)
                        }
                    },
                    modifier = Modifier
                        .width(90.dp)
                        .height(48.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold
                    )
                )
            }
        }
    }
}
