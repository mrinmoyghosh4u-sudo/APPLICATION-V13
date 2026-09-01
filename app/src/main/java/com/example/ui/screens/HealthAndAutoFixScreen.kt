package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.diagnostic.AZDiagnosticResult
import com.example.util.diagnostic.AutoRecoveryLog
import com.example.util.diagnostic.HealthState
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthAndAutoFixScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val azReport by viewModel.diagnosticEngine.fullAZReport.collectAsStateWithLifecycle()
    val recoveryLogs by viewModel.diagnosticEngine.recoveryLogs.collectAsStateWithLifecycle()
    val providerState by com.example.data.model.MarketDataStore.providerState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🩺 Health & Auto-Fix") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Live Provider Status Card
            item {
                CurrentLiveStatusCard(providerState)
            }

            item {
                Button(
                    onClick = { viewModel.diagnosticEngine.runFullAZCheck() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("RUN FULL A–Z CHECK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            if (azReport.isNotEmpty()) {
                item {
                    HealthScoreSummary(azReport)
                }

                item {
                    Text(
                        text = "FULL A–Z HEALTH REPORT",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(azReport) { result ->
                    AZResultCard(result)
                }
            } else {
                item {
                    Text(
                        "Tap the button above to run a full diagnostic sweep.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (recoveryLogs.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "RECOVERY HISTORY (PAST EVENTS)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(recoveryLogs) { log ->
                    RecoveryLogCard(log)
                }
            }
        }
    }
}

@Composable
fun CurrentLiveStatusCard(state: com.example.data.model.MarketDataProviderState) {
    val isLive = state.live && !state.stale
    val statusColor = if (isLive) Color(0xFF4CAF50) else if (state.stale) Color(0xFFFFC107) else Color(0xFF2196F3)
    val displayName = com.example.data.model.MarketDataProviders.getDisplayName(state.provider).uppercase()
    val tickAgeMs = if (state.lastUpdate > 0L) (System.currentTimeMillis() - state.lastUpdate).coerceAtLeast(0L) else 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CURRENT FEED STATUS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isLive) "● LIVE" else if (state.stale) "▲ STALE" else "○ ${state.status}",
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Active Provider:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tick Freshness:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (state.lastUpdate > 0L) "${tickAgeMs}ms ago" else "No Tick",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (tickAgeMs < 10_000L && state.lastUpdate > 0L) Color(0xFF4CAF50) else Color(0xFFFFC107)
                )
            }
        }
    }
}

@Composable
fun HealthScoreSummary(report: List<AZDiagnosticResult>) {
    val verified = report.count { it.status == HealthState.HEALTHY }
    val warnings = report.count { it.status == HealthState.DEGRADED || it.status == HealthState.STALE }
    val errors = report.count { it.status == HealthState.AUTH_FAILED || it.status == HealthState.OFFLINE || it.status == HealthState.ORDER_BLOCKED }
    val pending = report.count { it.status == HealthState.OFFLINE || it.status == HealthState.NO_TICK }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("System Overview", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🟢 Verified: $verified")
                Text("🟡 Warnings: $warnings")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🔴 Errors: $errors")
                Text("🔵 Pending: $pending")
            }
        }
    }
}

@Composable
fun AZResultCard(result: AZDiagnosticResult) {
    val (icon, color) = when (result.status) {
        HealthState.HEALTHY -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        HealthState.DEGRADED, HealthState.STALE -> Icons.Default.Warning to Color(0xFFFFC107)
        HealthState.OFFLINE, HealthState.NO_TICK -> Icons.Default.Help to Color(0xFF2196F3)
        else -> Icons.Default.Error to Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(result.name, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(result.status.name, color = color, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(result.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (result.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                result.details.forEach { (k, v) ->
                    Text("$k: $v", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun RecoveryLogCard(log: AutoRecoveryLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${log.component} - ${log.problem}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            Text("Action: ${log.action}", style = MaterialTheme.typography.bodySmall)
            Text("Verification: ${log.verification}", style = MaterialTheme.typography.bodySmall)
            Text("Result: ${log.result.name}", style = MaterialTheme.typography.labelSmall, color = if (log.result == HealthState.RECOVERED) Color(0xFF4CAF50) else Color(0xFFF44336))
        }
    }
}
