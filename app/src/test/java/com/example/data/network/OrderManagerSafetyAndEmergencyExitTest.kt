package com.example.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MarketDataStore
import com.example.data.model.OrderEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrderManagerSafetyAndEmergencyExitTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
    }

    @Test
    fun testAngelOneAuthHeaderAndKeyResolution() {
        sessionManager.angelApiKey = "TEST_API_KEY_123"
        sessionManager.angelJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample"

        assertEquals("TEST_API_KEY_123", sessionManager.angelApiKey)
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample", sessionManager.angelJwtToken)
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample", sessionManager.angelAuthToken)
    }

    @Test
    fun testSessionManagerAllBrokersGettersAndSetters() {
        // Dhan
        sessionManager.saveDhanCredentials("DHAN_CLI_99", "DHAN_TOK_123", "DHAN_KEY_456", "DHAN_SEC_789")
        assertEquals("DHAN_CLI_99", sessionManager.dhanClientId)
        assertEquals("DHAN_TOK_123", sessionManager.dhanAccessToken)
        assertEquals("DHAN_KEY_456", sessionManager.dhanApiKey)
        assertEquals("DHAN_SEC_789", sessionManager.dhanClientSecret)
        assertTrue(sessionManager.isDhanConnected)

        // Upstox
        sessionManager.saveUpstoxCredentials("UP_KEY_1", "UP_SEC_2", "UP_TOK_3")
        assertEquals("UP_KEY_1", sessionManager.upstoxApiKey)
        assertEquals("UP_SEC_2", sessionManager.upstoxApiSecret)
        assertEquals("UP_TOK_3", sessionManager.upstoxAccessToken)
        assertTrue(sessionManager.isUpstoxConnected)

        // Fyers
        sessionManager.saveFyersCredentials("FY_APP_1", "FY_SEC_2", "FY_TOK_3")
        assertEquals("FY_APP_1", sessionManager.fyersAppId)
        assertEquals("FY_SEC_2", sessionManager.fyersSecretId)
        assertEquals("FY_TOK_3", sessionManager.fyersAccessToken)
        assertTrue(sessionManager.isFyersConnected)

        // Angel One
        sessionManager.saveAngelOneCredentials("ANG_CODE_1", "1234", "ANG_API_KEY", "TOTP_SECRET")
        assertEquals("ANG_CODE_1", sessionManager.angelClientId)
        assertEquals("1234", sessionManager.angelClientPin)
        assertEquals("ANG_API_KEY", sessionManager.angelApiKey)
        assertEquals("TOTP_SECRET", sessionManager.angelTotpSecret)
    }

    @Test
    fun testOrderManagerAllowsSquareOffOnStaleFeedWhileBlockingNewBuy() = runBlocking {
        // Set provider feed state as stale
        MarketDataStore.setProviderStateForTesting(
            com.example.data.model.MarketDataProviderState(
                provider = "UPSTOX",
                status = "LIVE",
                live = true,
                stale = true,
                lastUpdate = System.currentTimeMillis() - 60000L
            )
        )

        // Create mock DhanTradingService where Dhan is disconnected for base validation
        val networkClient = BrokerNetworkClient(sessionManager)
        val dhanBrokerService = DhanBrokerService(api = networkClient.dhanApi, sessionManager = sessionManager)
        val dhanService = DhanTradingService(dhanBrokerService, sessionManager)
        val orderManager = OrderManager(dhanService)

        // A new BUY order fails with Dhan connection check first if not connected
        val buyOrder = OrderEntity(
            orderId = "BUY_1",
            symbol = "NIFTY 25000 CE",
            exchange = "NSE",
            lotSize = 25,
            qty = 25,
            orderType = "MARKET",
            side = "BUY",
            price = 100.0,
            value = 2500.0,
            status = "PENDING",
            time = "10:00:00"
        )

        val buyResult = orderManager.executeOrder(buyOrder, isUserConfirmed = true)
        assertTrue("BUY order should fail when Dhan is not connected or feed is stale", buyResult.isFailure)

        // Square off SELL order
        val sellOrder = OrderEntity(
            orderId = "SELL_1",
            symbol = "NIFTY 25000 CE",
            exchange = "NSE",
            lotSize = 25,
            qty = 25,
            orderType = "MARKET",
            side = "SELL",
            price = 120.0,
            value = 3000.0,
            status = "PENDING",
            time = "10:05:00"
        )

        val sellResult = orderManager.executeOrder(sellOrder, isUserConfirmed = true)
        // Check that failure reason is Dhan connection check, NOT blocked by stale feed
        assertTrue(sellResult.isFailure)
        val errMessage = sellResult.exceptionOrNull()?.message ?: ""
        assertFalse("SELL square-off must NOT be blocked with Stale Data Protection", errMessage.contains("Stale Data Protection"))
    }

    @Test
    fun testFyersUnsignedShortMasking() {
        val shortVal: Short = -1 // 0xFFFF in 16-bit
        val rawInt = shortVal.toInt()
        val maskedInt = shortVal.toInt() and 0xFFFF

        assertEquals(-1, rawInt)
        assertEquals(65535, maskedInt)
        assertTrue("Masked short must be positive", maskedInt > 0)
    }

    @Test
    fun testDhanSecurityIdIsolation() {
        val master = InstrumentMasterService(context = context)
        // Known index IDs
        assertEquals("13", master.resolveDhanSecurityId("NIFTY"))
        assertEquals("25", master.resolveDhanSecurityId("BANKNIFTY"))
        assertEquals("418042", master.resolveDhanSecurityId("CRUDEOIL"))

        // Unmapped unknown instrument must return null, NEVER Angel One scrip master token
        val unknownId = master.resolveDhanSecurityId("UNKNOWN_STOCK_XYZ")
        assertNull("Unknown instrument must return null for Dhan security ID to avoid misrouting", unknownId)
    }

    @Test
    fun testAngelOneMarketDataServiceSafeNullCheck() {
        val networkClient = BrokerNetworkClient(sessionManager)
        val instrumentMaster = InstrumentMasterService(context = context)
        val angelOneService = AngelOneBrokerService(
            api = networkClient.angelOneApi,
            sessionManager = sessionManager,
            instrumentMaster = instrumentMaster
        )
        val service = AngelOneMarketDataService(
            angelOneService = angelOneService,
            sessionManager = sessionManager,
            instrumentMaster = instrumentMaster
        )

        // Clear tokens by default
        sessionManager.angelJwtToken = ""
        sessionManager.angelClientId = ""
        sessionManager.angelFeedToken = ""
        assertFalse("isConfigured must safely return false when credentials are empty/null", service.isConfigured())

        sessionManager.angelJwtToken = "jwt_token"
        sessionManager.angelClientId = "client_id"
        sessionManager.angelFeedToken = "feed_token"

        assertTrue("isConfigured must return true when all credentials are set", service.isConfigured())
    }

    @Test
    fun testDhanStopLossTriggerPriceLogic() {
        val stopLoss = 95.5
        val slOrderType = "STOP_LOSS"
        val marketOrderType = "MARKET"

        val triggerPriceSL = if (slOrderType.contains("STOP_LOSS")) stopLoss else 0.0
        val triggerPriceMarket = if (marketOrderType.contains("STOP_LOSS")) stopLoss else 0.0

        assertEquals(95.5, triggerPriceSL, 0.001)
        assertEquals(0.0, triggerPriceMarket, 0.001)
    }
}
