package com.example.util.indicators

import org.junit.Assert.*
import org.junit.Test

class CandleStoreTest {

    @Test
    fun `cumulative volume increments correctly for active candle`() {
        CandleStore.clear()
        val key = CandleStore.makeKey("NIFTY", "5 MIN")
        
        CandleStore.onLiveTick("NIFTY", 22000.0, 1000, timestamp = 1000L)
        var candles = CandleStore.getCandles("NIFTY", "5 MIN")
        assertEquals(1, candles.size)
        assertEquals(1000.0, candles[0].volume, 0.001)
        
        CandleStore.onLiveTick("NIFTY", 22050.0, 1500, timestamp = 1500L)
        candles = CandleStore.getCandles("NIFTY", "5 MIN")
        assertEquals(1, candles.size)
        assertEquals(1500.0, candles[0].volume, 0.001)
    }

    @Test
    fun `new candle starts with correct volume delta`() {
        CandleStore.clear()
        
        CandleStore.onLiveTick("NIFTY", 22000.0, 1000, timestamp = 1000L)
        CandleStore.onLiveTick("NIFTY", 22050.0, 1500, timestamp = 1500L)
        
        val intervalMs = CandleStore.getTimeframeIntervalMs("5 MIN")
        val newBucket = 300000L
        CandleStore.onLiveTick("NIFTY", 22100.0, 500, timestamp = newBucket)
        
        val candles = CandleStore.getCandles("NIFTY", "5 MIN")
        assertEquals(2, candles.size)
        assertEquals(1500.0, candles[0].volume, 0.001)
        assertEquals(500.0, candles[1].volume, 0.001)
    }

    @Test
    fun `volume does not reset on every tick`() {
        CandleStore.clear()
        
        CandleStore.onLiveTick("NIFTY", 22000.0, 1000, timestamp = 1000L)
        CandleStore.onLiveTick("NIFTY", 22001.0, 500, timestamp = 1500L)
        CandleStore.onLiveTick("NIFTY", 22002.0, 300, timestamp = 2000L)
        
        val candles = CandleStore.getCandles("NIFTY", "5 MIN")
        assertEquals(1, candles.size)
        assertTrue("Volume should be cumulative", candles[0].volume > 1000.0)
    }

    @Test
    fun `ohlc updates correctly for active candle`() {
        CandleStore.clear()
        
        CandleStore.onLiveTick("NIFTY", 22000.0, 1000, timestamp = 1000L)
        CandleStore.onLiveTick("NIFTY", 22050.0, 500, timestamp = 1500L)
        CandleStore.onLiveTick("NIFTY", 21950.0, 300, timestamp = 2000L)
        
        val candles = CandleStore.getCandles("NIFTY", "5 MIN")
        assertEquals(22050.0, candles[0].high, 0.001)
        assertEquals(21950.0, candles[0].low, 0.001)
        assertEquals(21950.0, candles[0].close, 0.001)
    }
}
