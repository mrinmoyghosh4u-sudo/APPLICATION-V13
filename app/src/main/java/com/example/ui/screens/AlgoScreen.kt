package com.example.ui.screens

import com.example.data.model.AlgoTradeHistory






import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.FilterList

import androidx.compose.material.icons.outlined.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.example.data.model.AlgoStrategy

import com.example.ui.components.CrownLogo

import com.example.ui.theme.*

import com.example.util.AlgoEngine

import com.example.viewmodel.MainViewModel


val AlgoTradeHistory.pnl: Double
    get() {
        if (this.status.contains("PROFIT")) {
            val amt = this.status.substringAfter("₹").replace(",", "").toDoubleOrNull() ?: 0.0
            return amt
        }
        if (this.status.contains("LOSS")) {
            val amt = this.status.substringAfter("₹").replace(",", "").toDoubleOrNull() ?: 0.0
            return -amt
        }
        return 0.0
    }
    
val AlgoTradeHistory.strategyId: String
    get() = this.strategyName

enum class AlgoScreenState {
    DASHBOARD,
    CURRENT_SIGNAL_DETAIL,
    AI_CREATE,
    STRATEGY_BUILDER,
    MY_STRATEGIES,
    RISK_MANAGEMENT,
    PERFORMANCE,
    TRADE_HISTORY
}
@Composable
fun AlgoScreen(
    viewModel: MainViewModel,
    onOpenNotificationCenter: () -> Unit,
    onNavigateToAISignals: () -> Unit = {}
) {
    var currentState by remember { mutableStateOf(AlgoScreenState.DASHBOARD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentState != AlgoScreenState.DASHBOARD) {
                    IconButton(onClick = { currentState = AlgoScreenState.DASHBOARD }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SecondaryGold
                        )
                    }
                } else {
                    CrownLogo(size = 36.dp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "KING KHAN AI TRADE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite
                    )
                    Text(
                        "Algo Trading",
                        fontSize = 11.sp,
                        color = SecondaryGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            IconButton(onClick = onOpenNotificationCenter) {
                Box {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Alerts",
                        tint = SecondaryGold
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ProfitGreen, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }

        when (currentState) {
            AlgoScreenState.DASHBOARD -> AlgoDashboard(
                onNavigate = { currentState = it }
            )
            AlgoScreenState.CURRENT_SIGNAL_DETAIL -> CurrentSignalDetailScreen(
                onBack = { currentState = AlgoScreenState.DASHBOARD }
            )
            AlgoScreenState.AI_CREATE -> AiCreateStrategy(onNavigate = { currentState = it })
            AlgoScreenState.STRATEGY_BUILDER -> StrategyBuilder(onNavigate = { currentState = it })
            AlgoScreenState.MY_STRATEGIES -> MyStrategies(onNavigate = { currentState = it })
            AlgoScreenState.RISK_MANAGEMENT -> RiskManagement()
            AlgoScreenState.PERFORMANCE -> AlgoPerformance()
            AlgoScreenState.TRADE_HISTORY -> TradeHistory()
        }
    }
}

@Composable
fun AlgoDashboard(
    onNavigate: (AlgoScreenState) -> Unit
) {
    val isAlgoActive by AlgoEngine.isAlgoRunning.collectAsState()
    val currentStrategy by AlgoEngine.currentStrategy.collectAsState()
    val selectedIndex by AlgoEngine.selectedIndex.collectAsState()
    val selectedOptionMode by AlgoEngine.selectedOptionMode.collectAsState()

    val todayPnl by AlgoEngine.todayPnl.collectAsState()
    val todayTradesCount by AlgoEngine.todayTradesCount.collectAsState()
    val activePositions by AlgoEngine.activePositions.collectAsState()
    val currentSignal by AlgoEngine.currentSignal.collectAsState()
    val marketBias by AlgoEngine.marketBias.collectAsState()
    val ceBuyScore by AlgoEngine.ceBuyScore.collectAsState()
    val peBuyScore by AlgoEngine.peBuyScore.collectAsState()
    val indicatorCheckmarks by AlgoEngine.indicatorCheckmarks.collectAsState()

    val riskPerTrade by AlgoEngine.riskPerTrade.collectAsState()
    val liveTradeHistory by AlgoEngine.liveTradeHistory.collectAsState()
    val maxDailyLossPercent by AlgoEngine.maxDailyLossPercent.collectAsState()
    val maxTradesPerDay by AlgoEngine.maxTradesPerDay.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. ALGO ENGINE CARD
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAlgoActive) ProfitGreen.copy(alpha = 0.6f) else SecondaryGold.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ALGO ENGINE",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (isAlgoActive) ProfitGreen.copy(alpha = 0.2f) else LossRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAlgoActive) ProfitGreen else LossRed
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isAlgoActive) ProfitGreen else LossRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isAlgoActive) "ON" else "OFF",
                                    color = if (isAlgoActive) ProfitGreen else LossRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Strategy, Index, Option Mode, Trading Mode Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Strategy", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                currentStrategy.name,
                                color = PrimaryGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Index", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                selectedIndex,
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Option Mode", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                selectedOptionMode,
                                color = SecondaryGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Trading Mode", color = TextGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "LIVE SIGNALS",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // START / STOP ALGO BUTTON
                    if (isAlgoActive) {
                        Button(
                            onClick = { AlgoEngine.emergencyStop() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "STOP ALGO",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = { AlgoEngine.toggleAlgo(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "START ALGO",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = DarkCardBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Stats Row inside Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Today's P&L", color = TextGray, fontSize = 10.sp)
                            val formattedPnl = if (todayPnl >= 0) "+ ₹${String.format("%.2f", todayPnl)}" else "- ₹${String.format("%.2f", kotlin.math.abs(todayPnl))}"
                            Text(
                                formattedPnl,
                                color = if (todayPnl >= 0) ProfitGreen else LossRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Trades", color = TextGray, fontSize = 10.sp)
                            Text(
                                "$todayTradesCount",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Win Rate", color = TextGray, fontSize = 10.sp)
                            val liveHistoryVal = liveTradeHistory
                            val wins = liveHistoryVal.count { it.pnl > 0 }
                            val winRateStr = if (liveHistoryVal.isEmpty()) "--" else String.format("%.2f%%", (wins.toDouble() / liveHistoryVal.size) * 100)
                            Text(
                                winRateStr,
                                color = if (winRateStr == "--") TextGray else ProfitGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Active", color = TextGray, fontSize = 10.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${activePositions.size}",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(ProfitGreen, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. CURRENT SIGNAL CARD
        item {
            val sig = currentSignal
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AlgoScreenState.CURRENT_SIGNAL_DETAIL) },
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CURRENT SIGNAL",
                        color = SecondaryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (sig != null) {
                        Text(
                            sig.actionType,
                            color = ProfitGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            sig.symbol,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Entry", color = TextGray, fontSize = 10.sp)
                                Text("₹${sig.entryZone}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("SL", color = TextGray, fontSize = 10.sp)
                                Text("₹${sig.stopLoss}", color = LossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("T1", color = TextGray, fontSize = 10.sp)
                                Text("₹${sig.target1}", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("T2", color = TextGray, fontSize = 10.sp)
                                Text("₹${sig.target2}", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("T3", color = TextGray, fontSize = 10.sp)
                                Text("₹${sig.target3}", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Trailing SL: ₹5.00 (1.5%)",
                            color = TextGray,
                            fontSize = 10.sp
                        )
                    } else {
                        // Default empty state if no signal active
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "NO ACTIVE SIGNAL",
                                color = TextGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. MARKET BIAS & AI OPTION DECISION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // MARKET BIAS
                val biasColor = when (marketBias) {
                    "BULLISH" -> ProfitGreen
                    "BEARISH" -> LossRed
                    else -> TextWhite
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "MARKET BIAS",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            marketBias,
                            color = biasColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val checkmarkMap = AlgoEngine.indicatorCheckmarks.collectAsState().value
                        val indicatorKeys = listOf("EMA", "VWAP", "RSI", "SUPER TREND", "OI", "VOLUME")

                        indicatorKeys.forEach { ind ->
                            val isPass = checkmarkMap[ind] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ind, color = TextGray, fontSize = 10.sp)
                                Text(
                                    if (isPass) "✓" else "✕",
                                    color = if (isPass) ProfitGreen else LossRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // AI OPTION DECISION
                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "AI OPTION DECISION",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("CE BUY", color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${ceBuyScore}%", color = ProfitGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("PE BUY", color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${peBuyScore}%", color = LossRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("CONFIDENCE", color = TextGray, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        val confidenceFraction = if (ceBuyScore + peBuyScore > 0) (ceBuyScore.toFloat() / (ceBuyScore + peBuyScore)) else 0.5f
                        LinearProgressIndicator(
                            progress = { confidenceFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (ceBuyScore >= peBuyScore) ProfitGreen else LossRed,
                            trackColor = DarkCardSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DarkCardSecondary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "NO TRADE",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. RISK MANAGEMENT CARD
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "RISK MANAGEMENT",
                        color = SecondaryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Risk Per Trade", color = TextGray, fontSize = 10.sp)
                            Text("${riskPerTrade.toInt()}%", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Max Trades / Day", color = TextGray, fontSize = 10.sp)
                            Text("$maxTradesPerDay", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("Max Daily Loss", color = TextGray, fontSize = 10.sp)
                            Text("${maxDailyLossPercent.toInt()}%", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Status", color = TextGray, fontSize = 10.sp)
                            Text("SAFE", color = ProfitGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Daily Loss", color = TextGray, fontSize = 10.sp)
                            Text("--", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. QUICK ACTIONS GRID
        item {
            Text(
                "QUICK ACTIONS",
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("AI CREATE", Icons.Outlined.AutoAwesome, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.AI_CREATE)
                    }
                    QuickActionCard("STRATEGY BUILDER", Icons.Outlined.Edit, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.STRATEGY_BUILDER)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("MY STRATEGIES", Icons.Outlined.ListAlt, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.MY_STRATEGIES)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("RISK CONTROL", Icons.Outlined.Security, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.RISK_MANAGEMENT)
                    }
                    QuickActionCard("PERFORMANCE", Icons.Outlined.TrendingUp, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.PERFORMANCE)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("TRADE HISTORY", Icons.Outlined.History, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.TRADE_HISTORY)
                    }
                    QuickActionCard("SYSTEM LOGS", Icons.Outlined.Terminal, Modifier.weight(1f)) {
                        // TODO: Implement logs view
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = SecondaryGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                title,
                color = TextWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------------------------
// CURRENT SIGNAL DETAILED SCREEN (Panel 3)
// -------------------------------------------------------------
@Composable
fun CurrentSignalDetailScreen(onBack: () -> Unit) {
    val signal by AlgoEngine.currentSignal.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SecondaryGold)
                }
                Text("CURRENT SIGNAL", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (signal != null) {
                Surface(
                    color = ProfitGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen)
                ) {
                    Text(
                        "LIVE",
                        color = ProfitGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        val sig = signal
        if (sig == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("NO ACTIVE SIGNAL", color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            // Signal Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(sig.actionType, color = if(sig.actionType.contains("CE")) ProfitGreen else LossRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(sig.symbol, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LTP ", color = TextGray, fontSize = 11.sp)
                        Text("₹${sig.ltp} ", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SignalDetailRow("Entry", sig.entryZone, TextWhite)
                    SignalDetailRow("Stop Loss", "₹${sig.stopLoss}", LossRed)
                    SignalDetailRow("Target 1", "₹${sig.target1}", ProfitGreen)
                    SignalDetailRow("Target 2", "₹${sig.target2}", ProfitGreen)
                    SignalDetailRow("Target 3", "₹${sig.target3}", ProfitGreen)
                    SignalDetailRow("Target 4", "₹${sig.target4}", ProfitGreen)
                    SignalDetailRow("Trailing SL", "₹${sig.trailingSl}", TextWhite)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // WHY THIS SIGNAL CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WHY THIS SIGNAL", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    val reasons = sig.reasons.split(",").filter { it.isNotBlank() }
                    if (reasons.isNotEmpty()) {
                        reasons.forEach { reason ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(reason, color = TextWhite, fontSize = 12.sp)
                                Text("✓", color = ProfitGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Text("Technical Breakout", color = TextWhite, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
@Composable
fun SignalDetailRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGray, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------
// MY STRATEGIES SCREEN (Panel 4)
// -------------------------------------------------------------
@Composable
fun MyStrategies(onNavigate: (AlgoScreenState) -> Unit) {
    val strategies by AlgoEngine.strategies.collectAsState()
    val liveTradeHistory by AlgoEngine.liveTradeHistory.collectAsState()
    val currentStrategy by AlgoEngine.currentStrategy.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MY STRATEGIES", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            IconButton(onClick = { onNavigate(AlgoScreenState.STRATEGY_BUILDER) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Strategy", tint = SecondaryGold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(strategies) { strat ->
                val isSelected = strat.id == currentStrategy.id
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ProfitGreen.copy(alpha = 0.5f) else DarkCardBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strat.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Surface(
                                color = if (isSelected) ProfitGreen.copy(alpha = 0.2f) else DarkCardSecondary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (isSelected) "ACTIVE" else "INACTIVE",
                                    color = if (isSelected) ProfitGreen else TextGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                                val stratWins = liveTradeHistory.count { it.pnl > 0 && it.strategyName == strat.name }
                                val stratTotal = liveTradeHistory.count { it.strategyName == strat.name }
                                val stratPnl = liveTradeHistory.filter { it.strategyName == strat.name }.sumOf { it.pnl }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Index", color = TextGray, fontSize = 10.sp)
                                Text(strat.index, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("Timeframe", color = TextGray, fontSize = 10.sp)
                                Text(strat.timeframe, color = SecondaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("Win Rate", color = TextGray, fontSize = 10.sp)
                                val stratWinRateStr = if (stratTotal == 0) "--" else String.format("%.2f%%", (stratWins.toDouble() / stratTotal) * 100)
                                Text(stratWinRateStr, color = if (stratWinRateStr == "--") TextGray else ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Mode", color = TextGray, fontSize = 10.sp)
                                Text(strat.optionMode, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("P&L", color = TextGray, fontSize = 10.sp)
                                val stratPnlStr = if (stratPnl == 0.0) "--" else String.format("%+₹.2f", stratPnl)
                                Text(stratPnlStr, color = if(stratPnl > 0) ProfitGreen else if(stratPnl < 0) LossRed else TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("Last Run", color = TextGray, fontSize = 10.sp)
                                Text("10:28 AM", color = TextWhite, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onNavigate(AlgoScreenState.STRATEGY_BUILDER) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Text("EDIT", color = TextWhite, fontSize = 11.sp)
                            }

                            if (isSelected) {
                                Button(
                                    onClick = { },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LossRed.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LossRed)
                                ) {
                                    Text("DISABLE", color = LossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { AlgoEngine.selectStrategy(strat) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen)
                                ) {
                                    Text("ENABLE", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STRATEGY BUILDER SCREEN (Panel 5)
// -------------------------------------------------------------
@Composable
fun StrategyBuilder(onNavigate: (AlgoScreenState) -> Unit) {
    var strategyName by remember { mutableStateOf("NIFTY MOMENTUM") }
    var selectedIndex by remember { mutableStateOf("NIFTY 50") }
    var selectedMode by remember { mutableStateOf("AUTO CE / PE") }
    var selectedTimeframe by remember { mutableStateOf("5 MIN") }
    var selectedAction by remember { mutableStateOf("BUY CE") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("STRATEGY BUILDER", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            TextButton(onClick = {
                val newStrat = AlgoStrategy(
                    id = "strat_${System.currentTimeMillis()}",
                    name = strategyName,
                    index = selectedIndex,
                    optionMode = selectedMode,
                    tradingStyle = "INTRADAY",
                    timeframe = selectedTimeframe,
                    riskLevel = "MEDIUM",
                    capital = 100000.0,
                    isActive = false,
                    maxTrades = 5
                )
                AlgoEngine.saveStrategy(newStrat)
                onNavigate(AlgoScreenState.MY_STRATEGIES)
            }) {
                Text("SAVE", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Strategy Name", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = strategyName,
            onValueChange = { strategyName = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = DarkCardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Index", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BuilderDropdown(selectedIndex, listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX")) { selectedIndex = it }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Option Mode", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BuilderDropdown(selectedMode, listOf("AUTO CE / PE", "BUY CE ONLY", "BUY PE ONLY")) { selectedMode = it }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Timeframe", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BuilderDropdown(selectedTimeframe, listOf("5 MIN", "1 MIN", "15 MIN", "30 MIN")) { selectedTimeframe = it }

        Spacer(modifier = Modifier.height(16.dp))

        // ENTRY CONDITIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ENTRY CONDITIONS", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            BuilderDropdown("AND", listOf("AND", "OR")) { }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ConditionRow("EMA 20", ">", "EMA 50")
        ConditionRow("Price", ">", "VWAP")
        ConditionRow("RSI", ">", "55")
        ConditionRow("Supertrend", "=", "BULLISH")

        Spacer(modifier = Modifier.height(16.dp))

        // ACTION
        Text("ACTION", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedAction = "BUY CE" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAction == "BUY CE") ProfitGreen.copy(alpha = 0.3f) else DarkCard
                ),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedAction == "BUY CE") ProfitGreen else DarkCardBorder)
            ) {
                Text("BUY CE", color = if (selectedAction == "BUY CE") ProfitGreen else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { selectedAction = "BUY PE" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAction == "BUY PE") LossRed.copy(alpha = 0.3f) else DarkCard
                ),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedAction == "BUY PE") LossRed else DarkCardBorder)
            ) {
                Text("BUY PE", color = if (selectedAction == "BUY PE") LossRed else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { selectedAction = "NO TRADE" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAction == "NO TRADE") PrimaryGold.copy(alpha = 0.3f) else DarkCard
                ),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedAction == "NO TRADE") PrimaryGold else DarkCardBorder)
            ) {
                Text("NO TRADE", color = if (selectedAction == "NO TRADE") PrimaryGold else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RISK MANAGEMENT
        Text("RISK MANAGEMENT", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Stop Loss (%)", color = TextGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = "17",
                    onValueChange = {},
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Target 1 (%)", color = TextGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = "12",
                    onValueChange = {},
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BuilderDropdown(selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selected, color = TextWhite, fontSize = 12.sp)
                Text("∨", color = SecondaryGold, fontSize = 12.sp)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkCard)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = TextWhite) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ConditionRow(left: String, operator: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1.2f),
            color = DarkCard,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(left, color = TextWhite, fontSize = 11.sp)
            }
        }

        Surface(
            modifier = Modifier.weight(0.6f),
            color = DarkCard,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(operator, color = SecondaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            modifier = Modifier.weight(1.2f),
            color = DarkCard,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(right, color = TextWhite, fontSize = 11.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// PERFORMANCE SCREEN (Panel 6)
// -------------------------------------------------------------
@Composable
fun AlgoPerformance() {
    var selectedTimeframe by remember { mutableStateOf("Today") }
    val liveTradeHistory by AlgoEngine.liveTradeHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PERFORMANCE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = SecondaryGold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Today", "Week", "Month", "All Time").forEach { tf ->
                val isSelected = tf == selectedTimeframe
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTimeframe = tf },
                    color = if (isSelected) PrimaryGold else DarkCard,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(
                            tf,
                            color = if (isSelected) Color.Black else TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Grid (2x2)
        val perfHistory = liveTradeHistory
        val perfPnl = perfHistory.sumOf { it.pnl }
        val perfTrades = perfHistory.size
        val perfWins = perfHistory.count { it.pnl > 0 }
        val perfLosses = perfHistory.count { it.pnl < 0 }
        val perfWinRateStr = if (perfTrades == 0) "--" else String.format("%.2f%%", (perfWins.toDouble() / perfTrades) * 100)
        
        val perfTotalProfit = perfHistory.filter { it.pnl > 0 }.sumOf { it.pnl }
        val perfTotalLoss = kotlin.math.abs(perfHistory.filter { it.pnl < 0 }.sumOf { it.pnl })
        val perfProfitFactor = if (perfTotalLoss == 0.0) "--" else String.format("%.2f", perfTotalProfit / perfTotalLoss)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("P&L", if (perfPnl == 0.0) "--" else String.format("%+₹.2f", perfPnl), if (perfPnl > 0) ProfitGreen else if (perfPnl < 0) LossRed else TextGray, Modifier.weight(1f))
                PerformanceMetricBox("Net P&L %", "--", TextGray, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Trades", "$perfTrades", TextWhite, Modifier.weight(1f))
                PerformanceMetricBox("Win Rate", perfWinRateStr, if (perfWinRateStr == "--") TextGray else ProfitGreen, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Winning Trades", "$perfWins", ProfitGreen, Modifier.weight(1f))
                PerformanceMetricBox("Losing Trades", "$perfLosses", LossRed, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Profit Factor", perfProfitFactor, PrimaryGold, Modifier.weight(1f))
                PerformanceMetricBox("Max Drawdown", "--", TextGray, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // P&L OVER TIME CANVAS CHART
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("P&L OVER TIME", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                if (perfHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No trade performance history recorded yet.",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height

                            val cumPnlList = mutableListOf<Double>()
                            var runningSum = 0.0
                            perfHistory.forEach { trade ->
                                runningSum += trade.pnl
                                cumPnlList.add(runningSum)
                            }

                            val minPnl = cumPnlList.minOrNull()?.coerceAtMost(0.0) ?: 0.0
                            val maxPnl = cumPnlList.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                            val rangePnl = if (maxPnl == minPnl) 1.0 else (maxPnl - minPnl)

                            val stepX = width / (cumPnlList.size - 1).coerceAtLeast(1)

                            val points = cumPnlList.mapIndexed { idx, valPnl ->
                                val x = idx * stepX
                                val y = height - (((valPnl - minPnl) / rangePnl) * (height * 0.8f) + (height * 0.1f)).toFloat()
                                Offset(x, y)
                            }

                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }

                            val lineCol = if (runningSum >= 0) ProfitGreen else LossRed
                            drawPath(
                                path = path,
                                color = lineCol,
                                style = Stroke(width = 2.5.dp.toPx())
                            )

                            points.forEach { pt ->
                                drawCircle(
                                    color = lineCol,
                                    radius = 3.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val times = perfHistory.take(6).map { it.time.takeLast(8) }
                        times.forEach { t ->
                            Text(t, color = TextGray, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PerformanceMetricBox(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = TextGray, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------
// OTHER ALGO SUB-SCREENS
// -------------------------------------------------------------
@Composable
fun AiCreateStrategy(onNavigate: (AlgoScreenState) -> Unit) {
    var index by remember { mutableStateOf("NIFTY 50") }
    var optionMode by remember { mutableStateOf("AUTO CE / PE") }
    var generatedStrategy by remember { mutableStateOf<AlgoStrategy?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("AI CREATE STRATEGY", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
        Spacer(modifier = Modifier.height(16.dp))

        if (generatedStrategy == null) {
            Text("Index Selection", color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX").forEach { idx ->
                    Chip(text = idx, selected = index == idx, onClick = { index = idx })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Option Mode (BUY ONLY Enforcement)", color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("BUY CE ONLY", "BUY PE ONLY", "AUTO CE / PE").forEach { mode ->
                    Chip(text = mode, selected = optionMode == mode, onClick = { optionMode = mode })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    generatedStrategy = AlgoEngine.generateAIStrategy(index, "INTRADAY", "5 MIN", "MEDIUM", 50000.0, optionMode)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("GENERATE STRATEGY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Text("Strategy Generated Successfully", color = ProfitGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: ${generatedStrategy?.name}", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text("Index: ${generatedStrategy?.index}", color = TextGray, fontSize = 12.sp)
                    Text("Option Mode: ${generatedStrategy?.optionMode}", color = SecondaryGold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Market Rules: EMA Crossover + VWAP + RSI Confirmation", color = TextGray, fontSize = 12.sp)
                    Text("Risk: Max 1% per trade • 1:2.5 Risk-Reward", color = TextGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        generatedStrategy?.let { AlgoEngine.saveStrategy(it) }
                        onNavigate(AlgoScreenState.MY_STRATEGIES)
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE STRATEGY", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (selected) PrimaryGold.copy(alpha = 0.2f) else DarkCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PrimaryGold else DarkCardBorder)
    ) {
        Text(text, color = if (selected) PrimaryGold else TextGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
fun RiskManagement() {
    val riskPerTrade by AlgoEngine.riskPerTrade.collectAsState()
    val liveTradeHistory by AlgoEngine.liveTradeHistory.collectAsState()
    val maxDailyLossPercent by AlgoEngine.maxDailyLossPercent.collectAsState()
    val maxTradesPerDay by AlgoEngine.maxTradesPerDay.collectAsState()
    val onePosAtATime by AlgoEngine.onePositionAtATime.collectAsState()

    var sliderRisk by remember(riskPerTrade) { mutableStateOf(riskPerTrade.toFloat()) }
    var sliderLoss by remember(maxDailyLossPercent) { mutableStateOf(maxDailyLossPercent.toFloat()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("RISK CONTROL CENTER", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Risk Per Trade: ${String.format("%.1f", sliderRisk)}%", color = TextWhite, fontWeight = FontWeight.Bold)
                Slider(
                    value = sliderRisk,
                    onValueChange = { sliderRisk = it },
                    valueRange = 0.5f..5.0f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryGold, activeTrackColor = PrimaryGold)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Max Daily Loss Limit: ${String.format("%.1f", sliderLoss)}%", color = TextWhite, fontWeight = FontWeight.Bold)
                Slider(
                    value = sliderLoss,
                    onValueChange = { sliderLoss = it },
                    valueRange = 1.0f..10.0f,
                    colors = SliderDefaults.colors(thumbColor = LossRed, activeTrackColor = LossRed)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Limit to 1 Position at a time", color = TextWhite)
                    Switch(
                        checked = onePosAtATime,
                        onCheckedChange = { AlgoEngine.updateRiskSettings(sliderRisk.toDouble(), sliderLoss.toDouble(), maxTradesPerDay, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { AlgoEngine.updateRiskSettings(sliderRisk.toDouble(), sliderLoss.toDouble(), maxTradesPerDay, onePosAtATime) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("SAVE RISK PARAMETERS", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun TradeHistory() {
    val tradeHistory by AlgoEngine.liveTradeHistory.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("ALGO TRADE HISTORY", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
        Spacer(modifier = Modifier.height(16.dp))
        if (tradeHistory.isEmpty()) {
            Text("No recent algo trades recorded.", color = TextGray, fontSize = 12.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tradeHistory) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkCard,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${item.strike} (${item.action})", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${item.date} ${item.time} • ${item.index}", color = TextGray, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${item.price}", color = SecondaryGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(item.status, color = if (item.status.contains("PROFIT")) ProfitGreen else TextWhite, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

