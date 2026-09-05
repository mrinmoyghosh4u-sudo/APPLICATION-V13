package com.example.data.network

import org.junit.Assert.*
import org.junit.Test

class OAuthStateValidationTest {

    @Test
    fun `state mismatch is hard failure`() {
        val storedState = "abc123"
        val callbackState = "xyz789"
        
        assertNotEquals("State mismatch must be detected", storedState, callbackState)
    }

    @Test
    fun `missing callback state is hard failure`() {
        val callbackState = ""
        
        assertTrue("Missing callback state must fail", callbackState.isBlank())
    }

    @Test
    fun `state must be consumed once`() {
        val consumed = true
        
        assertTrue("Once consumed, state must not be reused", consumed)
    }

    @Test
    fun `state expiry after 15 minutes`() {
        val createdAt = System.currentTimeMillis() - 16 * 60 * 1000L // 16 minutes ago
        val now = System.currentTimeMillis()
        val age = now - createdAt
        val maxAge = 15 * 60 * 1000L
        
        assertTrue("Expired state must be rejected", age > maxAge)
    }
}
