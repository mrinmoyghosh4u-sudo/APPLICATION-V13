import re

filepath = "app/src/main/java/com/example/ui/components/CommonComponents.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
    }
    val badgeColor = if (isLive) ProfitGreen else Color.Gray
    val badgeText = if (isLive) "LIVE $cleanSource" else "OFFLINE"
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeColor.copy(alpha = 0.2f))
            .border(1.dp, badgeColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(badgeColor))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GoldCard(
    modifier: Modifier = Modifier,
    borderColor: Color = PrimaryGold,
    backgroundColor: Color = DarkCardBackground,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun CrownLogo(size: Dp = 36.dp) {
    Box(modifier = Modifier.size(size)) {
        // Placeholder Crown Logo
    }
}
"""

idx = content.rfind("}")
if idx != -1:
    content = content[:idx] + props
    with open(filepath, "w") as f:
        f.write(content)
