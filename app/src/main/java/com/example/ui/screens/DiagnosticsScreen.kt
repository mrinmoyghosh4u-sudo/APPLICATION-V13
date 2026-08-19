package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
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
import com.example.data.model.MarketDataSourceNames
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val angelConnectionState by viewModel.brokerManager.angelMarketDataService.connectionState.collectAsStateWithLifecycle()
    val mStockConnectionState by viewModel.brokerManager.mStockMarketDataService.connectionState.collectAsStateWithLifecycle()
    val angelHealth by MarketDataStore.angelOneHealth.collectAsStateWithLifecycle()
    val mStockHealth by MarketDataStore.mStockHealth.collectAsStateWithLifecycle()
    val yahooHealth by MarketDataStore.yahooHealth.collectAsStateWithLifecycle()
    
    val isMasterLoaded = viewModel.brokerManager.instrumentMasterService.isLoaded
    val angelAuthStatus = if (viewModel.sessionManager.angelJwtToken.isNullOrEmpty()) "FAIL (Unauthenticated)" else "PASS (Authenticated)"
    val angelFeedTokenStatus = if (viewModel.sessionManager.angelFeedToken.isNullOrEmpty()) "FAIL" else "PASS"
    val angelClientIdStatus = if (viewModel.sessionManager.angelClientId.isNullOrBlank()) "FAIL" else "PASS"
    val angelApiKeyStatus = if (viewModel.sessionManager.angelApiKey.isNullOrBlank()) "FAIL" else "PASS"

    val mstockConfigured = viewModel.sessionManager.isMStockConfigured()
    val dhanConfigured = !viewModel.sessionManager.dhanAccessToken.isNullOrBlank()

    // Core index token resolutions
    val niftyToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("NIFTY 50")
    val bankNiftyToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("BANKNIFTY")
    val finNiftyToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("FINNIFTY")
    val midcpNiftyToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("MIDCPNIFTY")
    val sensexToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("SENSEX")
    val bankexToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("BANKEX")
    val crudeToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("CRUDEOIL")
    val crudeMToken = viewModel.brokerManager.instrumentMasterService.resolveIndexToken("CRUDEOIL M")

    val hasAngelLiveTick = viewModel.brokerManager.angelMarketDataService.isConnectionLive()
    val hasLiveStream = hasAngelLiveTick || mStockHealth == "LIVE"

    val marketData = MarketDataStore.marketData.collectAsStateWithLifecycle().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "REAL DATA DIAGNOSTICS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    scope.launch {
                        viewModel.brokerManager.angelMarketDataService.reconnect()
                        if (viewModel.sessionManager.isMStockConfigured()) {
                            viewModel.brokerManager.mStockMarketDataService.connect()
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reconnect", tint = Color(0xFF00C853))
            }
        }

        // Real Data Status Banner
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasLiveStream) Color(0xFF1B382B) else Color(0xFF381B1B)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasLiveStream) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = "Status",
                    tint = if (hasLiveStream) Color(0xFF00E676) else Color(0xFFFF5252),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (hasLiveStream) "AUTHENTICATED LIVE BROKER STREAM" else "NO LIVE STREAM ACTIVE",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasLiveStream) "Real market ticks streaming with provenance" else "Connect Angel One or m.Stock with valid API credentials",
                        color = if (hasLiveStream) Color(0xFFB9F6CA) else Color(0xFFFFCDD2),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section: Live Feed Providers
        SectionHeader("PRIMARY FEED: ANGEL ONE (SmartAPI)")
        DiagnosticItem("Authentication Status", angelAuthStatus)
        DiagnosticItem("Feed Token", angelFeedTokenStatus)
        DiagnosticItem("Client Code", angelClientIdStatus)
        DiagnosticItem("API Key", angelApiKeyStatus)
        DiagnosticItem("WebSocket State", angelConnectionState)
        DiagnosticItem("Feed Health Status", angelHealth)
        DiagnosticItem("First Real Tick Received", if (hasAngelLiveTick) "YES (Verified)" else "NO")
        DiagnosticItem("Last Tick Time", viewModel.brokerManager.angelMarketDataService.getLastUpdatedTime().ifBlank { "None" })

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("SECONDARY FEED: m.STOCK (Mirae Asset)")
        DiagnosticItem("Credentials Configured", if (mstockConfigured) "PASS" else "NOT CONFIGURED")
        DiagnosticItem("WebSocket State", mStockConnectionState)
        DiagnosticItem("Feed Health Status", mStockHealth)

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("ORDER EXECUTION ENGINE: DHAN")
        DiagnosticItem("Dhan Token Configured", if (dhanConfigured) "PASS (Authenticated)" else "NOT CONFIGURED")
        DiagnosticItem("Role", "ORDER PLACEMENT ONLY (Not Market Feed)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("REFERENCE / BACKUP: YAHOO FINANCE")
        DiagnosticItem("Yahoo Data Role", "REFERENCE ONLY (Never Live Feed)")
        DiagnosticItem("Yahoo Health", yahooHealth)

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Instrument Master & Token Resolution
        SectionHeader("INSTRUMENT MASTER & TOKEN RESOLUTION")
        DiagnosticItem("Scrip Master Loaded", if (isMasterLoaded) "PASS (Loaded)" else "FAIL (Not Loaded)")
        DiagnosticItem("NIFTY 50 (NSE)", if (niftyToken != null) "RESOLVED [Token: ${niftyToken.token}]" else "FAILED")
        DiagnosticItem("BANKNIFTY (NSE)", if (bankNiftyToken != null) "RESOLVED [Token: ${bankNiftyToken.token}]" else "FAILED")
        DiagnosticItem("FINNIFTY (NSE)", if (finNiftyToken != null) "RESOLVED [Token: ${finNiftyToken.token}]" else "FAILED")
        DiagnosticItem("MIDCPNIFTY (NSE)", if (midcpNiftyToken != null) "RESOLVED [Token: ${midcpNiftyToken.token}]" else "FAILED")
        DiagnosticItem("SENSEX (BSE)", if (sensexToken != null) "RESOLVED [Token: ${sensexToken.token}]" else "FAILED")
        DiagnosticItem("BANKEX (BSE)", if (bankexToken != null) "RESOLVED [Token: ${bankexToken.token}]" else "FAILED")
        DiagnosticItem("CRUDEOIL (MCX Active Near)", if (crudeToken != null) "RESOLVED [Token: ${crudeToken.token} | ${crudeToken.symbol}]" else "FAILED")
        DiagnosticItem("CRUDEOIL M (MCX Mini Near)", if (crudeMToken != null) "RESOLVED [Token: ${crudeMToken.token} | ${crudeMToken.symbol}]" else "FAILED")

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Real-time Ingested Ticks
        SectionHeader("REAL-TIME INGESTED MARKET TICKS (${marketData.size} Active)")
        if (marketData.isNotEmpty()) {
            marketData.entries.take(15).forEach { (key, data) ->
                val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(data.receivedTimestamp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(data.symbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            val badgeColor = when (data.source) {
                                MarketDataSourceNames.ANGEL_ONE -> Color(0xFF00C853)
                                MarketDataSourceNames.MSTOCK -> Color(0xFF00B0FF)
                                else -> Color(0xFFFFB300)
                            }
                            Text(
                                text = "${data.source} • ${data.state}",
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exch: ${data.exchange} | Token: ${data.token.ifBlank { "N/A" }}", color = Color.LightGray, fontSize = 11.sp)
                            Text(
                                "LTP: ₹${String.format(Locale.getDefault(), "%.2f", data.ltp)}",
                                color = if (data.change >= 0) Color(0xFF00E676) else Color(0xFFFF5252),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Received: $timeStr", color = Color.Gray, fontSize = 10.sp)
                            Text("Seq: ${data.sequenceNumber} | Vol: ${data.volume}", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No market ticks received yet.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF81D4FA),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun DiagnosticItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(Color(0xFF161A22), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFB0BEC5), fontSize = 12.sp)
        val color = when {
            value.contains("PASS") || value.contains("RESOLVED") || value.contains("YES") || value == "LIVE" || value == "CONNECTED" -> Color(0xFF00E676)
            value.contains("FAIL") || value.contains("NO") || value == "DISCONNECTED" || value == "ERROR" || value == "OFFLINE" -> Color(0xFFFF5252)
            value.contains("STALE") || value.contains("REFERENCE") || value.contains("SUBSCRIBING") || value.contains("CONNECTING") -> Color(0xFFFFB300)
            else -> Color.White
        }
        Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
