package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    fun runUpdateCheck(overrideToken: String? = null) {
        coroutineScope.launch {
            isChecking = true
            statusMessage = "Checking for updates from GitHub..."
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

    if (showClearTokenConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearTokenConfirmation = false },
            containerColor = DarkBackground,
            title = { Text("Clear Saved Token", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Clear the saved GitHub update token?", color = TextWhite, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        appPreferences.clearGithubToken()
                        savedTokenState = ""
                        tokenInputText = ""
                        showTokenInput = false
                        showClearTokenConfirmation = false
                        statusMessage = "GitHub token cleared from secure storage."
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SecondaryGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "APP UPDATES",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Version Info Card
                GoldCard(borderColor = PrimaryGold, borderWidth = 1.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CURRENT VERSION", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                                Text("v$currentVersion", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                            }

                            val latestVer = when (val res = updateResult) {
                                is UpdateCheckResult.UpdateAvailable -> "v${res.info.versionName}"
                                is UpdateCheckResult.UpToDate -> "v${res.currentVersionName}"
                                else -> if (diagnostics != null && diagnostics.latestVersion != "N/A") "v${diagnostics.latestVersion}" else "Checking..."
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("LATEST VERSION", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                                Text(latestVer, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ProfitGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val statusText = when (val res = updateResult) {
                            is UpdateCheckResult.UpdateAvailable -> "UPDATE AVAILABLE"
                            is UpdateCheckResult.UpToDate -> "YOU ARE UP TO DATE"
                            is UpdateCheckResult.Error -> res.message
                            null -> "READY TO CHECK"
                        }

                        val statusColor = when {
                            statusText.contains("UPDATE AVAILABLE") -> ProfitGreen
                            statusText.contains("UP TO DATE") -> SecondaryGold
                            statusText.contains("ERROR") || statusText.contains("REQUIRED") || statusText.contains("NOT FOUND") || statusText.contains("LIMIT") -> LossRed
                            else -> TextGray
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCardSecondary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("STATUS", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Check Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardSecondary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Check for updates automatically", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Checks GitHub Releases silently on start", fontSize = 10.sp, color = TextGray)
                    }
                    Switch(
                        checked = isAutoCheckEnabled,
                        onCheckedChange = {
                            isAutoCheckEnabled = it
                            appPreferences.setAutoCheckUpdateEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = ProfitGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GitHub Token Management (Android Keystore Encrypted)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCardSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (hasSavedToken) ProfitGreen else DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = if (hasSavedToken) ProfitGreen else SecondaryGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GitHub Update Token", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }

                            if (hasSavedToken) {
                                Surface(
                                    color = ProfitGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "SAVED / VALID",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (hasSavedToken && !showTokenInput) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Token: ●●●●●●●●", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                                    Text("Stored securely in Android Keystore", fontSize = 9.sp, color = TextGray)
                                }

                                Row {
                                    TextButton(
                                        onClick = {
                                            tokenInputText = ""
                                            showTokenInput = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Change", fontSize = 10.sp, color = SecondaryGold)
                                    }

                                    TextButton(
                                        onClick = { showClearTokenConfirmation = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Clear Token", fontSize = 10.sp, color = LossRed)
                                    }
                                }
                            }
                        } else {
                            Text("Enter GitHub PAT for private repository authentication:", fontSize = 10.sp, color = TextGray)
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = tokenInputText,
                                onValueChange = { tokenInputText = it },
                                placeholder = { Text("ghp_xxxxxxxxxxxx", fontSize = 11.sp, color = TextGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
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
                                                when (checkResult) {
                                                    is UpdateCheckResult.Error -> {
                                                        appPreferences.setGithubToken(tokenInputText)
                                                        savedTokenState = appPreferences.getGithubToken()
                                                        showTokenInput = false
                                                        tokenInputText = ""
                                                        statusMessage = "VERIFICATION FAILED: ${checkResult.message} (Token saved anyway)"
                                                    }
                                                    else -> {
                                                        appPreferences.setGithubToken(tokenInputText)
                                                        savedTokenState = appPreferences.getGithubToken()
                                                        showTokenInput = false
                                                        tokenInputText = ""
                                                        statusMessage = "Token verified & securely saved to Keystore!"
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = tokenInputText.isNotBlank() && !isChecking,
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                                ) {
                                    Text("SAVE & VERIFY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Check Button
                Button(
                    onClick = { runUpdateCheck() },
                    enabled = !isChecking && downloadState !is DownloadState.Downloading,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CHECKING GITHUB...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CHECK FOR UPDATES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        msg,
                        fontSize = 11.sp,
                        color = when {
                            msg.contains("UPDATE AVAILABLE") || msg.contains("✓") || msg.contains("saved") -> ProfitGreen
                            msg.contains("UP TO DATE") -> SecondaryGold
                            else -> LossRed
                        },
                        fontWeight = FontWeight.Medium
                    )
                }

                // Diagnostics Toggle & Panel
                if (diagnostics != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDiagnostics = !showDiagnostics },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            if (showDiagnostics) Icons.Default.ExpandLess else Icons.Default.BugReport,
                            contentDescription = null,
                            tint = SecondaryGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (showDiagnostics) "Hide Release Diagnostics" else "View Release Diagnostics",
                            fontSize = 11.sp,
                            color = SecondaryGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showDiagnostics) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DarkCardSecondary,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("GITHUB RELEASE DIAGNOSTICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGold)
                                Spacer(modifier = Modifier.height(6.dp))

                                DiagnosticRow("GITHUB REPOSITORY", if (diagnostics.githubApiStatus == "PASS") "PASS" else diagnostics.repository)
                                DiagnosticRow("REPOSITORY ACCESS", diagnostics.repoAccessStatus)
                                DiagnosticRow("AUTHENTICATION", diagnostics.authStatus)
                                DiagnosticRow("EXACT RELEASE API", diagnostics.releaseApiUrl)
                                DiagnosticRow("HTTP STATUS", diagnostics.httpStatus)
                                DiagnosticRow("LATEST RELEASE", if (diagnostics.latestTag != "N/A") diagnostics.latestTag else "N/A")
                                DiagnosticRow("RELEASE PUBLISHED", if (diagnostics.isPublished == "true") "YES" else "NO")
                                DiagnosticRow("DRAFT", if (diagnostics.isDraft == "true") "YES" else "NO")
                                DiagnosticRow("PRERELEASE", if (diagnostics.isPrerelease == "true") "YES" else "NO")
                                DiagnosticRow("APK ASSET", diagnostics.apkAssetName)
                                DiagnosticRow("APK STATE", diagnostics.apkAssetState)
                                DiagnosticRow("APK SIZE", diagnostics.apkAssetFormattedSize)
                                DiagnosticRow("INSTALLED VERSION", "v${diagnostics.installedVersion}")
                                DiagnosticRow("LATEST VERSION", if (diagnostics.latestVersion != "N/A") "v${diagnostics.latestVersion}" else "N/A")
                                DiagnosticRow("VERSION COMPARISON", if (diagnostics.resultStatus == "UPDATE AVAILABLE") "UPDATE AVAILABLE" else diagnostics.versionComparisonStatus)
                                DiagnosticRow("UPDATE DETECTION", if (diagnostics.latestReleaseApiStatus == "PASS") "PASS" else "FAIL")
                            }
                        }
                    }
                }

                // Update Available Panel
                val availableInfo = (updateResult as? UpdateCheckResult.UpdateAvailable)?.info
                if (availableInfo != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkCardSecondary,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NewReleases, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Version Available: v${availableInfo.versionName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Release: ${availableInfo.releaseName}", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)

                            if (availableInfo.publishedAt.isNotBlank()) {
                                Text("Published: ${availableInfo.publishedAt.take(10)}", fontSize = 10.sp, color = TextGray)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("What's New:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .background(DarkBackground, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    availableInfo.releaseNotes.ifBlank { "Performance improvements and bug fixes." },
                                    fontSize = 10.sp,
                                    color = TextWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Download/Install Actions
                            when (val dState = downloadState) {
                                is DownloadState.Idle -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (isTradingOperationActive) {
                                                    statusMessage = "Update postponed until current trading operation completes."
                                                } else {
                                                    coroutineScope.launch {
                                                        val tokenToUse = if (tokenInputText.isNotBlank()) tokenInputText else savedTokenState
                                                        val downloadedFile = updateManager.downloadUpdateApk(availableInfo, tokenToUse)
                                                        if (downloadedFile != null) {
                                                            if (tokenInputText.isNotBlank() && savedTokenState.isBlank()) {
                                                                appPreferences.setGithubToken(tokenInputText)
                                                                savedTokenState = appPreferences.getGithubToken()
                                                            }
                                                            updateManager.installApk(downloadedFile)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen, contentColor = Color.Black)
                                        ) {
                                            Icon(Icons.Default.GetApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("DOWNLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                updateManager.openReleaseInBrowser(availableInfo)
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold)
                                        ) {
                                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("IN BROWSER", fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                is DownloadState.Downloading -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Downloading update...", fontSize = 11.sp, color = TextWhite)
                                            Text("${dState.progress}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryGold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { dState.progress / 100f },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = ProfitGreen,
                                            trackColor = DarkCardBorder
                                        )
                                    }
                                }
                                is DownloadState.Completed -> {
                                    Button(
                                        onClick = { updateManager.installApk(dState.apkFile) },
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen, contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("INSTALL UPDATE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                is DownloadState.Error -> {
                                    Column {
                                        Text(dState.message, fontSize = 11.sp, color = LossRed, fontWeight = FontWeight.Bold)
                                        if (dState.message.contains("404") || dState.message.contains("Token") || dState.message.contains("Private")) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "💡 Private repositories require a GitHub Personal Access Token (PAT) with 'repo' scope. Enter token above or click 'BROWSER' to download directly.",
                                                fontSize = 10.sp,
                                                color = SecondaryGold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        updateManager.resetDownloadState()
                                                        coroutineScope.launch {
                                                            val tokenToUse = if (tokenInputText.isNotBlank()) tokenInputText else savedTokenState
                                                            val downloadedFile = updateManager.downloadUpdateApk(availableInfo, tokenToUse)
                                                            if (downloadedFile != null) {
                                                                if (tokenInputText.isNotBlank() && savedTokenState.isBlank()) {
                                                                    appPreferences.setGithubToken(tokenInputText)
                                                                    savedTokenState = appPreferences.getGithubToken()
                                                                }
                                                                updateManager.installApk(downloadedFile)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).height(38.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("RETRY DOWNLOAD", fontSize = 11.sp, color = SecondaryGold, fontWeight = FontWeight.Bold)
                                                }

                                                Button(
                                                    onClick = {
                                                        updateManager.openReleaseInBrowser(availableInfo)
                                                    },
                                                    modifier = Modifier.weight(1f).height(38.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold, contentColor = Color.Black)
                                                ) {
                                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("BROWSER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (!hasSavedToken && !showTokenInput) {
                                                OutlinedButton(
                                                    onClick = { showTokenInput = true },
                                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
                                                ) {
                                                    Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("ENTER GITHUB TOKEN", fontSize = 11.sp, color = PrimaryGold, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = SecondaryGold, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 10.sp, color = TextGray)
        Text(
            value,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (value == "PASS" || value.startsWith("PASS") || value == "NO") ProfitGreen else TextWhite,
            maxLines = 1,
            modifier = Modifier.widthIn(max = 180.dp)
        )
    }
}
