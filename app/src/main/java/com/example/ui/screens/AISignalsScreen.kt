package com.example.ui.screens

import java.util.Calendar
import java.util.TimeZone
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AISignalEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.CrownLogo
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.theme.*
import com.example.util.MarketStatusUtil

@Composable
fun AISignalsScreen(
    userProfile: UserProfileEntity = UserProfileEntity(),
    signals: List<AISignalEntity>,
    onOpenNotificationCenter: () -> Unit = {},
    onExecuteSignal: (AISignalEntity) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Market Session Status per Exchange in IST
    val nseStatus = remember { MarketStatusUtil.getDetailedMarketStatus("NSE") }
    val bseStatus = remember { MarketStatusUtil.getDetailedMarketStatus("BSE") }
    val mcxStatus = remember { MarketStatusUtil.getDetailedMarketStatus("MCX") }

    val isAnyMarketOpen = nseStatus.isOpen || bseStatus.isOpen || mcxStatus.isOpen

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {

        // Filter Signals
        val filteredSignals = remember(signals, selectedFilter) {
            when (selectedFilter) {
                "NSE" -> signals.filter { it.exchange.equals("NSE", ignoreCase = true) }
                "BSE" -> signals.filter { it.exchange.equals("BSE", ignoreCase = true) }
                "MCX" -> signals.filter { it.exchange.equals("MCX", ignoreCase = true) }
                else -> signals
            }
        }

        val isBrokerConnected = (userProfile.isAngelConnected || userProfile.isDhanConnected) && userProfile.connectedBroker.isNotBlank()
        val activeBrokerName = if (isBrokerConnected) userProfile.connectedBroker.uppercase() else "BROKER API"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownLogo(size = 32.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("KING KHAN AI TRADE", fontSize = 15.sp, fontWeight = FontWeight.Black, color = PrimaryGold)
                        Text("Trade Like a King 👑", fontSize = 10.sp, color = SecondaryGold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenNotificationCenter) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = SecondaryGold, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Screen Title & Live Broker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Option Signals", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("Quantitative Options Algo Trading • Real-time CE/PE", fontSize = 11.sp, color = TextGray)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha=0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isBrokerConnected) ProfitGreen else LossRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Live Broker - $activeBrokerName", fontSize = 9.sp, color = PrimaryGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Market Session Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(if (isAnyMarketOpen) ProfitGreenBg else LossRedBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isAnyMarketOpen) ProfitGreen else LossRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAnyMarketOpen) "MARKET OPEN" else "MARKET CLOSED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAnyMarketOpen) ProfitGreen else LossRed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Session (IST)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    Text(
                        text = if (isAnyMarketOpen) "NSE: Open • MCX: Open" else nseStatus.nextOpeningTimeText,
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips Row
            val nseCount = signals.count { it.exchange.equals("NSE", ignoreCase = true) }
            val bseCount = signals.count { it.exchange.equals("BSE", ignoreCase = true) }
            val mcxCount = signals.count { it.exchange.equals("MCX", ignoreCase = true) }

            val filterList = listOf(
                "ALL" to "ALL (${signals.size})",
                "NSE" to "NSE ($nseCount)",
                "BSE" to "BSE ($bseCount)",
                "MCX" to "MCX ($mcxCount)"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterList.forEach { (filterKey, label) ->
                    val isSelected = selectedFilter == filterKey
                    Surface(
                        modifier = Modifier
                            .clickable { selectedFilter = filterKey }
                            .weight(1f),
                        color = if (isSelected) PrimaryGold else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextGray,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Signals List or Market Closed State
            if (filteredSignals.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredSignals) { signal ->
                        AISignalCard(
                            signal = signal, 
                            activeBrokerName = activeBrokerName,
                            onExecute = { onExecuteSignal(signal) }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "AI Signals are for option buyers only. Trade responsibly. Verify levels before executing.",
                                fontSize = 10.sp,
                                color = TextGray
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.DarkGray.copy(alpha=0.3f), CircleShape)
                                    .border(1.dp, if (isAnyMarketOpen) PrimaryGold.copy(alpha = 0.5f) else LossRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAnyMarketOpen) Icons.Default.Search else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isAnyMarketOpen) PrimaryGold else LossRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isAnyMarketOpen) "No Signals for Selected Filter" else "No Live Option Signals Available",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (!isAnyMarketOpen) {
                                    "Indian Stock & Commodity Markets are currently CLOSED.\n\nLive AI quantitative option signals (CE/PE) generate automatically during exchange trading hours:\n• NSE / BSE: Mon-Fri 09:15 AM - 03:30 PM IST\n• MCX Commodities: Mon-Fri 09:00 AM - 11:30 PM IST"
                                } else {
                                    "No live AI option signals matching the selected filter currently exist. Live signals trigger automatically when pattern probability exceeds 80% confidence."
                                },
                                fontSize = 12.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )

                            if (!isAnyMarketOpen) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .background(LossRedBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, LossRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "MARKET CLOSED • Opens Mon 09:15 AM IST",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LossRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AISignalCard(
    signal: AISignalEntity,
    activeBrokerName: String,
    onExecute: () -> Unit
) {
    val isBullish = signal.trend.equals("BULLISH", ignoreCase = true)
    val color = if (isBullish) ProfitGreen else LossRed
    val lightBg = if (isBullish) ProfitGreenBg else LossRedBg
    val icon = if (isBullish) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    
    val underlyingLtp = signal.underlyingLtp
    val underlyingChange = signal.underlyingChange
    val underlyingChangePercent = if (underlyingLtp > 0) (underlyingChange / (underlyingLtp - underlyingChange)) * 100 else 0.0

    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha=0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // Top Section (Underlying & Action)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Underlying
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = signal.symbol.split(" ").firstOrNull() ?: "INDEX", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Black, 
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.DarkGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(signal.exchange, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = String.format("%,.2f", underlyingLtp),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format("%+.2f (%+.2f%%)", underlyingChange, underlyingChangePercent),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = color
                        )
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                }
                
                // Middle: Trend & Source
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isBullish) "Bullish" else "Bearish", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = color
                        )
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("LTP Source", fontSize = 10.sp, color = TextGray)
                    Text(activeBrokerName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Time", fontSize = 10.sp, color = TextGray)
                    Text(signal.timestamp.substringAfter(" "), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                
                // Right: Action Button
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha=0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(signal.timestamp.substringAfter(" "), fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp).width(110.dp)
                    ) {
                        Text(
                            text = if (isBullish) "BUY CE" else "BUY PE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isBullish) Color.Black else TextWhite
                        )
                        Icon(
                            imageVector = if (isBullish) Icons.Default.ArrowOutward else Icons.Default.SouthEast,
                            contentDescription = null,
                            tint = if (isBullish) Color.Black else TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Confidence", fontSize = 9.sp, color = TextGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val confLevel = when {
                            signal.confidence >= 80 -> "HIGH"
                            signal.confidence >= 60 -> "MEDIUM"
                            else -> "LOW"
                        }
                        Text(confLevel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(12.dp, 8.dp).background(color))
                            Box(modifier = Modifier.size(12.dp, 8.dp).background(if (signal.confidence >= 50) color else Color.DarkGray))
                            Box(modifier = Modifier.size(12.dp, 8.dp).background(if (signal.confidence >= 75) color else Color.DarkGray))
                            Box(modifier = Modifier.size(12.dp, 8.dp).background(if (signal.confidence >= 90) color else Color.DarkGray))
                        }
                    }
                }
            }
            
            Divider(color = Color.DarkGray.copy(alpha=0.5f), thickness = 1.dp)
            
            // Middle Section (Grid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SignalMetric(Modifier.weight(1f), "Strike Price", signal.strikePrice.ifBlank { signal.symbol.substringAfter(" ") }, PrimaryGold)
                    SignalMetric(Modifier.weight(1f), "Expiry", signal.expiry.ifBlank { "Weekly" })
                    SignalMetric(Modifier.weight(1f), "Entry Price", "₹ ${String.format("%.2f", signal.ltp)}")
                    SignalMetric(Modifier.weight(1f), "Stop Loss", "₹ ${String.format("%.2f", signal.stopLoss)}")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SignalMetric(Modifier.weight(1f), "Target 1", "₹ ${String.format("%.2f", signal.target1)}")
                    SignalMetric(Modifier.weight(1f), "Target 2", "₹ ${String.format("%.2f", signal.target2)}")
                    SignalMetric(Modifier.weight(1f), "Target 3", if (signal.target3 > 0) "₹ ${String.format("%.2f", signal.target3)}" else "--")
                    SignalMetric(Modifier.weight(1f), "Target 4", if (signal.target4 > 0) "₹ ${String.format("%.2f", signal.target4)}" else "--")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SignalMetric(Modifier.weight(1f), "Trailing SL", if (signal.trailingSl > 0) "₹ ${String.format("%.2f", signal.trailingSl)}" else "--")
                    SignalMetric(Modifier.weight(1f), "Risk / Reward", signal.riskReward)
                    SignalMetric(Modifier.weight(1f), "Reason", if (signal.reasons.isNotBlank()) "Trend + Momentum" else "Technical")
                    SignalMetric(Modifier.weight(1f), "Signal Time", signal.timestamp.substringAfter(" "))
                }
            }
            
            // Bottom Section (Why This Signal)
            val checkmarks = signal.reasons.split(",").filter { it.isNotBlank() }
            if (checkmarks.isNotEmpty()) {
                Surface(
                    color = lightBg,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Why This Signal?", fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val displayChecks = checkmarks
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Break into rows of 3
                            displayChecks.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowItems.forEach { item ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = when (item) {
                                                    "EMA" -> if (isBullish) "EMA 9 > EMA 20" else "EMA 9 < EMA 20"
                                                    "VWAP" -> if (isBullish) "Price above VWAP" else "Price below VWAP"
                                                    "RSI" -> if (isBullish) "RSI > 50" else "RSI < 50"
                                                    "SUPERTREND" -> "Supertrend Confirmed"
                                                    "OI" -> if (isBullish) "Put OI Support" else "Call OI Resistance"
                                                    "VOLUME" -> "Volume Breakout"
                                                    else -> item
                                                },
                                                fontSize = 11.sp,
                                                color = TextWhite,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    // fill remaining space if row is not full
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalMetric(modifier: Modifier = Modifier, label: String, value: String, valueColor: Color = TextWhite) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = TextGray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

