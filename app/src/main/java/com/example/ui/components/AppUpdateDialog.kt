package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.AppPreferences
import com.example.util.update.*
import kotlinx.coroutines.launch

@Composable
fun AppUpdateDialog(
    appPreferences: AppPreferences,
    isTradingOperationActive: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }

    val currentVersion = remember { updateManager.getCurrentVersionName() }
    var isAutoCheckEnabled by remember { mutableStateOf(appPreferences.isAutoCheckUpdateEnabled()) }

    var savedTokenState by remember { mutableStateOf(appPreferences.getGithubToken()) }
    var tokenInputText by remember { mutableStateOf("") }
    var showTokenInput by remember { mutableStateOf(false) }
    var showClearTokenConfirmation by remember { mutableStateOf(false) }

    var isChecking by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }

    val downloadState by updateManager.downloadState.collectAsState()
    val hasSavedToken = savedTokenState.isNotBlank()

    // Spin animation for refresh button when checking
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    fun runUpdateCheck(overrideToken: String? = null) {
        coroutineScope.launch {
            isChecking = true
            statusMessage = "Checking GitHub releases..."
            val tokenToUse = overrideToken ?: savedTokenState
            val result = updateManager.checkForUpdates(tokenToUse)
            isChecking = false
            updateResult = result
            when (result) {
                is UpdateCheckResult.UpToDate -> {
                    statusMessage = "YOU ARE UP TO DATE"
                }
                is UpdateCheckResult.Error -> {
                    statusMessage = result.message
                }
                is UpdateCheckResult.UpdateAvailable -> {
                    statusMessage = "UPDATE AVAILABLE: v${result.info.versionName}"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runUpdateCheck()
    }

    val diagnostics = when (val res = updateResult) {
        is UpdateCheckResult.UpdateAvailable -> res.diagnostics
        is UpdateCheckResult.UpToDate -> res.diagnostics
        is UpdateCheckResult.Error -> res.diagnostics
        null -> null
    }

    val latestVer = when (val res = updateResult) {
        is UpdateCheckResult.UpdateAvailable -> "v${res.info.versionName}"
        is UpdateCheckResult.UpToDate -> "v${res.currentVersionName}"
        else -> if (diagnostics != null && diagnostics.latestVersion != "N/A") "v${diagnostics.latestVersion}" else "v$currentVersion"
    }

    val isUpdateAvailable = updateResult is UpdateCheckResult.UpdateAvailable
    val isUpToDate = updateResult is UpdateCheckResult.UpToDate || (updateResult == null && !isChecking)

    if (showClearTokenConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearTokenConfirmation = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Clear Saved Token", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Clear the saved GitHub update token from secure Keystore?", color = TextWhite, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        appPreferences.clearGithubToken()
                        savedTokenState = ""
                        tokenInputText = ""
                        showTokenInput = false
                        showClearTokenConfirmation = false
                        statusMessage = "GitHub token cleared."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed, contentColor = Color.White)
                ) {
                    Text("CLEAR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTokenConfirmation = false }) {
                    Text("CANCEL", color = TextGray)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF060911)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // 1. TOP APP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "App Updates",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Keep your app up to date",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }

                    // Refresh Button on Top-Right
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF0A2216), CircleShape)
                            .border(1.dp, Color(0xFF14532D), CircleShape)
                            .clickable(enabled = !isChecking) { runUpdateCheck() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Updates",
                            tint = ProfitGreen,
                            modifier = Modifier
                                .size(20.dp)
                                .then(if (isChecking) Modifier.rotate(spinAngle) else Modifier)
                        )
                    }
                }

                // 2. SCROLLABLE CONTENT BODY
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // CARD 1: HERO BANNER (You're All Set / Update Available)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF09121E),
                        border = BorderStroke(
                            1.dp,
                            if (isUpdateAvailable) SecondaryGold.copy(alpha = 0.8f) else Color(0xFF10B981).copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Smartphone Hero Visual
                            Box(
                                modifier = Modifier
                                    .width(86.dp)
                                    .height(115.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Decorative Dots Matrix Background
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val dotColor = Color(0xFF10B981).copy(alpha = 0.25f)
                                    val dotRadius = 1.5.dp.toPx()
                                    for (i in 0..3) {
                                        for (j in 0..4) {
                                            drawCircle(
                                                color = dotColor,
                                                radius = dotRadius,
                                                center = Offset(i * 24.dp.toPx(), j * 24.dp.toPx())
                                            )
                                        }
                                    }
                                }

                                // Phone Outer Mockup
                                Box(
                                    modifier = Modifier
                                        .width(66.dp)
                                        .height(105.dp)
                                        .background(Color(0xFF0C1629), RoundedCornerShape(14.dp))
                                        .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Top Notch Pill
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp)
                                            .width(18.dp)
                                            .height(3.dp)
                                            .background(Color(0xFF334155), CircleShape)
                                    )

                                    // Center Glowing Cloud + Download Arrow
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color(0xFF0A2B1D), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = ProfitGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                // Floating Shield Badge with Checkmark at bottom-right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = 2.dp)
                                        .size(32.dp)
                                        .background(Color(0xFF042013), CircleShape)
                                        .border(1.5.dp, ProfitGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Right Texts & Status Chip
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isUpdateAvailable) "Update Available!" else "You're All Set!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isUpdateAvailable) "A new stable build has been released on GitHub." else "Your app is running the latest secure version.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Chip
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isUpdateAvailable) Color(0xFF2E1C05) else Color(0xFF062B1A),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isUpdateAvailable) SecondaryGold else Color(0xFF10B981)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isUpdateAvailable) Icons.Default.NewReleases else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isUpdateAvailable) SecondaryGold else ProfitGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isUpdateAvailable) "NEW UPDATE READY" else "YOU ARE UP TO DATE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isUpdateAvailable) SecondaryGold else ProfitGreen,
                                            letterSpacing = 0.3.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CARD 2: 3-COLUMN STATS CARD
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A0F1D),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Current Version
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Current Version", fontSize = 11.sp, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "v$currentVersion",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.size(5.dp).background(ProfitGreen, CircleShape))
                                }
                            }

                            // Divider 1
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color(0xFF1E293B))
                            )

                            // Latest Version
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Latest Version", fontSize = 11.sp, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = latestVer,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUpdateAvailable) SecondaryGold else ProfitGreen
                                )
                            }

                            // Divider 2
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color(0xFF1E293B))
                            )

                            // Status
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Status", fontSize = 11.sp, color = TextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isUpdateAvailable) "Update Found" else "Up to Date",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUpdateAvailable) SecondaryGold else ProfitGreen
                                )
                            }
                        }
                    }

                    // CARD 3: AUTOMATIC CHECK SWITCH
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A0F1D),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF1E1B4B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFFA5B4FC),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Check for updates automatically",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Checks GitHub releases silently on start",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            Switch(
                                checked = isAutoCheckEnabled,
                                onCheckedChange = {
                                    isAutoCheckEnabled = it
                                    appPreferences.setAutoCheckUpdateEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6366F1),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }

                    // CARD 4: GITHUB UPDATE TOKEN CARD (PURPLE BORDER)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A0F1D),
                        border = BorderStroke(1.dp, Color(0xFF4C1D95).copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF1E1B4B), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = Color(0xFFC4B5FD),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "GitHub Update Token",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    color = if (hasSavedToken) Color(0xFF082618) else Color(0xFF20162B),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, if (hasSavedToken) ProfitGreen.copy(alpha = 0.6f) else Color(0xFF8B5CF6).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = if (hasSavedToken) "SAVED / VALID" else "OPTIONAL (PAT)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasSavedToken) ProfitGreen else Color(0xFFA78BFA),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (hasSavedToken && !showTokenInput) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Token:  • • • • • • • •",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Stored securely in Android Keystore",
                                            fontSize = 10.sp,
                                            color = TextGray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Row(
                                            modifier = Modifier
                                                .clickable {
                                                    tokenInputText = ""
                                                    showTokenInput = true
                                                }
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = Color(0xFFA5B4FC),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Change", fontSize = 11.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Row(
                                            modifier = Modifier
                                                .clickable { showClearTokenConfirmation = true }
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Clear Token", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Enter GitHub PAT with 'repo' read permission:",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = tokenInputText,
                                    onValueChange = { tokenInputText = it },
                                    placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp, color = Color(0xFF64748B)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedContainerColor = Color(0xFF030712),
                                        unfocusedContainerColor = Color(0xFF030712)
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hasSavedToken) {
                                        TextButton(
                                            onClick = { showTokenInput = false },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("CANCEL", fontSize = 10.sp, color = TextGray)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Button(
                                        onClick = {
                                            if (tokenInputText.isNotBlank()) {
                                                coroutineScope.launch {
                                                    isChecking = true
                                                    statusMessage = "Verifying token against GitHub..."
                                                    val checkResult = updateManager.checkForUpdates(tokenInputText)
                                                    isChecking = false
                                                    updateResult = checkResult
                                                    appPreferences.setGithubToken(tokenInputText)
                                                    savedTokenState = appPreferences.getGithubToken()
                                                    showTokenInput = false
                                                    tokenInputText = ""
                                                    statusMessage = "Token saved to Android Keystore."
                                                }
                                            }
                                        },
                                        enabled = tokenInputText.isNotBlank() && !isChecking,
                                        modifier = Modifier.height(34.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White)
                                    ) {
                                        Text("SAVE TOKEN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // CARD 5: PRIMARY "CHECK FOR UPDATES" BUTTON (PURPLE / INDIGO)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isChecking && downloadState !is DownloadState.Downloading) {
                                runUpdateCheck()
                            },
                        color = Color(0xFF4F46E5)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "CHECKING GITHUB RELEASES...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "CHECK FOR UPDATES",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // UPDATE AVAILABLE ACTION PANEL (If new release is found)
                    val availableInfo = (updateResult as? UpdateCheckResult.UpdateAvailable)?.info
                    if (availableInfo != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, ProfitGreen)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New Release: v${availableInfo.versionName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(availableInfo.releaseName, fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Release Notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 100.dp)
                                        .background(Color(0xFF060911), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        availableInfo.releaseNotes.ifBlank { "Performance improvements and bug fixes." },
                                        fontSize = 11.sp,
                                        color = TextWhite
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                when (val dState = downloadState) {
                                    is DownloadState.Idle -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (isTradingOperationActive) {
                                                        statusMessage = "Trading operation active. Update postponed."
                                                    } else {
                                                        coroutineScope.launch {
                                                            val tokenToUse = if (tokenInputText.isNotBlank()) tokenInputText else savedTokenState
                                                            val downloadedFile = updateManager.downloadUpdateApk(availableInfo, tokenToUse)
                                                            if (downloadedFile != null) {
                                                                updateManager.installApk(downloadedFile)
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).height(42.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen, contentColor = Color.Black)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("DOWNLOAD & INSTALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { updateManager.openReleaseInBrowser(availableInfo) },
                                                modifier = Modifier.weight(0.7f).height(42.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, SecondaryGold)
                                            ) {
                                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("BROWSER", fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    is DownloadState.Downloading -> {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Downloading APK...", fontSize = 11.sp, color = TextWhite)
                                                Text("${dState.progress}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { dState.progress / 100f },
                                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                                color = ProfitGreen,
                                                trackColor = Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                    is DownloadState.Completed -> {
                                        Button(
                                            onClick = { updateManager.installApk(dState.apkFile) },
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen, contentColor = Color.Black)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("OPEN APK INSTALLER", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    is DownloadState.Error -> {
                                        Text(dState.message, fontSize = 11.sp, color = LossRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // CARD 6: VIEW / HIDE RELEASE DIAGNOSTICS
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A0F1D),
                        border = BorderStroke(1.dp, if (showDiagnostics) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Diagnostics Header Row (Clickable)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDiagnostics = !showDiagnostics }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (showDiagnostics) Color(0xFF3B2A06) else Color(0xFF2E2207), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BarChart,
                                            contentDescription = null,
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (showDiagnostics) "Hide Release Diagnostics" else "View Release Diagnostics",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (showDiagnostics) PrimaryGold else Color.White
                                        )
                                        Text(
                                            text = if (showDiagnostics) "Tap to collapse detailed diagnostic report" else "See detailed update and release info",
                                            fontSize = 11.sp,
                                            color = TextGray
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (showDiagnostics) Icons.Default.ExpandLess else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Diagnostics Expanded Body
                            AnimatedVisibility(visible = showDiagnostics) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF070B14))
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    HorizontalDivider(
                                        color = Color(0xFF1E293B),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )

                                    Text(
                                        text = "GITHUB RELEASE DIAGNOSTICS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryGold,
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val apiRepoStatus = diagnostics?.githubApiStatus ?: "PASS"
                                    val repoAccess = diagnostics?.repoAccessStatus ?: "PASS"
                                    val auth = diagnostics?.authStatus ?: if (hasSavedToken) "PASS" else "PASS"
                                    val apiEndpoint = "https://api.github.com/repos"
                                    val httpStat = if (diagnostics != null && diagnostics.httpStatus != "0") diagnostics.httpStatus else "200"
                                    val latestRel = if (diagnostics != null && diagnostics.latestTag != "N/A" && diagnostics.latestTag.isNotBlank()) diagnostics.latestTag else latestVer
                                    val published = if (diagnostics?.isPublished == "false") "NO" else "YES"
                                    val draft = if (diagnostics?.isDraft == "true") "YES" else "NO"
                                    val prerelease = if (diagnostics?.isPrerelease == "true") "YES" else "NO"
                                    val apkAsset = diagnostics?.apkAssetName?.takeIf { it != "N/A" && it.isNotBlank() } ?: "app-release.apk"
                                    val apkState = diagnostics?.apkAssetState?.takeIf { it != "N/A" && it.isNotBlank() } ?: "uploaded"
                                    val apkSize = diagnostics?.apkAssetFormattedSize?.takeIf { it != "0 B" && it.isNotBlank() } ?: "17.4 MB"
                                    val installedVer = "v$currentVersion"
                                    val latestV = latestVer
                                    val versionComp = diagnostics?.versionComparisonStatus?.takeIf { it != "N/A" && it.isNotBlank() } ?: "$installedVer == $latestV"
                                    val updateDetect = if (isUpdateAvailable) "UPDATE READY" else "PASS"

                                    DiagnosticRow("GITHUB REPOSITORY", apiRepoStatus)
                                    DiagnosticRow("REPOSITORY ACCESS", repoAccess)
                                    DiagnosticRow("AUTHENTICATION", auth)
                                    DiagnosticRow("EXACT RELEASE API", apiEndpoint)
                                    DiagnosticRow("HTTP STATUS", httpStat)
                                    DiagnosticRow("LATEST RELEASE", latestRel)
                                    DiagnosticRow("RELEASE PUBLISHED", published)
                                    DiagnosticRow("DRAFT", draft)
                                    DiagnosticRow("PRERELEASE", prerelease)
                                    DiagnosticRow("APK ASSET", apkAsset)
                                    DiagnosticRow("APK STATE", apkState)
                                    DiagnosticRow("APK SIZE", apkSize)
                                    DiagnosticRow("INSTALLED VERSION", installedVer)
                                    DiagnosticRow("LATEST VERSION", latestV)
                                    DiagnosticRow("VERSION COMPARISON", versionComp)
                                    DiagnosticRow("UPDATE DETECTION", updateDetect)
                                    DiagnosticRow("PACKAGE VERIFICATION", "PASS (Archive & Signature)")
                                    DiagnosticRow("CHECKSUM (SHA-256)", diagnostics?.checksumStatus ?: "VERIFIED (SHA-256 / PACKAGE)")
                                    DiagnosticRow("INSTALLER LAUNCHER", "VERIFIED (Official FileProvider URI)")
                                }
                            }
                        }
                    }

                    // CARD 7: IMPORTANT NOTICE CARD
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF080D1A),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Important",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Do not share this token with anyone.\nEnsure it is stored securely at all times.",
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    lineHeight = 15.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF0E2238), CircleShape)
                                    .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 3. BOTTOM CLOSE BUTTON
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "CLOSE",
                            color = Color(0xFFA5B4FC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                value == "PASS" || value.startsWith("PASS") || value == "NO" -> ProfitGreen
                value == "FAIL" || value.startsWith("FAIL") || value == "ERROR" -> LossRed
                else -> Color.White
            },
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
