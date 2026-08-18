package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkCard
import com.example.ui.theme.LossRed

@Composable
fun ApiErrorBanner(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (errorMessage.isBlank() ||
        errorMessage.contains("missing", ignoreCase = true) ||
        errorMessage.contains("pnl", ignoreCase = true) ||
        errorMessage.contains("Required value", ignoreCase = true) ||
        errorMessage.contains("BEGIN_OBJECT", ignoreCase = true) ||
        errorMessage.contains("STRING", ignoreCase = true) ||
        errorMessage.contains("JsonDataException", ignoreCase = true)
    ) {
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = LossRed.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LossRed.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "API Error",
                    tint = LossRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Broker API Connection Error",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LossRed
                    )
                    Text(
                        text = errorMessage,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry API",
                    tint = LossRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
