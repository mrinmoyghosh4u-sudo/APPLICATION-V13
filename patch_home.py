import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'        Spacer\(modifier = Modifier.height\(14.dp\)\)\n\s+Row\(\n\s+modifier = Modifier.fillMaxWidth\(\).padding\(bottom = 12.dp\),\n\s+verticalAlignment = Alignment.CenterVertically\n\s+\) \{\n\s+Icon\(Icons.Default.AccountBalance, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size\(20.dp\)\)\n\s+Spacer\(modifier = Modifier.width\(8.dp\)\)\n\s+Text\(\n\s+text = selectedExchange,\n\s+fontSize = 16.sp,\n\s+fontWeight = FontWeight.Bold,\n\s+color = TextWhite\n\s+\)\n\s+\}')

new_content = pattern.sub('', content)

if new_content != content:
    with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
        f.write(new_content)
    print("HomeScreen Patched!")
else:
    print("HomeScreen Patch failed!")

