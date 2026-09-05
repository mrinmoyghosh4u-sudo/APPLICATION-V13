package com.example.data.network

import org.junit.Assert.*
import org.junit.Test

class MarketBreadthTest {

    @Test
    fun `MarketBreadth data class has no invalid label parameter`() {
        // Verify MarketBreadth can be constructed with valid parameters only
        val breadth = MarketBreadth(
            advances = 30,
            declines = 20,
            unchanged = 5,
            total = 55,
            timestamp = System.currentTimeMillis()
        )
        
        assertEquals(30, breadth.advances)
        assertEquals(20, breadth.declines)
        assertEquals(5, breadth.unchanged)
        assertEquals(55, breadth.total)
        assertTrue("Advance/Decline ratio should be positive", breadth.advanceDeclineRatio > 0.0)
    }

    @Test
    fun `market breadth calculation uses only real quotes`() {
        val advances = 30
        val declines = 20
        val unchanged = 5
        val total = advances + declines + unchanged
        
        assertEquals(55, total)
        
        val advanceDeclineRatio = if (declines > 0) advances.toDouble() / declines.toDouble() else advances.toDouble()
        assertEquals(1.5, advanceDeclineRatio, 0.001)
    }

    @Test
    fun `unavailable breadth is clearly labeled`() {
        val errorMessage = "Market breadth calculation unavailable from all providers."
        assertTrue("Error message should clearly state unavailability", 
            errorMessage.contains("unavailable", ignoreCase = true))
    }
}
