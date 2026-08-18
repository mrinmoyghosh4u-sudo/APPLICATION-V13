package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

import androidx.compose.ui.platform.LocalContext
import com.example.util.update.UpdateManager

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (!pInfo.versionName.isNullOrBlank()) pInfo.versionName else com.example.BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            com.example.BuildConfig.VERSION_NAME
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkCardSecondary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SecondaryGold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("ABOUT KING KHAN AI TRADE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("King Khan AI Algorithmic Platform", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                AboutRowItem("Version", "v$versionName", isHighlight = false)
                AboutRowItem("Trading Mode", "OPTIONS BUYER ONLY", isHighlight = true)
                AboutRowItem("Supported Brokers", "Dhan, Angel One", isHighlight = true)
                AboutRowItem("Supported Markets", "NSE, BSE, MCX", isHighlight = true)
                AboutRowItem("Developer", "KING KHAN AI TRADE", isHighlight = true)

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AboutRowItem(label: String, value: String, isHighlight: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextGray)
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) ProfitGreen else TextWhite
        )
    }
}
