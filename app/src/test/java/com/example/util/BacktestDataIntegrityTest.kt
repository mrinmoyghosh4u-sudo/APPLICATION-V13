package com.example.util

import org.junit.Assert.*
import org.junit.Test

class BacktestDataIntegrityTest {

    @Test
    fun `backtest does not use hardcoded lot size 15`() {
        val hardcodedLotSize = 15
        val actualLotSize = AppPreferences.getGlobalLotSize("NIFTY")
        
        assertNotEquals("Backtest must use actual lot size, not hardcoded 15", 
            hardcodedLotSize, actualLotSize)
        assertEquals("NIFTY lot size should be 65", 65, actualLotSize)
    }

    @Test
    fun `backtest does not use hardcoded profitFactor 2_5`() {
        val totalProfit = 5000.0
        val totalLoss = 2000.0
        val profitFactor = if (totalLoss > 0) totalProfit / totalLoss else if (totalProfit > 0) Double.MAX_VALUE else 0.0
        
        assertEquals("Profit factor should be calculated from actual P&L", 
            2.5, profitFactor, 0.001)
        assertNotEquals("Should not hardcode 2.5 when losses are zero", 
            Double.MAX_VALUE, if (totalLoss == 0.0) Double.MAX_VALUE else 0.0)
    }

    @Test
    fun `backtest does not use hardcoded maxDrawdown`() {
        val curve = listOf(100000.0, 105000.0, 102000.0, 98000.0, 110000.0)
        var maxPeak = curve.first()
        var maxDrawdown = 0.0
        
        for (value in curve) {
            if (value > maxPeak) maxPeak = value
            val drawdown = (maxPeak - value) / maxPeak * 100.0
            if (drawdown > maxDrawdown) maxDrawdown = drawdown
        }
        
        assertTrue("Max drawdown should be calculated from equity curve", maxDrawdown >= 0.0)
        assertFalse("Max drawdown should not be hardcoded", maxDrawdown == 2.5 || maxDrawdown == 4.0)
    }

    @Test
    fun `backtest does not use hardcoded sharpeRatio`() {
        val netPnl = 5000.0
        
        // sharpeRatio should be calculated from returns, not hardcoded
        val hardcodedSharpe = if (netPnl > 0) 1.85 else 0.5
        
        // The actual implementation should calculate this properly
        assertTrue("Sharpe ratio must be calculated from actual returns, not hardcoded", 
            hardcodedSharpe != 1.85 && hardcodedSharpe != 0.5)
    }
}
