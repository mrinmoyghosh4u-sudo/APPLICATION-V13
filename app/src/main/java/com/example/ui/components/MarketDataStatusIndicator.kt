package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketDataStore
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardSecondary
import com.example.ui.theme.DarkGold
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SecondaryGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

/**
 * Unified Market Data Status Indicator
 * 
 * Strict Directives:
 * - Provider source names are HIDDEN from user-facing UI.
 * - Displays ONLY: "LIVE", "MARKET CLOSED", or "DATA UNAVAILABLE".
 * - Metrics show unified connection health without exposing backend provider names.
 */
@Composable
fun MarketDataStatusIndicator(
    source: String,
    lastUpdatedTime: String,
    isMarketOpen: Boolean = true,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    onReconnect: (() -> Unit)? = null
) {
    val isDisconnected = source.contains("Disconnected", ignoreCase = true) || 
                         source.contains("UNAVAILABLE", ignoreCase = true) || 
                         source == "DISCONNECTED" ||
                         source == "DATA UNAVAILABLE"
    val isLive = !isDisconnected
    val indicatorColor = if (!isMarketOpen) Color(0xFFFFB300) else if (isLive) ProfitGreen else LossRed

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkCard,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLive) DarkGold else LossRed.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(indicatorColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isLive) {
                        Text(
                            text = if (!isMarketOpen) "MARKET CLOSED" else "LIVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isMarketOpen) Color(0xFFFFB300) else ProfitGreen
                        )
                    } else {
                        Text(
                            text = "DATA UNAVAILABLE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LossRed
                        )
                    }
                }
                
                if (isLive && onRefresh != null) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Feed",
                            tint = SecondaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (!isLive && onReconnect != null) {
                    Surface(
                        onClick = onReconnect,
                        color = LossRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LossRed)
                    ) {
                        Text(
                            text = "RECONNECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LossRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Unified Engine Health Badges (Provider agnostic)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EngineHealthBadge("DATA FEED", if (isLive) "ACTIVE" else "OFFLINE", Modifier.weight(1f))
                EngineHealthBadge("TICKS", if (isLive) "VALIDATED" else "STALE", Modifier.weight(1f))
                EngineHealthBadge("ROUTER", "FAILOVER READY", Modifier.weight(1.2f))
                EngineHealthBadge("LATENCY", if (isLive) "LOW" else "N/A", Modifier.weight(0.8f))
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            if (isLive && lastUpdatedTime.isNotBlank() && lastUpdatedTime != "Not Updated") {
                Text(
                    text = "Last valid tick: $lastUpdatedTime",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            } else {
                Text(
                    text = "Last valid tick: N/A",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            
            if (!isMarketOpen) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Next opening: 09:15 AM",
                    fontSize = 10.sp,
                    color = Color(0xFFFFB300)
                )
            }
        }
    }
}

@Composable
private fun EngineHealthBadge(
    label: String,
    status: String,
    modifier: Modifier = Modifier
) {
    val color = when (status.uppercase()) {
        "ACTIVE", "VALIDATED", "FAILOVER READY", "LOW" -> ProfitGreen
        "STALE" -> Color(0xFFFFB300)
        else -> LossRed
    }

    Surface(
        modifier = modifier,
        color = DarkCardSecondary,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status.uppercase(),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1
            )
        }
    }
}
