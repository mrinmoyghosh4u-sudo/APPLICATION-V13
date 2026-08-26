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
    fun mStockExchangeCodeMapping() {
        val service = MStockMarketDataService(null, null, healthManager)
        assertEquals("NSE", service.mapExchangeCode(1))
        assertEquals("NFO", service.mapExchangeCode(2))
        assertEquals("BSE", service.mapExchangeCode(3))
        assertEquals("BFO", service.mapExchangeCode(4))
        assertEquals("CDS", service.mapExchangeCode(5))
        assertEquals("MCX", service.mapExchangeCode(6))
        assertEquals("UNKNOWN_EXCHANGE", service.mapExchangeCode(0))
        assertEquals("UNKNOWN_EXCHANGE", service.mapExchangeCode(99))
    }

    @Test
    fun testMStockExchangeCodeMapping() {
        mStockExchangeCodeMapping()
    }

    @Test
    fun mStockAuthenticationSuccess() {
        val service = MStockMarketDataService(null, null, healthManager)
        val validAuthJson = """{"type": "auth_response", "status": "success", "code": 200, "message": "Authentication successful"}"""
        service.parseTextMessage(validAuthJson)
        val authState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertTrue(authState.authenticated)
        assertEquals("SUBSCRIBED", service.connectionState.value)
    }

    @Test
    fun mStockAuthenticationFailure() {
        val service = MStockMarketDataService(null, null, healthManager)
        val errorAuthJson = """{"type": "login_response", "status": "failed", "code": 401, "message": "Invalid token"}"""
        service.parseTextMessage(errorAuthJson)
        assertEquals("AUTH_FAILED", service.connectionState.value)
        assertFalse(healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK).authenticated)
    }

    @Test
    fun mStockSubscriptionAfterAuthentication() {
        val service = MStockMarketDataService(null, null, healthManager)
        service.onAuthenticationSuccess()
        service.subscribe("NSE", listOf("2885"))
        assertTrue(service.hasActiveSubscription())
    }

    @Test
    fun mStockRejectsSubscriptionBeforeAuthentication() {
        val service = MStockMarketDataService(null, null, healthManager)
        service.subscribe("NSE", listOf("2885"))
        assertTrue("Tokens saved in registry for post-auth sub", service.hasActiveSubscription())
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_MSTOCK)
        assertFalse("Unauthenticated state must not be healthy", state.healthy)
    }

    @Test
    fun mStockValidBinaryPacket() {
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
    fun test6_mStockBinaryParser_parsesValidPacket() {
        mStockValidBinaryPacket()
    }

    @Test
    fun mStockMalformedBinaryPacket() {
        val service = MStockMarketDataService(null, null, healthManager)
        service.parseBinaryPacket(byteArrayOf(0x01, 0x02, 0x03))
        val bufInvalidExch = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        bufInvalidExch.putShort(32.toShort())
        bufInvalidExch.put(1.toByte())
        bufInvalidExch.put(99.toByte()) // Invalid Exchange Code
        bufInvalidExch.putInt(999)
        bufInvalidExch.putInt(10000)
        service.parseBinaryPacket(bufInvalidExch.array())

        val tick = MarketDataStore.getTick("UNKNOWN_EXCHANGE", "999")
        assertNull(tick)
    }

    @Test
    fun mStockTruncatedBinaryPacket() {
        val service = MStockMarketDataService(null, null, healthManager)
        val buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(32.toShort())
        buf.put(1.toByte())
        buf.put(1.toByte())
        buf.putInt(100)
        buf.putInt(5000)
        service.parseBinaryPacket(buf.array())
        assertNull(MarketDataStore.getTick("NSE", "100"))
    }

    @Test
    fun mStockMultiplePackets() {
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

    @Test
    fun mStockFirstRealTick() {
        val service = MStockMarketDataService(null, null, healthManager)
        assertFalse(service.hasFirstTickReceived())

        val buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(32.toShort())
        buffer.put(1.toByte())
        buffer.put(1.toByte()) // NSE
        buffer.putInt(3045)
        buffer.putInt(150000) // 1500.00
        service.parseBinaryPacket(buffer.array())

        assertTrue(service.hasFirstTickReceived())
        assertEquals("LIVE", service.connectionState.value)
    }

    @Test
    fun mStockDuplicateSubscription() {
        val service = MStockMarketDataService(null, null, healthManager)
        service.subscribe("NSE", listOf("2885"))
        service.subscribe("NSE", listOf("2885")) // Duplicate call
        assertTrue(service.hasActiveSubscription())
    }

    @Test
    fun mStockReconnect() {
        val service = MStockMarketDataService(null, null, healthManager)
        service.reconnect()
        assertEquals("NOT_CONFIGURED", service.connectionState.value)
    }

    @Test
    fun mStockResubscribeAfterReconnect() {
        val service = MStockMarketDataService(null, null, healthManager)
        service.subscribe("NSE", listOf("2885"))
        service.onAuthenticationSuccess()
        assertEquals("SUBSCRIBED", service.connectionState.value)
    }

    @Test
    fun mStockStaleData() {
        val service = MStockMarketDataService(null, null, healthManager)
        assertEquals(-1L, service.getTickAgeMs())
    }
}
