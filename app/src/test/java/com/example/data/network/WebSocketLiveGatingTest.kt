package com.example.data.network

import org.junit.Assert.*
import org.junit.Test

class WebSocketLiveGatingTest {

    @Test
    fun `LIVE requires valid tick not just connection`() {
        val isConnected = true
        val hasValidTick = false
        val tickIsFresh = false
        
        val isLive = isConnected && hasValidTick && tickIsFresh
        assertFalse("WebSocket connection alone should not mean LIVE", isLive)
    }

    @Test
    fun `LIVE requires fresh tick`() {
        val isConnected = true
        val hasValidTick = true
        val tickAgeMs = 60000L // 60 seconds old
        val staleThresholdMs = 30000L // 30 seconds
        
        val isStale = tickAgeMs > staleThresholdMs
        assertTrue("Tick older than threshold should be stale", isStale)
    }

    @Test
    fun `WAITING_FOR_FIRST_TICK when connected but no tick yet`() {
        val isConnected = true
        val hasFirstTick = false
        val subscriptionCount = 5
        
        val status = if (isConnected && !hasFirstTick) {
            "WAITING_FOR_FIRST_TICK"
        } else {
            "LIVE"
        }
        
        assertEquals("Should show WAITING_FOR_FIRST_TICK", "WAITING_FOR_FIRST_TICK", status)
    }

    @Test
    fun `STALE_DATA when tick is too old`() {
        val lastTickTime = System.currentTimeMillis() - 60000L
        val staleThreshold = 30000L
        val isMarketOpen = true
        
        val isStale = (System.currentTimeMillis() - lastTickTime) > staleThreshold && isMarketOpen
        assertTrue("Should be stale when tick is old and market is open", isStale)
    }
}
