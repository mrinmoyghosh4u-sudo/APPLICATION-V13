package com.example.util.update

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateManagerTest {

    @Test
    fun `test version comparison - latest is newer`() {
        assertTrue(VersionUtils.isUpdateAvailable("1.0.0", "1.0.1"))
        assertTrue(VersionUtils.isUpdateAvailable("1.0.0", "1.1.0"))
        assertTrue(VersionUtils.isUpdateAvailable("1.0.0", "2.0.0"))
    }

    @Test
    fun `test version comparison - no update when equal`() {
        assertFalse(VersionUtils.isUpdateAvailable("1.0.0", "1.0.0"))
        assertFalse(VersionUtils.isUpdateAvailable("1.0.1", "1.0.1"))
    }

    @Test
    fun `test version comparison - downgrade prevention`() {
        assertFalse(VersionUtils.isUpdateAvailable("1.0.1", "1.0.0"))
        assertFalse(VersionUtils.isUpdateAvailable("2.0.0", "1.9.9"))
    }

    @Test
    fun `test version comparison - handles v prefix`() {
        assertTrue(VersionUtils.isUpdateAvailable("v1.0.0", "v1.0.1"))
        assertFalse(VersionUtils.isUpdateAvailable("v1.0.1", "v1.0.1"))
        assertFalse(VersionUtils.isUpdateAvailable("v1.0.2", "v1.0.1"))
    }

    @Test
    fun `test cleanVersion formatting`() {
        assertEquals("1.0.1", VersionUtils.cleanVersion("v1.0.1"))
        assertEquals("1.0.1", VersionUtils.cleanVersion("V1.0.1"))
        assertEquals("1.0.1", VersionUtils.cleanVersion(" 1.0.1 "))
    }

    @Test
    fun `test github release json parsing with valid apk asset`() {
        val jsonString = """
            {
              "tag_name": "v1.0.1",
              "name": "Release v1.0.1",
              "body": "Fixed order execution bug.",
              "published_at": "2026-08-10T10:00:00Z",
              "assets": [
                {
                  "name": "app-release.apk",
                  "browser_download_url": "https://github.com/mrinmoyghosh4u-sudo/APPLICATION-V12/releases/download/v1.0.1/app-release.apk",
                  "size": 15420000
                }
              ]
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val tagName = json.getString("tag_name")
        val cleanVersion = VersionUtils.cleanVersion(tagName)
        val assets = json.getJSONArray("assets")
        val asset = assets.getJSONObject(0)

        assertEquals("v1.0.1", tagName)
        assertEquals("1.0.1", cleanVersion)
        assertEquals("app-release.apk", asset.getString("name"))
        assertEquals("https://github.com/mrinmoyghosh4u-sudo/APPLICATION-V12/releases/download/v1.0.1/app-release.apk", asset.getString("browser_download_url"))
        assertEquals(15420000L, asset.getLong("size"))
        assertTrue(VersionUtils.isUpdateAvailable("1.0.0", cleanVersion))
    }

    @Test
    fun `test github release json parsing with missing apk asset`() {
        val jsonString = """
            {
              "tag_name": "v1.0.1",
              "name": "Release v1.0.1",
              "body": "Fixed bug.",
              "assets": [
                {
                  "name": "source.zip",
                  "browser_download_url": "https://github.com/mrinmoyghosh4u-sudo/APPLICATION-V12/archive/refs/tags/v1.0.1.zip",
                  "size": 1200000
                }
              ]
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val assets = json.getJSONArray("assets")
        var apkUrl = ""
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                apkUrl = asset.getString("browser_download_url")
                break
            }
        }

        assertTrue(apkUrl.isBlank())
    }

    @Test
    fun `test update diagnostics model formatting`() {
        val diag = UpdateDiagnostics(
            httpStatus = "200",
            repository = "mrinmoyghosh4u-sudo/APPLICATION-V12",
            releaseCount = 1,
            latestTag = "v1.0.1",
            apkAssetName = "app-release.apk",
            apkAssetUrl = "https://github.com/...",
            installedVersion = "1.0.0",
            latestVersion = "1.0.1"
        )

        assertEquals("200", diag.httpStatus)
        assertEquals("mrinmoyghosh4u-sudo/APPLICATION-V12", diag.repository)
        assertEquals(1, diag.releaseCount)
        assertEquals("v1.0.1", diag.latestTag)
    }

    @Test
    fun `test secure token manager encryption and clearing`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val tokenManager = com.example.util.SecureTokenManager.getInstance(context)

        tokenManager.clearGithubToken()
        assertFalse(tokenManager.hasGithubToken())
        assertEquals("NOT SET", tokenManager.getMaskedGithubToken())

        val sampleToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz"
        assertTrue(tokenManager.saveGithubToken(sampleToken))
        assertTrue(tokenManager.hasGithubToken())
        assertEquals(sampleToken, tokenManager.getGithubToken())
        assertEquals("●●●●●●●●", tokenManager.getMaskedGithubToken())

        tokenManager.clearGithubToken()
        assertFalse(tokenManager.hasGithubToken())
        assertEquals("", tokenManager.getGithubToken())
        assertEquals("NOT SET", tokenManager.getMaskedGithubToken())
    }
}
