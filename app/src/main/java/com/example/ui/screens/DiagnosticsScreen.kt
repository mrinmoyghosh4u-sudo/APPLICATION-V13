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
import com.example.data.model.RealTimePriceTick
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
    val marketData by MarketDataStore.ticks.collectAsStateWithLifecycle()
    val tickCount by kotlinx.coroutines.flow.MutableStateFlow(0).collectAsStateWithLifecycle() // Forces recomposition on new ticks
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val upstoxConnectionState by viewModel.brokerManager.upstoxMarketDataService.connectionState.collectAsStateWithLifecycle()
    val fyersConnectionState by viewModel.brokerManager.fyersMarketDataService.connectionState.collectAsStateWithLifecycle()
    val angelConnectionState by viewModel.brokerManager.angelMarketDataService.connectionState.collectAsStateWithLifecycle()

}
