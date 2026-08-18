import re

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    text = f.read()

btn_pattern = """                Spacer\(modifier = Modifier\.height\(10\.dp\)\)\n                OutlinedButton\(\n                    onClick = onNavigateToDiagnostics,\n                    modifier = Modifier\.fillMaxWidth\(\)\.height\(38\.dp\),\n                    shape = RoundedCornerShape\(6\.dp\),\n                    border = androidx\.compose\.foundation\.BorderStroke\(1\.dp, PrimaryGold\)\n                \) \{\n                    Row\(verticalAlignment = Alignment\.CenterVertically\) \{\n                        Icon\(Icons\.Default\.Settings, contentDescription = null, tint = SecondaryGold, modifier = Modifier\.size\(16\.dp\)\)\n                        Spacer\(modifier = Modifier\.width\(8\.dp\)\)\n                        Text\("RUN LIVE DATA TEST \(DIAGNOSTICS\)", fontSize = 12\.sp, fontWeight = FontWeight\.Bold, color = SecondaryGold\)\n                    \}\n                \}"""

text = re.sub(btn_pattern + r'\s*' + btn_pattern, btn_pattern, text)

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(text)
