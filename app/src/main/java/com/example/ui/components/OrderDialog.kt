package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun OrderDialog(
    symbol: String,
    initialSide: String,
    initialPrice: Double? = null,
    lotSize: Int? = null,
    appPreferences: com.example.util.AppPreferences? = null,
    onDismiss: () -> Unit,
    onConfirmOrder: (side: String, orderType: String, qty: Int, price: Double) -> Unit
) {
    val instPref = remember(symbol) {
        appPreferences?.getInstrumentPreference(symbol) ?: com.example.util.InstrumentOrderPreference()
    }

    var side by remember { mutableStateOf(initialSide) }
    var orderType by remember { mutableStateOf(instPref.defaultOrderType) }

    val baseLot = lotSize ?: com.example.util.AppPreferences.getGlobalLotSize(symbol)
    val defaultLot = baseLot * instPref.defaultLots * instPref.lotMultiplier

    val defaultPrice = initialPrice ?: 0.0

    var qtyText by remember { mutableStateOf(defaultLot.toString()) }
    var priceText by remember { mutableStateOf(String.format("%.2f", defaultPrice)) }
    var triggerPriceText by remember { mutableStateOf(String.format("%.2f", defaultPrice * 0.98)) }

    val isMarketOpen = remember { com.example.util.MarketStatusUtil.getDetailedMarketStatus("NSE").isOpen }

    var isSubmitting by remember { mutableStateOf(false) }

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
                Column {
                    Text(symbol, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("NSE • Lot Size: $defaultLot", fontSize = 11.sp, color = TextGray)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                }
            }
        },
        text = {
            Column {
                if (!isMarketOpen) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        color = LossRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LossRed)
                    ) {
                        Text(
                            text = "Market Closed • Live order placement disabled until market opens.",
                            fontSize = 11.sp,
                            color = LossRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else if (defaultPrice <= 0.0 && orderType == "MARKET") {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        color = SecondaryGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold)
                    ) {
                        Text(
                            text = "Real Price Unavailable • Cannot execute MARKET order without verified market feed.",
                            fontSize = 11.sp,
                            color = SecondaryGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Side Selector (BUY / SELL)
                val isOption = symbol.contains("CE") || symbol.contains("PE")
                val availableSides = if (isOption) listOf("BUY") else listOf("BUY", "SELL")
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardSecondary, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    availableSides.forEach { s ->
                        val isSelected = side == s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(
                                    color = if (isSelected) (if (s == "BUY") ProfitGreen else LossRed) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { side = s },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = s,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Order Type Selector (MARKET, LIMIT, SL, SL-M)
                Text("Order Type", fontSize = 10.sp, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("MARKET", "LIMIT", "SL", "SL-M").forEach { ot ->
                        val isSelected = orderType == ot
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { orderType = ot },
                            color = if (isSelected) PrimaryGold else DarkCardSecondary,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryGold else DarkCardBorder)
                        ) {
                            Text(
                                text = ot,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextWhite,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity Input
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quantity", color = SecondaryGold, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkCardSecondary,
                        unfocusedContainerColor = DarkCardSecondary,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Price Input (if LIMIT or SL)
                if (orderType == "LIMIT" || orderType == "SL") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Limit Price (₹)", color = SecondaryGold, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkCardSecondary,
                            unfocusedContainerColor = DarkCardSecondary,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                // Trigger Price Input (if SL or SL-M)
                if (orderType == "SL" || orderType == "SL-M") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = triggerPriceText,
                        onValueChange = { triggerPriceText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Trigger Price (₹)", color = SecondaryGold, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkCardSecondary,
                            unfocusedContainerColor = DarkCardSecondary,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val qty = qtyText.toIntOrNull() ?: defaultLot
                val price = if (orderType == "MARKET") defaultPrice else (priceText.toDoubleOrNull() ?: defaultPrice)
                val totalVal = qty * price

                val slDiff = if (instPref.stopLossType == "PERCENT") (price * instPref.stopLossValue / 100.0) else instPref.stopLossValue
                val slVal = if (side == "BUY") (price - slDiff).coerceAtLeast(0.0) else (price + slDiff)

                val t1Diff = if (instPref.stopLossType == "PERCENT") (price * instPref.target1 / 100.0) else instPref.target1
                val t2Diff = if (instPref.stopLossType == "PERCENT") (price * instPref.target2 / 100.0) else instPref.target2
                val t3Diff = if (instPref.stopLossType == "PERCENT") (price * instPref.target3 / 100.0) else instPref.target3

                val t1Val = if (side == "BUY") (price + t1Diff) else (price - t1Diff).coerceAtLeast(0.0)
                val t2Val = if (side == "BUY") (price + t2Diff) else (price - t2Diff).coerceAtLeast(0.0)
                val t3Val = if (side == "BUY") (price + t3Diff) else (price - t3Diff).coerceAtLeast(0.0)

                // AI Trade Parameters Breakdown
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤖 AI TRADE CONFIRMATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                            Text("R:R  1 : 2.5", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entry Price:", fontSize = 10.sp, color = TextGray)
                            Text(String.format("₹%.2f", price), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Stop Loss:", fontSize = 10.sp, color = TextGray)
                            Text(String.format("₹%.2f", slVal), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LossRed)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Target 1 | 2 | 3:", fontSize = 10.sp, color = TextGray)
                            Text(
                                String.format("₹%.2f | ₹%.2f | ₹%.2f", t1Val, t2Val, t3Val),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Margin Required:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                            Text(String.format("₹%,.2f", totalVal), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextGray)
                ) {
                    Text("Cancel", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (!isSubmitting) {
                            isSubmitting = true
                            val qty = qtyText.toIntOrNull() ?: defaultLot
                            val price = if (orderType == "MARKET") defaultPrice else (priceText.toDoubleOrNull() ?: defaultPrice)
                            onConfirmOrder(side, orderType, qty, price)
                        }
                    },
                    enabled = !isSubmitting && (orderType != "MARKET" || defaultPrice > 0.0),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (side == "BUY") ProfitGreen else LossRed
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Placing...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Confirm Trade",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        dismissButton = null
    )
}

