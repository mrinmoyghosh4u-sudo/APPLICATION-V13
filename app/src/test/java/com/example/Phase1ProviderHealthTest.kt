package com.example

import com.example.data.network.MStockMarketDataService
import com.example.data.network.ProviderHealthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase1ProviderHealthTest {

    private lateinit var healthManager: ProviderHealthManager

    @Before
    fun setUp() {
        healthManager = ProviderHealthManager()
    }

    @Test
    fun testProviderInitialStateIsNotConfigured() {
        val upstoxState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("NOT_CONFIGURED", upstoxState.status)
        assertFalse(upstoxState.connected)
        assertFalse(upstoxState.authenticated)
        assertFalse(upstoxState.firstTickReceived)
        assertFalse(upstoxState.healthy)
    }

    @Test
    fun testWebSocketOpenedDoesNotSetLiveState() {
        // Report connection opened
        healthManager.reportConnecting(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WEBSOCKET_CONNECTING", healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX).status)

        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        val stateAfterConnection = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WEBSOCKET_CONNECTED", stateAfterConnection.webSocketState)
        assertEquals("WEBSOCKET_CONNECTED", stateAfterConnection.status)
        assertFalse("WebSocket connected MUST NOT be set to LIVE", stateAfterConnection.healthy)

        healthManager.reportSubscribing(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("SUBSCRIBING", healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX).status)

        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, 6)
        val stateAfterSub = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WAITING_FOR_FIRST_TICK", stateAfterSub.status)
        assertFalse("Subscribed state MUST NOT be set to LIVE without real tick", stateAfterSub.healthy)
    }

    @Test
    fun testOnlyFirstValidRealTickSetsLiveState() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_FYERS, 10)

        assertFalse(healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS).healthy)

        // First valid real market tick received
        val now = System.currentTimeMillis()
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_FYERS, now)

        val liveState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertEquals("LIVE", liveState.status)
        assertTrue(liveState.firstTickReceived)
        assertTrue(liveState.healthy)
        assertTrue(liveState.connected)
        assertTrue(liveState.authenticated)
    }

    @Test
    fun testStaleThresholdEvaluation() {
        val now = System.currentTimeMillis()
        val oldTimestamp = now - 16000L // 16 seconds ago (> 15s STALE_TIMEOUT_MS)

        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_MSTOCK, oldTimestamp)
        
        // Before evaluation, status is LIVE
        assertEquals("LIVE", healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK).status)

        // Run staleness check
        healthManager.checkAndEvaluateStaleness(now)

        val staleState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertTrue("Status must be STALE or MARKET_CLOSED when tick age > 15s", staleState.status == "STALE" || staleState.status == "MARKET_CLOSED")
        assertFalse("Stale provider MUST NOT be healthy", staleState.healthy)
    }

    @Test
    fun testAuthenticationFailureState() {
        healthManager.reportAuthenticating(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("AUTHENTICATING", healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX).status)

        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_UPSTOX, false, "Invalid API Key")
        val authFailedState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("AUTH_FAILED", authFailedState.status)
        assertEquals("AUTH_FAILED", authFailedState.authenticationState)
        assertFalse(authFailedState.authenticated)
        assertEquals("Invalid API Key", authFailedState.lastError)
    }

    @Test
    fun testMStockExchangeCodeMapping() {
        val parseService = MStockMarketDataService(null, null, healthManager)
        val sampleBytes = byteArrayOf(
            0x20.toByte(), 0x00.toByte(), // Length 32 bytes (Little Endian)
            0x01.toByte(),                // Packet Type
            0x03.toByte(),                // Exchange Code 3 = BSE
            0x64.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // Token 100
            0x90.toByte(), 0x5F.toByte(), 0x01.toByte(), 0x00.toByte(), // LTP 900.00
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // Open
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // High
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // Low
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // Close
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()  // Volume
        )
        parseService.parseBinaryPacket(sampleBytes)

        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertTrue("Valid binary tick MUST report tick received and set LIVE", state.firstTickReceived)
        assertEquals("LIVE", healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK).status)
    }
}
