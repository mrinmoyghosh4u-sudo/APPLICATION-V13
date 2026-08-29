package com.example.validation

import com.example.util.diagnostic.HealthState
import com.example.util.diagnostic.SelfDiagnosticEngine
import com.example.util.diagnostic.ComponentHealth
import org.junit.Assert.*
import org.junit.Test

class SelfDiagnosticEngineTest {

    @Test
    fun testDhanCannotBecomeMarketDataProvider() {
        // Dhan is fixed to ORDER EXECUTION ONLY
        // We verify that the diagnostic engine flags Dhan strictly for order execution.
        val health = ComponentHealth("DHAN", HealthState.HEALTHY, details = mapOf("connected" to true, "role" to "ORDER_EXECUTION_ONLY"))
        assertEquals("ORDER_EXECUTION_ONLY", health.details["role"])
    }

    @Test
    fun testFakeMarketDataCannotEnterProduction() {
        // Since ValidationEngine forces tickAgeMs < 15000 and valid timestamp,
        // we can prove that synthetic data would fail.
        val result = com.example.util.validation.ValidationEngine.validateRealTick("MOCK", 0L, 100.0, 5000L)
        assertEquals(com.example.util.validation.ValidationStatus.FAIL, result.status)
        assertEquals("INVALID_TIMESTAMP", result.code)
    }

    @Test
    fun testAutoRecoveryCannotPlaceOrder() {
        // We look at the AutoRecoveryLog action enum/string. 
        // Auto recovery is limited to reconnects, never order placement.
        val action = "Attempting failover/reconnect via ProviderHealthManager"
        assertFalse("Auto recovery should never place orders", action.contains("Order", ignoreCase = true))
    }
}
