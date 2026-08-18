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

    val minPrice = candles.minOfOrNull { it.low } ?: 0f
    val maxPrice = candles.maxOfOrNull { it.high } ?: 1f
    // add small padding to min/max
    val range = maxPrice - minPrice
    val yMin = minPrice - (range * 0.05f)
    val yMax = maxPrice + (range * 0.05f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val chartHeight = h * 0.75f
        val volumeHeight = h * 0.20f
        val volumeTop = h * 0.80f

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = (chartHeight / gridLines) * i
            drawLine(
                color = Color(0xFF222222),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        val candleWidth = w / (candles.size * 1.5f)
        val candleSpacing = candleWidth * 0.5f

        val maxVol = candles.maxOfOrNull { it.volume }?.takeIf { it > 0 } ?: 1f

        candles.forEachIndexed { i, candle ->
            val isGreen = candle.close >= candle.open
            val color = if (isGreen) ProfitGreen else LossRed

            val x = i * (candleWidth + candleSpacing) + candleWidth / 2

            // Price Y mapping
            val highY = chartHeight - ((candle.high - yMin) / (yMax - yMin)) * chartHeight
            val lowY = chartHeight - ((candle.low - yMin) / (yMax - yMin)) * chartHeight
            val openY = chartHeight - ((candle.open - yMin) / (yMax - yMin)) * chartHeight
            val closeY = chartHeight - ((candle.close - yMin) / (yMax - yMin)) * chartHeight

            // Draw wick
            drawLine(
                color = color,
                start = Offset(x, highY),
                end = Offset(x, lowY),
                strokeWidth = 2f
            )

            // Draw body
            val topBodyY = minOf(openY, closeY)
            val bodyHeight = maxOf(Math.abs(openY - closeY), 4f)

            drawRect(
                color = color,
                topLeft = Offset(x - candleWidth / 2, topBodyY),
                size = Size(candleWidth, bodyHeight)
            )

            // Draw volume bar
            val volBarHeight = (candle.volume / maxVol) * volumeHeight
            drawRect(
                color = color.copy(alpha = 0.5f),
                topLeft = Offset(x - candleWidth / 2, h - volBarHeight),
                size = Size(candleWidth, volBarHeight)
            )
        }

        // Draw current price line if provided and within range
        if (currentPrice != null) {
            val currentY = chartHeight - ((currentPrice - yMin) / (yMax - yMin)) * chartHeight
            if (currentY in 0f..chartHeight) {
                drawLine(
                    color = ProfitGreen, // or determine color based on change
                    start = Offset(0f, currentY),
                    end = Offset(w, currentY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }
        }
    }
}
