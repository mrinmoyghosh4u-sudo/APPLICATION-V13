import re

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'r') as f:
    content = f.read()

pattern = r'// Default mockup signal view if no signal active.*?Text\(\s*"Trailing SL: ₹5\.00 \(1\.5%\)".*?fontSize = 10\.sp\s*\)'

replacement = """// Default mockup signal view if no signal active
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
                        }"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/AlgoScreen.kt', 'w') as f:
    f.write(new_content)
