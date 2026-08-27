package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class CandleData(
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float
)

@Composable
fun CandlestickChart(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(260.dp),
    candles: List<CandleData> = emptyList(),
    currentPrice: Float? = null
) {
    if (candles.isEmpty()) {
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(
                text = "CHART DATA UNAVAILABLE",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val validCandles = candles.filter { it.high > 0f && it.low > 0f && it.open > 0f && it.close > 0f }
    if (validCandles.isEmpty()) {
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(
                text = "CHART DATA UNAVAILABLE",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val minPrice = validCandles.minOf { it.low }
    val maxPrice = validCandles.maxOf { it.high }
    val rawRange = maxPrice - minPrice
    val effRange = if (rawRange <= 0.0001f) (minPrice * 0.01f).coerceAtLeast(1f) else rawRange
    val yMin = minPrice - (effRange * 0.05f)
    val yMax = maxPrice + (effRange * 0.05f)
    val span = (yMax - yMin).coerceAtLeast(0.0001f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val chartHeight = h * 0.78f
        val volumeHeight = h * 0.18f
        val volumeTop = h * 0.82f

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = (chartHeight / gridLines) * i
            drawLine(
                color = Color(0xFF262626),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        val count = validCandles.size
        val slotWidth = w / count.coerceAtLeast(1)
        val candleWidth = (slotWidth * 0.65f).coerceIn(2f, 24f)

        val maxVol = validCandles.maxOfOrNull { it.volume }?.takeIf { it > 0f } ?: 1f

        validCandles.forEachIndexed { i, candle ->
            val isGreen = candle.close >= candle.open
            val color = if (isGreen) ProfitGreen else LossRed

            val xCenter = (i + 0.5f) * slotWidth

            // Price Y mapping (safe against division by zero)
            val highY = chartHeight - ((candle.high - yMin) / span) * chartHeight
            val lowY = chartHeight - ((candle.low - yMin) / span) * chartHeight
            val openY = chartHeight - ((candle.open - yMin) / span) * chartHeight
            val closeY = chartHeight - ((candle.close - yMin) / span) * chartHeight

            // Draw wick
            drawLine(
                color = color,
                start = Offset(xCenter, highY.coerceIn(0f, chartHeight)),
                end = Offset(xCenter, lowY.coerceIn(0f, chartHeight)),
                strokeWidth = 2f
            )

            // Draw body
            val topBodyY = minOf(openY, closeY).coerceIn(0f, chartHeight)
            val bottomBodyY = maxOf(openY, closeY).coerceIn(0f, chartHeight)
            val bodyHeight = maxOf(bottomBodyY - topBodyY, 2f)

            drawRect(
                color = color,
                topLeft = Offset(xCenter - candleWidth / 2f, topBodyY),
                size = Size(candleWidth, bodyHeight)
            )

            // Draw volume bar
            if (maxVol > 0f && candle.volume > 0f) {
                val volRatio = (candle.volume / maxVol).coerceIn(0f, 1f)
                val volBarHeight = volRatio * volumeHeight
                drawRect(
                    color = color.copy(alpha = 0.45f),
                    topLeft = Offset(xCenter - candleWidth / 2f, h - volBarHeight),
                    size = Size(candleWidth, volBarHeight)
                )
            }
        }

        // Draw current live price dashed reference line
        if (currentPrice != null && currentPrice > 0f) {
            val currentY = chartHeight - ((currentPrice - yMin) / span) * chartHeight
            if (currentY in 0f..chartHeight) {
                drawLine(
                    color = SecondaryGold,
                    start = Offset(0f, currentY),
                    end = Offset(w, currentY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )
            }
        }
    }
}
