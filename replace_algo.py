import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

# We need to replace AlgoDashboard and below until AiCreateStrategy
start_idx = content.find('@Composable\nfun AlgoDashboard(onNavigate:')
end_idx = content.find('@Composable\nfun AiCreateStrategy(onNavigate:')

new_dashboard = """@Composable
fun AlgoDashboard(onNavigate: (AlgoScreenState) -> Unit) {
    var isAlgoActive by remember { mutableStateOf(AlgoEngine.isAlgoRunning) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // ALGO STATUS CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("ALGO ENGINE", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(if (isAlgoActive) ProfitGreen else LossRed, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAlgoActive) "ON" else "OFF", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Current Strategy", color = TextGray, fontSize = 10.sp)
                            Text("KK BUY-ONLY AI", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Index", color = TextGray, fontSize = 10.sp)
                            Text("NIFTY 50", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Option Mode", color = TextGray, fontSize = 10.sp)
                            Text("AUTO CE / PE", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Trading Mode", color = TextGray, fontSize = 10.sp)
                            Text("PAPER", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isAlgoActive) {
                        Button(
                            onClick = { 
                                AlgoEngine.emergencyStop()
                                isAlgoActive = false 
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LossRed)
                        ) {
                            Text("STOP ALGO", color = LossRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { 
                                AlgoEngine.toggleAlgo(true)
                                isAlgoActive = true 
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen)
                        ) {
                            Text("START ALGO", color = ProfitGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        
        item {
            // PERFORMANCE SUMMARY (2x2 Grid)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlgoStatBox("TODAY P&L", "₹0.00", TextWhite, Modifier.weight(1f))
                    AlgoStatBox("TRADES", "0", TextWhite, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlgoStatBox("WIN RATE", "0%", TextWhite, Modifier.weight(1f))
                    AlgoStatBox("ACTIVE", "NONE", TextGray, Modifier.weight(1f))
                }
            }
        }

        item {
            // CURRENT SIGNAL CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold.copy(alpha=0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CURRENT SIGNAL", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("NO ACTIVE SIGNAL", color = TextGray, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Waiting for market confirmation...", color = TextGray, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            // MARKET BIAS & CE/PE DECISION
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // MARKET BIAS
                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("MARKET BIAS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("NEUTRAL", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val indicators = listOf("EMA", "VWAP", "RSI", "SUPERTREND", "OI", "VOLUME")
                        indicators.forEach { ind ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ind, color = TextGray, fontSize = 9.sp)
                                Text("✓", color = TextGray, fontSize = 9.sp)
                            }
                        }
                    }
                }
                
                // CE / PE DECISION
                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("AI OPTION DECISION", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CE BUY", color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("50%", color = ProfitGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PE BUY", color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("50%", color = LossRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("NO TRADE", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }

        item {
            // ACTIVE STRATEGY & AUTO TRADING
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // ACTIVE STRATEGY
                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ACTIVE STRATEGY", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("KK BUY-ONLY AI", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Idx: NIFTY 50", color = TextGray, fontSize = 10.sp)
                        Text("TF: 5 MIN", color = TextGray, fontSize = 10.sp)
                        Text("Mode: AUTO", color = TextGray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = {}, modifier = Modifier.weight(1f).height(24.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = DarkCardSecondary), shape = RoundedCornerShape(4.dp)) {
                                Text("EDIT", color = TextWhite, fontSize = 8.sp)
                            }
                            Button(onClick = {}, modifier = Modifier.weight(1f).height(24.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = DarkCardSecondary), shape = RoundedCornerShape(4.dp)) {
                                Text("PAUSE", color = TextWhite, fontSize = 8.sp)
                            }
                        }
                    }
                }

                // AUTO TRADING STATUS
                Surface(
                    modifier = Modifier.weight(1f),
                    color = DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("AUTO TRADING", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.size(6.dp).background(LossRed, CircleShape))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Order Mode: BUY ONLY", color = TextWhite, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✓ BUY CE", color = ProfitGreen, fontSize = 10.sp)
                        Text("✓ BUY PE", color = ProfitGreen, fontSize = 10.sp)
                        Text("✕ SELL CE", color = LossRed, fontSize = 10.sp)
                        Text("✕ SELL PE", color = LossRed, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            // RISK CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RISK MANAGEMENT", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Risk / Trade:", color = TextGray, fontSize = 10.sp)
                            Text("1%", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Max Trades:", color = TextGray, fontSize = 10.sp)
                            Text("5", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Daily Loss:", color = TextGray, fontSize = 10.sp)
                            Text("₹0.00", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Max Daily Loss:", color = TextGray, fontSize = 10.sp)
                            Text("3%", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Open Positions:", color = TextGray, fontSize = 10.sp)
                            Text("0 / 1", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Status:", color = TextGray, fontSize = 10.sp)
                            Text("SAFE", color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        item {
            // QUICK ACTIONS
            Text("QUICK ACTIONS", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlgoQuickActionTile("AI CREATE", Modifier.weight(1f)) { onNavigate(AlgoScreenState.AI_CREATE) }
                    AlgoQuickActionTile("STRATEGY BUILDER", Modifier.weight(1f)) { onNavigate(AlgoScreenState.STRATEGY_BUILDER) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlgoQuickActionTile("MY STRATEGIES", Modifier.weight(1f)) { onNavigate(AlgoScreenState.MY_STRATEGIES) }
                    AlgoQuickActionTile("PAPER TRADING", Modifier.weight(1f)) { onNavigate(AlgoScreenState.PAPER_TRADING) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlgoQuickActionTile("AUTO TRADING", Modifier.weight(1f)) { onNavigate(AlgoScreenState.AUTO_TRADING) }
                    AlgoQuickActionTile("RISK CONTROL", Modifier.weight(1f)) { onNavigate(AlgoScreenState.RISK_MANAGEMENT) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlgoQuickActionTile("PERFORMANCE", Modifier.weight(1f)) { onNavigate(AlgoScreenState.PERFORMANCE) }
                    AlgoQuickActionTile("TRADE HISTORY", Modifier.weight(1f)) { onNavigate(AlgoScreenState.TRADE_HISTORY) }
                }
            }
        }

        item {
            // RECENT ALGO ACTIVITY
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RECENT ACTIVITY", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No recent Algo activity.", color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AlgoStatBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextGray, fontSize = 10.sp, maxLines = 1, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun AlgoQuickActionTile(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = DarkCardSecondary,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Box(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(text, color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
"""

content = content[:start_idx] + new_dashboard + content[end_idx:]

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(content)

