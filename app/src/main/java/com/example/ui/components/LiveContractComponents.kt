package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.screens.ContractItemData
import com.example.ui.theme.*

@Composable
fun LiveContractRow(
    item: ContractItemData,
    exchange: String,
    onNavigateToIndexDetails: (String, String) -> Unit,
    onOpenOrderDialog: (String, String, Double, Int) -> Unit
) {
    val tick by MarketDataStore.getTickFlow(item.name).collectAsStateWithLifecycle(initialValue = MarketDataStore.getTick(item.name))
    val ltp = tick?.ltp ?: item.price
    val pct = tick?.changePercent ?: item.changePct
    val isPositive = pct >= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13161C), RoundedCornerShape(8.dp))
            .border(0.6.dp, Color(0xFF23272F), RoundedCornerShape(8.dp))
            .clickable { onNavigateToIndexDetails(exchange, item.name) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = item.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "$exchange • Lot: ${item.lotSize}", color = TextGray, fontSize = 9.sp)
        }
        SparklineChart(
            isPositive = isPositive,
            modifier = Modifier.size(width = 50.dp, height = 20.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (ltp > 0.0) "₹${String.format("%,.2f", ltp)}" else "LTP: --",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (ltp > 0.0) "${if (isPositive) "+" else ""}${String.format("%.2f", pct)}%" else "--",
                    color = if (ltp > 0.0) (if (isPositive) ProfitGreen else LossRed) else TextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { onOpenOrderDialog(item.name, "BUY", ltp, item.lotSize) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "TRADE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
