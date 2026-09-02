package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.FyersApi
import com.example.data.network.FyersHistoryResponse
import com.example.data.network.FyersMarketDataService
import com.example.data.network.FyersOptionChainResponse
import com.example.data.network.FyersProfileData
import com.example.data.network.FyersProfileResponse
import com.example.data.network.FyersQuotesResponse
import com.example.data.network.FyersRefreshTokenRequest
import com.example.data.network.FyersTokenRequest
import com.example.data.network.FyersTokenResponse
import com.example.data.network.MarketDataEngine
import com.example.data.network.ProviderHealthManager
import com.example.data.network.SessionManager
import com.example.data.network.UpstoxApi
import com.example.data.network.UpstoxCandlesData
import com.example.data.network.UpstoxFeedAuthData
import com.example.data.network.UpstoxFeedAuthResponse
import com.example.data.network.UpstoxHistoricalCandlesResponse
import com.example.data.network.UpstoxMarketDataService
import com.example.data.network.UpstoxMarketQuotesResponse
import com.example.data.network.UpstoxOptionChainResponse
import com.example.data.network.UpstoxOptionContractsResponse
import com.example.data.network.UpstoxProfileData
import com.example.data.network.UpstoxProfileResponse
import com.example.data.network.UpstoxSymbolMapper
import com.example.data.network.UpstoxTokenResponse
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase2MarketDataWsTest {

    private lateinit var healthManager: ProviderHealthManager
    private lateinit var sessionManager: SessionManager
    private lateinit var marketDataEngine: MarketDataEngine
    private lateinit var upstoxService: UpstoxMarketDataService
    private lateinit var fyersService: FyersMarketDataService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        healthManager = ProviderHealthManager()
        sessionManager = SessionManager(context)
        marketDataEngine = MarketDataEngine()

        // Configure dummy credentials
        sessionManager.saveUpstoxCredentials("test_key", "test_secret", "test_token")
        sessionManager.saveFyersCredentials("test_app_id", "test_secret", "test_token")

        upstoxService = UpstoxMarketDataService(sessionManager, marketDataEngine, FakeUpstoxApi(), healthManager)
        fyersService = FyersMarketDataService(sessionManager, marketDataEngine, FakeFyersApi(), healthManager)
    }

    @Test
    fun test1_upstoxProtobufBinaryParsingAndFirstTickSetsLive() {
        // Initial state before tick
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, 4)
        val stateBefore = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WAITING_FOR_FIRST_TICK", stateBefore.status)
        assertFalse(stateBefore.healthy)

        // Construct synthetic Upstox V3 Protobuf binary feed for "NSE_INDEX|Nifty 50" with LTP = 22500.50
        val key = "NSE_INDEX|Nifty 50"
        val keyBytes = key.toByteArray(Charsets.UTF_8)

        val ltpcBuf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        ltpcBuf.put(0x09.toByte())
        ltpcBuf.putDouble(22500.50)
        ltpcBuf.put(0x10.toByte())
        writeVarint(ltpcBuf, 1700000000000L)
        ltpcBuf.put(0x21.toByte())
        ltpcBuf.putDouble(22400.00)
        
        val ltpcBytes = ByteArray(ltpcBuf.position())
        System.arraycopy(ltpcBuf.array(), 0, ltpcBytes, 0, ltpcBytes.size)

        val feedBuf = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        feedBuf.put(0x0A.toByte())
        writeVarint(feedBuf, ltpcBytes.size.toLong())
        feedBuf.put(ltpcBytes)
        val feedBytes = ByteArray(feedBuf.position())
        System.arraycopy(feedBuf.array(), 0, feedBytes, 0, feedBytes.size)

        val mapEntryBuf = ByteBuffer.allocate(200).order(ByteOrder.LITTLE_ENDIAN)
        mapEntryBuf.put(0x0A.toByte())
        writeVarint(mapEntryBuf, keyBytes.size.toLong())
        mapEntryBuf.put(keyBytes)
        mapEntryBuf.put(0x12.toByte())
        writeVarint(mapEntryBuf, feedBytes.size.toLong())
        mapEntryBuf.put(feedBytes)
        val mapBytes = ByteArray(mapEntryBuf.position())
        System.arraycopy(mapEntryBuf.array(), 0, mapBytes, 0, mapBytes.size)

        val outerBuf = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)
        outerBuf.put(0x0A.toByte())
        writeVarint(outerBuf, mapBytes.size.toLong())
        outerBuf.put(mapBytes)

        val payload = ByteArray(outerBuf.position())
        System.arraycopy(outerBuf.array(), 0, payload, 0, payload.size)

        // Parse packet via Upstox Market Data Service
        val parsedCount = upstoxService.parseBinaryPacket(payload)
        assertEquals(1, parsedCount)

        val stateAfter = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("LIVE", stateAfter.status)
        assertTrue(stateAfter.firstTickReceived)
        assertTrue(stateAfter.healthy)
    }

    @Test
    fun test2_fyersBinaryTopicInitAndLiteModeParsingSetsLive() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportAuthentication(ProviderHealthManager.PROVIDER_FYERS, true)
        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_FYERS, 1)

        val stateBefore = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertEquals("WAITING_FOR_FIRST_TICK", stateBefore.status)

        val symbol = "NSE:NIFTY50-INDEX"
        val symBytes = symbol.toByteArray(Charsets.US_ASCII)
        
        val initBuf = ByteBuffer.allocate(128).order(ByteOrder.BIG_ENDIAN)
        initBuf.putShort(0)
        initBuf.put(6.toByte())
        initBuf.putInt(1001)
        initBuf.putShort(1)
        initBuf.put(83.toByte())
        initBuf.putShort(1)
        initBuf.put(7.toByte())
        initBuf.put("Nifty50".toByteArray())
        initBuf.put(0.toByte())
        initBuf.putShort(100)
        initBuf.put(2.toByte())
        initBuf.put(3.toByte())
        initBuf.put("NSE".toByteArray())
        initBuf.put(1.toByte())
        initBuf.put("1".toByteArray())
        initBuf.put(symBytes.size.toByte())
        initBuf.put(symBytes)

        val initLen = initBuf.position()
        initBuf.putShort(0, (initLen - 2).toShort())
        val initPayload = ByteArray(initLen)
        System.arraycopy(initBuf.array(), 0, initPayload, 0, initLen)

        fyersService.parseBinaryPacket(initPayload)

        val liteBuf = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN)
        liteBuf.putShort(0)
        liteBuf.put(76.toByte())
        liteBuf.putShort(1)
        liteBuf.putInt(2250000)

        val liteLen = liteBuf.position()
        liteBuf.putShort(0, (liteLen - 2).toShort())
        val litePayload = ByteArray(liteLen)
        System.arraycopy(liteBuf.array(), 0, litePayload, 0, liteLen)

        fyersService.parseBinaryPacket(litePayload)

        val stateAfter = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertEquals("LIVE", stateAfter.status)
        assertTrue(stateAfter.firstTickReceived)
        assertTrue(stateAfter.healthy)
    }

    @Test
    fun test3_webSocketOpenAloneMustNotSetLiveState() {
        healthManager.reportConnecting(ProviderHealthManager.PROVIDER_UPSTOX)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)

        val upstoxState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WEBSOCKET_CONNECTED", upstoxState.status)
        assertFalse("WebSocket connection alone MUST NOT mark status as LIVE", upstoxState.healthy)

        healthManager.reportConnecting(ProviderHealthManager.PROVIDER_FYERS)
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_FYERS, true)

        val fyersState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertEquals("WEBSOCKET_CONNECTED", fyersState.status)
        assertFalse("WebSocket connection alone MUST NOT mark status as LIVE", fyersState.healthy)
    }

    @Test
    fun test4_fyersHeartbeatHandling() {
        fyersService.parseTextMessage("Ping")
        fyersService.parseTextMessage("ping")
    }

    @Test
    fun test5_dynamicSubscriptionAndUnsubscribe() {
        upstoxService.parseBinaryPacket(ByteArray(0))
        fyersService.subscribeSymbols(listOf("NSE:NIFTY50-INDEX", "NSE:BANKNIFTY-INDEX"))
        
        val fyersState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertEquals(2, fyersState.activeSubscriptionCount)

        fyersService.unsubscribeSymbols(listOf("NSE:BANKNIFTY-INDEX"))
        val fyersStateAfterUnsub = healthManager.getHealthState(ProviderHealthManager.PROVIDER_FYERS)
        assertEquals(1, fyersStateAfterUnsub.activeSubscriptionCount)
    }

    @Test
    fun test6_disconnectionResetsLiveStatus() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, 5)
        healthManager.reportTickReceived(ProviderHealthManager.PROVIDER_UPSTOX, System.currentTimeMillis())

        val liveState = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("LIVE", liveState.status)

        healthManager.reportDisconnected(ProviderHealthManager.PROVIDER_UPSTOX)
        val stateAfterDisconnect = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("DISCONNECTED", stateAfterDisconnect.status)
        assertFalse(stateAfterDisconnect.healthy)
    }

    @Test
    fun test7_upstoxSubscriptionConfirmationViaServerJson() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportSubscribing(ProviderHealthManager.PROVIDER_UPSTOX)

        assertEquals("SUBSCRIBING", healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX).status)

        // Server returns JSON subscription success
        val successJson = """{"type":"sub","status":"success","data":{"mode":"ltpc","instrumentKeys":["NSE_INDEX|Nifty 50"]}}"""
        upstoxService.parseTextMessage(successJson)

        val stateAfterAck = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WAITING_FOR_FIRST_TICK", stateAfterAck.status)
        assertFalse("Must be WAITING_FOR_FIRST_TICK, not LIVE yet", stateAfterAck.healthy)
    }

    @Test
    fun test8_upstoxSubscriptionRejectedViaServerJson() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportSubscribing(ProviderHealthManager.PROVIDER_UPSTOX)

        // Server returns JSON subscription error
        val errorJson = """{"status":"error","message":"Invalid instrument key or unauthorized subscription"}"""
        upstoxService.parseTextMessage(errorJson)

        assertEquals("SUBSCRIPTION_ERROR", upstoxService.connectionState.value)
    }

    @Test
    fun test9_malformedBinaryPacketSafelyHandledWithoutLiveState() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, 1)

        val malformedBytes = byteArrayOf(0x01, 0x02, 0xFF.toByte(), 0xAA.toByte(), 0x55)
        val parsedTicks = upstoxService.parseBinaryPacket(malformedBytes)

        assertEquals(0, parsedTicks)
        assertFalse("Malformed packet must not trigger first tick", upstoxService.hasFirstTickReceived())
        assertFalse("Malformed packet must not set LIVE", upstoxService.isConnectionLive())
    }

    @Test
    fun test10_emptyBinaryPacketSafelyHandled() {
        val emptyBytes = ByteArray(0)
        val parsedTicks = upstoxService.parseBinaryPacket(emptyBytes)

        assertEquals(0, parsedTicks)
        assertFalse(upstoxService.hasFirstTickReceived())
    }

    @Test
    fun test11_restQuoteDoesNotSetWebSocketLive() {
        assertFalse("Initially not live", upstoxService.isConnectionLive())
        assertFalse("No first tick", upstoxService.hasFirstTickReceived())

        com.example.data.model.MarketDataStore.updateTick(
            source = com.example.data.model.MarketDataSourceNames.UPSTOX,
            symbol = "NIFTY",
            token = "NSE_INDEX|Nifty 50",
            exchange = "NSE",
            ltp = 22550.0,
            receivedTimestamp = System.currentTimeMillis(),
            state = "REST_QUOTE_AVAILABLE"
        )

        assertFalse("REST quote MUST NOT make WebSocket state LIVE", upstoxService.isConnectionLive())
        assertFalse("REST quote MUST NOT mark first real tick received", upstoxService.hasFirstTickReceived())
        assertEquals("REST_QUOTE_AVAILABLE", com.example.data.model.MarketDataStore.getTick("NIFTY")?.state)
    }

    @Test
    fun test12_upstoxInstrumentKeyPreservationAndDisplaySymbolResolution() {
        // 1. Valid Upstox instrument_key must remain completely unchanged (never uppercase already-valid keys)
        assertEquals("NSE_INDEX|Nifty 50", UpstoxSymbolMapper.toUpstoxInstrumentKey("NSE_INDEX|Nifty 50"))
        assertEquals("NSE_INDEX|Nifty Bank", UpstoxSymbolMapper.toUpstoxInstrumentKey("NSE_INDEX|Nifty Bank"))
        assertEquals("NSE_INDEX|Nifty Fin Service", UpstoxSymbolMapper.toUpstoxInstrumentKey("NSE_INDEX|Nifty Fin Service"))
        assertEquals("NSE_EQ|INE002A01018", UpstoxSymbolMapper.toUpstoxInstrumentKey("NSE_EQ|INE002A01018"))
        assertEquals("BSE_INDEX|SENSEX", UpstoxSymbolMapper.toUpstoxInstrumentKey("BSE_INDEX|SENSEX"))
        assertEquals("MCX_FO|CRUDEOIL", UpstoxSymbolMapper.toUpstoxInstrumentKey("MCX_FO|CRUDEOIL"))

        // 2. Display symbols must resolve through existing instrument mapping
        assertEquals("NSE_INDEX|Nifty 50", UpstoxSymbolMapper.toUpstoxInstrumentKey("NIFTY"))
        assertEquals("NSE_INDEX|Nifty 50", UpstoxSymbolMapper.toUpstoxInstrumentKey("NIFTY 50"))
        assertEquals("NSE_INDEX|Nifty 50", UpstoxSymbolMapper.toUpstoxInstrumentKey("nifty 50"))
        assertEquals("NSE_INDEX|Nifty Bank", UpstoxSymbolMapper.toUpstoxInstrumentKey("BANKNIFTY"))
        assertEquals("NSE_INDEX|Nifty Fin Service", UpstoxSymbolMapper.toUpstoxInstrumentKey("FINNIFTY"))
        assertEquals("BSE_INDEX|SENSEX", UpstoxSymbolMapper.toUpstoxInstrumentKey("SENSEX"))
        assertEquals("NSE_EQ|INE002A01018", UpstoxSymbolMapper.toUpstoxInstrumentKey("RELIANCE"))

        // 3. Reverse resolution
        val (niftySym, niftyExch) = UpstoxSymbolMapper.fromUpstoxInstrumentKey("NSE_INDEX|Nifty 50")
        assertEquals("NIFTY 50", niftySym)
        assertEquals("NSE", niftyExch)

        val (relianceSym, relianceExch) = UpstoxSymbolMapper.fromUpstoxInstrumentKey("NSE_EQ|INE002A01018")
        assertEquals("RELIANCE", relianceSym)
        assertEquals("NSE", relianceExch)
    }

    @Test
    fun test13_upstoxBinarySubscriptionPayloadConstruction() {
        val keys = listOf("NSE_INDEX|Nifty 50", "NSE_INDEX|Nifty Bank")
        val json = JSONObject().apply {
            put("guid", "test-guid-1234")
            put("method", "sub")
            put("data", JSONObject().apply {
                put("mode", "ltpc")
                put("instrumentKeys", org.json.JSONArray(keys))
            })
        }

        val payload = json.toString().toByteArray(Charsets.UTF_8)
        val byteString = payload.toByteString()

        assertTrue(byteString.size > 0)
        val decodedJson = JSONObject(byteString.utf8())
        assertEquals("sub", decodedJson.getString("method"))
        assertEquals("ltpc", decodedJson.getJSONObject("data").getString("mode"))
        assertEquals(2, decodedJson.getJSONObject("data").getJSONArray("instrumentKeys").length())
        assertEquals("NSE_INDEX|Nifty 50", decodedJson.getJSONObject("data").getJSONArray("instrumentKeys").getString(0))
        assertEquals("NSE_INDEX|Nifty Bank", decodedJson.getJSONObject("data").getJSONArray("instrumentKeys").getString(1))
    }

    @Test
    fun test14_upstoxBinaryFeedProducesLtpAndSetsLive() {
        healthManager.reportConnection(ProviderHealthManager.PROVIDER_UPSTOX, true)
        healthManager.reportSubscribed(ProviderHealthManager.PROVIDER_UPSTOX, 1)

        val stateBefore = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("WAITING_FOR_FIRST_TICK", stateBefore.status)
        assertFalse(stateBefore.healthy)

        // Construct valid Upstox V3 binary Protobuf feed with LTP = 22450.75
        val key = "NSE_INDEX|Nifty 50"
        val keyBytes = key.toByteArray(Charsets.UTF_8)

        val ltpcBuf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        ltpcBuf.put(0x09.toByte())
        ltpcBuf.putDouble(22450.75)
        ltpcBuf.put(0x10.toByte())
        writeVarint(ltpcBuf, System.currentTimeMillis())

        val ltpcBytes = ByteArray(ltpcBuf.position())
        System.arraycopy(ltpcBuf.array(), 0, ltpcBytes, 0, ltpcBytes.size)

        val feedBuf = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        feedBuf.put(0x0A.toByte())
        writeVarint(feedBuf, ltpcBytes.size.toLong())
        feedBuf.put(ltpcBytes)
        val feedBytes = ByteArray(feedBuf.position())
        System.arraycopy(feedBuf.array(), 0, feedBytes, 0, feedBytes.size)

        val mapEntryBuf = ByteBuffer.allocate(200).order(ByteOrder.LITTLE_ENDIAN)
        mapEntryBuf.put(0x0A.toByte())
        writeVarint(mapEntryBuf, keyBytes.size.toLong())
        mapEntryBuf.put(keyBytes)
        mapEntryBuf.put(0x12.toByte())
        writeVarint(mapEntryBuf, feedBytes.size.toLong())
        mapEntryBuf.put(feedBytes)
        val mapBytes = ByteArray(mapEntryBuf.position())
        System.arraycopy(mapEntryBuf.array(), 0, mapBytes, 0, mapBytes.size)

        val outerBuf = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)
        outerBuf.put(0x0A.toByte())
        writeVarint(outerBuf, mapBytes.size.toLong())
        outerBuf.put(mapBytes)

        val payload = ByteArray(outerBuf.position())
        System.arraycopy(outerBuf.array(), 0, payload, 0, payload.size)

        val parsedTicks = upstoxService.parseBinaryPacket(payload)
        assertEquals(1, parsedTicks)
        assertTrue(upstoxService.hasFirstTickReceived())
        assertEquals("LIVE", upstoxService.connectionState.value)

        val stateAfter = healthManager.getHealthState(ProviderHealthManager.PROVIDER_UPSTOX)
        assertEquals("LIVE", stateAfter.status)
        assertTrue(stateAfter.healthy)
        assertEquals("", stateAfter.lastError)
    }

    private fun writeVarint(buffer: ByteBuffer, value: Long) {
        var v = value
        while (v and -0x80L != 0L) {
            buffer.put(((v and 0x7FL) or 0x80L).toByte())
            v = v ushr 7
        }
        buffer.put((v and 0x7FL).toByte())
    }

    private class FakeUpstoxApi : UpstoxApi {
        override suspend fun getAccessToken(
            apiVersion: String,
            code: String,
            clientId: String,
            clientSecret: String,
            redirectUri: String,
            grantType: String
        ): Response<UpstoxTokenResponse> {
            return Response.success(UpstoxTokenResponse(accessToken = "test_token"))
        }

        override suspend fun exchangeTokenSecurely(
            url: String,
            code: String,
            redirectUri: String,
            clientId: String?,
            clientSecret: String?
        ): Response<UpstoxTokenResponse> {
            return Response.success(UpstoxTokenResponse(accessToken = "test_token"))
        }

        override suspend fun getUserProfile(token: String, apiVersion: String): Response<UpstoxProfileResponse> {
            return Response.success(UpstoxProfileResponse("ok", UpstoxProfileData("123", "Test User", "test@example.com")))
        }

        override suspend fun getMarketQuotes(token: String, instrumentKeys: String, apiVersion: String): Response<UpstoxMarketQuotesResponse> {
            return Response.success(UpstoxMarketQuotesResponse("ok", emptyMap()))
        }

        override suspend fun getOHLCQuotes(token: String, instrumentKeys: String, interval: String, apiVersion: String): Response<UpstoxMarketQuotesResponse> {
            return Response.success(UpstoxMarketQuotesResponse("ok", emptyMap()))
        }

        override suspend fun getLTPQuotes(token: String, instrumentKeys: String, apiVersion: String): Response<UpstoxMarketQuotesResponse> {
            return Response.success(UpstoxMarketQuotesResponse("ok", emptyMap()))
        }

        override suspend fun getOptionChain(token: String, instrumentKey: String, expiryDate: String, apiVersion: String): Response<UpstoxOptionChainResponse> {
            return Response.success(UpstoxOptionChainResponse("ok", emptyList()))
        }

        override suspend fun getOptionContracts(token: String, instrumentKey: String, apiVersion: String): Response<UpstoxOptionContractsResponse> {
            return Response.success(UpstoxOptionContractsResponse("ok", emptyList()))
        }

        override suspend fun getHistoricalCandles(token: String, instrumentKey: String, unit: String, interval: String, toDate: String, fromDate: String, apiVersion: String): Response<UpstoxHistoricalCandlesResponse> {
            return Response.success(UpstoxHistoricalCandlesResponse("ok", UpstoxCandlesData(emptyList())))
        }

        override suspend fun getIntradayCandles(token: String, instrumentKey: String, interval: String, apiVersion: String): Response<UpstoxHistoricalCandlesResponse> {
            return Response.success(UpstoxHistoricalCandlesResponse("ok", UpstoxCandlesData(emptyList())))
        }

        override suspend fun getWebSocketFeedAuth(token: String): Response<UpstoxFeedAuthResponse> {
            return Response.success(UpstoxFeedAuthResponse("ok", UpstoxFeedAuthData(authorizedRedirectUri = "wss://api.upstox.com/v3/feed/market-data-feed")))
        }
    }

    private class FakeFyersApi : FyersApi {
        override suspend fun getProfile(auth: String): Response<FyersProfileResponse> {
            return Response.success(FyersProfileResponse("ok", 200, "OK", FyersProfileData("Test User", "123", "test@example.com")))
        }

        override suspend fun getOptionChain(auth: String, symbol: String, strikecount: Int, timestamp: String): Response<FyersOptionChainResponse> {
            return Response.success(FyersOptionChainResponse("ok", null))
        }

        override suspend fun validateRefreshToken(request: FyersRefreshTokenRequest): Response<FyersTokenResponse> {
            return Response.success(FyersTokenResponse("ok", 200, "OK", "token", "refresh"))
        }

        override suspend fun validateAuthCode(request: FyersTokenRequest): Response<FyersTokenResponse> {
            return Response.success(FyersTokenResponse("ok", 200, "OK", "token", "refresh"))
        }

        override suspend fun exchangeTokenSecurely(url: String, code: String, redirectUri: String): Response<FyersTokenResponse> {
            return Response.success(FyersTokenResponse("ok", 200, "OK", "token", "refresh"))
        }

        override suspend fun getHistory(auth: String, symbol: String, resolution: String, dateFormat: Int, from: String, to: String): Response<FyersHistoryResponse> {
            return Response.success(FyersHistoryResponse("ok", emptyList()))
        }

        override suspend fun getQuotes(auth: String, symbols: String): Response<FyersQuotesResponse> {
            return Response.success(FyersQuotesResponse("ok", emptyList()))
        }
    }
}
