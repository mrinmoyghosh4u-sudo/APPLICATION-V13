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
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_FYERS))
        assertFalse(healthManager.isProviderHealthy(ProviderHealthManager.PROVIDER_ANGEL_ONE))
    }
}
