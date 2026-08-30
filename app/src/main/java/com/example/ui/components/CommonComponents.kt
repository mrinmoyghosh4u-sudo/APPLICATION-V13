package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun KingKhanTagline(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Trade ",
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = ProfitGreen
        )
        Text(
            text = "Like a ",
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = Color.White
        )
        Text(
            text = "King ",
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = LossRed
        )
        Text(
            text = "👑",
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun KingKhanHeaderBrand(
    modifier: Modifier = Modifier,
    logoSize: Dp = 36.dp,
    titleSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    taglineSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    showLogo: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            CrownLogo(size = logoSize)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column {
            Text(
                text = "KING KHAN AI TRADE",
                fontSize = titleSize,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
            KingKhanTagline(fontSize = taglineSize)
        }
    }
}

@Composable
fun LiveStatusBadge(
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

@Composable
fun GoldCard(
    modifier: Modifier = Modifier,
    borderColor: Color = PrimaryGold,
    backgroundColor: Color = Color(0xFF1E1E1E),
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
    Image(
        painter = painterResource(id = R.drawable.ic_king_khan_logo),
        contentDescription = "King Khan Logo",
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun SparklineChart(isPositive: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(if (isPositive) ProfitGreen.copy(alpha = 0.2f) else LossRed.copy(alpha = 0.2f)))
}
