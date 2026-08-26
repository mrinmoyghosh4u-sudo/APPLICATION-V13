package com.example

import com.example.data.model.MarketDataStore
import com.example.data.network.MStockMarketDataService
import com.example.data.network.ProviderHealthManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MStockMarketDataTest {

    private lateinit var healthManager: ProviderHealthManager

    @Before
    fun setUp() {
        healthManager = ProviderHealthManager()
    }

    @Test
    fun test1_exchangeCodeMapping_allExchangesAndUnknown() {
        val service = MStockMarketDataService(null, null, healthManager)
        assertEquals("NSE", service.mapExchangeCode(1))
        assertEquals("NFO", service.mapExchangeCode(2))
        assertEquals("BSE", service.mapExchangeCode(3))
        assertEquals("BFO", service.mapExchangeCode(4))
        assertEquals("CDS", service.mapExchangeCode(5))
        assertEquals("MCX", service.mapExchangeCode(6))
        assertEquals("UNKNOWN", service.mapExchangeCode(0))
        assertEquals("UNKNOWN", service.mapExchangeCode(99))
    }

    @Test
    fun test2_validBinaryPacket_littleEndian_parsesCorrectly() {
        val buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(32.toShort()) // Length
        buffer.put(1.toByte())        // Mode
        buffer.put(1.toByte())        // Exchange Code 1 = NSE
        buffer.putInt(2885)           // Token 2885
        buffer.putInt(250050)         // LTP in paise = 2500.50
        buffer.putInt(249000)         // Open
        buffer.putInt(251000)         // High
        buffer.putInt(248000)         // Low
        buffer.putInt(249500)         // Close
        buffer.putLong(100000L)       // Volume
        val bytes = buffer.array()

        val service = MStockMarketDataService(null, null, healthManager)
        service.parseBinaryPacket(bytes)

        val tick = MarketDataStore.getTick("NSE", "2885") ?: MarketDataStore.getTick("2885")
        assertNotNull("Tick must be ingested into MarketDataStore", tick)
        if (tick != null) {
            assertEquals("mStock", tick.source)
            assertEquals(2500.50, tick.ltp, 0.01)
            assertEquals("LIVE", tick.state)
        }

        val healthState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertTrue(healthState.firstTickReceived)
        assertEquals("LIVE", healthState.status)
    }

    @Test
    fun test3_malformedAndTruncatedPackets_handledSafely() {
        val service = MStockMarketDataService(null, null, healthManager)

        // Empty bytes
        service.parseBinaryPacket(byteArrayOf())

        // Less than 12 bytes
        service.parseBinaryPacket(byteArrayOf(0x01, 0x02, 0x03))

        // Truncated header claims 32 bytes but only 16 bytes provided
        val buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(32.toShort())
        buf.put(1.toByte())
        buf.put(1.toByte())
        buf.putInt(100)
        buf.putInt(5000)
        service.parseBinaryPacket(buf.array())

        // Out of bounds exchange code (99)
        val bufInvalidExch = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        bufInvalidExch.putShort(32.toShort())
        bufInvalidExch.put(1.toByte())
        bufInvalidExch.put(99.toByte()) // Invalid Exchange Code
        bufInvalidExch.putInt(999)
        bufInvalidExch.putInt(10000)
        service.parseBinaryPacket(bufInvalidExch.array())

        // None of these should crash or pollute store with invalid exchange
        val tick = MarketDataStore.getTick("UNKNOWN", "999")
        assertNull(tick)
    }

    @Test
    fun test4_authenticationStateFlow_andFirstTickLIVE() {
        val service = MStockMarketDataService(null, null, healthManager)

        // Report connecting
        healthManager.reportConnecting(ProviderHealthManager.PROVIDER_MSTOCK)
        assertEquals("WEBSOCKET_CONNECTING", healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK).status)

        // Report authenticating
        healthManager.reportAuthenticating(ProviderHealthManager.PROVIDER_MSTOCK)
        assertEquals("AUTHENTICATING", healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK).status)

        // Trigger auth success
        service.onAuthenticationSuccess()
        val authState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertTrue(authState.authenticated)
        assertFalse("Authenticated without tick MUST NOT be marked healthy/LIVE", authState.healthy)

        // Send valid tick
        val buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(32.toShort())
        buffer.put(1.toByte())
        buffer.put(1.toByte()) // NSE
        buffer.putInt(3045)
        buffer.putInt(150000) // 1500.00
        service.parseBinaryPacket(buffer.array())

        val liveState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertEquals("LIVE", liveState.status)
        assertTrue(liveState.firstTickReceived)
        assertTrue(liveState.healthy)
    }

    @Test
    fun test5_subscriptionBeforeAuthentication_doesNotSendPayloadEarly() {
        val service = MStockMarketDataService(null, null, healthManager)

        // Subscribe before auth
        service.subscribe("NSE", listOf("2885", "3045"))
        assertTrue("Tokens should be registered", service.hasActiveSubscription())

        // Verify state is not LIVE or SUBSCRIBED prematurely
        val stateBeforeAuth = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertFalse(stateBeforeAuth.healthy)

        // Now authenticate
        service.onAuthenticationSuccess()
        val stateAfterAuth = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertTrue(stateAfterAuth.authenticated)
        assertEquals("SUBSCRIBED", stateAfterAuth.subscriptionState)
    }

    @Test
    fun test6_reconnectStateFlow() {
        val service = MStockMarketDataService(null, null, healthManager)
        service.reconnect()

        // When unconfigured, reconnect transitions through RECONNECTING -> DISCONNECTED -> NOT_CONFIGURED
        assertEquals("NOT_CONFIGURED", service.connectionState.value)
    }

    @Test
    fun test7_strictTextMessageAuthValidation() {
        val service = MStockMarketDataService(null, null, healthManager)

        // 1. Non-auth message with status="success" must NOT trigger authentication
        val nonAuthJson = """{"type": "market_status", "status": "success", "message": "Market is open"}"""
        service.parseTextMessage(nonAuthJson)
        assertFalse("Generic message must not authenticate service", healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK).authenticated)

        // 2. Explicit auth response with status="success" MUST trigger authentication
        val validAuthJson = """{"type": "auth_response", "status": "success", "code": 200, "message": "Authentication successful"}"""
        service.parseTextMessage(validAuthJson)
        assertEquals("SUBSCRIBED", service.connectionState.value)
        assertTrue("Valid auth response must authenticate service", healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK).authenticated)

        // 3. Explicit auth error response must set AUTH_FAILED
        val failService = MStockMarketDataService(null, null, healthManager)
        val errorAuthJson = """{"type": "login_response", "status": "failed", "code": 401, "message": "Invalid token"}"""
        failService.parseTextMessage(errorAuthJson)
        assertEquals("AUTH_FAILED", failService.connectionState.value)
    }

    @Test
    fun test8_multiPacketBinaryStream_parsesSequentially() {
        val service = MStockMarketDataService(null, null, healthManager)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, true)
        service.onAuthenticationSuccess()

        val buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)

        // Packet 1: NSE Token 2885, LTP 2500.50
        buffer.putShort(36.toShort())
        buffer.put(1.toByte()) // Mode
        buffer.put(1.toByte()) // Exchange Code 1 = NSE
        buffer.putInt(2885)
        buffer.putInt(250050)  // LTP
        buffer.putInt(249000)
        buffer.putInt(251000)
        buffer.putInt(248000)
        buffer.putInt(249500)
        buffer.putLong(100000L)

        // Packet 2: NFO Token 54321, LTP 125.50
        buffer.putShort(36.toShort())
        buffer.put(1.toByte()) // Mode
        buffer.put(2.toByte()) // Exchange Code 2 = NFO
        buffer.putInt(54321)
        buffer.putInt(12550)   // LTP
        buffer.putInt(12000)
        buffer.putInt(13000)
        buffer.putInt(11500)
        buffer.putInt(12200)
        buffer.putLong(50000L)

        val multiPacketBytes = buffer.array()
        service.parseBinaryPacket(multiPacketBytes)

        val tick1 = MarketDataStore.getTick("NSE", "2885") ?: MarketDataStore.getTick("2885")
        assertNotNull("Tick 1 (NSE:2885) must be ingested", tick1)
        assertEquals(2500.50, tick1?.ltp ?: 0.0, 0.01)

        val tick2 = MarketDataStore.getTick("NFO", "54321") ?: MarketDataStore.getTick("54321")
        assertNotNull("Tick 2 (NFO:54321) must be ingested", tick2)
        assertEquals(125.50, tick2?.ltp ?: 0.0, 0.01)

        val healthState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertTrue(healthState.firstTickReceived)
        assertEquals("LIVE", healthState.status)
    }
}
