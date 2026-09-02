package com.example.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OAuthStateSecurityTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.clearPendingOAuthSession()
    }

    @Test
    fun testValidOAuthStateValidation() {
        val state = "valid_oauth_state_123456"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "UPSTOX",
            state = state,
            createdAt = System.currentTimeMillis(),
            redirectUri = "kingkhan://oauth/callback",
            consumed = false
        )

        val result = sessionManager.validateAndConsumeOAuthSession("UPSTOX", state)
        assertTrue("Valid state must be accepted", result is SessionManager.OAuthValidationResult.Valid)
        assertTrue("Session must be consumed after validation", sessionManager.pendingOAuthSession?.consumed == true)
    }

    @Test
    fun testBlankCallbackStateRejected() {
        val state = "valid_stored_state"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "UPSTOX",
            state = state,
            createdAt = System.currentTimeMillis(),
            redirectUri = "kingkhan://oauth/callback",
            consumed = false
        )

        val resultBlank = sessionManager.validateAndConsumeOAuthSession("UPSTOX", "")
        assertTrue("Blank callback state must be rejected", resultBlank is SessionManager.OAuthValidationResult.MissingCallbackState)

        val resultSpaces = sessionManager.validateAndConsumeOAuthSession("UPSTOX", "   ")
        assertTrue("Whitespace-only callback state must be rejected", resultSpaces is SessionManager.OAuthValidationResult.MissingCallbackState)
    }

    @Test
    fun testMissingPendingSessionRejected() {
        sessionManager.clearPendingOAuthSession()
        val result = sessionManager.validateAndConsumeOAuthSession("UPSTOX", "some_state")
        assertTrue("Missing pending session must be rejected", result is SessionManager.OAuthValidationResult.MissingPendingSession)
    }

    @Test
    fun testMissingStoredStateRejected() {
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "UPSTOX",
            state = "",
            createdAt = System.currentTimeMillis(),
            redirectUri = "kingkhan://oauth/callback",
            consumed = false
        )

        val result = sessionManager.validateAndConsumeOAuthSession("UPSTOX", "some_callback_state")
        assertTrue("Blank stored state must be rejected", result is SessionManager.OAuthValidationResult.MissingStoredState)
    }

    @Test
    fun testMismatchedStateRejected() {
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "FYERS",
            state = "expected_state_abc",
            createdAt = System.currentTimeMillis(),
            redirectUri = "kingkhan://oauth/callback",
            consumed = false
        )

        val result = sessionManager.validateAndConsumeOAuthSession("FYERS", "malicious_injected_state_xyz")
        assertTrue("Mismatched state must be rejected", result is SessionManager.OAuthValidationResult.StateMismatch)
        assertFalse("State mismatch must NOT mark session consumed", sessionManager.pendingOAuthSession?.consumed == true)
    }

    @Test
    fun testExpiredSessionRejected() {
        val oldTimestamp = System.currentTimeMillis() - (16 * 60 * 1000L) // 16 mins ago (> 15m window)
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "UPSTOX",
            state = "expired_state_123",
            createdAt = oldTimestamp,
            redirectUri = "kingkhan://oauth/callback",
            consumed = false
        )

        val result = sessionManager.validateAndConsumeOAuthSession("UPSTOX", "expired_state_123")
        assertTrue("Expired session (>15m) must be rejected", result is SessionManager.OAuthValidationResult.SessionExpired)
    }

    @Test
    fun testReusedAlreadyConsumedStateRejected() {
        val state = "replayed_state_789"
        sessionManager.pendingOAuthSession = SessionManager.PendingOAuthSession(
            provider = "DHAN",
            state = state,
            createdAt = System.currentTimeMillis(),
            redirectUri = "kingkhan://oauth/callback",
            consumed = false
        )

        // First attempt -> Valid
        val firstResult = sessionManager.validateAndConsumeOAuthSession("DHAN", state)
        assertTrue("First validation must succeed", firstResult is SessionManager.OAuthValidationResult.Valid)

        // Second attempt with same state -> Rejected
        val replayResult = sessionManager.validateAndConsumeOAuthSession("DHAN", state)
        assertTrue("Replay attempt must be rejected as AlreadyConsumed", replayResult is SessionManager.OAuthValidationResult.AlreadyConsumed)
    }
}
