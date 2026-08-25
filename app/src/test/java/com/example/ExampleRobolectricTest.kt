package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KING KHAN AI TRADE", appName)
  }

  @Test
  fun `verify Upstox and Fyers OAuth states and credentials are completely isolated`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sessionManager = com.example.data.network.SessionManager(context)

    // Reset initially
    sessionManager.pendingUpstoxOAuthState = ""
    sessionManager.pendingFyersOAuthState = ""

    // 1. Assert initial states are blank
    assertEquals("", sessionManager.pendingUpstoxOAuthState)
    assertEquals("", sessionManager.pendingFyersOAuthState)

    // 2. Set Upstox state and verify Fyers state is untouched
    val upstoxState = "upstox_test_123"
    sessionManager.pendingUpstoxOAuthState = upstoxState
    assertEquals(upstoxState, sessionManager.pendingUpstoxOAuthState)
    assertEquals("", sessionManager.pendingFyersOAuthState)

    // 3. Set Fyers state and verify Upstox state is untouched
    val fyersState = "fyers_test_456"
    sessionManager.pendingFyersOAuthState = fyersState
    assertEquals(fyersState, sessionManager.pendingFyersOAuthState)
    assertEquals(upstoxState, sessionManager.pendingUpstoxOAuthState) // Must remain unchanged!
  }
}
