package com.example.validation

import com.example.util.validation.ValidationEngine
import com.example.util.validation.ValidationStatus
import com.example.util.OptionExpiryUtil
import org.junit.Assert.*
import org.junit.Test

class MarketDataValidationTest {

    @Test
    fun testExpiryFallbackImpossible() {
        // OptionBuyer rules strictly forbid synthetic fallback dates
        val expiries = OptionExpiryUtil.getUpcomingExpiriesForSymbol("NIFTY 50", emptyList())
        assertEquals("UNAVAILABLE", expiries.first())
        
        val validation = ValidationEngine.validateExpiry(expiries.first())
        assertEquals(ValidationStatus.FAIL, validation.status)
        assertEquals("EXPIRY_UNAVAILABLE", validation.code)
    }

    @Test
    fun testLiveImpossibleWithoutRealTickEvidence() {
        val result1 = ValidationEngine.validateRealTick("UPSTOX", 123456789L, 100.0, 5000L)
        assertEquals(ValidationStatus.PASS, result1.status)

        // Missing broker
        val result2 = ValidationEngine.validateRealTick("", 123456789L, 100.0, 5000L)
        assertEquals(ValidationStatus.FAIL, result2.status)
        
        // Stale tick
        val result3 = ValidationEngine.validateRealTick("UPSTOX", 123456789L, 100.0, 16000L)
        assertEquals(ValidationStatus.FAIL, result3.status)
        assertEquals("STALE_TICK", result3.code)
    }

    @Test
    fun testStaleTickBlocksSignal() {
        val signalResult = ValidationEngine.validateSignal(
            hasRealUnderlyingTick = true,
            hasRealOptionTick = true,
            hasOfficialContract = true,
            hasOfficialExpiry = true,
            isFreshData = false // Stale!
        )
        assertEquals(ValidationStatus.FAIL, signalResult.status)
        assertEquals("STALE_DATA", signalResult.code)
    }

    @Test
    fun testStaleTickBlocksOrder() {
        val orderResult = ValidationEngine.validateOrder(
            isDhanAuthenticated = true,
            hasValidSecurityId = true,
            isMarketDataLive = true,
            isDataFresh = false // Stale!
        )
        assertEquals(ValidationStatus.FAIL, orderResult.status)
        assertEquals("STALE_DATA", orderResult.code)
    }

    @Test
    fun testInvalidOptionContractBlocksSignal() {
        val signalResult = ValidationEngine.validateSignal(
            hasRealUnderlyingTick = true,
            hasRealOptionTick = true,
            hasOfficialContract = false, // Invalid
            hasOfficialExpiry = true,
            isFreshData = true
        )
        assertEquals(ValidationStatus.FAIL, signalResult.status)
        assertEquals("NO_CONTRACT", signalResult.code)
    }

    @Test
    fun testInvalidExpiryBlocksSignal() {
        val signalResult = ValidationEngine.validateSignal(
            hasRealUnderlyingTick = true,
            hasRealOptionTick = true,
            hasOfficialContract = true,
            hasOfficialExpiry = false, // Invalid
            isFreshData = true
        )
        assertEquals(ValidationStatus.FAIL, signalResult.status)
        assertEquals("NO_EXPIRY", signalResult.code)
    }

    @Test
    fun testDhanDisconnectedDoesNotMakeMarketDataOffline() {
        val orderResult = ValidationEngine.validateOrder(
            isDhanAuthenticated = false, // Dhan offline
            hasValidSecurityId = true,
            isMarketDataLive = true,   // But market data is LIVE (e.g. Upstox)
            isDataFresh = true
        )
        // Market data is live, but order fails because Dhan is offline.
        // It proves Market data != Dhan.
        assertEquals(ValidationStatus.FAIL, orderResult.status)
        assertEquals("DHAN_UNAUTHENTICATED", orderResult.code)
    }
}
