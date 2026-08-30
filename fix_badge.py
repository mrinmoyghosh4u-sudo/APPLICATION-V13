import re

filepath = "app/src/main/java/com/example/ui/components/CommonComponents.kt"
with open(filepath, "r") as f:
    content = f.read()

start_idx = content.find("fun LiveStatusBadge(")
if start_idx != -1:
    new_badge = """fun LiveStatusBadge(
    isLive: Boolean,
    dataSource: String = "",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cleanSource = when {
        dataSource.contains("UPSTOX", ignoreCase = true) -> "UPSTOX"
        dataSource.contains("FYERS", ignoreCase = true) -> "FYERS"
        dataSource.contains("ANGEL", ignoreCase = true) -> "ANGEL ONE"
        else -> dataSource
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
"""
    # Replace from start_idx to the @Composable before GoldCard, or just replace the whole function
    end_idx = content.find("@Composable\nfun GoldCard", start_idx)
    content = content[:start_idx] + new_badge + "\n" + content[end_idx:]
    with open(filepath, "w") as f:
        f.write(content)
