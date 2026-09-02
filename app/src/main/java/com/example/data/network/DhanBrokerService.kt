package com.example.data.network

import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.data.model.OptionStrikeItem
import com.example.data.model.PnlState
import com.example.data.model.MarginState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DhanBrokerService(
    override val brokerName: String = "Dhan",
    private val api: DhanApi,
    private val sessionManager: SessionManager
) : IBrokerService {

    private val directClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun getProfile(): Result<UserProfileEntity> = withContext(Dispatchers.IO) {
        val token = sessionManager.dhanAccessToken.trim()
        val clientId = sessionManager.dhanClientId.trim()
        if (token.isEmpty()) {
            return@withContext Result.failure(Exception("Dhan account is not connected. Please connect your Dhan account."))
        }

        runCatching {
            var avail: Double? = null
            var collateral = 0.0
            var returnedClientId = clientId
            var tokenInvalid = false
            var authErrorCode = 0

            // 1. Try Retrofit API call
            runCatching {
                val response = api.getFundLimit()
                if (response.isSuccessful) {
                    val fund = response.body()
                    val parsedAvail = fund?.availableBalance 
                        ?: fund?.altAvailableBalance 
                        ?: fund?.netMarginAvailable 
                        ?: fund?.sodLimit 
                        ?: fund?.cashBalance 
                        ?: fund?.withdrawableBalance
                    if (parsedAvail != null) {
                        avail = parsedAvail
                    }
                    fund?.collateralAmount?.let { collateral = it }
                    fund?.dhanClientId?.takeIf { it.isNotBlank() }?.let { returnedClientId = it }
                } else if (response.code() == 401 || response.code() == 403) {
                    tokenInvalid = true
                    authErrorCode = response.code()
                }
            }

            if (tokenInvalid) {
                sessionManager.isDhanConnected = false
                throw Exception("Dhan access token expired or invalid (HTTP $authErrorCode). Please reconnect your Dhan account.")
            }

            // 2. If Retrofit didn't get fund value or failed, use direct OkHttp fallback
            if (avail == null) {
                val directResult = fetchFundsDirectly(token, clientId)
                if (directResult != null) {
                    avail = directResult.first
                    collateral = directResult.second
                    if (directResult.third.isNotBlank()) {
                        returnedClientId = directResult.third
                    }
                }
            }

            // Critical Guard: If balance could not be retrieved from either method, do NOT default to 0.0
            val finalAvail = avail ?: throw Exception("Unable to fetch Dhan balance. Broker fund limit endpoint returned no valid balance.")

            // 3. Fetch positions to get realized/unrealized P&L
            var totalRealized = 0.0
            var totalUnrealized = 0.0
            var pnlState = PnlState.UNAVAILABLE.name
            var lastPnlTime = 0L
            var pnlError = ""

            try {
                val posRes = api.getPositions()
                if (posRes.isSuccessful) {
                    val posList = posRes.body()
                    if (posList != null) {
                        posList.forEach { 
                            totalRealized += it.realizedProfit
                            totalUnrealized += it.unrealizedProfit
                        }
                    }
                    pnlState = PnlState.AVAILABLE.name
                    lastPnlTime = System.currentTimeMillis()
                } else {
                    pnlState = if (posRes.code() == 401 || posRes.code() == 403) PnlState.AUTH_ERROR.name else PnlState.UNAVAILABLE.name
                    pnlError = "Dhan P&L refresh failed: HTTP ${posRes.code()}"
                    android.util.Log.w("DhanBrokerService", pnlError)
                }
            } catch (e: Exception) {
                pnlState = if (e is java.io.IOException) PnlState.NETWORK_ERROR.name else PnlState.UNAVAILABLE.name
                pnlError = "Dhan P&L fetch error: ${e.message}"
                android.util.Log.w("DhanBrokerService", "Dhan P&L fetch exception: ${e.javaClass.simpleName}")
            }

            val finalClientId = returnedClientId.ifBlank { clientId }
            val accountName = if (finalClientId.isNotBlank()) "Dhan Account ($finalClientId)" else "Dhan Account"

            UserProfileEntity(
                id = 1,
                name = accountName,
                email = "",
                availableMargin = finalAvail,
                accountBalance = finalAvail,
                totalBalance = finalAvail + collateral,
                todaysPnl = totalRealized + totalUnrealized,
                todaysPnlPercent = 0.0,
                connectedBroker = "Dhan",
                isDhanConnected = true,
                dhanClientId = finalClientId,
                realizedPnl = totalRealized,
                unrealizedPnl = totalUnrealized,
                pnlStatus = pnlState,
                marginStatus = MarginState.AVAILABLE.name,
                lastPnlSyncTime = lastPnlTime,
                lastMarginSyncTime = System.currentTimeMillis(),
                pnlErrorMessage = pnlError
            )
        }
    }

    private fun fetchFundsDirectly(token: String, clientId: String): Triple<Double, Double, String>? {
        val endpoints = listOf(
            "https://api.dhan.co/v2/fundlimit"
        )
        for (url in endpoints) {
            try {
                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("access-token", token)
                if (clientId.isNotBlank()) {
                    reqBuilder.header("client-id", clientId)
                }
                val response = directClient.newCall(reqBuilder.build()).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotBlank() && bodyStr.trim().startsWith("{")) {
                        val json = JSONObject(bodyStr)
                        val parsedAvail: Double? = when {
                            json.has("availabelBalance") -> json.optDouble("availabelBalance")
                            json.has("availableBalance") -> json.optDouble("availableBalance")
                            json.has("netMarginAvailable") -> json.optDouble("netMarginAvailable")
                            json.has("sodLimit") -> json.optDouble("sodLimit")
                            json.has("withdrawableBalance") -> json.optDouble("withdrawableBalance")
                            json.has("cashBalance") -> json.optDouble("cashBalance")
                            else -> null
                        }
                        if (parsedAvail != null && !parsedAvail.isNaN()) {
                            val collateral = json.optDouble("collateralAmount", 0.0)
                            val cId = json.optString("dhanClientId", "")
                            return Triple(parsedAvail, collateral, cId)
                        }
                    }
                } else if (response.code == 401 || response.code == 403) {
                    android.util.Log.w("DhanBrokerService", "Direct fund fetch: token expired or invalid (HTTP ${response.code})")
                    return null
                }
            } catch (e: Exception) {
                android.util.Log.e("DhanBrokerService", "Direct fund fetch failed on $url: ${e.message}")
            }
        }
        return null
    }

    override suspend fun getFunds(): Result<Double> {
        return runCatching {
            getProfile().getOrThrow().availableMargin
        }
    }

    override suspend fun getHoldings(): Result<List<PortfolioHoldingEntity>> {
        if (sessionManager.dhanAccessToken.isNullOrEmpty()) return Result.success(emptyList())

        return runCatching {
            val response = api.getHoldings()
            if (response.isSuccessful) {
                response.body()?.map { item ->
                    val invested = item.totalQty * item.avgCostPrice
                    val currentValue = item.totalQty * item.lastTradedPrice
                    val pnl = currentValue - invested
                    val pnlPct = if (invested > 0) (pnl / invested) * 100 else 0.0

                    PortfolioHoldingEntity(
                        symbol = item.tradingSymbol.ifEmpty { item.isin },
                        exchange = item.exchange,
                        qty = item.totalQty,
                        avgPrice = item.avgCostPrice,
                        ltp = item.lastTradedPrice,
                        currentValue = currentValue,
                        pnl = pnl,
                        pnlPercent = pnlPct,
                        type = "EQUITY"
                    )
                } ?: emptyList()
            } else if (response.code() == 401 || response.code() == 403) {
                android.util.Log.w("DhanBrokerService", "Holdings fetch: Dhan token expired (HTTP ${response.code()})")
                emptyList()
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun getOrders(): Result<List<OrderEntity>> {
        if (sessionManager.dhanAccessToken.isNullOrEmpty()) return Result.success(emptyList())

        return runCatching {
            val response = api.getOrders()
            if (response.isSuccessful) {
                response.body()?.map { item ->
                    val lot = com.example.util.AppPreferences.getGlobalLotSize(item.tradingSymbol)
                    OrderEntity(
                        orderId = item.orderId,
                        symbol = item.tradingSymbol,
                        exchange = item.exchangeSegment,
                        lotSize = lot,
                        qty = item.quantity,
                        filledQty = item.tradedQty,
                        remainingQty = item.quantity - item.tradedQty,
                        orderType = item.orderType,
                        productType = item.productType,
                        side = item.transactionType,
                        price = item.price,
                        avgPrice = item.averagePrice,
                        value = item.quantity * item.price,
                        stopLoss = item.triggerPrice, // Use triggerPrice for SL mapping
                        target = 0.0,
                        status = item.orderStatus.uppercase(),
                        time = item.updateTime ?: item.createTime ?: "",
                        brokerOrderId = item.orderId,
                        remarks = item.remarks ?: item.text ?: ""
                    )
                } ?: emptyList()
            } else if (response.code() == 401 || response.code() == 403) {
                android.util.Log.w("DhanBrokerService", "Orders fetch: Dhan token expired (HTTP ${response.code()})")
                emptyList()
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    suspend fun getTrades(): Result<List<DhanTradeItem>> {
        if (sessionManager.dhanAccessToken.isNullOrEmpty()) return Result.success(emptyList())

        return runCatching {
            val response = api.getTrades()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                val code = response.code()
                android.util.Log.w("DhanBrokerService", "Dhan trades fetch failed: HTTP $code")
                throw Exception("Trades API Error: HTTP $code")
            }
        }
    }

    override suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> {
        if (sessionManager.dhanAccessToken.isNullOrEmpty()) return Result.failure(Exception("Dhan account is not connected."))

        return runCatching {
            val response = api.getPositions()
            if (response.isSuccessful) {
                val posList = response.body() ?: emptyList()
                posList.map { item ->
                    val isClosed = item.netQty == 0 || item.positionType.equals("CLOSED", ignoreCase = true)
                    val posStatus = if (isClosed) "CLOSED" else "OPEN"
                    val isLong = item.positionType.equals("LONG", ignoreCase = true) || item.netQty > 0
                    
                    // Option Type determination (PE vs CE)
                    val rawOptType = item.drvOptionType?.uppercase() ?: ""
                    val symUpper = item.tradingSymbol.uppercase()
                    val optType = when {
                        rawOptType == "CALL" || rawOptType == "CE" || symUpper.contains("-CE") || symUpper.contains(" CE") || symUpper.endsWith("CE") -> "CE"
                        rawOptType == "PUT" || rawOptType == "PE" || symUpper.contains("-PE") || symUpper.contains(" PE") || symUpper.endsWith("PE") || symUpper.contains("PUT") -> "PE"
                        symUpper.contains("FUT") -> "FUT"
                        else -> "EQUITY"
                    }

                    // Strike Price determination
                    val strike = item.drvStrikePrice ?: run {
                        val numbers = Regex("\\d+").findAll(symUpper).map { it.value }.toList()
                        numbers.lastOrNull { it.length >= 4 && !it.startsWith("202") }?.toDoubleOrNull() ?: 0.0
                    }

                    // Expiry Date determination
                    val formattedExpiry = item.expiryDate ?: ""

                    val avgPrice = if (isClosed) {
                        if (item.buyAvg > 0) item.buyAvg else item.sellAvg
                    } else if (isLong) item.buyAvg else item.sellAvg

                    val pnl = item.realizedProfit + item.unrealizedProfit
                    val invested = abs(item.netQty * avgPrice)
                    val pnlPct = if (invested > 0) (pnl / invested) * 100 else 0.0

                    PortfolioHoldingEntity(
                        symbol = item.tradingSymbol.ifEmpty { "POS_${item.securityId}" },
                        exchange = item.exchangeSegment,
                        type = optType,
                        expiry = formattedExpiry,
                        qty = item.netQty,
                        avgPrice = avgPrice,
                        ltp = 0.0,
                        currentValue = if (isClosed) 0.0 else (invested + item.unrealizedProfit),
                        pnl = pnl,
                        pnlPercent = pnlPct,
                        realizedPnl = item.realizedProfit,
                        unrealizedPnl = item.unrealizedProfit,
                        securityId = item.securityId,
                        buyAvg = item.buyAvg,
                        buyQty = item.buyQty,
                        sellAvg = item.sellAvg,
                        sellQty = item.sellQty,
                        optionType = optType,
                        strikePrice = strike,
                        positionStatus = posStatus,
                        productType = item.productType
                    )
                }
            } else {
                val code = response.code()
                android.util.Log.w("DhanBrokerService", "Dhan positions fetch failed: HTTP $code")
                throw Exception("Positions API Error: HTTP $code")
            }
        }
    }

    override suspend fun placeOrder(order: OrderEntity): Result<String> {
        val secId = if (order.securityId.isNotBlank()) order.securityId else com.example.util.InstrumentMapUtil.getDhanSecurityId(order.symbol, order.exchange)
        return placeOrder(
            symbol = order.symbol,
            exchange = order.exchange,
            action = order.side,
            type = order.orderType,
            qty = order.qty,
            price = order.price,
            productType = order.productType,
            stopLoss = order.stopLoss,
            target = order.target,
            securityId = secId
        )
    }

    suspend fun placeOrder(
        symbol: String,
        exchange: String,
        action: String,
        type: String,
        qty: Int,
        price: Double,
        productType: String,
        stopLoss: Double,
        target: Double,
        securityId: String = ""
    ): Result<String> {
        return runCatching {
            val clientId = sessionManager.dhanClientId ?: throw Exception("Dhan Client ID not found. Re-login required.")
            
            // Map productType to Dhan specific formats
            val dhanProductType = when (productType.uppercase()) {
                "INTRADAY", "MIS" -> "INTRADAY"
                "MARGIN", "NRML" -> "MARGIN"
                "DELIVERY", "CNC" -> "CNC"
                "MTF" -> "MTF"
                "CO" -> "CO"
                "BO" -> "BO"
                else -> "INTRADAY"
            }
            
            // Map exchange
            val dhanExchange = when (exchange.uppercase()) {
                "NSE" -> "NSE_EQ"
                "BSE" -> "BSE_EQ"
                "NFO", "NSE_FNO" -> "NSE_FNO"
                "MCX" -> "MCX_COMM"
                else -> "NSE_EQ"
            }

            // Map Order Type
            val dhanOrderType = when (type.uppercase()) {
                "MARKET" -> "MARKET"
                "LIMIT" -> "LIMIT"
                "STOPLOSS", "SL", "STOP_LOSS" -> "STOP_LOSS"
                "STOPLOSS_MARKET", "SL-M", "SL_M", "STOP_LOSS_MARKET" -> "STOP_LOSS_MARKET"
                else -> "MARKET"
            }

            val finalSecId = if (securityId.isNotBlank()) securityId else com.example.util.InstrumentMapUtil.getDhanSecurityId(symbol, exchange)
            if (finalSecId.isBlank()) {
                throw Exception("INSTRUMENT NOT FOUND: Cannot resolve Dhan security ID for $symbol")
            }

            val request = DhanPlaceOrderRequest(
                dhanClientId = clientId,
                transactionType = action.uppercase(), // BUY / SELL
                exchangeSegment = dhanExchange,
                productType = dhanProductType,
                orderType = dhanOrderType,
                tradingSymbol = symbol,
                securityId = finalSecId,
                quantity = qty,
                price = price
            )

            val response = api.placeOrder(request)
            if (response.isSuccessful) {
                response.body()?.orderId ?: throw Exception("Order ID not returned by Dhan API")
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String): Result<Boolean> {
        return runCatching {
            val clientId = sessionManager.dhanClientId ?: throw Exception("Dhan Client ID not found.")
            val req = DhanModifyOrderRequest(
                dhanClientId = clientId,
                orderId = orderId,
                orderType = orderType.uppercase(),
                quantity = newQty,
                price = newPrice
            )
            val res = api.modifyOrder(orderId, req)
            if (res.isSuccessful) {
                true
            } else {
                val err = res.errorBody()?.string() ?: ""
                throw Exception("Modify order failed on Dhan: $err")
            }
        }
    }

    override suspend fun cancelOrder(orderId: String): Result<Boolean> {
        return runCatching {
            val response = api.cancelOrder(orderId)
            if (response.isSuccessful) {
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return Result.success(emptyList())
    }

    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> {
        return Result.failure(Exception("Dhan Option Chain disabled"))
    }
    
    override suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return Result.failure(Exception("Dhan Option Chain disabled"))
    }
    
    suspend fun searchInstrument(query: String): Result<List<WatchlistItem>> {
        return Result.success(emptyList())
    }

    override suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<com.example.ui.components.CandleData>> {
        return Result.success(emptyList())
    }
}
