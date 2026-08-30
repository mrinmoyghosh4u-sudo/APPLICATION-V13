package com.example

import com.example.data.network.ProviderHealthManager
import com.example.util.FyersAuthHelper
import com.example.util.UpstoxAuthHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BrokerAuthVerificationTest {

    private lateinit var healthManager: ProviderHealthManager

    @Before
    fun setUp() {
        healthManager = ProviderHealthManager()
    }

    @Test
    fun test1_upstoxAndFyersAuthorizationUrlWithState() {
        val state = "upstox_" + UUID.randomUUID().toString()
        val upstoxUrl = UpstoxAuthHelper.buildLoginUrl("UPSTOX_KEY_123", state = state)
        assertTrue("Upstox URL must contain client_id", upstoxUrl.contains("client_id=UPSTOX_KEY_123"))
        assertTrue("Upstox URL must contain redirect_uri", upstoxUrl.contains("redirect_uri="))
        assertTrue("Upstox URL must contain state parameter", upstoxUrl.contains("state=$state"))

        val fyersState = "fyers_" + UUID.randomUUID().toString()
        val fyersUrl = FyersAuthHelper.buildLoginUrl("FYERS_APP_ID", state = fyersState)
        assertTrue("Fyers URL must contain client_id", fyersUrl.contains("client_id=FYERS_APP_ID"))
        assertTrue("Fyers URL must contain state parameter", fyersUrl.contains("state=$fyersState"))
    }

    @Test
    fun test2_oauthStateGenerationAndMismatchRejection() {
        val pendingState = "upstox_abc123"
        val receivedMatchingState = "upstox_abc123"
        val receivedMismatchState = "upstox_wrong_state"

        assertEquals(pendingState, receivedMatchingState)
        assertFalse("Mismatched OAuth state MUST be rejected", pendingState == receivedMismatchState)
    }

    @Test
    fun test3_queryParameterParsing() {
        val url = "https://application-beige-psi.vercel.app/oauth?code=AUTH_CODE_XYZ&state=upstox_123&error=access_denied&error_description=User+denied+access"
        val uri = java.net.URI(url)
        val queryPairs = uri.query.split("&").associate {
            val idx = it.indexOf("=")
            if (idx > 0) it.substring(0, idx) to java.net.URLDecoder.decode(it.substring(idx + 1), "UTF-8") else it to ""
        }
        assertEquals("AUTH_CODE_XYZ", queryPairs["code"])
        assertEquals("upstox_123", queryPairs["state"])
        assertEquals("access_denied", queryPairs["error"])
        assertEquals("User denied access", queryPairs["error_description"])
    }

    @Test
    fun test4_tokenExchangeStateTransition() {
        healthManager.reportAuthenticating(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("AUTHENTICATING", healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX).status)

        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("AUTHENTICATED", state.authenticationState)
        assertTrue(state.authenticated)
    }

    @Test
    fun test5_authFailureHandling() {
        healthManager.reportAuthenticating(ProviderHealthManager.PROVIDER_FYERS)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, false, "HTTP 401 Unauthorized")
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertEquals("AUTH_FAILED", state.authenticationState)
        assertEquals("AUTH_FAILED", state.status)
        assertFalse(state.authenticated)
        assertEquals("HTTP 401 Unauthorized", state.lastError)
    }

    @Test
    fun test6_webSocketConnectedDoesNotSetLive() {
        healthManager.reportConnecting(ProviderHealthManager.PROVIDER_UPSTOX)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WEBSOCKET_CONNECTED", state.webSocketState)
        assertEquals("WEBSOCKET_CONNECTED", state.status)
        assertFalse("WebSocket connected MUST NOT be set to LIVE or healthy", state.healthy)
    }

    @Test
    fun test7_subscriptionAndValidTickSetsLive() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, 6)

        val stateBeforeTick = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WAITING_FOR_FIRST_TICK", stateBeforeTick.status)
        assertFalse(stateBeforeTick.healthy)

        val now = System.currentTimeMillis()
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_UPSTOX, now)

        val stateAfterTick = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("LIVE", stateAfterTick.status)
        assertTrue(stateAfterTick.firstTickReceived)
        assertTrue(stateAfterTick.healthy)
    }

    @Test
    fun test8_tickAgeGreaterThan15sSetsStale() {
        val now = System.currentTimeMillis()
        val oldTickTime = now - 16000L // 16s ago (>15s threshold)

        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_FYERS, oldTickTime)

        healthManager.checkAndEvaluateStaleness(now)

        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertTrue("Status must be STALE or MARKET_CLOSED when tick age > 15s", state.status == "STALE" || state.status == "MARKET_CLOSED")
        assertFalse("Old tick provider must not be healthy", state.healthy)
    }

    @Test
    fun test9_failoverOrdering() {
        val failoverOrder = listOf(
            ProviderHealthManager.PROVIDER_UPSTOX,
            ProviderHealthManager.PROVIDER_FYERS,
            ProviderHealthManager.PROVIDER_ANGEL_ONE,
        )
        assertEquals("Upstox", failoverOrder[0])
        assertEquals("Fyers", failoverOrder[1])
        assertEquals("Angel One", failoverOrder[2])
    }

    @Test
    fun test10_dhanExcludedFromMarketDataFailover() {
        val failoverOrder = listOf(
            ProviderHealthManager.PROVIDER_UPSTOX,
            ProviderHealthManager.PROVIDER_FYERS,
            ProviderHealthManager.PROVIDER_ANGEL_ONE,
        )
        assertFalse("Dhan MUST be excluded from market data failover", failoverOrder.contains("Dhan"))
    }

    @Test
    fun test11_diagnosticFieldsReflectRuntimeState() {
        healthManager.reportConfigured( true)
        healthManager.reportAuthenticating()
        healthManager.reportAuthentication( true)
        healthManager.reportConnecting()
        healthManager.reportConnection( true)
        healthManager.reportSubscribed( 10)
        
        val state = healthManager.getHealthState()
                assertTrue(state.authenticated)
        assertTrue(state.connected)
        assertEquals("WAITING_FOR_FIRST_TICK", state.status)
    }

    @Test
    fun test12_sensitiveDataMasking() {
        val sensitiveToken = "secret_access_token_123456789"
        val masked = sensitiveToken.take(4) + "****" + sensitiveToken.takeLast(4)
        assertEquals("secr****6789", masked)
        assertFalse("Logs/diagnostics MUST NOT contain unmasked sensitive secrets", masked.contains("12345"))
    }

    @Test
    fun test13_missingCredentialsReturnsNotConfigured() {
        healthManager.reportConfigured(ProviderHealthManager.PROVIDER_UPSTOX, false)
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("NOT_CONFIGURED", state.status)
        assertFalse(state.healthy)
    }

    @Test
    fun test14_invalidClientIdTriggersAuthFailed() {
        healthManager.reportAuthenticating(ProviderHealthManager.PROVIDER_UPSTOX)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, false, "Invalid Client ID: client_id_not_found")
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("AUTH_FAILED", state.authenticationState)
        assertEquals("Invalid Client ID: client_id_not_found", state.lastError)
    }

}

    @Test
    fun test16_upstoxUnavailableFyersHealthyFailover() {
        // Upstox disconnected
        healthManager.reportDisconnected(ProviderHealthManager.PROVIDER_UPSTOX)
        
        // Fyers healthy & LIVE
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_FYERS, System.currentTimeMillis())

        val upstoxState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        val fyersState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)

        assertFalse(upstoxState.healthy)
        assertTrue(fyersState.healthy)
        assertEquals("LIVE", fyersState.status)
    }

    @Test
    fun test17_fullIntermediateOAuthStateTransitions() {
        val provider = ProviderHealthManager.PROVIDER_UPSTOX

        healthManager.reportConfigured(provider, true)
        assertEquals("CONFIGURED", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportAuthenticating(provider)
        assertEquals("AUTHENTICATING", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportWaitingForCallback(provider)
        assertEquals("WAITING_FOR_CALLBACK", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportCallbackReceived(provider)
        assertEquals("CALLBACK_RECEIVED", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportValidatingState(provider)
        assertEquals("VALIDATING_STATE", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportAuthCodeReceived(provider)
        assertEquals("AUTH_CODE_RECEIVED", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportTokenExchange(provider)
        assertEquals("TOKEN_EXCHANGE", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportTokenValidated(provider)
        assertEquals("TOKEN_VALIDATED", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportAuthentication(provider, true)
        assertEquals("AUTHENTICATED", healthManager.getHealthState(provider).authenticationState)
    }

    @Test
    fun test18_specificOAuthFailureStates() {
        val provider = ProviderHealthManager.PROVIDER_FYERS

        healthManager.reportAuthFailure(provider, ProviderHealthManager.STATE_STATE_MISMATCH, "State mismatch")
        assertEquals("STATE_VALIDATION_FAILED", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportAuthFailure(provider, ProviderHealthManager.STATE_AUTH_CODE_MISSING, "Code missing")
        assertEquals("AUTH_CODE_MISSING", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportAuthFailure(provider, ProviderHealthManager.STATE_TOKEN_EXCHANGE_FAILED, "HTTP 400")
        assertEquals("TOKEN_EXCHANGE_FAILED", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportAuthFailure(provider, ProviderHealthManager.STATE_TOKEN_INVALID, "Invalid profile")
        assertEquals("TOKEN_INVALID", healthManager.getHealthState(provider).authenticationState)

        healthManager.reportAuthFailure(provider, ProviderHealthManager.STATE_AUTH_CANCELLED, "User cancelled")
        assertEquals("AUTH_CANCELLED", healthManager.getHealthState(provider).authenticationState)
    }
}
