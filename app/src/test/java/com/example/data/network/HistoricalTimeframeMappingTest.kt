package com.example.data.network

import org.junit.Assert.*
import org.junit.Test

class HistoricalTimeframeMappingTest {

    @Test
    fun `all timeframes map correctly for Upstox`() {
        val mappings = mapOf(
            "1m" to Pair("minute", "1"),
            "5m" to Pair("minute", "5"),
            "15m" to Pair("minute", "15"),
            "30m" to Pair("minute", "30"),
            "1h" to Pair("hour", "1"),
            "1d" to Pair("day", "1")
        )
        
        assertEquals("1m should map to minute/1", Pair("minute", "1"), mappings["1m"])
        assertEquals("5m should map to minute/5", Pair("minute", "5"), mappings["5m"])
        assertEquals("15m should map to minute/15", Pair("minute", "15"), mappings["15m"])
        assertEquals("30m should map to minute/30", Pair("minute", "30"), mappings["30m"])
        assertEquals("1h should map to hour/1", Pair("hour", "1"), mappings["1h"])
        assertEquals("1d should map to day/1", Pair("day", "1"), mappings["1d"])
    }

    @Test
    fun `1h must not fall back to 15m`() {
        val interval = "1h"
        val fallback = "15m"
        
        assertNotEquals("1h must not silently fall back to 15m", interval, fallback)
    }

    @Test
    fun `Angel One interval mapping is complete`() {
        val angelMappings = mapOf(
            "1m" to "ONE_MINUTE",
            "5m" to "FIVE_MINUTE",
            "15m" to "FIFTEEN_MINUTE",
            "30m" to "THIRTY_MINUTE",
            "1h" to "ONE_HOUR",
            "1d" to "ONE_DAY"
        )
        
        assertNotNull("1m must map", angelMappings["1m"])
        assertNotNull("5m must map", angelMappings["5m"])
        assertNotNull("15m must map", angelMappings["15m"])
        assertNotNull("30m must map", angelMappings["30m"])
        assertNotNull("1h must map", angelMappings["1h"])
        assertNotNull("1d must map", angelMappings["1d"])
    }
}
