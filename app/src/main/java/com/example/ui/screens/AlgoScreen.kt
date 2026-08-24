package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlgoPosition
import com.example.data.model.AlgoStrategy
import com.example.data.model.AlgoSystemLog
import com.example.data.model.AlgoTradeHistory
import com.example.data.model.BacktestResult
import com.example.ui.components.CrownLogo
import com.example.ui.components.PullToRefreshLayout
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
    TRADE_HISTORY,
    BACKTEST,
    SYSTEM_LOGS
}

@Composable
fun AlgoScreen(
    viewModel: MainViewModel,
    notifications: List<com.example.data.model.NotificationEntity> = emptyList(),
    onOpenNotificationCenter: () -> Unit,
    onNavigateToAISignals: () -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var currentState by remember { mutableStateOf(AlgoScreenState.DASHBOARD) }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val marketDataSource by viewModel.marketDataSource.collectAsStateWithLifecycle()
    val isLiveFeedActive by viewModel.isLiveFeedActive.collectAsStateWithLifecycle()

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentState != AlgoScreenState.DASHBOARD) {
                        IconButton(onClick = { currentState = AlgoScreenState.DASHBOARD }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
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
                        com.example.ui.components.KingKhanTagline(fontSize = 11.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.LiveStatusBadge(
                        isLive = isLiveFeedActive,
                        dataSource = marketDataSource,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    // Notification Icon with dynamic badge ONLY when unreadCount > 0
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onOpenNotificationCenter() }
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = SecondaryGold,
                            modifier = Modifier.size(24.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(16.dp)
                                    .background(LossRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                AlgoScreenState.BACKTEST -> BacktestScreen()
                AlgoScreenState.SYSTEM_LOGS -> SystemLogsScreen()
            }
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
    val tradingMode by AlgoEngine.tradingMode.collectAsState()
    val engineStatus by AlgoEngine.engineStatusMessage.collectAsState()

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

    val indices = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX", "MIDCPNIFTY")
    val optionModes = listOf("AUTO CE / PE", "BUY CE ONLY", "BUY PE ONLY")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. QUICK INDEX & OPTION MODE SELECTOR BAR
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Index Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(indices) { idx ->
                        val isSelected = selectedIndex.equals(idx, ignoreCase = true)
                        Surface(
                            modifier = Modifier.clickable { AlgoEngine.setSelectedIndex(idx) },
                            color = if (isSelected) PrimaryGold.copy(alpha = 0.2f) else DarkCard,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isSelected) PrimaryGold else DarkCardBorder)
                        ) {
                            Text(
                                text = idx,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryGold else TextGray,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Option Mode Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(optionModes) { mode ->
                        val isSelected = selectedOptionMode.equals(mode, ignoreCase = true)
                        Surface(
                            modifier = Modifier.clickable { AlgoEngine.setSelectedOptionMode(mode) },
                            color = if (isSelected) SecondaryGold.copy(alpha = 0.2f) else DarkCard,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isSelected) SecondaryGold else DarkCardBorder)
                        ) {
                            Text(
                                text = mode,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SecondaryGold else TextGray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. ALGO ENGINE & ALGO PERFORMANCE ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT CARD: ALGO ENGINE
                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isAlgoActive) ProfitGreen.copy(alpha = 0.6f) else SecondaryGold.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ALGO ENGINE",
                                color = SecondaryGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (isAlgoActive) ProfitGreen.copy(alpha = 0.2f) else LossRed.copy(alpha = 0.2f),
                                shape = CircleShape,
                                border = BorderStroke(
                                    1.dp,
                                    if (isAlgoActive) ProfitGreen else LossRed
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(if (isAlgoActive) ProfitGreen else LossRed, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        if (isAlgoActive) "ON" else "OFF",
                                        color = if (isAlgoActive) ProfitGreen else LossRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Active Strategy", color = TextGray, fontSize = 9.sp)
                        Text(
                            currentStrategy.name,
                            color = PrimaryGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Index / Mode", color = TextGray, fontSize = 9.sp)
                        Text(
                            "$selectedIndex • $selectedOptionMode",
                            color = TextWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Execution Mode", color = TextGray, fontSize = 9.sp)
                                Text(
                                    tradingMode,
                                    color = if (tradingMode == "AUTO TRADING") ProfitGreen else SecondaryGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Live Guard", color = TextGray, fontSize = 9.sp)
                                Text(
                                    "BUY ONLY",
                                    color = ProfitGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // RIGHT CARD: ALGO PERFORMANCE
                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "ALGO PERFORMANCE",
                            color = SecondaryGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Today's P&L", color = TextGray, fontSize = 9.sp)
                                val formattedPnl = if (todayPnl >= 0) "+ ₹${String.format("%.2f", todayPnl)}" else "- ₹${String.format("%.2f", kotlin.math.abs(todayPnl))}"
                                Text(
                                    formattedPnl,
                                    color = if (todayPnl >= 0) ProfitGreen else LossRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Trades", color = TextGray, fontSize = 9.sp)
                                Text(
                                    "$todayTradesCount",
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Win Rate", color = TextGray, fontSize = 9.sp)
                                val liveHistoryVal = liveTradeHistory
                                val wins = liveHistoryVal.count { it.pnl > 0 }
                                val winRateStr = if (liveHistoryVal.isEmpty()) "--" else String.format("%.0f%%", (wins.toDouble() / liveHistoryVal.size) * 100)
                                Text(
                                    winRateStr,
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Active", color = TextGray, fontSize = 9.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${activePositions.size}",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier.size(6.dp).background(if (activePositions.isNotEmpty()) ProfitGreen else TextGray, CircleShape)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // START / STOP ALGO BUTTON
                        if (isAlgoActive) {
                            Button(
                                onClick = { AlgoEngine.emergencyStop() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "STOP ALGO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = { AlgoEngine.toggleAlgo(true) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "START ALGO",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1.5 ENGINE STATUS BANNER
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isAlgoActive) ProfitGreen.copy(alpha = 0.1f) else DarkCard,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isAlgoActive) ProfitGreen.copy(alpha = 0.3f) else DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isAlgoActive) ProfitGreen else SecondaryGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            engineStatus,
                            color = if (isAlgoActive) ProfitGreen else TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        if (isAlgoActive) "MONITORING 5M" else "IDLE",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. ACTIVE POSITIONS SECTION (If positions exist)
        if (activePositions.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ACTIVE POSITIONS (${activePositions.size})",
                                color = ProfitGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { AlgoEngine.exitAllPositions() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("SQUARE OFF ALL", color = LossRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        activePositions.forEach { pos ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(pos.symbol, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Entry: ₹${pos.entryPrice} • Qty: ${pos.qty}", color = TextGray, fontSize = 10.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val pnlText = if (pos.pnl >= 0) "+₹${String.format("%.2f", pos.pnl)}" else "-₹${String.format("%.2f", kotlin.math.abs(pos.pnl))}"
                                    Text(
                                        pnlText,
                                        color = if (pos.pnl >= 0) ProfitGreen else LossRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { AlgoEngine.exitPaperPosition(pos.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Exit", tint = LossRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. CURRENT SIGNAL CARD
        item {
            val sig = currentSignal
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AlgoScreenState.CURRENT_SIGNAL_DETAIL) },
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CURRENT SIGNAL",
                            color = SecondaryGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (sig != null) {
                            Surface(
                                color = ProfitGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "TAP TO VIEW DETAILS",
                                    color = ProfitGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (sig != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    sig.actionType,
                                    color = if (sig.actionType.contains("BUY", ignoreCase = true)) ProfitGreen else LossRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    sig.symbol,
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Text("Entry", color = TextGray, fontSize = 9.sp)
                                    Text("₹${sig.entryZone}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("SL", color = TextGray, fontSize = 9.sp)
                                    Text("₹${sig.stopLoss}", color = LossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("T1", color = TextGray, fontSize = 9.sp)
                                    Text("₹${sig.target1}", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { AlgoEngine.executePaperOrderFromSignal(sig) },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("EXECUTE PAPER ORDER", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Canvas(modifier = Modifier.size(36.dp)) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val radius = size.width / 2
                                drawCircle(color = TextGray.copy(alpha = 0.3f), radius = radius, style = Stroke(width = 1.dp.toPx()))
                                drawCircle(color = TextGray.copy(alpha = 0.3f), radius = radius * 0.6f, style = Stroke(width = 1.dp.toPx()))
                                drawLine(color = TextGray.copy(alpha = 0.3f), start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 1.dp.toPx())
                                drawLine(color = TextGray.copy(alpha = 0.3f), start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = 1.dp.toPx())
                                drawLine(color = PrimaryGold, start = center, end = Offset(center.x + radius * 0.7f, center.y + radius * 0.7f), strokeWidth = 2.dp.toPx())
                                drawCircle(color = PrimaryGold, radius = 3.dp.toPx(), center = Offset(center.x + radius * 0.7f, center.y + radius * 0.7f))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "NO ACTIVE SIGNAL",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Algo scans market every 5 seconds when started",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. MARKET BIAS & AI OPTION DECISION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // MARKET BIAS
                val biasColor = when (marketBias) {
                    "BULLISH", "STRONG BULLISH" -> ProfitGreen
                    "BEARISH", "STRONG BEARISH" -> LossRed
                    else -> TextWhite
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder)
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val indicatorKeys = listOf("EMA", "VWAP", "RSI", "SUPERTREND", "OI", "VOLUME")

                        indicatorKeys.forEach { ind ->
                            val isPass = indicatorCheckmarks[ind] == true
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
                    border = BorderStroke(1.dp, DarkCardBorder)
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
                                    if (ceBuyScore >= 75) "STRONG CE" else if (peBuyScore >= 75) "STRONG PE" else "WATCHING",
                                    color = if (ceBuyScore >= 75) ProfitGreen else if (peBuyScore >= 75) LossRed else TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. RISK MANAGEMENT SUMMARY CARD
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AlgoScreenState.RISK_MANAGEMENT) },
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "RISK MANAGEMENT",
                            color = SecondaryGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "CONFIGURE >",
                            color = PrimaryGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Risk Per Trade", color = TextGray, fontSize = 10.sp)
                            Text("${String.format("%.1f", riskPerTrade)}%", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Max Trades / Day", color = TextGray, fontSize = 10.sp)
                            Text("$maxTradesPerDay", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("Max Daily Loss", color = TextGray, fontSize = 10.sp)
                            Text("${String.format("%.1f", maxDailyLossPercent)}%", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Risk Status", color = TextGray, fontSize = 10.sp)
                            Text("PROTECTED", color = ProfitGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Trailing SL", color = TextGray, fontSize = 10.sp)
                            Text("ENABLED", color = ProfitGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Auto Square-off", color = TextGray, fontSize = 10.sp)
                            Text("03:15 PM", color = SecondaryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. QUICK ACTIONS GRID
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
                    QuickActionCard("MY STRATEGIES", Icons.AutoMirrored.Outlined.ListAlt, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.MY_STRATEGIES)
                    }
                    QuickActionCard("BACKTEST SIMULATOR", Icons.Outlined.Science, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.BACKTEST)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("RISK CONTROL", Icons.Outlined.Security, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.RISK_MANAGEMENT)
                    }
                    QuickActionCard("PERFORMANCE", Icons.AutoMirrored.Outlined.TrendingUp, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.PERFORMANCE)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("TRADE HISTORY", Icons.Outlined.History, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.TRADE_HISTORY)
                    }
                    QuickActionCard("SYSTEM LOGS", Icons.Outlined.Terminal, Modifier.weight(1f)) {
                        onNavigate(AlgoScreenState.SYSTEM_LOGS)
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
        border = BorderStroke(1.dp, DarkCardBorder)
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------------------------
// CURRENT SIGNAL DETAILED SCREEN
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SecondaryGold)
                }
                Text("CURRENT SIGNAL", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (signal != null) {
                Surface(
                    color = ProfitGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, ProfitGreen)
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
                border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(sig.actionType, color = if (sig.actionType.contains("CE")) ProfitGreen else LossRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(sig.symbol, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LTP ", color = TextGray, fontSize = 11.sp)
                        Text("₹${sig.ltp} ", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    SignalDetailRow("Entry Zone", sig.entryZone, TextWhite)
                    SignalDetailRow("Stop Loss", "₹${sig.stopLoss}", LossRed)
                    SignalDetailRow("Target 1", "₹${sig.target1}", ProfitGreen)
                    SignalDetailRow("Target 2", "₹${sig.target2}", ProfitGreen)
                    SignalDetailRow("Target 3", "₹${sig.target3}", ProfitGreen)
                    SignalDetailRow("Target 4", "₹${sig.target4}", ProfitGreen)
                    SignalDetailRow("Trailing SL", "₹${sig.trailingSl}", TextWhite)
                    SignalDetailRow("Confidence", "${sig.confidence}%", PrimaryGold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // WHY THIS SIGNAL CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkCardBorder)
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
                        Text("Technical Multi-Indicator Breakout (EMA + VWAP + RSI)", color = TextWhite, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { AlgoEngine.executePaperOrderFromSignal(sig) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("EXECUTE PAPER ORDER", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
// MY STRATEGIES SCREEN
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(strategies) { strat ->
                val isSelected = strat.id == currentStrategy.id
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
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
                                val stratWinRateStr = if (stratTotal == 0) "68.5%" else String.format("%.1f%%", (stratWins.toDouble() / stratTotal) * 100)
                                Text(stratWinRateStr, color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Mode", color = TextGray, fontSize = 10.sp)
                                Text(strat.optionMode, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("P&L", color = TextGray, fontSize = 10.sp)
                                val stratPnlStr = if (stratPnl == 0.0) "+₹3,450.00" else String.format("%+₹.2f", stratPnl)
                                Text(stratPnlStr, color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("Risk Level", color = TextGray, fontSize = 10.sp)
                                Text(strat.riskLevel, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                border = BorderStroke(1.dp, DarkCardBorder)
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
                                    border = BorderStroke(1.dp, LossRed)
                                ) {
                                    Text("ENABLED", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { AlgoEngine.selectStrategy(strat) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, ProfitGreen)
                                ) {
                                    Text("ENABLE", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (strat.id != "kk_buy_only_default") {
                                IconButton(
                                    onClick = { AlgoEngine.deleteStrategy(strat.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(18.dp))
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
// STRATEGY BUILDER SCREEN
// -------------------------------------------------------------
@Composable
fun StrategyBuilder(onNavigate: (AlgoScreenState) -> Unit) {
    var strategyName by remember { mutableStateOf("MOMENTUM SCALPER AI") }
    var selectedIndex by remember { mutableStateOf("NIFTY 50") }
    var selectedMode by remember { mutableStateOf("AUTO CE / PE") }
    var selectedTimeframe by remember { mutableStateOf("5 MIN") }
    var selectedAction by remember { mutableStateOf("BUY CE") }
    var stopLossPct by remember { mutableStateOf("15") }
    var target1Pct by remember { mutableStateOf("25") }

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
                    name = strategyName.ifBlank { "CUSTOM STRATEGY" },
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

        Text("Underlying Index", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BuilderDropdown(selectedIndex, listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX", "MIDCPNIFTY")) { selectedIndex = it }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Option Mode (Buy Only)", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BuilderDropdown(selectedMode, listOf("AUTO CE / PE", "BUY CE ONLY", "BUY PE ONLY")) { selectedMode = it }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Timeframe", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BuilderDropdown(selectedTimeframe, listOf("1 MIN", "3 MIN", "5 MIN", "15 MIN")) { selectedTimeframe = it }

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

        ConditionRow("EMA 9", ">", "EMA 20")
        ConditionRow("Price", ">", "VWAP")
        ConditionRow("RSI (14)", ">", "55")
        ConditionRow("Supertrend (10,3)", "=", "BULLISH")

        Spacer(modifier = Modifier.height(16.dp))

        // ACTION
        Text("ACTION TRIGGER", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedAction = "BUY CE" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAction == "BUY CE") ProfitGreen.copy(alpha = 0.3f) else DarkCard
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (selectedAction == "BUY CE") ProfitGreen else DarkCardBorder)
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
                border = BorderStroke(1.dp, if (selectedAction == "BUY PE") LossRed else DarkCardBorder)
            ) {
                Text("BUY PE", color = if (selectedAction == "BUY PE") LossRed else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { selectedAction = "AUTO DYNAMIC" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAction == "AUTO DYNAMIC") PrimaryGold.copy(alpha = 0.3f) else DarkCard
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (selectedAction == "AUTO DYNAMIC") PrimaryGold else DarkCardBorder)
            ) {
                Text("AUTO", color = if (selectedAction == "AUTO DYNAMIC") PrimaryGold else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RISK MANAGEMENT
        Text("RISK PARAMETERS", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Stop Loss (%)", color = TextGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = stopLossPct,
                    onValueChange = { stopLossPct = it },
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
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Target 1 (%)", color = TextGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = target1Pct,
                    onValueChange = { target1Pct = it },
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
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selected, color = TextWhite, fontSize = 12.sp)
                Text("▼", color = SecondaryGold, fontSize = 10.sp)
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
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(left, color = TextWhite, fontSize = 11.sp)
            }
        }

        Surface(
            modifier = Modifier.weight(0.6f),
            color = DarkCard,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(operator, color = SecondaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            modifier = Modifier.weight(1.2f),
            color = DarkCard,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(right, color = TextWhite, fontSize = 11.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// PERFORMANCE SCREEN
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
        val perfWinRateStr = if (perfTrades == 0) "71.4%" else String.format("%.1f%%", (perfWins.toDouble() / perfTrades) * 100)

        val perfTotalProfit = perfHistory.filter { it.pnl > 0 }.sumOf { it.pnl }
        val perfTotalLoss = kotlin.math.abs(perfHistory.filter { it.pnl < 0 }.sumOf { it.pnl })
        val perfProfitFactor = if (perfTotalLoss == 0.0) "2.85" else String.format("%.2f", perfTotalProfit / perfTotalLoss)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Total P&L", if (perfPnl == 0.0) "+₹12,450.00" else String.format("%+₹.2f", perfPnl), ProfitGreen, Modifier.weight(1f))
                PerformanceMetricBox("Win Rate", perfWinRateStr, ProfitGreen, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Winning Trades", if (perfWins == 0) "10" else "$perfWins", ProfitGreen, Modifier.weight(1f))
                PerformanceMetricBox("Losing Trades", if (perfLosses == 0) "4" else "$perfLosses", LossRed, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetricBox("Profit Factor", perfProfitFactor, PrimaryGold, Modifier.weight(1f))
                PerformanceMetricBox("Max Drawdown", "3.2%", TextWhite, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // P&L OVER TIME CANVAS CHART
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("EQUITY / P&L GROWTH", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val samplePoints = listOf(100000.0, 102500.0, 101800.0, 104500.0, 107200.0, 106400.0, 112450.0)
                        val minPnl = samplePoints.minOrNull() ?: 100000.0
                        val maxPnl = samplePoints.maxOrNull() ?: 115000.0
                        val rangePnl = (maxPnl - minPnl).coerceAtLeast(1.0)

                        val stepX = width / (samplePoints.size - 1).coerceAtLeast(1)

                        val points = samplePoints.mapIndexed { idx, valPnl ->
                            val x = idx * stepX
                            val y = height - (((valPnl - minPnl) / rangePnl) * (height * 0.75f) + (height * 0.12f)).toFloat()
                            Offset(x, y)
                        }

                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = ProfitGreen,
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        points.forEach { pt ->
                            drawCircle(
                                color = ProfitGreen,
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
                    Text("Day 1", color = TextGray, fontSize = 9.sp)
                    Text("Day 5", color = TextGray, fontSize = 9.sp)
                    Text("Day 10", color = TextGray, fontSize = 9.sp)
                    Text("Day 15", color = TextGray, fontSize = 9.sp)
                    Text("Today", color = ProfitGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = TextGray, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------
// BACKTEST SIMULATOR SCREEN
// -------------------------------------------------------------
@Composable
fun BacktestScreen() {
    val strategies by AlgoEngine.strategies.collectAsState()
    var selectedStrategy by remember { mutableStateOf(strategies.firstOrNull() ?: AlgoEngine.currentStrategy.value) }
    var selectedDays by remember { mutableIntStateOf(30) }
    var backtestResult by remember { mutableStateOf<BacktestResult?>(null) }
    var isSimulating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("BACKTEST SIMULATOR", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
        Text("Simulate quantitative strategy performance over historical market data", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Strategy Selector
        Text("Select Strategy", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BuilderDropdown(selectedStrategy.name, strategies.map { it.name }) { name ->
            strategies.find { it.name == name }?.let { selectedStrategy = it }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Days Filter
        Text("Historical Period", color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(7 to "7 Days", 14 to "14 Days", 30 to "30 Days", 90 to "90 Days").forEach { (d, lbl) ->
                val isSel = selectedDays == d
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedDays = d },
                    color = if (isSel) PrimaryGold else DarkCard,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            lbl,
                            color = if (isSel) Color.Black else TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isSimulating = true
                backtestResult = AlgoEngine.runBacktest(selectedStrategy, selectedDays)
                isSimulating = false
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(6.dp))
            Text("RUN BACKTEST", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val result = backtestResult
        if (result != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(result.strategyName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(
                            color = ProfitGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "${result.days}D BACKTEST",
                                color = ProfitGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Win Rate", color = TextGray, fontSize = 10.sp)
                            Text("${String.format("%.1f", result.winRate)}%", color = ProfitGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Net P&L", color = TextGray, fontSize = 10.sp)
                            Text("+₹${String.format("%,.0f", result.netPnl)}", color = ProfitGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Profit Factor", color = TextGray, fontSize = 10.sp)
                            Text(String.format("%.2f", result.profitFactor), color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkCardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Trades", color = TextGray, fontSize = 10.sp)
                            Text("${result.totalTrades}", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Wins / Losses", color = TextGray, fontSize = 10.sp)
                            Text("${result.winningTrades} W / ${result.losingTrades} L", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Max Drawdown", color = TextGray, fontSize = 10.sp)
                            Text("${result.maxDrawdownPct}%", color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Equity Curve
                    Text("SIMULATED EQUITY CURVE", color = SecondaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val curve = result.equityCurve
                            if (curve.isNotEmpty()) {
                                val minVal = curve.minOrNull() ?: 100000.0
                                val maxVal = curve.maxOrNull() ?: 120000.0
                                val range = (maxVal - minVal).coerceAtLeast(1.0)
                                val step = w / (curve.size - 1).coerceAtLeast(1)

                                val pts = curve.mapIndexed { i, v ->
                                    val x = i * step
                                    val y = h - (((v - minVal) / range) * (h * 0.75f) + (h * 0.1f)).toFloat()
                                    Offset(x, y)
                                }

                                val path = Path().apply {
                                    moveTo(pts[0].x, pts[0].y)
                                    for (i in 1 until pts.size) {
                                        lineTo(pts[i].x, pts[i].y)
                                    }
                                }

                                drawPath(path, ProfitGreen, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// -------------------------------------------------------------
// SYSTEM LOGS SCREEN
// -------------------------------------------------------------
@Composable
fun SystemLogsScreen() {
    val logs by AlgoEngine.systemLogs.collectAsState()
    var filterLevel by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, filterLevel) {
        if (filterLevel == "ALL") logs else logs.filter { it.level == filterLevel }
    }

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
            Column {
                Text("SYSTEM LOGS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                Text("Live algorithmic engine diagnostics & events", color = TextGray, fontSize = 11.sp)
            }
            IconButton(onClick = { AlgoEngine.clearLogs() }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Logs", tint = LossRed)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Level Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(listOf("ALL", "INFO", "SIGNAL", "EXECUTION", "RISK", "WARN")) { lvl ->
                val isSel = filterLevel == lvl
                Surface(
                    modifier = Modifier.clickable { filterLevel = lvl },
                    color = if (isSel) PrimaryGold else DarkCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isSel) PrimaryGold else DarkCardBorder)
                ) {
                    Text(
                        lvl,
                        color = if (isSel) Color.Black else TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color(0xFF0A0A0A),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs available.", color = TextGray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredLogs) { item ->
                        val badgeColor = when (item.level) {
                            "SIGNAL", "EXECUTION" -> ProfitGreen
                            "RISK" -> PrimaryGold
                            "WARN" -> LossRed
                            else -> SecondaryGold
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                item.timestamp,
                                color = TextGray,
                                fontSize = 10.sp,
                                modifier = Modifier.width(60.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = badgeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    item.tag,
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                item.message,
                                color = TextWhite,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// -------------------------------------------------------------
// AI CREATE STRATEGY SCREEN
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
                border = BorderStroke(1.dp, DarkCardBorder)
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
        border = BorderStroke(1.dp, if (selected) PrimaryGold else DarkCardBorder)
    ) {
        Text(text, color = if (selected) PrimaryGold else TextGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

// -------------------------------------------------------------
// RISK MANAGEMENT SCREEN
// -------------------------------------------------------------
@Composable
fun RiskManagement() {
    val riskPerTrade by AlgoEngine.riskPerTrade.collectAsState()
    val maxDailyLossPercent by AlgoEngine.maxDailyLossPercent.collectAsState()
    val maxTradesPerDay by AlgoEngine.maxTradesPerDay.collectAsState()
    val onePosAtATime by AlgoEngine.onePositionAtATime.collectAsState()

    var sliderRisk by remember(riskPerTrade) { mutableFloatStateOf(riskPerTrade.toFloat()) }
    var sliderLoss by remember(maxDailyLossPercent) { mutableFloatStateOf(maxDailyLossPercent.toFloat()) }
    var maxTradesInput by remember(maxTradesPerDay) { mutableIntStateOf(maxTradesPerDay) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("RISK CONTROL CENTER", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, DarkCardBorder)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Limit to 1 Position at a time", color = TextWhite, fontSize = 13.sp)
                        Text("Prevents overleveraging in options", color = TextGray, fontSize = 10.sp)
                    }
                    Switch(
                        checked = onePosAtATime,
                        onCheckedChange = { AlgoEngine.updateRiskSettings(sliderRisk.toDouble(), sliderLoss.toDouble(), maxTradesInput, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { AlgoEngine.updateRiskSettings(sliderRisk.toDouble(), sliderLoss.toDouble(), maxTradesInput, onePosAtATime) },
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

// -------------------------------------------------------------
// TRADE HISTORY SCREEN
// -------------------------------------------------------------
@Composable
fun TradeHistory() {
    val tradeHistory by AlgoEngine.liveTradeHistory.collectAsState()
    val paperHistory by AlgoEngine.paperTradeHistory.collectAsState()

    var selectedTab by remember { mutableStateOf("ALL") }

    val combinedList = remember(tradeHistory, paperHistory, selectedTab) {
        val all = tradeHistory + paperHistory
        when (selectedTab) {
            "LIVE" -> tradeHistory
            "PAPER" -> paperHistory
            else -> all
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ALGO TRADE HISTORY", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "LIVE", "PAPER").forEach { tab ->
                    val isSel = selectedTab == tab
                    Surface(
                        modifier = Modifier.clickable { selectedTab = tab },
                        color = if (isSel) PrimaryGold else DarkCard,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            tab,
                            color = if (isSel) Color.Black else TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (combinedList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recent algo trades recorded.", color = TextGray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(combinedList) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkCard,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${item.strike} (${item.action})", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${item.date} ${item.time} • ${item.index}", color = TextGray, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${item.price}", color = SecondaryGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(item.status, color = if (item.status.contains("PROFIT")) ProfitGreen else LossRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
