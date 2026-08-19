package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel
import com.example.data.model.MarketDataStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.brokerManager.angelMarketDataService.connectionState.collectAsStateWithLifecycle()
    val isMasterLoaded = viewModel.brokerManager.instrumentMasterService.isLoaded
    val authStatus = if (viewModel.sessionManager.angelJwtToken.isNullOrEmpty()) "FAIL" else "PASS"
    val feedTokenStatus = if (viewModel.sessionManager.angelFeedToken.isNullOrEmpty()) "FAIL" else "PASS"

    val niftyToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("NIFTY 50")
    val bankNiftyToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("BANKNIFTY")
    val finNiftyToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("FINNIFTY")
    val midcpNiftyToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("MIDCPNIFTY")

    val hasFirstTick = viewModel.brokerManager.angelMarketDataService.isConnectionLive()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("RUNTIME DIAGNOSTICS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        DiagnosticItem("Angel Authentication", authStatus)
        DiagnosticItem("Feed Token", feedTokenStatus)
        DiagnosticItem("Client Code", if (viewModel.sessionManager.angelClientId.isNullOrBlank()) "FAIL" else "PASS")
        DiagnosticItem("API Key", if (viewModel.sessionManager.angelApiKey.isNullOrBlank()) "FAIL" else "PASS")
        DiagnosticItem("Instrument Master", if (isMasterLoaded) "PASS" else "FAIL")
        DiagnosticItem("NIFTY Token", if (niftyToken != null) "RESOLVED (${niftyToken.token})" else "FAILED")
        DiagnosticItem("BANKNIFTY Token", if (bankNiftyToken != null) "RESOLVED (${bankNiftyToken.token})" else "FAILED")
        DiagnosticItem("FINNIFTY Token", if (finNiftyToken != null) "RESOLVED (${finNiftyToken.token})" else "FAILED")
        DiagnosticItem("MIDCPNIFTY Token", if (midcpNiftyToken != null) "RESOLVED (${midcpNiftyToken.token})" else "FAILED")
        DiagnosticItem("WebSocket", connectionState)
        DiagnosticItem("Subscription", if (connectionState == "LIVE" || connectionState == "SUBSCRIBING") "PASS" else "FAIL")
        DiagnosticItem("First Real Tick", if (hasFirstTick) "YES" else "NO")


        Spacer(modifier = Modifier.height(24.dp))
        
        val marketData = MarketDataStore.marketData.collectAsStateWithLifecycle().value
        if (marketData.isNotEmpty()) {
            Text("RECEIVED TICKS:", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            marketData.entries.take(10).forEach { (token, data) ->
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.receivedTimestamp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.DarkGray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Symbol: ${data.symbol}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Source: ${data.source} | Token: $token", color = Color.LightGray, fontSize = 12.sp)
                    Text("LTP: ${data.ltp}", color = Color(0xFF00C853), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Timestamp: $timeStr", color = Color.LightGray, fontSize = 12.sp)
                    Text("State: ${data.state} | Connection: $connectionState", color = Color.LightGray, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!hasFirstTick) {

            Text(
                text = "REAL MARKET DATA NOT YET RECEIVED",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Text(
                text = "REAL BROKER DATA VERIFIED: PASS",
                color = Color(0xFF00C853), // Profit Green
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun DiagnosticItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.DarkGray, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 14.sp)
        val color = when {
            value.contains("PASS") || value.contains("RESOLVED") || value == "YES" || value == "LIVE" || value == "CONNECTED" -> Color(0xFF00C853)
            value.contains("FAIL") || value == "NO" || value == "DISCONNECTED" || value == "ERROR" -> Color(0xFFFF5252)
            else -> Color.White
        }
        Text(text = value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
