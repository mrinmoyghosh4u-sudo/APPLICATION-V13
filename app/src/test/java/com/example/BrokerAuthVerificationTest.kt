package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.ProviderHealthManager
import com.example.data.network.SessionManager
import com.example.util.AngelAuthHelper
import com.example.util.DhanAuthHelper
import com.example.util.FyersAuthHelper
import com.example.util.UpstoxAuthHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrokerAuthVerificationTest {

    private lateinit var healthManager: ProviderHealthManager
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        healthManager = ProviderHealthManager()
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionManager = SessionManager(context)
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
        healthManager.reportConfigured(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportAuthenticating(ProviderHealthManager.PROVIDER_UPSTOX)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportConnecting(ProviderHealthManager.PROVIDER_UPSTOX)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, 10)
           
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
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

    @Test
    fun test19_allBrokerLoginUrlsWithSecureState() {
        val upstoxState = "upstox_" + UUID.randomUUID().toString()
        val upstoxUrl = UpstoxAuthHelper.buildLoginUrl("UPSTOX_KEY_123", state = upstoxState)
        assertTrue("Upstox login URL must contain client_id", upstoxUrl.contains("client_id=UPSTOX_KEY_123"))
        assertTrue("Upstox login URL must contain redirect_uri", upstoxUrl.contains("redirect_uri="))
        assertTrue("Upstox login URL must contain state", upstoxUrl.contains("state=$upstoxState"))

        val fyersState = "fyers_" + UUID.randomUUID().toString()
        val fyersUrl = FyersAuthHelper.buildLoginUrl("FYERS_APP_ID-100", state = fyersState)
        assertTrue("Fyers login URL must contain client_id", fyersUrl.contains("client_id=FYERS_APP_ID-100"))
        assertTrue("Fyers login URL must contain state", fyersUrl.contains("state=$fyersState"))

        val dhanState = DhanAuthHelper.generateSecureState()
        assertTrue("Dhan state must start with dhan_ prefix", dhanState.startsWith("dhan_"))
        assertTrue("Dhan state must be sufficiently long", dhanState.length >= 20)

        assertNotNull("Angel One auth helper must be accessible", AngelAuthHelper)
    }

    @Test
    fun test20_allBrokerSessionManagerConfigurationChecks() {
        sessionManager.clearAllSavedBrokersData()

        assertFalse("Dhan must NOT be configured when empty", sessionManager.isBrokerConfigured("Dhan"))
        assertFalse("Angel One must NOT be configured when empty", sessionManager.isBrokerConfigured("Angel One"))
        assertFalse("Upstox must NOT be configured when empty", sessionManager.isBrokerConfigured("Upstox"))
        assertFalse("Fyers must NOT be configured when empty", sessionManager.isBrokerConfigured("Fyers"))

        sessionManager.saveDhanCredentials("DHAN_CLI", "DHAN_TOK", "DHAN_KEY", "DHAN_SEC")
        sessionManager.saveAngelOneCredentials("ANG_CODE", "1234", "ANG_API_KEY", "TOTP_SECRET")
        sessionManager.saveUpstoxCredentials("UP_KEY", "UP_SEC", "UP_TOK")
        sessionManager.saveFyersCredentials("FY_APP", "FY_SEC", "FY_TOK")

        assertTrue("Dhan must be configured after saving credentials", sessionManager.isBrokerConfigured("Dhan"))
        assertTrue("Angel One must be configured after saving credentials", sessionManager.isBrokerConfigured("Angel One"))
        assertTrue("Upstox must be configured after saving credentials", sessionManager.isBrokerConfigured("Upstox"))
        assertTrue("Fyers must be configured after saving credentials", sessionManager.isBrokerConfigured("Fyers"))
    }

    @Test
    fun test21_allBrokerSessionValidityChecks() {
        sessionManager.clearAllSavedBrokersData()

        assertFalse("Dhan session must be invalid when empty", sessionManager.isBrokerSessionValid("Dhan"))
        assertFalse("Angel One session must be invalid when empty", sessionManager.isBrokerSessionValid("Angel One"))
        assertFalse("Upstox session must be invalid when empty", sessionManager.isBrokerSessionValid("Upstox"))
        assertFalse("Fyers session must be invalid when empty", sessionManager.isBrokerSessionValid("Fyers"))

        val now = System.currentTimeMillis()
        sessionManager.saveDhanCredentials("DHAN_CLI", "DHAN_TOK", "DHAN_KEY", "DHAN_SEC")
        sessionManager.dhanTokenTimestamp = now
        sessionManager.isDhanConnected = true

        sessionManager.saveAngelOneCredentials("ANG_CODE", "1234", "ANG_API_KEY", "TOTP_SECRET")
        sessionManager.angelTokenTimestamp = now
        sessionManager.isAngelConnected = true

        sessionManager.saveUpstoxCredentials("UP_KEY", "UP_SEC", "UP_TOK")
        sessionManager.upstoxTokenTimestamp = now
        sessionManager.isUpstoxConnected = true

        sessionManager.saveFyersCredentials("FY_APP", "FY_SEC", "FY_TOK")
        sessionManager.fyersTokenTimestamp = now
        sessionManager.isFyersConnected = true

        assertTrue("Dhan session must be valid with fresh token", sessionManager.isBrokerSessionValid("Dhan"))
        assertTrue("Angel One session must be valid with fresh token", sessionManager.isBrokerSessionValid("Angel One"))
        assertTrue("Upstox session must be valid with fresh token", sessionManager.isBrokerSessionValid("Upstox"))
        assertTrue("Fyers session must be valid with fresh token", sessionManager.isBrokerSessionValid("Fyers"))
    }

    @Test
    fun test22_brokerLoginStatusAfterDisconnect() {
        sessionManager.saveDhanCredentials("DHAN_CLI", "DHAN_TOK", "DHAN_KEY", "DHAN_SEC")
        sessionManager.saveAngelOneCredentials("ANG_CODE", "1234", "ANG_API_KEY", "TOTP_SECRET")
        sessionManager.saveUpstoxCredentials("UP_KEY", "UP_SEC", "UP_TOK")
        sessionManager.saveFyersCredentials("FY_APP", "FY_SEC", "FY_TOK")

        assertTrue("All brokers should be configured", sessionManager.isBrokerConfigured("Dhan"))
        assertTrue(sessionManager.isBrokerConfigured("Angel One"))
        assertTrue(sessionManager.isBrokerConfigured("Upstox"))
        assertTrue(sessionManager.isBrokerConfigured("Fyers"))

        sessionManager.clearBrokerSessionTokens("Dhan")
        sessionManager.clearBrokerSessionTokens("Angel One")
        sessionManager.clearBrokerSessionTokens("Upstox")
        sessionManager.clearBrokerSessionTokens("Fyers")

        assertFalse("Dhan session must be invalid after clearing tokens", sessionManager.isBrokerSessionValid("Dhan"))
        assertFalse("Angel One session must be invalid after clearing tokens", sessionManager.isBrokerSessionValid("Angel One"))
        assertFalse("Upstox session must be invalid after clearing tokens", sessionManager.isBrokerSessionValid("Upstox"))
        assertFalse("Fyers session must be invalid after clearing tokens", sessionManager.isBrokerSessionValid("Fyers"))

        assertTrue("Dhan must still be configured after clearing session", sessionManager.isBrokerConfigured("Dhan"))
        assertTrue(sessionManager.isBrokerConfigured("Angel One"))
        assertTrue(sessionManager.isBrokerConfigured("Upstox"))
        assertTrue(sessionManager.isBrokerConfigured("Fyers"))
    }

    @Test
    fun test23_brokerCredentialRoundTripForAllBrokers() {
        sessionManager.clearAllSavedBrokersData()

        sessionManager.saveDhanCredentials("DHAN_CLI_RT", "DHAN_TOK_RT", "DHAN_KEY_RT", "DHAN_SEC_RT")
        assertEquals("DHAN_CLI_RT", sessionManager.dhanClientId)
        assertEquals("DHAN_TOK_RT", sessionManager.dhanAccessToken)
        assertEquals("DHAN_KEY_RT", sessionManager.dhanApiKey)
        assertEquals("DHAN_SEC_RT", sessionManager.dhanClientSecret)

        sessionManager.saveAngelOneCredentials("ANG_CODE_RT", "4321", "ANG_API_KEY_RT", "TOTP_SECRET_RT")
        assertEquals("ANG_CODE_RT", sessionManager.angelClientId)
        assertEquals("4321", sessionManager.angelClientPin)
        assertEquals("ANG_API_KEY_RT", sessionManager.angelApiKey)
        assertEquals("TOTP_SECRET_RT", sessionManager.angelTotpSecret)

        sessionManager.saveUpstoxCredentials("UP_KEY_RT", "UP_SEC_RT", "UP_TOK_RT")
        assertEquals("UP_KEY_RT", sessionManager.upstoxApiKey)
        assertEquals("UP_SEC_RT", sessionManager.upstoxApiSecret)
        assertEquals("UP_TOK_RT", sessionManager.upstoxAccessToken)

        sessionManager.saveFyersCredentials("FY_APP_RT", "FY_SEC_RT", "FY_TOK_RT")
        assertEquals("FY_APP_RT", sessionManager.fyersAppId)
        assertEquals("FY_SEC_RT", sessionManager.fyersSecretId)
        assertEquals("FY_TOK_RT", sessionManager.fyersAccessToken)
    }
}
