package com.example

import com.example.util.UpstoxAuthHelper
import com.example.util.FyersAuthHelper
import org.junit.Assert.*
import org.junit.Test

class DhanIsolationAndOAuthTest {

    @Test
    fun testUpstoxOAuthRedirectUriAndState() {
        val authUrl = UpstoxAuthHelper.getAuthorizationUrl(apiKey = "TEST_API_KEY", state = "TEST_UUID_STATE")
        assertTrue("Auth URL must contain production redirect domain", authUrl.contains("application-beige-psi.vercel.app"))
        assertTrue("Auth URL must contain response_type=code", authUrl.contains("response_type=code"))
        assertTrue("Auth URL must contain state=TEST_UUID_STATE", authUrl.contains("state=TEST_UUID_STATE"))
    }

    @Test
    fun testFyersOAuthRedirectUriAndState() {
        val authUrl = FyersAuthHelper.buildLoginUrl(appId = "TEST_APP_ID-100", state = "TEST_UUID_STATE")
        assertTrue("Auth URL must contain production redirect domain", authUrl.contains("application-beige-psi.vercel.app"))
        assertTrue("Auth URL must contain response_type=code", authUrl.contains("response_type=code"))
        assertTrue("Auth URL must contain state=TEST_UUID_STATE", authUrl.contains("state=TEST_UUID_STATE"))
    }
}
