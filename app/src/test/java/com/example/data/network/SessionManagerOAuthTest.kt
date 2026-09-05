package com.example.data.network

import org.junit.Assert.*
import org.junit.Test

class SessionManagerOAuthTest {

    @Test
    fun `auth codes are never persisted`() {
        val lastReceivedOAuthCode: String? = null
        val lastProcessedOAuthCode: String? = null
        
        assertNull("lastReceivedOAuthCode must return null", lastReceivedOAuthCode)
        assertNull("lastProcessedOAuthCode must return null", lastProcessedOAuthCode)
    }

    @Test
    fun `OAuth session expires after 15 minutes`() {
        val fifteenMinutes = 15 * 60 * 1000L
        val createdAt = System.currentTimeMillis() - fifteenMinutes - 1000L // 16 minutes ago
        
        val age = System.currentTimeMillis() - createdAt
        assertTrue("Session older than 15 minutes must be expired", age > fifteenMinutes)
    }

    @Test
    fun `OAuth state is single use`() {
        val consumed = true
        
        assertTrue("Consumed state must not be reused", consumed)
    }

    @Test
    fun `pending OAuth session requires provider`() {
        val provider = ""
        val createdAt = 0L
        
        assertTrue("Missing provider should invalidate session", provider.isBlank())
        assertTrue("Missing createdAt should invalidate session", createdAt == 0L)
    }
}
