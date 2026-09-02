package com.example.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class DhanTokenRenewalTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.clearDhanSessionTokens()
    }

    @Test
    fun testDhanTokenTimestampPersistence() {
        val testTime = System.currentTimeMillis() - (15L * 60L * 60L * 1000L) // 15 hours ago
        sessionManager.dhanTokenTimestamp = testTime

        assertEquals("dhanTokenTimestamp should be saved in preferences", testTime, sessionManager.dhanTokenTimestamp)
    }

    @Test
    fun testTokenNotRenewedWhenUnder12HoursOld() = runBlocking {
        var renewCalled = false
        val mockApi = object : FakeDhanApi() {
            override suspend fun renewToken(
                currentToken: String,
                clientId: String
            ): Response<Map<String, Any>> {
                renewCalled = true
                return Response.success(mapOf("accessToken" to "NEW_TOKEN"))
            }
        }

        sessionManager.dhanClientId = "10001"
        sessionManager.dhanAccessToken = "VALID_OLD_TOKEN"
        // 5 hours old
        sessionManager.dhanTokenTimestamp = System.currentTimeMillis() - (5L * 60L * 60L * 1000L)

        val dhanService = DhanBrokerService(api = mockApi, sessionManager = sessionManager)
        val renewed = dhanService.checkAndRenewTokenIfNeeded()

        assertFalse("Token under 12 hours old should not be renewed", renewed)
        assertFalse("Renew endpoint should not have been called", renewCalled)
        assertEquals("VALID_OLD_TOKEN", sessionManager.dhanAccessToken)
    }

    @Test
    fun testTokenRenewedWhenOver12HoursOld() = runBlocking {
        var capturedCurrentToken = ""
        var capturedClientId = ""
        val mockApi = object : FakeDhanApi() {
            override suspend fun renewToken(
                currentToken: String,
                clientId: String
            ): Response<Map<String, Any>> {
                capturedCurrentToken = currentToken
                capturedClientId = clientId
                return Response.success(
                    mapOf(
                        "dhanClientId" to clientId,
                        "accessToken" to "RENEWED_TOKEN_XYZ",
                        "status" to "success"
                    )
                )
            }
        }

        sessionManager.dhanClientId = "10001"
        sessionManager.dhanAccessToken = "EXPIRED_TOKEN_ABC"
        // 14 hours old
        val fourteenHoursAgo = System.currentTimeMillis() - (14L * 60L * 60L * 1000L)
        sessionManager.dhanTokenTimestamp = fourteenHoursAgo

        val dhanService = DhanBrokerService(api = mockApi, sessionManager = sessionManager)
        val renewed = dhanService.checkAndRenewTokenIfNeeded()

        assertTrue("Token over 12 hours old should be successfully renewed", renewed)
        assertEquals("EXPIRED_TOKEN_ABC", capturedCurrentToken)
        assertEquals("10001", capturedClientId)
        assertEquals("RENEWED_TOKEN_XYZ", sessionManager.dhanAccessToken)
        assertTrue("Timestamp must be updated to current time", sessionManager.dhanTokenTimestamp > fourteenHoursAgo)
    }

    private open class FakeDhanApi : DhanApi {
        override suspend fun getFundLimit(): Response<DhanFundLimitResponse> = Response.success(
            DhanFundLimitResponse(availableBalance = 100000.0)
        )
        override suspend fun getOrders(): Response<List<DhanOrderBookItem>> = Response.success(emptyList())
        override suspend fun placeOrder(request: DhanPlaceOrderRequest): Response<DhanOrderResponse> =
            Response.success(DhanOrderResponse(orderId = "123", orderStatus = "TRANSIT"))
        override suspend fun modifyOrder(orderId: String, request: DhanModifyOrderRequest): Response<DhanOrderResponse> =
            Response.success(DhanOrderResponse(orderId = orderId, orderStatus = "TRANSIT"))
        override suspend fun cancelOrder(orderId: String): Response<DhanOrderResponse> =
            Response.success(DhanOrderResponse(orderId = orderId, orderStatus = "CANCELLED"))
        override suspend fun getHoldings(): Response<List<DhanHoldingItem>> = Response.success(emptyList())
        override suspend fun getPositions(): Response<List<DhanPositionItem>> = Response.success(emptyList())
        override suspend fun getTrades(): Response<List<DhanTradeItem>> = Response.success(emptyList())
        override suspend fun renewToken(
            currentToken: String,
            clientId: String
        ): Response<Map<String, Any>> = Response.success(emptyMap())
    }
}
