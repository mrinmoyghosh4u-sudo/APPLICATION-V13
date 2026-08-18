import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/IndexDetailsScreen.kt', 'r') as f:
    content = f.read()

# Add userProfile to OptionChainTabContent
new_content = content.replace(
    'val strikes by viewModel.optionStrikes.collectAsStateWithLifecycle()',
    'val strikes by viewModel.optionStrikes.collectAsStateWithLifecycle()\n    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()'
)

replacement = """        // Table Content
        if (!userProfile.isAngelConnected && !userProfile.isDhanConnected) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Connect your broker account to load live Option Chain data.",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
        } else if (strikes.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Option Chain unavailable for this instrument",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
        } else {"""

new_content = new_content.replace(
    """        // Table Content
        if (strikes.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Option Chain unavailable for this instrument",
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
        } else {""", replacement)

with open('/app/applet/app/src/main/java/com/example/ui/screens/IndexDetailsScreen.kt', 'w') as f:
    f.write(new_content)

print("Patched!")
