package com.example.data.network

import org.junit.Assert.*
import org.junit.Test

class InstrumentResolversTest {

    @Test
    fun `DhanInstrumentResolver returns OPT segment for options`() {
        val resolver = DhanInstrumentResolver(null)
        
        val ceResult = resolver.resolve("NIFTY 24850 CE", "NSE")
        assertNotNull(ceResult)
        assertEquals("OPT", ceResult!!.segment)
        assertEquals("OPTION", ceResult.instrumentType)
        
        val peResult = resolver.resolve("BANKNIFTY 52400 PE", "NSE")
        assertNotNull(peResult)
        assertEquals("OPT", peResult!!.segment)
        assertEquals("OPTION", peResult.instrumentType)
    }

    @Test
    fun `DhanInstrumentResolver returns FUT segment for futures`() {
        val resolver = DhanInstrumentResolver(null)
        
        val futResult = resolver.resolve("NIFTY 24NOVFUT", "NFO")
        assertNotNull(futResult)
        assertEquals("FUT", futResult!!.segment)
        assertEquals("EQUITY", futResult.instrumentType)
    }

    @Test
    fun `DhanInstrumentResolver returns EQ segment for equities`() {
        val resolver = DhanInstrumentResolver(null)
        
        val eqResult = resolver.resolve("RELIANCE", "NSE")
        assertNotNull(eqResult)
        assertEquals("EQ", eqResult!!.segment)
        assertEquals("EQUITY", eqResult.instrumentType)
    }

    @Test
    fun `DhanInstrumentResolver returns null for blank security ID`() {
        val resolver = DhanInstrumentResolver(null)
        val result = resolver.resolve("UNKNOWNINSTRUMENT", "NSE")
        assertNull(result)
    }
}
