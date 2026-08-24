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
        dataSource.contains("FYERS", ignoreCase = true) -> "FYERS"
        dataSource.contains("ANGEL", ignoreCase = true) -> "ANGEL ONE"
        dataSource.contains("M.STOCK", ignoreCase = true) || dataSource.contains("MSTOCK", ignoreCase = true) -> "m.STOCK"
        else -> ""
    }
    val isStale = dataSource.contains("STALE", ignoreCase = true)
    val badgeText = when {
        isLive && cleanSource.isNotBlank() -> "LIVE • $cleanSource"
        isLive -> "LIVE"
        isStale -> "STALE DATA"
        dataSource.contains("CONNECTING", ignoreCase = true) -> "CONNECTING"
        else -> "REAL MARKET DATA UNAVAILABLE"
    }
    val activeColor = when {
        isLive -> ProfitGreen
        isStale -> SecondaryGold
        else -> TextGray
    }

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(16.dp),
        color = if (isLive) activeColor.copy(alpha = 0.15f) else DarkCardSecondary,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isLive) activeColor.copy(alpha = 0.6f) else DarkCardBorder
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = activeColor,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badgeText,
                color = activeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun CrownLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(GoldGlow.copy(alpha = 0.5f), Color.Transparent)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_king_khan_logo),
            contentDescription = "King Khan AI Trade Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun GoldCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DarkCardBorder,
    borderWidth: Dp = 1.dp,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    backgroundColor: Color = DarkCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, shape)
            .clip(shape),
        color = backgroundColor,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(8.dp, RoundedCornerShape(8.dp), spotColor = PrimaryGold),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGold,
            contentColor = Color.Black
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun SparklineChart(
    isPositive: Boolean,
    modifier: Modifier = Modifier.size(width = 60.dp, height = 24.dp)
) {
    val color = if (isPositive) ProfitGreen else LossRed
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val points = if (isPositive) {
            listOf(0.8f, 0.7f, 0.75f, 0.5f, 0.6f, 0.3f, 0.1f)
        } else {
            listOf(0.2f, 0.3f, 0.25f, 0.5f, 0.4f, 0.7f, 0.9f)
        }

        val path = Path()
        points.forEachIndexed { index, yRatio ->
            val x = (index.toFloat() / (points.size - 1)) * w
            val y = yRatio * h
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun DonutChart(
    equity: Float = 72.35f,
    derivatives: Float = 18.42f,
    commodity: Float = 7.85f,
    cash: Float = 1.38f,
    modifier: Modifier = Modifier.size(130.dp)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 22.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f

            // Equity (Gold/Yellow)
            val equityAngle = (equity / 100f) * 360f
            drawArc(
                color = SecondaryGold,
                startAngle = startAngle,
                sweepAngle = equityAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth)
            )
            startAngle += equityAngle

            // Derivatives (Green)
            val derivAngle = (derivatives / 100f) * 360f
            drawArc(
                color = ProfitGreen,
                startAngle = startAngle,
                sweepAngle = derivAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth)
            )
            startAngle += derivAngle

            // Commodity (Blue)
            val commAngle = (commodity / 100f) * 360f
            drawArc(
                color = Color(0xFF29B6F6),
                startAngle = startAngle,
                sweepAngle = commAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth)
            )
            startAngle += commAngle

            // Cash (Purple)
            val cashAngle = (cash / 100f) * 360f
            drawArc(
                color = Color(0xFFAB47BC),
                startAngle = startAngle,
                sweepAngle = cashAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOTAL", fontSize = 10.sp, color = TextGray)
            Text("₹2.48L", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
    }
}

@Composable
fun CircularGauge(
    percentage: Int = 87,
    modifier: Modifier = Modifier.size(72.dp)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            // Background arc
            drawArc(
                color = DarkCardBorder,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth)
            )

            // Active green arc
            val sweep = (percentage / 100f) * 360f
            drawArc(
                color = ProfitGreen,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth)
            )
        }

        Text(
            text = "$percentage%",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ProfitGreen
        )
    }
}
