import re

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    text = f.read()

text = text.replace(
    "onNavigateToTelegramSettings: () -> Unit = {},",
    "onNavigateToTelegramSettings: () -> Unit = {},\n    onNavigateToDiagnostics: () -> Unit = {},"
)

button_code = """                OutlinedButton(
                    onClick = onNavigateToTelegramSettings,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TEST TELEGRAM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onNavigateToDiagnostics,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RUN LIVE DATA TEST (DIAGNOSTICS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }
                }"""

text = text.replace("""                OutlinedButton(
                    onClick = onNavigateToTelegramSettings,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TEST TELEGRAM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                    }
                }""", button_code)

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(text)

