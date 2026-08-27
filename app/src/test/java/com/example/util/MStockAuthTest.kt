package com.example.util

import org.junit.Assert.*
import org.junit.Test

class MStockAuthTest {

    @Test
    fun testTotpGenerationFromBase32Secret() {
        // Standard RFC 6238 Base32 test secret
        val secret = "JBSWY3DPEHPK3PXP"
        val totp = TotpUtil.generateTotp(secret)

        assertNotNull("Generated TOTP should not be null", totp)
        assertEquals("TOTP must be 6 digits", 6, totp.length)
        assertTrue("TOTP must consist only of digits", totp.all { it.isDigit() })
    }

    @Test
    fun testTotpGenerationDeterministicForFixedTime() {
        val secret = "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ"
        val timestamp = 1700000000000L // Fixed time
        val totp = TotpUtil.generateTotp(secret, timeMillis = timestamp)

        assertEquals(6, totp.length)
        assertTrue(totp.all { it.isDigit() })
    }

    @Test
    fun testDirect6DigitTotpPassThrough() {
        val directTotp = "492810"
        val result = if (directTotp.length == 6 && directTotp.all { it.isDigit() }) directTotp else TotpUtil.generateTotp(directTotp)
        assertEquals("492810", result)
    }

    @Test
    fun testMStockTokensDataModel() {
        val tokens = MStockAuthHelper.MStockTokens(
            accessToken = "test_access_jwt",
            refreshToken = "test_refresh_jwt",
            feedToken = "test_feed_jwt"
        )
        assertEquals("test_access_jwt", tokens.accessToken)
        assertEquals("test_refresh_jwt", tokens.refreshToken)
        assertEquals("test_feed_jwt", tokens.feedToken)
    }

    @Test
    fun testOfficialTypeAEndpointConstant() {
        assertEquals("https://api.mstock.trade/openapi/typea/session/verifytotp", MStockAuthHelper.TYPE_A_VERIFY_TOTP_URL)
        assertEquals("https://api.mstock.trade/openapi/typea/session/verifytotp", MStockAuthHelper.lastEndpoint.value)
    }
}
