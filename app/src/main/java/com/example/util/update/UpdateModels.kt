package com.example.util.update

import java.io.File

data class UpdateDiagnostics(
    val githubOwner: String = "mrinmoyghosh4u-sudo",
    val githubRepo: String = "APPLICATION-V13",
    val releaseApiUrl: String = "https://api.github.com/repos/mrinmoyghosh4u-sudo/APPLICATION-V13/releases/latest",
    val repository: String = "mrinmoyghosh4u-sudo/APPLICATION-V13",
    val githubApiStatus: String = "PASS",
    val repoAccessStatus: String = "PASS",
    val authStatus: String = "PASS",
    val latestReleaseApiStatus: String = "PASS",
    val httpStatus: String = "200",
    val responseContentType: String = "N/A",
    val responseSnippet: String = "N/A",
    val releaseCount: Int = 0,
    val latestTag: String = "N/A",
    val isDraft: String = "false",
    val isPrerelease: String = "false",
    val isPublished: String = "true",
    val installedVersion: String = "N/A",
    val latestVersion: String = "N/A",
    val versionComparisonStatus: String = "N/A",
    val apkAssetName: String = "N/A",
    val apkAssetState: String = "N/A",
    val apkAssetUrl: String = "N/A",
    val apkAssetFound: String = "NOT FOUND",
    val apkAssetFormattedSize: String = "0 B",
    val apkDownloadStatus: String = "READY",
    val installerStatus: String = "VERIFIED (FileProvider)",
    val checksumStatus: String = "N/A",
    val verifiedSha256: String = "N/A",
    val resultStatus: String = "N/A",
    val hasAuthToken: Boolean = false
)

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val versionCode: Int = 0,
    val releaseName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val publishedAt: String,
    val apkSize: Long = 0L,
    val apkAssetName: String = "",
    val assetId: Long = 0L,
    val assetApiUrl: String = "",
    val browserDownloadUrl: String = "",
    val expectedSha256: String? = null,
    val diagnostics: UpdateDiagnostics = UpdateDiagnostics()
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: UpdateInfo, val diagnostics: UpdateDiagnostics) : UpdateCheckResult()
    data class UpToDate(val currentVersionName: String, val diagnostics: UpdateDiagnostics) : UpdateCheckResult()
    data class Error(val message: String, val diagnostics: UpdateDiagnostics) : UpdateCheckResult()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Completed(val apkFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
