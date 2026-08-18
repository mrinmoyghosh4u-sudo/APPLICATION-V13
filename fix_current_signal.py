import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    lines = f.readlines()

out = []
in_func = False
for i, line in enumerate(lines):
    if line.startswith('fun CurrentSignalDetailScreen(onBack: () -> Unit) {'):
        in_func = True
        out.append("""@Composable
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
""")
        continue
        
    if in_func:
        if line.startswith('@Composable') and 'fun SignalDetailRow' in lines[i+1]:
            in_func = False
            out.append(line)
        elif line.startswith('fun SignalDetailRow'):
            in_func = False
            out.append(line)
        else:
            continue
    else:
        # Avoid duplicate printing of @Composable for CurrentSignalDetailScreen
        if line.startswith('@Composable') and 'fun CurrentSignalDetailScreen' in lines[i+1]:
            continue
        out.append(line)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.writelines(out)
