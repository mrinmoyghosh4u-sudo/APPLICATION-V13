package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.util.AppPreferences
import com.example.util.InstrumentOrderPreference
import com.example.util.OrderPreferences

@Composable
fun OrderPreferencesDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
    onSave: (OrderPreferences) -> Unit
) {
    val context = LocalContext.current
    val currentGlobal = remember { appPreferences.getOrderPreferences() }

    var selectedExchange by remember { mutableStateOf("NSE") }
    var selectedInstrument by remember { mutableStateOf("NIFTY") }

    var confirmBeforeOrder by remember { mutableStateOf(currentGlobal.confirmBeforeOrder) }
    var confirmBeforeSquareOff by remember { mutableStateOf(currentGlobal.confirmBeforeSquareOff) }
    var confirmBeforeModify by remember { mutableStateOf(currentGlobal.confirmBeforeModify) }
    var confirmBeforeCancel by remember { mutableStateOf(currentGlobal.confirmBeforeCancel) }
    var duplicateOrderProtection by remember { mutableStateOf(currentGlobal.duplicateOrderProtection) }
    var orderRetryProtection by remember { mutableStateOf(currentGlobal.orderRetryProtection) }
    var applyToAllInstruments by remember { mutableStateOf(currentGlobal.applyToAllInstruments) }

    val instrumentMap = remember {
        mutableStateMapOf<String, InstrumentOrderPreference>().apply {
            appPreferences.supportedInstrumentsList.forEach { (_, instr) ->
                put(instr, appPreferences.getInstrumentPreference(instr))
            }
        }
    }

    val currentInstPref = instrumentMap[selectedInstrument] ?: InstrumentOrderPreference(
        exchange = selectedExchange,
        instrument = selectedInstrument
    )

    fun updateCurrentInst(updater: (InstrumentOrderPreference) -> InstrumentOrderPreference) {
        val updated = updater(currentInstPref)
        instrumentMap[selectedInstrument] = updated
    }

    val orderTypes = listOf("LIMIT", "MARKET", "SL", "SL-M")
    val productTypes = if (selectedExchange == "MCX") {
        listOf("INTRADAY", "MARGIN", "CARRY FORWARD")
    } else {
        listOf("INTRADAY", "MARGIN", "CNC", "CARRY FORWARD")
    }

    val currentLotSize = appPreferences.getLotSizeForSymbol(selectedInstrument)

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
                        Icon(Icons.Default.Settings, contentDescription = null, tint = SecondaryGold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Order Execution Preferences", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Configure default order settings per instrument", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // 1. Exchange Selector: [NSE] [BSE] [MCX]
                Text("Select Exchange", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("NSE", "BSE", "MCX").forEach { exch ->
                        val isSel = selectedExchange == exch
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .background(
                                    if (isSel) PrimaryGold else DarkCardSecondary,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    selectedExchange = exch
                                    selectedInstrument = when (exch) {
                                        "BSE" -> "SENSEX"
                                        "MCX" -> "CRUDEOIL"
                                        else -> "NIFTY"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(exch, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (isSel) Color.Black else TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Instrument Selector
                val availableInstruments = when (selectedExchange) {
                    "BSE" -> listOf("SENSEX", "BANKEX")
                    "MCX" -> listOf("CRUDEOIL", "CRUDEOILM")
                    else -> listOf("NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY")
                }

                Text("Select Instrument", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableInstruments.forEach { inst ->
                        val isSel = selectedInstrument == inst
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .background(
                                    if (isSel) SecondaryGold else DarkCardSecondary,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(1.dp, if (isSel) PrimaryGold else DarkCardBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedInstrument = inst },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(inst, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Selected Instrument Title Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$selectedExchange • $selectedInstrument", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                    Text("Base Lot Size: $currentLotSize", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Default Order Type
                Text("Default Order Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    orderTypes.forEach { type ->
                        val isSelected = currentInstPref.defaultOrderType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(
                                    if (isSelected) PrimaryGold else DarkCardSecondary,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { updateCurrentInst { it.copy(defaultOrderType = type) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Default Product Type
                Text("Default Product Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    productTypes.forEach { prod ->
                        val isSelected = currentInstPref.defaultProductType == prod
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(
                                    if (isSelected) PrimaryGold else DarkCardSecondary,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { updateCurrentInst { it.copy(defaultProductType = prod) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(prod, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else TextWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Quantity / Lot Size Stepper
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Lot & Quantity Calculator", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Default Lots:", fontSize = 11.sp, color = TextGray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentInstPref.defaultLots > 1) {
                                            updateCurrentInst { it.copy(defaultLots = it.defaultLots - 1) }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = SecondaryGold)
                                }
                                Text("${currentInstPref.defaultLots}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(
                                    onClick = {
                                        updateCurrentInst { it.copy(defaultLots = it.defaultLots + 1) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = SecondaryGold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lot Multiplier:", fontSize = 11.sp, color = TextGray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentInstPref.lotMultiplier > 1) {
                                            updateCurrentInst { it.copy(lotMultiplier = it.lotMultiplier - 1) }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Multiplier", tint = SecondaryGold)
                                }
                                Text("${currentInstPref.lotMultiplier}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(
                                    onClick = {
                                        updateCurrentInst { it.copy(lotMultiplier = it.lotMultiplier + 1) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Multiplier", tint = SecondaryGold)
                                }
                            }
                        }

                        HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                        val finalQty = currentLotSize * currentInstPref.defaultLots * currentInstPref.lotMultiplier
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Calculated Quantity:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                            Text("$currentLotSize × ${currentInstPref.defaultLots} × ${currentInstPref.lotMultiplier} = $finalQty Qty", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ProfitGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Stop Loss Preferences
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
                            Text("Stop Loss Preference", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("PERCENT", "POINTS").forEach { mode ->
                                    val isSel = currentInstPref.stopLossType == mode
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSel) PrimaryGold else DarkCard, RoundedCornerShape(4.dp))
                                            .clickable { updateCurrentInst { it.copy(stopLossType = mode) } }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (mode == "PERCENT") "%" else "Pts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextWhite)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stop Loss Value (${if (currentInstPref.stopLossType == "PERCENT") "%" else "Pts"}):", fontSize = 11.sp, color = TextGray)
                            OutlinedTextField(
                                value = currentInstPref.stopLossValue.toString(),
                                onValueChange = { str ->
                                    val db = str.toDoubleOrNull() ?: 0.0
                                    updateCurrentInst { it.copy(stopLossValue = db) }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.width(100.dp).height(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto SL Calculation", fontSize = 11.sp, color = TextGray)
                            Switch(
                                checked = currentInstPref.autoSlEnabled,
                                onCheckedChange = { chk -> updateCurrentInst { it.copy(autoSlEnabled = chk) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Target Preferences (Target 1, Target 2, Target 3, Target 4)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Target Preferences (Points / %)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("T1", fontSize = 10.sp, color = TextGray)
                                OutlinedTextField(
                                    value = currentInstPref.target1.toString(),
                                    onValueChange = { updateCurrentInst { it.copy(target1 = it.target1) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, unfocusedBorderColor = DarkCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("T2", fontSize = 10.sp, color = TextGray)
                                OutlinedTextField(
                                    value = currentInstPref.target2.toString(),
                                    onValueChange = { updateCurrentInst { it.copy(target2 = it.target2) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, unfocusedBorderColor = DarkCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("T3", fontSize = 10.sp, color = TextGray)
                                OutlinedTextField(
                                    value = currentInstPref.target3.toString(),
                                    onValueChange = { updateCurrentInst { it.copy(target3 = it.target3) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, unfocusedBorderColor = DarkCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("T4", fontSize = 10.sp, color = TextGray)
                                OutlinedTextField(
                                    value = currentInstPref.target4.toString(),
                                    onValueChange = { updateCurrentInst { it.copy(target4 = it.target4) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, unfocusedBorderColor = DarkCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 8. Trailing Stop Loss
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
                            Text("Trailing Stop Loss", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Switch(
                                checked = currentInstPref.trailingSlEnabled,
                                onCheckedChange = { chk -> updateCurrentInst { it.copy(trailingSlEnabled = chk) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                            )
                        }

                        if (currentInstPref.trailingSlEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("POINTS", "PERCENT").forEach { mode ->
                                        val isSel = currentInstPref.trailingSlType == mode
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSel) PrimaryGold else DarkCard, RoundedCornerShape(4.dp))
                                                .clickable { updateCurrentInst { it.copy(trailingSlType = mode) } }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(if (mode == "PERCENT") "%" else "Pts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else TextWhite)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = currentInstPref.trailingSlValue.toString(),
                                    onValueChange = { str ->
                                        val db = str.toDoubleOrNull() ?: 0.0
                                        updateCurrentInst { it.copy(trailingSlValue = db) }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGold, unfocusedBorderColor = DarkCardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                                    modifier = Modifier.width(100.dp).height(44.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // 9. Confirmations Header
                Text("ORDER SAFETY & CONFIRMATIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Confirm Before Placing Order", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Show prompt prior to sending order to broker", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = confirmBeforeOrder,
                        onCheckedChange = { confirmBeforeOrder = it },
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
                        Text("Confirm Before Square-Off / Exit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Show prompt before closing position", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = confirmBeforeSquareOff,
                        onCheckedChange = { confirmBeforeSquareOff = it },
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
                        Text("Confirm Before Modify", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Confirm before price/qty updates", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = confirmBeforeModify,
                        onCheckedChange = { confirmBeforeModify = it },
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
                        Text("Confirm Before Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Confirm before cancelling open order", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = confirmBeforeCancel,
                        onCheckedChange = { confirmBeforeCancel = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Advanced Protection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Duplicate Order Protection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Block identical rapid order submission", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = duplicateOrderProtection,
                        onCheckedChange = { duplicateOrderProtection = it },
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
                        Text("Order Retry Protection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Verify broker order status on network timeout", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = orderRetryProtection,
                        onCheckedChange = { orderRetryProtection = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 10. Apply to All Instruments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Apply to All Instruments", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                        Text("Copy current settings to all NSE, BSE & MCX symbols", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = applyToAllInstruments,
                        onCheckedChange = { chk ->
                            applyToAllInstruments = chk
                            if (chk) {
                                val template = currentInstPref
                                appPreferences.supportedInstrumentsList.forEach { (ex, inst) ->
                                    instrumentMap[inst] = template.copy(exchange = ex, instrument = inst)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: RESET & SAVE PREFERENCES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val defaultPref = InstrumentOrderPreference(exchange = selectedExchange, instrument = selectedInstrument)
                            instrumentMap[selectedInstrument] = defaultPref
                            Toast.makeText(context, "Reset $selectedInstrument preferences", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextGray)
                    ) {
                        Text("RESET", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            // Validation
                            val isValid = instrumentMap.values.all { p ->
                                p.defaultLots >= 1 && p.lotMultiplier >= 1 && p.stopLossValue >= 0.0
                            }

                            if (!isValid) {
                                Toast.makeText(context, "Invalid preference. Please check the selected instrument.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            val finalMap = if (applyToAllInstruments) {
                                val template = currentInstPref
                                appPreferences.supportedInstrumentsList.associate { (ex, inst) ->
                                    inst to template.copy(exchange = ex, instrument = inst)
                                }
                            } else {
                                instrumentMap.toMap()
                            }

                            val newGlobal = OrderPreferences(
                                confirmBeforeOrder = confirmBeforeOrder,
                                confirmBeforeSquareOff = confirmBeforeSquareOff,
                                confirmBeforeModify = confirmBeforeModify,
                                confirmBeforeCancel = confirmBeforeCancel,
                                duplicateOrderProtection = duplicateOrderProtection,
                                orderRetryProtection = orderRetryProtection,
                                applyToAllInstruments = applyToAllInstruments,
                                instrumentPreferences = finalMap,
                                defaultOrderType = currentInstPref.defaultOrderType,
                                defaultQuantity = currentLotSize * currentInstPref.defaultLots * currentInstPref.lotMultiplier,
                                defaultProductType = currentInstPref.defaultProductType,
                                defaultStopLossPercent = currentInstPref.stopLossValue
                            )

                            appPreferences.saveOrderPreferences(newGlobal)
                            onSave(newGlobal)
                            Toast.makeText(context, "Preferences saved successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("SAVE PREFERENCES", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
