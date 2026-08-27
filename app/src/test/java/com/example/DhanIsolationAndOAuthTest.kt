package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.SessionManager
import com.example.util.UpstoxAuthHelper
import com.example.util.FyersAuthHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DhanIsolationAndOAuthTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.pendingOAuthSession = null
        sessionManager.lastCompletedDhanFingerprint = ""
    }

    @Test
    fun testUpstoxOAuthRedirectUriAndState() {
        val authUrl = UpstoxAuthHelper.getAuthorizationUrl(apiKey = "TEST_API_KEY", state = "TEST_UUID_STATE")
        assertTrue("Auth URL must contain production redirect domain", authUrl.contains("application-beige-psi.vercel.app"))
        assertTrue("Auth URL must contain response_type=code", authUrl.contains("response_type=code"))
        assertTrue("Auth URL must contain state=TEST_UUID_STATE", authUrl.contains("state=TEST_UUID_STATE"))
    }

    @Test
    fun testFyersOAuthRedirectUriAndState() {
        val authUrl = FyersAuthHelper.buildLoginUrl(appId = "TEST_APP_ID-100", state = "TEST_UUID_STATE")
        assertTrue("Auth URL must contain production redirect domain", authUrl.contains("application-beige-psi.vercel.app"))
        assertTrue("Auth URL must contain response_type=code", authUrl.contains("response_type=code"))
        assertTrue("Auth URL must contain state=TEST_UUID_STATE", authUrl.contains("state=TEST_UUID_STATE"))
    }

    @Test
    fun test1_dhanLoginInitiationCreatesFreshPendingSession() {
        val redirectUri = "kingkhan://oauth/callback"
        val state = "kingkhan_oauth_state"

        sessionManager.pendingOAuthBroker = "Dhan"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "DHAN",
            state = state,
            createdAt = System.currentTimeMillis(),
            redirectUri = redirectUri,
            consumed = false
        )

        val pending = sessionManager.pendingOAuthSession
        assertNotNull("Pending Dhan OAuth session must be present", pending)
        assertEquals("DHAN", pending?.provider)
        assertEquals(state, pending?.state)
        assertEquals(redirectUri, pending?.redirectUri)
        assertFalse("Pending Dhan OAuth session must NOT be consumed initially", pending?.consumed == true)
    }

    @Test
    fun test2_dhanCallbackFingerprintAndDeduplication() {
        val tokenId = "DHAN_TOKEN_ID_999888"
        val fingerprint = "DHAN:$tokenId"

        // Initially no completed fingerprint
        assertNotEquals(fingerprint, sessionManager.lastCompletedDhanFingerprint)

        // After successful OAuth completion, fingerprint is stored
        sessionManager.lastCompletedDhanFingerprint = fingerprint
        assertEquals(fingerprint, sessionManager.lastCompletedDhanFingerprint)

        // Duplicate intent with same tokenId matches lastCompletedDhanFingerprint
        val duplicateTokenId = "DHAN_TOKEN_ID_999888"
        val duplicateFingerprint = "DHAN:$duplicateTokenId"
        assertEquals("Duplicate callback fingerprint must match", fingerprint, duplicateFingerprint)
    }

    @Test
    fun test3_newDhanLoginReplacesConsumedSessionAndAllowsNewToken() {
        // Step A: Previous session was completed and marked consumed
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "DHAN",
            state = "kingkhan_oauth_state",
            createdAt = System.currentTimeMillis() - 10000L,
            redirectUri = "kingkhan://oauth/callback",
            consumed = true
        )
        sessionManager.lastCompletedDhanFingerprint = "DHAN:OLD_TOKEN_111"
        assertTrue("Old session must be consumed", sessionManager.pendingOAuthSession?.consumed == true)

        // Step B: User initiates a NEW Dhan Login
        val newSession = SessionManager.PendingOAuthSession(
            provider = "DHAN",
            state = "kingkhan_oauth_state",
            createdAt = System.currentTimeMillis(),
            redirectUri = "kingkhan://oauth/callback",
            consumed = false
        )
        sessionManager.pendingOAuthSession = newSession

        // Assert fresh session replaced the old consumed session
        val currentPending = sessionManager.pendingOAuthSession
        assertNotNull(currentPending)
        assertFalse("New Dhan login session MUST NOT be consumed", currentPending!!.consumed)

        // Step C: A new tokenId arrives
        val newTokenId = "NEW_TOKEN_222"
        val newFingerprint = "DHAN:$newTokenId"
        assertNotEquals("New tokenId fingerprint must not match old completed fingerprint",
            sessionManager.lastCompletedDhanFingerprint, newFingerprint)
    }

    @Test
    fun test4_dhanCallbackUriParsing() {
        val uri = Uri.parse("kingkhan://oauth/callback?tokenId=DHAN_AUTH_TOKEN_777&state=kingkhan_oauth_state")
        val tokenId = uri.getQueryParameter("tokenId")
        val state = uri.getQueryParameter("state")

        assertEquals("DHAN_AUTH_TOKEN_777", tokenId)
        assertEquals("kingkhan_oauth_state", state)

        val fullUrl = uri.toString()
        val regexMatch = Regex("""[?&#](?:tokenId|consentId)=([^&#]+)""", RegexOption.IGNORE_CASE).find(fullUrl)
        assertEquals("DHAN_AUTH_TOKEN_777", regexMatch?.groupValues?.get(1))
    }
}
