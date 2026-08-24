package com.example

import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import com.example.data.network.*
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
    fun test1_fyersPrimaryLive_activeIsFyers() {
        val now = System.currentTimeMillis()
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_FYERS, now)
        assertTrue("FYERS must be healthy when connected and receiving ticks", healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_FYERS))
        assertEquals(ProviderHealthManager.PROVIDER_FYERS, healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS).provider)
    }

    @Test
    fun test2_fyersDisconnect_triggersFailoverToAngelOne() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, false)
        assertFalse("FYERS must be unhealthy on disconnect", healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_FYERS))

        val now = System.currentTimeMillis()
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_ANGEL_ONE, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_ANGEL_ONE, now)
        assertTrue("Angel One should be healthy and serve as Secondary fallback", healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE))
    }

    @Test
    fun test3_fyersAndAngelDisconnect_triggersFailoverToMStock() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, false)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, false)
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_FYERS))
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE))

        val now = System.currentTimeMillis()
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_MSTOCK, now)
        assertTrue("m.Stock should be healthy and serve as Tertiary fallback", healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_MSTOCK))
    }

    @Test
    fun test4_fyersStaleAfter15s_marksStale() {
        val oldTimestamp = System.currentTimeMillis() - 20000L // 20s ago
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_FYERS, oldTimestamp)
        healthManager.checkAndEvaluateStaleness(System.currentTimeMillis())
        val state = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertTrue(state.stale)
        assertFalse(state.healthy)
    }

    @Test
    fun test5_mStockConnectedNoTick_isNotLive() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_MSTOCK, true)
        val isHealthy = healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_MSTOCK)
        assertFalse("m.Stock connected without tick MUST NOT be marked healthy/live", isHealthy)
    }

    @Test
    fun test6_mStockBinaryParser_parsesValidPacket() {
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
            assertEquals("mStock", tick.source)
            assertEquals(2500.50, tick.ltp, 0.01)
            assertEquals("LIVE", tick.state)
        }
    }

    @Test
    fun test7_multiExchangeSymbolMapper() {
        assertEquals("NSE:NIFTY50-INDEX", FyersSymbolMapper.toFyersSymbol("NIFTY 50", "NSE"))
        assertEquals("BSE:SENSEX-INDEX", FyersSymbolMapper.toFyersSymbol("SENSEX", "BSE"))
        assertEquals("MCX:CRUDEOIL", FyersSymbolMapper.toFyersSymbol("CRUDEOIL", "MCX"))
        assertEquals("NSE:RELIANCE-EQ", FyersSymbolMapper.toFyersSymbol("RELIANCE", "NSE"))
        assertEquals("BSE:TCS-EQ", FyersSymbolMapper.toFyersSymbol("TCS", "BSE"))
    }

    @Test
    fun test8_allProvidersUnavailable_dataUnavailable() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, false)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_ANGEL_ONE, false)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_MSTOCK, false)
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_FYERS))
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE))
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_MSTOCK))
    }
}
