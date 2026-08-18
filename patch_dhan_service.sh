cat << 'INNER_EOF' > app/src/main/java/com/example/data/network/DhanBrokerService.kt
package com.example.data.network

import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem

class DhanBrokerService(
    private val api: DhanApi,
    private val sessionManager: SessionManager
) : IBrokerService {

    override val brokerName: String = "Dhan"

    override suspend fun getProfile(): Result<UserProfileEntity> {
        if (sessionManager.dhanAccessToken.isNullOrEmpty()) {
            return Result.failure(Exception("Dhan account is not connected. Please connect your Dhan account."))
        }
        return runCatching {
            val response = api.getFundLimit()
            if (response.isSuccessful) {
                val fund = response.body()
                val avail = fund?.availableBalance ?: fund?.altAvailableBalance ?: 0.0
                UserProfileEntity(
                    name = "Dhan Client",
                    connectedBroker = "Dhan",
                    isDhanConnected = true,
                    dhanClientId = fund?.dhanClientId ?: sessionManager.dhanClientId,
                    availableMargin = avail,
                    accountBalance = avail + (fund?.collateralAmount ?: 0.0)
                )
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun getFunds(): Result<Double> {
        return runCatching {
            val response = api.getFundLimit()
            if (response.isSuccessful) {
                val fund = response.body()
                fund?.availableBalance ?: fund?.altAvailableBalance ?: 0.0
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun getOrders(): Result<List<OrderEntity>> {
        return runCatching {
            val response = api.getOrders()
            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                items.map { item ->
                    val lot = when {
                        item.tradingSymbol.contains("CRUDEOILM") -> 10
                        item.tradingSymbol.contains("CRUDE") -> 100
                        item.tradingSymbol.contains("NATURAL") -> 1250
                        item.tradingSymbol.contains("GOLD") -> 100
                        item.tradingSymbol.contains("SILVER") -> 30
                        item.tradingSymbol.contains("MIDCPNIFTY") -> 75
                        item.tradingSymbol.contains("BANKNIFTY") -> 30
                        item.tradingSymbol.contains("FINNIFTY") -> 40
                        item.tradingSymbol.contains("SENSEX") -> 20
                        item.tradingSymbol.contains("BANKEX") -> 15
                        item.tradingSymbol.contains("NIFTY") -> 65
                        else -> 1
                    }
                    OrderEntity(
                        orderId = item.orderId,
                        symbol = item.tradingSymbol,
                        exchange = item.exchangeSegment,
                        lotSize = lot,
                        qty = item.quantity,
                        orderType = item.orderType,
                        side = item.transactionType,
                        price = item.price,
                        value = item.quantity * item.price,
                        stopLoss = 0.0,
                        target = 0.0,
                        status = item.orderStatus.uppercase(),
                        time = item.createTime ?: "",
                        brokerOrderId = item.orderId
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun placeOrder(order: OrderEntity): Result<String> {
        return runCatching {
            val req = DhanPlaceOrderRequest(
                dhanClientId = sessionManager.dhanClientId,
                transactionType = order.side,
                exchangeSegment = if (order.exchange == "MCX") "MCX_COMM" else "NSE_EQ",
                orderType = order.orderType,
                tradingSymbol = order.symbol,
                quantity = order.qty,
                price = order.price
            )
            val response = api.placeOrder(req)
            if (response.isSuccessful) {
                response.body()?.orderId ?: "DHAN_${System.currentTimeMillis()}"
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("Order placement failed (${response.code()}): $errorBody")
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
                throw Exception("Cancel order failed (${response.code()}): $errorBody")
            }
        }
    }

    override suspend fun getHoldings(): Result<List<PortfolioHoldingEntity>> {
        return runCatching {
            val response = api.getHoldings()
            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                items.map { item ->
                    val pnl = (item.lastTradedPrice - item.avgCostPrice) * item.totalQty
                    val pnlPct = if (item.avgCostPrice > 0) (pnl / (item.avgCostPrice * item.totalQty)) * 100 else 0.0
                    PortfolioHoldingEntity(
                        symbol = item.tradingSymbol,
                        exchange = item.exchange,
                        type = "EQUITY",
                        qty = item.totalQty,
                        avgPrice = item.avgCostPrice,
                        ltp = item.lastTradedPrice,
                        currentValue = item.totalQty * item.lastTradedPrice,
                        pnl = pnl,
                        pnlPercent = pnlPct
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                // Dhan returns 400 with "Holdings does not exist" sometimes. Let's parse it or return empty.
                if (errorBody.contains("Holdings does not exist", ignoreCase = true) || errorBody.contains("DH-1111")) {
                    emptyList()
                } else {
                    throw Exception("API Error ${response.code()}: $errorBody")
                }
            }
        }
    }

    override suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> {
        return runCatching {
            val response = api.getPositions()
            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                items.map { item ->
                    val ltp = if (item.netQty != 0) item.buyAvg else 0.0
                    PortfolioHoldingEntity(
                        symbol = item.tradingSymbol,
                        exchange = "NSE",
                        type = "CE",
                        qty = item.netQty,
                        avgPrice = item.buyAvg,
                        ltp = ltp,
                        currentValue = item.netQty * ltp,
                        pnl = item.unrealizedProfit + item.realizedProfit,
                        pnlPercent = 0.0
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                throw Exception("API Error ${response.code()}: $errorBody")
            }
        }
    }

    override suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return runCatching {
            emptyList()
        }
    }

    override suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return runCatching {
            val apiResponse = runCatching { api.getOptionExpiries(symbol) }.getOrNull()
            val liveExpiries = if (apiResponse != null && apiResponse.isSuccessful) {
                apiResponse.body() ?: emptyList()
            } else {
                emptyList()
            }
            com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol, liveExpiries)
        }
    }

    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> {
        return runCatching {
            val apiExpiry = if (expiry.isNotBlank()) {
                com.example.util.OptionExpiryUtil.formatForApi(expiry)
            } else {
                val firstExpiry = com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol).firstOrNull() ?: ""
                com.example.util.OptionExpiryUtil.formatForApi(firstExpiry)
            }

            val req = DhanOptionChainRequest(expiry = apiExpiry)
            val response = runCatching { api.getOptionChain(req) }.getOrNull()
            if (response != null && response.isSuccessful && !response.body().isNullOrEmpty()) {
                val items = response.body()!!
                val spotPrice = 24850.40
                val step = if (symbol.contains("BANKNIFTY") || symbol.contains("SENSEX") || symbol.contains("BANKEX")) 100.0 else 50.0
                val atmStrike = (spotPrice / step).let { Math.round(it) * step }
                items.map { item ->
                    OptionStrikeItem(
                        strikePrice = item.strikePrice,
                        callOi = String.format("%.1fk", item.callOi / 1000.0),
                        callChgOi = "+12.4k",
                        callIv = 12.5,
                        callLtp = item.callLtp,
                        callDelta = 0.48,
                        putDelta = -0.52,
                        putLtp = item.putLtp,
                        putIv = 13.1,
                        putChgOi = "+15.2k",
                        putOi = String.format("%.1fk", item.putOi / 1000.0),
                        isAtm = item.strikePrice == atmStrike
                    )
                }
            } else {
                emptyList<OptionStrikeItem>()
            }
        }
    }
}
INNER_EOF
