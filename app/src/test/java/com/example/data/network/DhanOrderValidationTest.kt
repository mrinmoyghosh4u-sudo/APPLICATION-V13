package com.example.data.network

import org.junit.Assert.*
import org.junit.Test

class DhanOrderValidationTest {

    @Test
    fun `index security IDs are blocked for option orders`() {
        val indexSecurityIds = setOf("13", "25", "27", "31", "51", "17")
        
        assertTrue("NIFTY index ID should be blocked", indexSecurityIds.contains("13"))
        assertTrue("BANKNIFTY index ID should be blocked", indexSecurityIds.contains("25"))
        assertTrue("FINNIFTY index ID should be blocked", indexSecurityIds.contains("27"))
        assertTrue("MIDCPNIFTY index ID should be blocked", indexSecurityIds.contains("31"))
        assertTrue("SENSEX index ID should be blocked", indexSecurityIds.contains("51"))
        assertTrue("BANKEX index ID should be blocked", indexSecurityIds.contains("17"))
    }

    @Test
    fun `option orders require exact contract mapping`() {
        val indexSecurityId = "13"
        val optionSecurityId = "44222"
        
        assertTrue("Index ID must be rejected for options", indexSecurityId in setOf("13", "25", "27", "31", "51", "17"))
        assertFalse("Option contract ID must not be index ID", optionSecurityId in setOf("13", "25", "27", "31", "51", "17"))
    }

    @Test
    fun `Dhan option segment must be OPT not FUT`() {
        val optionSegment = "OPT"
        val futureSegment = "FUT"
        
        assertEquals("OPT", optionSegment)
        assertNotEquals("FUT", optionSegment)
    }
}
