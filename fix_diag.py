import re

with open('app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt', 'r') as f:
    text = f.read()

text = text.replace(
    'import androidx.lifecycle.compose.collectAsStateWithLifecycle\nimport com.example.viewmodel.MainViewModel',
    'import androidx.lifecycle.compose.collectAsStateWithLifecycle\nimport com.example.viewmodel.MainViewModel\nimport com.example.data.model.MarketDataStore\nimport java.text.SimpleDateFormat\nimport java.util.Date\nimport java.util.Locale'
)

new_code = """
        Spacer(modifier = Modifier.height(24.dp))
        
        val marketData = MarketDataStore.marketData.collectAsStateWithLifecycle().value
        if (marketData.isNotEmpty()) {
            Text("RECEIVED TICKS:", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            marketData.entries.take(10).forEach { (token, data) ->
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.timestamp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.DarkGray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Symbol: ${data.symbol}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Token: $token", color = Color.LightGray, fontSize = 12.sp)
                    Text("LTP: ${data.ltp}", color = Color(0xFF00C853), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Timestamp: $timeStr", color = Color.LightGray, fontSize = 12.sp)
                    Text("Connection State: $connectionState", color = Color.LightGray, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!hasFirstTick) {
"""

text = text.replace('        Spacer(modifier = Modifier.height(24.dp))\n        \n        if (!hasFirstTick) {', new_code)

with open('app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt', 'w') as f:
    f.write(text)

