package com.example

import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import com.example.data.network.MStockMarketDataService
import com.example.data.network.ProviderHealthManager
import com.example.data.network.SessionManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MarketDataFailoverTest {

    private lateinit var healthManager: ProviderHealthManager

    @Before
    fun setUp() {
        healthManager = ProviderHealthManager()
    }

    @Test
    fun test1_angelOneLive_activeIsAngelOne() {
        val now = System.currentTimeMillis()
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_ANGEL_ONE, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_ANGEL_ONE, now)

        assertTrue(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE))
        assertEquals(ProviderHealthManager.PROVIDER_ANGEL_ONE, healthManager.getHealthState(ProviderHealthManager.PROVIDER_ANGEL_ONE).provider)
    }

    @Test
    fun test2_angelOneDisconnect_triggersFailoverCondition() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, false)
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE))
    }

    @Test
    fun test3_angelOneStaleAfter15s_marksStale() {
        val oldTimestamp = System.currentTimeMillis() - 20000L // 20s ago
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_ANGEL_ONE, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_ANGEL_ONE, oldTimestamp)

        healthManager.checkAndEvaluateStaleness(System.currentTimeMillis())
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_ANGEL_ONE)

        assertTrue(state.stale)
        assertFalse(state.healthy)
    }

    @Test
    fun test4_mStockConnectedNoTick_isNotLive() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, true)

        val isHealthy = healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_MSTOCK)
        assertFalse("m.Stock connected without tick MUST NOT be marked healthy/live", isHealthy)
    }

    @Test
    fun test5_mStockReceivesRealTick_marksLive() {
        val now = System.currentTimeMillis()
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_MSTOCK, now)

        val isHealthy = healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_MSTOCK)
        assertTrue("m.Stock with recent tick MUST be healthy", isHealthy)
    }

    @Test
    fun test6_mStockBinaryParser_parsesValidPacket() {
        // Construct binary packet:
        // Header: packetLength=32 (2B), packetType=1 (1B), exchangeCode=1 (1B), tokenNumber=2885 (4B)
        // Payload: rawLtp=250050 (4B, 2500.50), open=249000 (4B), high=251000 (4B), low=248000 (4B), close=249500 (4B), volume=100000L (8B)
        val buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(32.toShort())
        buffer.put(1.toByte()) // Mode
        buffer.put(1.toByte()) // Exchange Code 1 = NSE
        buffer.putInt(2885) // Token 2885 = RELIANCE
        buffer.putInt(250050) // LTP in paise = 2500.50
        buffer.putInt(249000) // Open
        buffer.putInt(251000) // High
        buffer.putInt(248000) // Low
        buffer.putInt(249500) // Close
        buffer.putLong(100000L) // Volume

        val bytes = buffer.array()

        val service = MStockMarketDataService(null, null)

        service.parseBinaryPacket(bytes)

        val tick = MarketDataStore.getTick("NSE", "2885") ?: MarketDataStore.getTick("2885")
        assertNotNull("Tick must be ingested into MarketDataStore", tick)
        if (tick != null) {
            assertEquals("MSTOCK", tick.source)
            assertEquals(2500.50, tick.ltp, 0.01)
            assertEquals("LIVE", tick.state)
        }
    }

    @Test
    fun test7_bothProvidersUnavailable_dataUnavailable() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, false)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, false)

        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE))
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_MSTOCK))
    }
}
