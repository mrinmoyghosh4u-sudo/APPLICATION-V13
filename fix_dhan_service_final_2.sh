cat << 'INNER_EOF' > app/src/main/java/com/example/data/network/DhanBrokerService.kt
package com.example.data.network

import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.data.model.OptionStrikeItem
import kotlin.math.abs

class DhanBrokerService(
    override val brokerName: String = "Dhan",
    private val api: DhanApi,
    private val sessionManager: SessionManager
) : IBrokerService {

    override suspend fun getProfile(): Result<UserProfileEntity> {
        if (sessionManager.dhanAccessToken.isNullOrEmpty()) {
            return Result.failure(Exception("Dhan account is not connected. Please connect your Dhan account."))
        }

        return runCatching {
            val response = api.getFundLimit()
            if (response.isSuccessful) {
                val fund = response.body()
                val avail = fund?.availableBalance ?: fund?.altAvailableBalance ?: 0.0
                
                // Fetch positions to get realized/unrealized P&L
                var totalRealized = 0.0
                var totalUnrealized = 0.0
                val posRes = api.getPositions()
                if (posRes.isSuccessful) {
                    posRes.body()?.forEach { 
                        totalRealized += it.realizedProfit
                        totalUnrealized += it.unrealizedProfit
                    }
                }

                UserProfileEntity(
                    id = 1,
                    name = "Dhan User",
                    email = "user@dhan.co",
                    availableMargin = avail,
                    accountBalance = avail,
                    todaysPnl = totalRealized + totalUnrealized,
                    todaysPnlPercent = 0.0,
                    connectedBroker = "Dhan",
                    dhanClientId = fund?.dhanClientId ?: "",
                    realizedPnl = totalRealized,
                    unrealizedPnl = totalUnrealized
                )
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
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
                    val lot = if (item.exchangeSegment == "NSE_FNO") 25 else 1
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
                        brokerOrderId = item.orderId
                    )
                } ?: emptyList()
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> {
        if (sessionManager.dhanAccessToken.isNullOrEmpty()) return Result.success(emptyList())

        return runCatching {
            val response = api.getPositions()
            if (response.isSuccessful) {
                response.body()?.map { item ->
                    val isLong = item.positionType.equals("LONG", ignoreCase = true) || item.netQty > 0
                    val avgPrice = if (isLong) item.buyAvg else item.sellAvg
                    
                    val pnl = item.realizedProfit + item.unrealizedProfit
                    val invested = abs(item.netQty * avgPrice)
                    val pnlPct = if (invested > 0) (pnl / invested) * 100 else 0.0

                    PortfolioHoldingEntity(
                        symbol = "${item.tradingSymbol}_${item.productType}",
                        exchange = item.exchangeSegment,
                        qty = item.netQty,
                        avgPrice = avgPrice,
                        ltp = 0.0, // Position API in v2 doesn't return LTP directly, usually calculate from unrealized PnL or fetch separately
                        currentValue = invested + item.unrealizedProfit, // Approx
                        pnl = pnl,
                        pnlPercent = pnlPct,
                        type = item.productType,
                        realizedPnl = item.realizedProfit,
                        unrealizedPnl = item.unrealizedProfit
                    )
                } ?: emptyList()
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun placeOrder(
        symbol: String,
        exchange: String,
        action: String,
        type: String,
        qty: Int,
        price: Double,
        productType: String,
        stopLoss: Double,
        target: Double
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
                "STOPLOSS" -> "STOP_LOSS"
                "STOPLOSS_MARKET" -> "STOP_LOSS_MARKET"
                else -> "MARKET"
            }

            val request = DhanPlaceOrderRequest(
                dhanClientId = clientId,
                transactionType = action.uppercase(), // BUY / SELL
                exchangeSegment = dhanExchange,
                productType = dhanProductType,
                orderType = dhanOrderType,
                tradingSymbol = symbol,
                quantity = qty,
                price = price
            )

            val response = api.placeOrder(request)
            if (response.isSuccessful) {
                response.body()?.orderId ?: throw Exception("Order ID not returned")
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun searchInstrument(query: String): Result<List<WatchlistItem>> {
        // Dhan doesn't have an open unauthenticated search API in v2 yet, usually relies on CSV dump
        // We will mock this or skip it as requested not to use mock data for trading.
        // For the sake of the interface, return empty list if not implemented natively.
        return Result.success(emptyList())
    }

    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> {
        return runCatching {
            val request = DhanOptionChainRequest(
                underlyingScrip = 13, // Example mapping, requires proper symbol to scrip ID mapping in prod
                underlyingSeg = "IDX_I",
                expiry = expiry
            )
            val response = api.getOptionChain(request)
            if (response.isSuccessful) {
                response.body()?.map { item ->
                    OptionStrikeItem(
                        strikePrice = item.strikePrice.toString(),
                        callLtp = item.callLtp.toString(),
                        callOi = item.callOi.toString(),
                        putLtp = item.putLtp.toString(),
                        putOi = item.putOi.toString(),
                        callChgOi = "0",
                        callIv = "0",
                        callDelta = "0",
                        putDelta = "0",
                        putIv = "0",
                        putChgOi = "0"
                    )
                } ?: emptyList()
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }
}
INNER_EOF
