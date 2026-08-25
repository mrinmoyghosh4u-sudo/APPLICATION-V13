package com.example.util.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.util.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class UpdateManager(
    private val context: Context,
    private val owner: String = "mrinmoyghosh4u-sudo",
    private val repo: String = "APPLICATION-V13"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pInfo.versionName
            if (!vName.isNullOrBlank()) vName else BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val scaled = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(java.util.Locale.US, "%.1f %s", scaled, units[digitGroups.coerceAtMost(units.size - 1)])
    }

    private fun resolveAuthToken(customToken: String?): String {
        val prefsToken = try { AppPreferences.getInstance(context).getGithubToken() } catch (e: Exception) { "" }
        return customToken?.takeIf { it.isNotBlank() }
            ?: prefsToken.takeIf { it.isNotBlank() }
            ?: BuildConfig.GITHUB_TOKEN.takeIf { it.isNotBlank() }
            ?: System.getenv("GITHUB_TOKEN")?.takeIf { it.isNotBlank() }
            ?: System.getenv("GH_TOKEN")?.takeIf { it.isNotBlank() }
            ?: ""
    }

    private fun buildGitHubRequest(url: String, token: String): Request {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "KING-KHAN-AI-TRADE")
            .header("Cache-Control", "no-cache, no-store")

        if (token.isNotBlank()) {
            val authHeader = if (token.startsWith("Bearer ") || token.startsWith("token ")) token else "Bearer $token"
            builder.header("Authorization", authHeader)
        }
        return builder.build()
    }

    private fun extractErrorMessage(bodyStr: String, defaultMsg: String): String {
        return try {
            val json = JSONObject(bodyStr)
            val msg = json.optString("message", "")
            if (msg.isNotBlank()) msg else defaultMsg
        } catch (e: Exception) {
            defaultMsg
        }
    }

    suspend fun checkForUpdates(customToken: String? = null): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersionName()
        val primaryOwner = owner
        val repoPath = "$primaryOwner/$repo"
        val latestUrl = "https://api.github.com/repos/$primaryOwner/$repo/releases/latest"
        val effectiveToken = resolveAuthToken(customToken)
        val hasToken = effectiveToken.isNotBlank()

        var diagnostics = UpdateDiagnostics(
            githubOwner = primaryOwner,
            githubRepo = repo,
            releaseApiUrl = latestUrl,
            repository = repoPath,
            installedVersion = currentVersion,
            hasAuthToken = hasToken,
            githubApiStatus = "PASS",
            repoAccessStatus = "PASS",
            authStatus = if (hasToken) "PASS" else "NOT SET",
            latestReleaseApiStatus = "FAIL",
            httpStatus = "0"
        )

        try {
            // Step 1: Test authenticated repository access
            val repoAccessUrl = "https://api.github.com/repos/$primaryOwner/$repo"
            val repoRequest = buildGitHubRequest(repoAccessUrl, effectiveToken)
            Log.d("UpdateManager", "Testing repository access: $repoAccessUrl")

            var repoAccessCode = 0
            var responseSnippet = "N/A"
            var contentType = "N/A"

            client.newCall(repoRequest).execute().use { response ->
                repoAccessCode = response.code
                contentType = response.header("Content-Type", "N/A") ?: "N/A"
                val bodyStr = response.body?.string().orEmpty()
                responseSnippet = if (bodyStr.length > 500) bodyStr.take(500) + "..." else bodyStr
            }

            if (repoAccessCode != 200) {
                val repoErrorMsg = when (repoAccessCode) {
                    401 -> "GitHub Auth Failed (401): Invalid Token"
                    403 -> "GitHub Auth Failed (403): Token lacks permissions"
                    404 -> if (effectiveToken.isBlank()) "GitHub authentication required for private repo." else "Not Found (404): Repo missing or Token lacks access."
                    else -> "REPOSITORY ACCESS ERROR (HTTP $repoAccessCode)"
                }

                diagnostics = diagnostics.copy(
                    repoAccessStatus = "FAIL",
                    authStatus = if (repoAccessCode in listOf(401, 403, 404)) "FAIL" else diagnostics.authStatus,
                    httpStatus = repoAccessCode.toString(),
                    responseContentType = contentType,
                    responseSnippet = responseSnippet,
                    latestReleaseApiStatus = "FAIL",
                    versionComparisonStatus = "FAIL",
                    apkAssetFound = "NOT FOUND",
                    resultStatus = repoErrorMsg
                )

                return@withContext UpdateCheckResult.Error(
                    repoErrorMsg,
                    diagnostics
                )
            }

            // Repo access succeeded
            diagnostics = diagnostics.copy(
                repoAccessStatus = "PASS",
                authStatus = "PASS"
            )

            // Step 2: Request releases/latest
            val request = buildGitHubRequest(latestUrl, effectiveToken)
            Log.d("UpdateManager", "Fetching latest release: $latestUrl")

            var httpCode = 0
            var releaseJson: JSONObject? = null
            var totalReleases = 0

            client.newCall(request).execute().use { response ->
                httpCode = response.code
                contentType = response.header("Content-Type", "N/A") ?: "N/A"
                val bodyStr = response.body?.string().orEmpty()
                responseSnippet = if (bodyStr.length > 500) bodyStr.take(500) + "..." else bodyStr

                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val jsonObj = JSONObject(bodyStr)
                    val isDraft = jsonObj.optBoolean("draft", false)
                    val isPrerelease = jsonObj.optBoolean("prerelease", false)
                    val tagName = jsonObj.optString("tag_name", "")
                    if (!isDraft && !isPrerelease && tagName.isNotBlank()) {
                        releaseJson = jsonObj
                        totalReleases = 1
                    }
                }
            }

            // Step 3: Fallback if /releases/latest failed or returned empty
            if (releaseJson == null) {
                val listUrl = "https://api.github.com/repos/$primaryOwner/$repo/releases"
                val listRequest = buildGitHubRequest(listUrl, effectiveToken)
                Log.d("UpdateManager", "Fetching releases list fallback: $listUrl")

                client.newCall(listRequest).execute().use { response ->
                    val listCode = response.code
                    if (httpCode == 0 || httpCode == 404) {
                        httpCode = listCode
                        contentType = response.header("Content-Type", "N/A") ?: "N/A"
                    }
                    val bodyStr = response.body?.string().orEmpty()
                    if (bodyStr.isNotBlank() && bodyStr.startsWith("[")) {
                        responseSnippet = if (bodyStr.length > 500) bodyStr.take(500) + "..." else bodyStr
                        val array = JSONArray(bodyStr)
                        val publishedReleases = mutableListOf<JSONObject>()
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val isDraft = item.optBoolean("draft", false)
                            val isPrerelease = item.optBoolean("prerelease", false)
                            val tagName = item.optString("tag_name", "")
                            if (!isDraft && !isPrerelease && tagName.isNotBlank()) {
                                publishedReleases.add(item)
                            }
                        }
                        totalReleases = publishedReleases.size
                        if (publishedReleases.isNotEmpty()) {
                            publishedReleases.sortWith { a, b ->
                                val vA = VersionUtils.cleanVersion(a.optString("tag_name", ""))
                                val vB = VersionUtils.cleanVersion(b.optString("tag_name", ""))
                                VersionUtils.compareVersions(vB, vA)
                            }
                            releaseJson = publishedReleases.first()
                        }
                    }
                }
            }

            diagnostics = diagnostics.copy(
                httpStatus = httpCode.toString(),
                responseContentType = contentType,
                responseSnippet = responseSnippet,
                releaseCount = totalReleases
            )

            if (releaseJson == null) {
                val apiReason = extractErrorMessage(responseSnippet, "HTTP $httpCode")
                val errorMsg = when (httpCode) {
                    200 -> "APK release asset not found. Please create/publish the GitHub Release APK."
                    401 -> "GitHub Auth Failed (401): Invalid Token"
                    403 -> "GitHub Auth Failed (403): Token lacks permissions"
                    404 -> if (effectiveToken.isBlank()) "GitHub authentication required for private repo." else "Not Found (404): Repo missing or Token lacks access."
                    429 -> "GITHUB RATE LIMIT EXCEEDED"
                    in 500..599 -> "GITHUB SERVER ERROR (HTTP $httpCode)"
                    0 -> "GITHUB NETWORK ERROR"
                    else -> "GITHUB API ERROR (HTTP $httpCode: $apiReason)"
                }

                diagnostics = diagnostics.copy(
                    latestReleaseApiStatus = "FAIL",
                    versionComparisonStatus = "FAIL",
                    apkAssetFound = "NOT FOUND",
                    resultStatus = errorMsg
                )

                return@withContext UpdateCheckResult.Error(
                    errorMsg,
                    diagnostics
                )
            }

            val json = releaseJson!!
            val tagName = json.optString("tag_name", "")
            val releaseName = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "No release notes provided.")
            val publishedAt = json.optString("published_at", "")
            val isDraft = json.optBoolean("draft", false)
            val isPrerelease = json.optBoolean("prerelease", false)

            val assets = json.optJSONArray("assets")
            var assetId = 0L
            var apkUrl = ""
            var apkName = ""
            var apkState = ""
            var apkSize = 0L
            var assetApiUrl = ""
            var browserDownloadUrl = ""

            if (assets != null && assets.length() > 0) {
                var selectedAsset: JSONObject? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.equals("app-release.apk", ignoreCase = true)) {
                        selectedAsset = asset
                        break
                    }
                }
                if (selectedAsset == null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "").lowercase()
                        if (name.endsWith(".apk") && !name.contains("debug") && !name.contains("unaligned")) {
                            selectedAsset = asset
                            break
                        }
                    }
                }
                if (selectedAsset == null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "").lowercase()
                        if (name.endsWith(".apk")) {
                            selectedAsset = asset
                            break
                        }
                    }
                }

                if (selectedAsset != null) {
                    assetId = selectedAsset.optLong("id", 0L)
                    apkName = selectedAsset.optString("name", "")
                    apkState = selectedAsset.optString("state", "uploaded")
                    assetApiUrl = selectedAsset.optString("url", "")
                    if (assetApiUrl.isBlank() && assetId > 0) {
                        assetApiUrl = "https://api.github.com/repos/$primaryOwner/$repo/releases/assets/$assetId"
                    }
                    browserDownloadUrl = selectedAsset.optString("browser_download_url", "")
                    apkUrl = if (assetApiUrl.isNotBlank()) assetApiUrl else browserDownloadUrl
                    apkSize = selectedAsset.optLong("size", 0L)
                }
            }

            val cleanLatest = VersionUtils.cleanVersion(tagName)
            val cleanCurrent = VersionUtils.cleanVersion(currentVersion)
            val isNewer = VersionUtils.isUpdateAvailable(cleanCurrent, cleanLatest)
            val formattedSize = formatBytes(apkSize)
            val expectedSha256 = extractExpectedSha256(releaseNotes, apkName)

            val comparisonStatus = if (isNewer) {
                "v$cleanCurrent < v$cleanLatest"
            } else if (VersionUtils.compareVersions(cleanCurrent, cleanLatest) == 0) {
                "v$cleanCurrent == v$cleanLatest"
            } else {
                "v$cleanCurrent > v$cleanLatest"
            }

            diagnostics = diagnostics.copy(
                githubApiStatus = "PASS",
                latestReleaseApiStatus = "PASS",
                latestTag = tagName,
                latestVersion = cleanLatest,
                isDraft = isDraft.toString(),
                isPrerelease = isPrerelease.toString(),
                isPublished = (!isDraft && !isPrerelease).toString(),
                versionComparisonStatus = comparisonStatus,
                apkAssetName = if (apkName.isNotBlank()) apkName else "app-release.apk",
                apkAssetState = if (apkState.isNotBlank()) apkState else "NOT FOUND",
                apkAssetUrl = if (apkUrl.isNotBlank()) apkUrl else "NOT FOUND",
                apkAssetFound = if (apkUrl.isNotBlank() && apkSize > 0) "PASS" else "NOT FOUND",
                apkAssetFormattedSize = formattedSize,
                checksumStatus = if (!expectedSha256.isNullOrBlank()) "PROVIDED (SHA-256)" else "OPTIONAL (PACKAGE VERIFIED)",
                resultStatus = if (apkUrl.isBlank() || apkSize <= 0) "APK release asset not found. Please create/publish the GitHub Release APK." else if (isNewer) "UPDATE AVAILABLE" else "YOU ARE UP TO DATE"
            )

            if (apkUrl.isBlank() || apkSize <= 0) {
                return@withContext UpdateCheckResult.Error(
                    "APK release asset not found. Please create/publish the GitHub Release APK.",
                    diagnostics
                )
            }

            val info = UpdateInfo(
                tagName = tagName,
                versionName = cleanLatest,
                releaseName = releaseName,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl,
                publishedAt = publishedAt,
                apkSize = apkSize,
                apkAssetName = apkName,
                assetId = assetId,
                assetApiUrl = assetApiUrl,
                browserDownloadUrl = browserDownloadUrl,
                expectedSha256 = expectedSha256,
                diagnostics = diagnostics
            )

            if (isNewer) {
                UpdateCheckResult.UpdateAvailable(info, diagnostics)
            } else {
                UpdateCheckResult.UpToDate(cleanCurrent, diagnostics)
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error checking for updates", e)
            val isTimeout = e is java.net.SocketTimeoutException
            val errorMsg = if (isTimeout) "GITHUB REQUEST TIMEOUT" else "NETWORK ERROR: ${e.localizedMessage}"

            val errDiag = diagnostics.copy(
                githubApiStatus = "FAIL",
                latestReleaseApiStatus = "FAIL",
                httpStatus = if (isTimeout) "TIMEOUT" else "EXCEPTION",
                responseSnippet = e.localizedMessage ?: "Unknown Exception",
                versionComparisonStatus = "FAIL",
                apkAssetFound = "NOT FOUND",
                resultStatus = errorMsg
            )
            UpdateCheckResult.Error(
                errorMsg,
                errDiag
            )
        }
    }

    fun calculateSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to calculate SHA-256 for file: ${file.absolutePath}", e)
            ""
        }
    }

    fun extractExpectedSha256(releaseNotes: String, assetName: String): String? {
        if (releaseNotes.isBlank()) return null
        val hex64Regex = Regex("[a-fA-F0-9]{64}")
        val lines = releaseNotes.lines()
        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains(assetName.lowercase()) || lower.contains("sha256") || lower.contains("sha-256") || lower.contains("checksum")) {
                val match = hex64Regex.find(line)
                if (match != null) {
                    return match.value.lowercase()
                }
            }
        }
        val fallbackMatch = hex64Regex.find(releaseNotes)
        return fallbackMatch?.value?.lowercase()
    }

    fun verifyApk(apkFile: File, expectedVersion: String? = null, expectedSha256: String? = null): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0) {
            Log.w("UpdateManager", "verifyApk failed: file does not exist or empty")
            return false
        }
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (info == null || info.packageName.isNullOrBlank()) {
                Log.e("UpdateManager", "verifyApk failed: package archive info could not be parsed")
                return false
            }

            if (!expectedVersion.isNullOrBlank()) {
                val parsedVersion = info.versionName ?: ""
                if (parsedVersion.isNotBlank()) {
                    Log.d("UpdateManager", "verifyApk parsed APK version: $parsedVersion (expected: $expectedVersion)")
                }
            }

            if (!expectedSha256.isNullOrBlank()) {
                val calculatedSha = calculateSha256(apkFile)
                if (!calculatedSha.equals(expectedSha256.trim(), ignoreCase = true)) {
                    Log.e("UpdateManager", "verifyApk SHA-256 mismatch! Expected: $expectedSha256, Actual: $calculatedSha")
                    return false
                } else {
                    Log.d("UpdateManager", "verifyApk SHA-256 checksum matched: $calculatedSha")
                }
            }

            true
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to parse APK archive: ${e.localizedMessage}", e)
            false
        }
    }

    data class PostInstallVerification(
        val isUpdated: Boolean,
        val currentVersion: String,
        val previousVersion: String,
        val message: String
    )

    fun checkAndVerifyInstalledVersion(prefs: AppPreferences): PostInstallVerification {
        val current = getCurrentVersionName()
        val lastInstalled = prefs.getLastInstalledVersion()
        val pending = prefs.getPendingUpdateVersion()

        val isNewer = if (lastInstalled.isNotBlank()) {
            VersionUtils.compareVersions(current, lastInstalled) > 0
        } else false

        val matchesPending = pending.isNotBlank() && VersionUtils.cleanVersion(current) == VersionUtils.cleanVersion(pending)

        if (isNewer || matchesPending) {
            prefs.setLastInstalledVersion(current)
            prefs.clearPendingUpdateVersion()
            Log.d("UpdateManager", "Post-install verification successful: v$current installed (previous: $lastInstalled)")
            return PostInstallVerification(
                isUpdated = true,
                currentVersion = current,
                previousVersion = if (lastInstalled.isNotBlank()) lastInstalled else "previous",
                message = "🎉 Successfully updated to v$current! Verification passed."
            )
        }

        if (lastInstalled.isBlank()) {
            prefs.setLastInstalledVersion(current)
        }

        return PostInstallVerification(
            isUpdated = false,
            currentVersion = current,
            previousVersion = lastInstalled,
            message = "Running version v$current"
        )
    }

    suspend fun autoDownloadAndInstall(updateInfo: UpdateInfo, customToken: String? = null): File? {
        val downloadedApk = downloadUpdateApk(updateInfo, customToken)
        if (downloadedApk != null && verifyApk(downloadedApk, updateInfo.versionName, updateInfo.expectedSha256)) {
            AppPreferences.getInstance(context).setPendingUpdateVersion(updateInfo.versionName)
            installApk(downloadedApk)
            return downloadedApk
        }
        return null
    }

    suspend fun downloadUpdateApk(updateInfo: UpdateInfo, customToken: String? = null): File? = withContext(Dispatchers.IO) {
        val downloadDir = File(context.cacheDir, "app_updates")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        val apkFileName = "app-release-${updateInfo.versionName}.apk"
        val apkFile = File(downloadDir, apkFileName)
        val tempFile = File(downloadDir, "app-release-${updateInfo.versionName}.apk.tmp")

        if (apkFile.exists() && apkFile.length() > 0) {
            if (verifyApk(apkFile, updateInfo.versionName, updateInfo.expectedSha256) && (updateInfo.apkSize <= 0 || apkFile.length() == updateInfo.apkSize)) {
                Log.d("UpdateManager", "Cached APK is valid and verified: ${apkFile.absolutePath}")
                _downloadState.value = DownloadState.Completed(apkFile)
                return@withContext apkFile
            } else {
                Log.w("UpdateManager", "Cached APK invalid or size mismatch, deleting")
                apkFile.delete()
            }
        }

        if (tempFile.exists()) {
            tempFile.delete()
        }

        downloadDir.listFiles()?.forEach { file ->
            if (file.name != apkFileName) {
                file.delete()
            }
        }

        _downloadState.value = DownloadState.Downloading(0, 0L, updateInfo.apkSize)

        val effectiveToken = resolveAuthToken(customToken)
        val rawToken = effectiveToken.trim()
        val cleanToken = rawToken
            .replace("Bearer ", "", ignoreCase = true)
            .replace("token ", "", ignoreCase = true)
            .trim()

        val assetApiUrl = when {
            updateInfo.assetId > 0 -> "https://api.github.com/repos/$owner/$repo/releases/assets/${updateInfo.assetId}"
            updateInfo.assetApiUrl.isNotBlank() -> updateInfo.assetApiUrl
            else -> ""
        }

        data class DownloadStrategy(
            val name: String,
            val url: String,
            val isApiUrl: Boolean,
            val authHeaderVal: String
        )

        val strategies = mutableListOf<DownloadStrategy>()

        if (cleanToken.isNotBlank()) {
            if (assetApiUrl.isNotBlank()) {
                strategies.add(DownloadStrategy("API Asset (Bearer Auth)", assetApiUrl, isApiUrl = true, authHeaderVal = "Bearer $cleanToken"))
                strategies.add(DownloadStrategy("API Asset (Token Auth)", assetApiUrl, isApiUrl = true, authHeaderVal = "token $cleanToken"))
            }
            if (updateInfo.apkUrl.isNotBlank() && updateInfo.apkUrl.contains("api.github.com") && updateInfo.apkUrl != assetApiUrl) {
                strategies.add(DownloadStrategy("API Fallback (Bearer Auth)", updateInfo.apkUrl, isApiUrl = true, authHeaderVal = "Bearer $cleanToken"))
            }
        }

        if (assetApiUrl.isNotBlank() && !strategies.any { it.url == assetApiUrl && it.authHeaderVal.isBlank() }) {
            strategies.add(DownloadStrategy("API Asset (Public)", assetApiUrl, isApiUrl = true, authHeaderVal = ""))
        }

        if (updateInfo.browserDownloadUrl.isNotBlank()) {
            strategies.add(DownloadStrategy("Browser Download (Public)", updateInfo.browserDownloadUrl, isApiUrl = false, authHeaderVal = ""))
        }

        if (updateInfo.apkUrl.isNotBlank() && !strategies.any { it.url == updateInfo.apkUrl }) {
            val isApi = updateInfo.apkUrl.contains("api.github.com")
            strategies.add(DownloadStrategy("Fallback URL", updateInfo.apkUrl, isApiUrl = isApi, authHeaderVal = ""))
        }

        var lastErrorMessage = "Release APK asset could not be accessed. Please check release asset and repository access."

        val noRedirectClient = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        for (strategy in strategies) {
            Log.d("UpdateManager", "Attempting download using strategy '${strategy.name}': ${strategy.url}")
            try {
                var response: okhttp3.Response? = null

                if (strategy.isApiUrl) {
                    val requestBuilder = Request.Builder()
                        .url(strategy.url)
                        .header("User-Agent", "KING-KHAN-AI-TRADE")
                        .header("Accept", "application/octet-stream")
                        .header("X-GitHub-Api-Version", "2022-11-28")

                    if (strategy.authHeaderVal.isNotBlank()) {
                        requestBuilder.header("Authorization", strategy.authHeaderVal)
                    }

                    val initialResponse = noRedirectClient.newCall(requestBuilder.build()).execute()
                    Log.d("UpdateManager", "GitHub API (${strategy.name}) initial response code: ${initialResponse.code}")

                    if (initialResponse.code in 300..399) {
                        val redirectUrl = initialResponse.header("Location")
                        initialResponse.close()

                        if (!redirectUrl.isNullOrBlank()) {
                            Log.d("UpdateManager", "Following S3 redirect (without Authorization header): $redirectUrl")
                            val redirectRequest = Request.Builder()
                                .url(redirectUrl)
                                .header("User-Agent", "KING-KHAN-AI-TRADE")
                                .build()
                            response = client.newCall(redirectRequest).execute()
                            Log.d("UpdateManager", "Redirect response code: ${response.code}")
                        } else {
                            lastErrorMessage = "Release APK asset could not be accessed. Please check release asset and repository access."
                        }
                    } else if (initialResponse.code == 200) {
                        response = initialResponse
                    } else {
                        val code = initialResponse.code
                        val errSnippet = initialResponse.body?.string().orEmpty().take(200)
                        initialResponse.close()
                        lastErrorMessage = when (code) {
                            401 -> "GitHub authentication failed. Please check GitHub Token."
                            403 -> "GitHub access denied. Please check repository permissions/token."
                            404 -> "Release APK asset could not be accessed. Please check release asset and repository access."
                            in 500..599 -> "GitHub server temporarily unavailable. Please retry."
                            else -> "Release APK asset could not be accessed (HTTP $code)."
                        }
                        Log.w("UpdateManager", "Download failed for ${strategy.name}: HTTP $code - $errSnippet")
                    }
                } else {
                    val requestBuilder = Request.Builder()
                        .url(strategy.url)
                        .header("User-Agent", "KING-KHAN-AI-TRADE")

                    if (strategy.authHeaderVal.isNotBlank()) {
                        requestBuilder.header("Authorization", strategy.authHeaderVal)
                    }

                    response = client.newCall(requestBuilder.build()).execute()
                }

                if (response != null) {
                    response.use { res ->
                        if (!res.isSuccessful) {
                            val code = res.code
                            lastErrorMessage = when (code) {
                                401 -> "GitHub authentication failed. Please check GitHub Token."
                                403 -> "GitHub access denied. Please check repository permissions/token."
                                404 -> "Release APK asset could not be accessed. Please check release asset and repository access."
                                in 500..599 -> "GitHub server temporarily unavailable. Please retry."
                                else -> "Release APK asset could not be accessed (HTTP $code)."
                            }
                            Log.w("UpdateManager", "Download failed for ${strategy.name}: HTTP $code")
                            return@use
                        }

                        val body = res.body ?: run {
                            lastErrorMessage = "Release APK asset response body was empty."
                            return@use
                        }

                        val totalBytes = if (updateInfo.apkSize > 0) updateInfo.apkSize else body.contentLength()
                        var downloadedBytes = 0L

                        body.byteStream().use { input ->
                            FileOutputStream(tempFile).use { output ->
                                val buffer = ByteArray(16384)
                                var bytesRead: Int
                                var lastProgress = -1

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    downloadedBytes += bytesRead

                                    val progress = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        _downloadState.value = DownloadState.Downloading(progress, downloadedBytes, totalBytes)
                                    }
                                }
                                output.flush()
                            }
                        }

                        if (!tempFile.exists() || tempFile.length() <= 0) {
                            tempFile.delete()
                            lastErrorMessage = "Downloaded APK file was 0 bytes."
                            return@use
                        }

                        // VERIFY THE DOWNLOADED APK ARCHIVE BEFORE FINALIZING
                        if (!verifyApk(tempFile, updateInfo.versionName, updateInfo.expectedSha256)) {
                            Log.e("UpdateManager", "Downloaded file failed APK package verification: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
                            tempFile.delete()
                            lastErrorMessage = "APK VERIFICATION FAILED (Invalid or corrupted APK package or checksum mismatch)"
                            return@use
                        }

                        // Atomic move from tempFile to apkFile
                        if (apkFile.exists()) {
                            apkFile.delete()
                        }
                        val success = tempFile.renameTo(apkFile) || run {
                            tempFile.copyTo(apkFile, overwrite = true)
                            tempFile.delete()
                            true
                        }

                        if (success && apkFile.exists() && apkFile.length() > 0) {
                            Log.d("UpdateManager", "Successfully downloaded and verified APK (${apkFile.length()} bytes) via strategy '${strategy.name}'")
                            _downloadState.value = DownloadState.Completed(apkFile)
                            return@withContext apkFile
                        } else {
                            lastErrorMessage = "Failed to finalize downloaded APK file."
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Error downloading via strategy '${strategy.name}'", e)
                val isTimeout = e is java.net.SocketTimeoutException || e is java.io.InterruptedIOException
                lastErrorMessage = if (isTimeout) {
                    "Download timed out. Please check your internet connection and retry."
                } else {
                    "Download failed: ${e.localizedMessage ?: "Network Error"}"
                }
            }
        }

        _downloadState.value = DownloadState.Error(lastErrorMessage)
        null
    }

    fun openReleaseInBrowser(updateInfo: UpdateInfo): Boolean {
        return try {
            val targetUrl = if (updateInfo.browserDownloadUrl.isNotBlank()) {
                updateInfo.browserDownloadUrl
            } else if (updateInfo.tagName.isNotBlank()) {
                "https://github.com/$owner/$repo/releases/tag/${updateInfo.tagName}"
            } else {
                "https://github.com/$owner/$repo/releases/latest"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to open browser for release", e)
            false
        }
    }

    fun installApk(apkFile: File): Boolean {
        if (!verifyApk(apkFile)) {
            Log.e("UpdateManager", "Cannot install invalid or corrupted APK file")
            _downloadState.value = DownloadState.Error("INSTALLATION FAILED: Invalid or corrupted APK file")
            return false
        }
        return try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to launch package installer", e)
            _downloadState.value = DownloadState.Error("INSTALLER FAILED: ${e.localizedMessage}")
            false
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
}
