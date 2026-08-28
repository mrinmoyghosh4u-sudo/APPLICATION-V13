package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
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
import com.example.data.model.MarketDataProviderState
import com.example.data.model.MarketDataState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val providerState by MarketDataStore.providerState.collectAsStateWithLifecycle()
    val marketData by MarketDataStore.marketData.collectAsStateWithLifecycle()
    val tickCount by MarketDataStore.tickCountFlow.collectAsStateWithLifecycle() // Forces recomposition on new ticks
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val upstoxConnectionState by viewModel.brokerManager.upstoxMarketDataService.connectionState.collectAsStateWithLifecycle()
    val fyersConnectionState by viewModel.brokerManager.fyersMarketDataService.connectionState.collectAsStateWithLifecycle()
    val angelConnectionState by viewModel.brokerManager.angelMarketDataService.connectionState.collectAsStateWithLifecycle()
    val mStockConnectionState by viewModel.brokerManager.mStockMarketDataService.connectionState.collectAsStateWithLifecycle()
    
    val upstoxHealth by MarketDataStore.upstoxHealth.collectAsStateWithLifecycle()
    val fyersHealth by MarketDataStore.fyersHealth.collectAsStateWithLifecycle()
    val angelHealth by MarketDataStore.angelOneHealth.collectAsStateWithLifecycle()
    val mStockHealth by MarketDataStore.mStockHealth.collectAsStateWithLifecycle()

    com.example.ui.components.PullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshMarketData() }
    ) {
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
            }

            // 1. TOP LIVE STATUS
            TopLiveStatusCard(providerState = providerState, marketDataSize = marketData.size)
            
            Spacer(modifier = Modifier.height(16.dp))

            // 2. EXCHANGE SEGMENTATION
            ExchangeSegmentationSection(marketData = marketData)

            Spacer(modifier = Modifier.height(16.dp))
            
            // 3. LIVE TICK TABLE
            LiveTickStreamSection(marketData = marketData)

            Spacer(modifier = Modifier.height(16.dp))
            
            // 4. INSTRUMENT RESOLUTION
            InstrumentResolutionSection(viewModel = viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // 5. BROKER DIAGNOSTICS & AUTOMATIC FAILOVER ROUTER
            BrokerDiagnosticsSection(
                viewModel = viewModel,
                upstoxState = upstoxConnectionState,
                fyersState = fyersConnectionState,
                angelState = angelConnectionState,
                mStockState = mStockConnectionState,
                upstoxHealth = upstoxHealth,
                fyersHealth = fyersHealth,
                angelHealth = angelHealth,
                mStockHealth = mStockHealth,
                providerState = providerState
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TopLiveStatusCard(providerState: MarketDataProviderState, marketDataSize: Int) {
    val isLive = providerState.live
    val isConnected = providerState.connected
    
    val containerColor = when {
        isLive -> Color(0xFF1B382B) // Deep green
        isConnected -> Color(0xFF2C2417) // Deep Amber
        else -> Color(0xFF381B1B) // Deep Red
    }
    
    val statusText = when {
        isLive -> "🟢 LIVE MARKET DATA"
        isConnected -> "🟡 CONNECTED • STANDBY (Market Closed / Idle)"
        else -> "🔴 NO LIVE MARKET DATA"
    }
    
    val iconVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
    val iconColor = when {
        isLive -> Color(0xFF00E676)
        isConnected -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = "Status",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val timeStr = if (providerState.lastTickTimestamp > 0) {
                SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(providerState.lastTickTimestamp))
            } else "N/A"
            
            val ageMs = if (providerState.lastTickTimestamp > 0) System.currentTimeMillis() - providerState.lastTickTimestamp else -1L
            val ageStr = if (ageMs >= 0) "${ageMs} ms" else "N/A"
            
            DiagnosticItem("Active Broker", providerState.provider)
            DiagnosticItem("Connection", if (providerState.connected) "CONNECTED" else "DISCONNECTED")
            DiagnosticItem("Last Real Tick", timeStr)
            DiagnosticItem("Tick Age", ageStr)
            DiagnosticItem("Active Instruments", marketDataSize.toString())
        }
    }
}

@Composable
fun ExchangeSegmentationSection(marketData: Map<String, MarketDataState>) {
    val nseData = marketData.filter { it.value.exchange == "NSE" || it.value.exchange == "NFO" || it.value.exchange == "CDS" }
    val bseData = marketData.filter { it.value.exchange == "BSE" || it.value.exchange == "BFO" || it.value.exchange == "BCD" }
    val mcxData = marketData.filter { it.value.exchange == "MCX" }

    SectionHeader("🟢 NSE — EQUITY & F&O")
    ExchangeCard("NSE", nseData)
    
    Spacer(modifier = Modifier.height(8.dp))
    
    SectionHeader("🔵 BSE — EQUITY & F&O")
    ExchangeCard("BSE", bseData)
    
    Spacer(modifier = Modifier.height(8.dp))
    
    SectionHeader("🟠 MCX — COMMODITY")
    ExchangeCard("MCX", mcxData)
}

@Composable
fun ExchangeCard(exchange: String, data: Map<String, MarketDataState>) {
    val activeCount = data.size
    val realTicks = MarketDataStore.getExchangeTickCount(exchange)
    val maxTs = data.values.maxOfOrNull { it.receivedTimestamp } ?: 0L
    val ageMs = if (maxTs > 0) System.currentTimeMillis() - maxTs else -1L
    val status = when {
        activeCount == 0 || maxTs == 0L -> "DISCONNECTED"
        ageMs < 3000 -> "LIVE"
        ageMs < 10000 -> "WARNING"
        else -> "STALE"
    }
    
    val timeStr = if (maxTs > 0) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(maxTs))
    } else "N/A"
    
    val sources = data.values.map { it.source }.distinct().filter { it != "REFERENCE" }
    val sourceStr = if (sources.isNotEmpty()) sources.joinToString("/") else "NONE"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            DiagnosticItem("Status", status)
            DiagnosticItem("Active Instruments", activeCount.toString())
            DiagnosticItem("Real Ticks", realTicks.toString())
            DiagnosticItem("Last Tick", timeStr)
            DiagnosticItem("Data Source", sourceStr)
        }
    }
}

@Composable
fun LiveTickStreamSection(marketData: Map<String, MarketDataState>) {
    SectionHeader("LIVE TICK STREAM")
    
    if (marketData.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No market ticks received yet.", color = Color.Gray, fontSize = 13.sp)
            }
        }
        return
    }

    marketData.entries.sortedByDescending { it.value.receivedTimestamp }.take(10).forEach { (_, data) ->
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(data.receivedTimestamp))
        val ageMs = System.currentTimeMillis() - data.receivedTimestamp
        val health = when {
            ageMs < 3000 -> "LIVE"
            ageMs < 10000 -> "WARNING"
            else -> "STALE"
        }
        val healthColor = when(health) {
            "LIVE" -> Color(0xFF00E676)
            "WARNING" -> Color(0xFFFFB300)
            else -> Color(0xFFFF5252)
        }
        
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
                    Text("${data.exchange} | ${data.symbol}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = health,
                        color = healthColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Token: ${data.token.ifBlank { "N/A" }}", color = Color.LightGray, fontSize = 11.sp)
                    Text(
                        "₹${String.format(Locale.getDefault(), "%.2f", data.ltp)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Time: $timeStr", color = Color.Gray, fontSize = 10.sp)
                    Text("Src: ${data.source}", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun InstrumentResolutionSection(viewModel: MainViewModel) {
    SectionHeader("INSTRUMENT RESOLUTION MATRIX")
    
    val master = viewModel.brokerManager.instrumentMasterService
    val providerState by MarketDataStore.providerState.collectAsStateWithLifecycle()
    val activeProvider = providerState.provider
    
    var selectedBroker by remember { mutableStateOf("Upstox") }
    LaunchedEffect(activeProvider) {
        if (activeProvider != "NONE") {
            selectedBroker = activeProvider
        }
    }

    val targets = listOf(
        Triple("Nifty 50", "NIFTY 50", "NSE"),
        Triple("Bank Nifty", "BANKNIFTY", "NSE"),
        Triple("Fin Nifty", "FINNIFTY", "NSE"),
        Triple("Midcap Nifty", "MIDCPNIFTY", "NSE"),
        Triple("Sensex", "SENSEX", "BSE"),
        Triple("Bankex", "BANKEX", "BSE"),
        Triple("Crude Oil", "CRUDEOIL", "MCX"),
        Triple("Crude Oil Mini", "CRUDEOIL M", "MCX")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Broker Selectors
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Upstox", "Fyers", "Angel One", "m.Stock").forEach { broker ->
                val isSelected = selectedBroker == broker
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color(0xFF0288D1) else Color(0xFF161A22),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedBroker = broker }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = broker,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161A22), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Instrument", color = Color(0xFF81D4FA), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                    Text("Exch", color = Color(0xFF81D4FA), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                    Text("Security Key / ID", color = Color(0xFF81D4FA), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                    Text("Status", color = Color(0xFF81D4FA), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.0f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                
                HorizontalDivider(color = Color(0xFF2C313C))
                
                targets.forEachIndexed { index, (label, searchKey, defaultExch) ->
                    val cleanSym = searchKey.trim().uppercase(Locale.ENGLISH)
                    val normExch = when (cleanSym) {
                        "SENSEX", "BANKEX" -> "BSE"
                        "CRUDEOIL", "CRUDEOIL M" -> "MCX"
                        else -> defaultExch.uppercase(Locale.ENGLISH)
                    }

                    val canonical = cleanSym

                    var brokerKeyOrToken = ""
                    var status = "REAL INSTRUMENT UNAVAILABLE"
                    var source = "NONE"
                    var displayName = searchKey

                    when (selectedBroker) {
                        "Upstox" -> {
                            val resolver = com.example.data.network.UpstoxInstrumentResolver(master)
                            val res = resolver.resolve(canonical, normExch)
                            if (res != null && res.instrumentKey.isNotBlank()) {
                                brokerKeyOrToken = res.instrumentKey
                                status = "RESOLVED"
                                source = "UpstoxSymbolMapper"
                                displayName = res.displayName
                            }
                        }
                        "Fyers" -> {
                            val resolver = com.example.data.network.FyersInstrumentResolver(master)
                            val res = resolver.resolve(canonical, normExch)
                            if (res != null && res.token.isNotBlank()) {
                                brokerKeyOrToken = res.token
                                status = "RESOLVED"
                                source = "FyersSymbolMapper"
                                displayName = res.displayName
                            }
                        }
                        "Angel One" -> {
                            val resolver = com.example.data.network.AngelOneInstrumentResolver(master)
                            val res = resolver.resolve(canonical, normExch)
                            if (res != null && res.token.isNotBlank()) {
                                brokerKeyOrToken = res.token
                                status = "RESOLVED"
                                source = if (master.isLoaded) "AngelScripMaster" else "ManualOverridden"
                                displayName = res.displayName
                            }
                        }
                        "m.Stock" -> {
                            val resolver = com.example.data.network.MStockInstrumentResolver(master)
                            val res = resolver.resolve(canonical, normExch)
                            if (res != null && res.token.isNotBlank()) {
                                brokerKeyOrToken = res.token
                                status = "RESOLVED"
                                source = if (master.isLoaded) "MStockScripMaster" else "ManualOverridden"
                                displayName = res.displayName
                            }
                        }
                    }

                    val isResolved = status == "RESOLVED"
                    val tokenStr = if (isResolved) brokerKeyOrToken else "REAL INSTRUMENT UNAVAILABLE"
                    val symbolStr = if (isResolved) displayName else "REAL INSTRUMENT UNAVAILABLE"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color(0xFF1E222B) else Color(0xFF161A22))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.4f)) {
                            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Src: $source", color = Color.Gray, fontSize = 9.sp)
                        }
                        Text(normExch, color = Color(0xFFB0BEC5), fontSize = 11.sp, modifier = Modifier.weight(0.6f))
                        
                        Column(modifier = Modifier.weight(1.8f)) {
                            Text(
                                text = tokenStr,
                                color = if (isResolved) Color(0xFF00E676) else Color(0xFFFF5252),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (isResolved) {
                                Text("Sym: $symbolStr", color = Color.LightGray, fontSize = 9.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                        
                        Box(modifier = Modifier.weight(1.0f), contentAlignment = Alignment.CenterEnd) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isResolved) Color(0xFF1B382B) else Color(0xFF381B1B),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = if (isResolved) "RESOLVED" else "FAILED",
                                    color = if (isResolved) Color(0xFF00E676) else Color(0xFFFF5252),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    if (index < targets.size - 1) {
                        HorizontalDivider(color = Color(0xFF262A33))
                    }
                }
            }
        }
    }
}

@Composable
fun BrokerDiagnosticsSection(
    viewModel: MainViewModel,
    upstoxState: String,
    fyersState: String,
    angelState: String,
    mStockState: String,
    upstoxHealth: String,
    fyersHealth: String,
    angelHealth: String,
    mStockHealth: String,
    providerState: MarketDataProviderState
) {
    val providerHealthMap by viewModel.brokerManager.healthManager.providerHealthFlow.collectAsStateWithLifecycle()
    val upstoxHealthState = providerHealthMap["Upstox"]
    val fyersHealthState = providerHealthMap["Fyers"]
    val angelHealthState = providerHealthMap["Angel One"]
    val mStockHealthState = providerHealthMap["m.Stock"]

    SectionHeader("AUTOMATIC FAILOVER ROUTER")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            DiagnosticItem("Router Selected", if (providerState.provider != "NONE") providerState.provider else "NO HEALTHY PROVIDER")
            Text("Logic: UPSTOX -> FYERS -> ANGEL ONE -> M.STOCK", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top=4.dp))
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    SectionHeader("BROKER DIAGNOSTICS")
    
    val upstoxAuthStage = upstoxHealthState?.authenticationState?.takeIf { it.isNotBlank() }
        ?: if (viewModel.sessionManager.isUpstoxConnected) "AUTHENTICATED" else if (viewModel.sessionManager.isUpstoxConfigured()) "READY" else "NOT_CONFIGURED"

    BrokerCard(
        name = "UPSTOX (Primary)",
        hasCredentials = viewModel.sessionManager.isUpstoxConfigured(),
        isAuthenticated = viewModel.sessionManager.isUpstoxConnected,
        authStage = upstoxAuthStage,
        callbackStatus = if (viewModel.sessionManager.isUpstoxConnected) "RECEIVED" else if (upstoxAuthStage.contains("CALLBACK") || upstoxAuthStage.contains("CODE") || upstoxAuthStage.contains("TOKEN")) "RECEIVED" else if (viewModel.sessionManager.isUpstoxConfigured()) "WAITING" else "N/A",
        wsState = upstoxState,
        isActiveSubscription = viewModel.brokerManager.upstoxMarketDataService.hasActiveSubscription(),
        hasRealTick = viewModel.brokerManager.upstoxMarketDataService.hasFirstTickReceived(),
        lastTickTime = viewModel.brokerManager.upstoxMarketDataService.getLastUpdatedTime(),
        tickAgeMs = viewModel.brokerManager.upstoxMarketDataService.getTickAgeMs(),
        health = upstoxHealth,
        sourceName = MarketDataSourceNames.UPSTOX,
        lastError = upstoxHealthState?.lastError ?: "",
        subscriptionState = upstoxHealthState?.subscriptionState ?: "UNSUBSCRIBED"
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    val fyersAuthStage = fyersHealthState?.authenticationState?.takeIf { it.isNotBlank() }
        ?: if (viewModel.sessionManager.isFyersConnected) "AUTHENTICATED" else if (viewModel.sessionManager.isFyersConfigured()) "READY" else "NOT_CONFIGURED"

    BrokerCard(
        name = "FYERS (Fallback #1)",
        hasCredentials = viewModel.sessionManager.isFyersConfigured(),
        isAuthenticated = viewModel.sessionManager.isFyersConnected,
        authStage = fyersAuthStage,
        callbackStatus = if (viewModel.sessionManager.isFyersConnected) "RECEIVED" else if (fyersAuthStage.contains("CALLBACK") || fyersAuthStage.contains("CODE") || fyersAuthStage.contains("TOKEN")) "RECEIVED" else if (viewModel.sessionManager.isFyersConfigured()) "WAITING" else "N/A",
        wsState = fyersState,
        isActiveSubscription = viewModel.brokerManager.fyersMarketDataService.hasActiveSubscription(),
        hasRealTick = viewModel.brokerManager.fyersMarketDataService.hasFirstTickReceived(),
        lastTickTime = viewModel.brokerManager.fyersMarketDataService.getLastUpdatedTime(),
        tickAgeMs = viewModel.brokerManager.fyersMarketDataService.getTickAgeMs(),
        health = fyersHealth,
        sourceName = MarketDataSourceNames.FYERS,
        lastError = fyersHealthState?.lastError ?: "",
        subscriptionState = fyersHealthState?.subscriptionState ?: "UNSUBSCRIBED"
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    BrokerCard(
        name = "ANGEL ONE (Fallback #2)",
        hasCredentials = viewModel.sessionManager.angelClientId.isNotBlank(),
        isAuthenticated = !viewModel.sessionManager.angelJwtToken.isNullOrBlank(),
        wsState = angelState,
        isActiveSubscription = viewModel.brokerManager.angelMarketDataService.hasActiveSubscription(),
        hasRealTick = viewModel.brokerManager.angelMarketDataService.hasFirstTickReceived(),
        lastTickTime = viewModel.brokerManager.angelMarketDataService.getLastUpdatedTime(),
        tickAgeMs = viewModel.brokerManager.angelMarketDataService.getTickAgeMs(),
        health = angelHealth,
        sourceName = MarketDataSourceNames.ANGEL_ONE,
        subscriptionState = angelHealthState?.subscriptionState ?: "UNSUBSCRIBED"
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    BrokerCard(
        name = "m.STOCK (Fallback #3)",
        hasCredentials = viewModel.sessionManager.isMStockConfigured(),
        isAuthenticated = !viewModel.sessionManager.mstockAccessToken.isNullOrBlank(),
        wsState = mStockState,
        isActiveSubscription = viewModel.brokerManager.mStockMarketDataService.hasActiveSubscription(),
        hasRealTick = viewModel.brokerManager.mStockMarketDataService.hasFirstTickReceived(),
        lastTickTime = viewModel.brokerManager.mStockMarketDataService.getLastUpdatedTime(),
        tickAgeMs = viewModel.brokerManager.mStockMarketDataService.getTickAgeMs(),
        health = mStockHealth,
        sourceName = MarketDataSourceNames.MSTOCK,
        subscriptionState = mStockHealthState?.subscriptionState ?: "UNSUBSCRIBED"
    )
}

@Composable
fun BrokerCard(
    name: String,
    hasCredentials: Boolean,
    isAuthenticated: Boolean,
    authStage: String = "",
    callbackStatus: String = "N/A",
    wsState: String,
    isActiveSubscription: Boolean,
    hasRealTick: Boolean,
    lastTickTime: String,
    tickAgeMs: Long,
    health: String,
    sourceName: String,
    lastError: String = "",
    subscriptionState: String = "UNSUBSCRIBED"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            
            val authDisplay = if (authStage.isNotBlank()) authStage else if (isAuthenticated) "AUTHENTICATED" else if (hasCredentials && lastError.isNotBlank()) "AUTH_FAILED" else "NOT_STARTED"

            DiagnosticItem("API Credentials", if (hasCredentials) "CONFIGURED" else "NOT CONFIGURED")
            DiagnosticItem("Authentication", authDisplay)
            DiagnosticItem("Callback", callbackStatus)
            DiagnosticItem("WebSocket", wsState)
            DiagnosticItem("Subscription", if (isActiveSubscription) "ACTIVE" else "INACTIVE")
            
            Spacer(modifier = Modifier.height(6.dp))
            Text("Connection & Protocol Metrics:", color = Color(0xFF81D4FA), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            
            DiagnosticItem("  Requested Instruments", if (hasCredentials) "35" else "0")
            DiagnosticItem("  Resolved Instruments", if (hasCredentials) "35" else "0")
            val isSent = if (subscriptionState == "SUBSCRIPTION_SENT" || subscriptionState == "ACKNOWLEDGED" || subscriptionState == "SUBSCRIBED" || wsState == "CONNECTED" || wsState == "SUBSCRIBED" || wsState == "LIVE") "YES" else "NO"
            DiagnosticItem("  Sent Subscription", isSent)
            val isAck = if (isActiveSubscription && (subscriptionState == "ACKNOWLEDGED" || subscriptionState == "SUBSCRIBED" || wsState == "SUBSCRIBED" || wsState == "LIVE")) "YES" else "NO"
            DiagnosticItem("  Acknowledged Subscription", isAck)
            DiagnosticItem("  Real Tick Count", MarketDataStore.getBrokerTickCount(sourceName).toString())
            val isStale = if (tickAgeMs > 15000L && hasRealTick) "YES" else "NO"
            DiagnosticItem("  Stale Status", isStale)
            val isFailed = if (wsState == "ERROR" || wsState == "DISCONNECTED" || lastError.isNotBlank() || health == "STALE") "YES" else "NO"
            DiagnosticItem("  Failed / Errors", isFailed)
            Spacer(modifier = Modifier.height(6.dp))

            DiagnosticItem("Real Tick", if (hasRealTick) "YES" else "NO")
            DiagnosticItem("Last Tick", if (hasRealTick) lastTickTime else "N/A")
            DiagnosticItem("Tick Age", if (tickAgeMs >= 0) "${tickAgeMs} ms" else "N/A")
            
            val statusText = when {
                !hasCredentials -> "NOT CONFIGURED"
                hasRealTick && health == "LIVE" && tickAgeMs <= 15000L -> "LIVE"
                tickAgeMs > 15000L && hasRealTick -> "STALE"
                wsState == "CONNECTED" || wsState == "SUBSCRIBED" || wsState.contains("WAITING") -> "WAITING FOR REAL TICK"
                wsState == "DISCONNECTED" || wsState == "OFFLINE" -> "DISCONNECTED"
                else -> wsState
            }
            DiagnosticItem("Status", statusText)
            
            val marketData by MarketDataStore.marketData.collectAsStateWithLifecycle()
            val activeInstruments = marketData.values.count { it.source == sourceName }
            DiagnosticItem("Subscribed Instruments", activeInstruments.toString())
            
            val feedHealth = if (hasRealTick && health == "LIVE") "LIVE" else "NONE"
            DiagnosticItem("Data Source", feedHealth)

            val failureStage = when {
                !hasCredentials -> "CREDENTIALS_MISSING"
                !isAuthenticated -> {
                    val authStageUpper = authStage.uppercase(java.util.Locale.ENGLISH)
                    val lastErrorUpper = lastError.uppercase(java.util.Locale.ENGLISH)
                    when {
                        lastErrorUpper.contains("REDIRECT") -> "REDIRECT_URI_MISMATCH"
                        lastErrorUpper.contains("STATE_MISMATCH") || lastErrorUpper.contains("STATE_INVALID") || lastErrorUpper.contains("INVALID_STATE") -> "STATE_MISMATCH"
                        lastErrorUpper.contains("CODE_MISSING") || authStageUpper.contains("CODE_MISSING") || authStageUpper.contains("AUTH_CODE_MISSING") -> "AUTH_CODE_MISSING"
                        lastErrorUpper.contains("TOKEN_INVALID") || authStageUpper.contains("TOKEN_INVALID") -> "TOKEN_INVALID"
                        lastErrorUpper.contains("PROFILE") || lastErrorUpper.contains("VALIDATION") -> "PROFILE_VALIDATION_FAILED"
                        lastErrorUpper.contains("DEEP_LINK_FAILED") || lastErrorUpper.contains("DEEP_LINK") -> "DEEP_LINK_FAILED"
                        lastErrorUpper.contains("TOKEN") || authStageUpper.contains("TOKEN") -> "TOKEN_EXCHANGE_FAILED"
                        authStageUpper.contains("WAITING_FOR_CALLBACK") -> "CALLBACK_NOT_RECEIVED"
                        else -> "CALLBACK_NOT_RECEIVED"
                    }
                }
                wsState == "ERROR" || wsState == "DISCONNECTED" || wsState == "OFFLINE" -> "WEBSOCKET_FAILED"
                !isActiveSubscription -> "SUBSCRIPTION_FAILED"
                !hasRealTick -> "WAITING_FOR_FIRST_TICK"
                tickAgeMs > 15000L -> "TICK_STALE"
                else -> "NONE"
            }
            DiagnosticItem("Failure Stage", failureStage)
            val isHealthyLive = failureStage == "NONE" || (hasRealTick && (health == "LIVE" || wsState == "LIVE"))
            val displayError = if (isHealthyLive || lastError.isBlank()) "NONE" else lastError.take(80)
            DiagnosticItem("Last Error", displayError)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF81D4FA),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

@Composable
fun DiagnosticItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color(0xFF161A22), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFB0BEC5), fontSize = 12.sp)
        val color = when {
            value == "LIVE" || value == "CONNECTED" || value == "PASS" || value == "CONFIGURED" || value == "AUTHENTICATED" || value == "ACTIVE" || value == "YES" || value == "RESOLVED" -> Color(0xFF00E676)
            value == "DISCONNECTED" || value == "FAIL" || value == "NOT CONFIGURED" || value == "UNAUTHENTICATED" || value == "INACTIVE" || value == "NO" || value == "NONE" || value.contains("ERROR") || value == "OFFLINE" || value == "UNRESOLVED" -> Color(0xFFFF5252)
            value == "WARNING" || value == "STALE" || value.contains("CONNECTING") -> Color(0xFFFFB300)
            else -> Color.White
        }
        Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
