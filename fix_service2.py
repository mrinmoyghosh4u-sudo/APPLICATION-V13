content = """package com.example.data.network

import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem

class AngelOneBrokerService(
    private val api: AngelOneApi,
    private val sessionManager: SessionManager,
    private val instrumentMaster: InstrumentMasterService
) : IBrokerService {

    override val brokerName: String = "Angel One"

    override suspend fun getProfile(): Result<UserProfileEntity> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) {
            return Result.failure(Exception("Angel One account is not connected. Please connect your account."))
        }
        return runCatching {
            val response = runCatching { api.getProfile() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                val data = response.body()?.data
                val rms = runCatching { api.getRMS().body()?.data }.getOrNull()
                val margin = rms?.availableMargin.toDoubleOrDefault(0.0)
                val balance = rms?.net.toDoubleOrDefault(0.0)

                var totalRealized = 0.0
                var totalUnrealized = 0.0
                val posRes = runCatching { api.getPositions() }.getOrNull()
                if (posRes != null && posRes.isSuccessful && posRes.body()?.status == true) {
                    posRes.body()?.data?.forEach { item ->
                        totalRealized += item.realisedPnL.toDoubleOrDefault(0.0)
                        totalUnrealized += item.unrealisedPnL.toDoubleOrDefault(0.0)
                    }
                }

                UserProfileEntity(
                    name = data?.name.toStringOrDefault(""),
                    email = data?.email.toStringOrDefault(""),
                    phone = data?.mobileNo.toStringOrDefault(""),
                    connectedBroker = "Angel One",
                    isAngelConnected = true,
                    angelClientId = data?.clientCode.toStringOrDefault(sessionManager.angelClientId),
                    availableMargin = margin,
                    accountBalance = balance,
                    realizedPnl = totalRealized,
                    unrealizedPnl = totalUnrealized
                )
            } else {
                val code = response?.code() ?: 0
                val msg = response?.body()?.message ?: ""
                if (code == 401 || code == 403 || response?.body()?.status == false || msg.contains("invalid", ignoreCase = true) || msg.contains("token", ignoreCase = true) || msg.contains("expired", ignoreCase = true)) {
                    throw Exception("Angel One session expired: $msg (Code: $code)")
                }
                throw Exception("Angel One profile request failed with code $code: $msg")
            }
        }
    }

    override suspend fun getFunds(): Result<Double> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) return Result.success(0.0)
        return runCatching {
            val response = runCatching { api.getRMS() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                response.body()?.data?.availableMargin.toDoubleOrDefault(0.0)
            } else {
                0.0
            }
        }
    }

    override suspend fun getOrders(): Result<List<OrderEntity>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) return Result.success(emptyList())
        return runCatching {
            val response = runCatching { api.getOrderBook() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                val items = response.body()?.data ?: emptyList()
                items.map { item ->
                    val qty = item.quantity.toIntOrDefault(0)
                    val price = item.price.toDoubleOrDefault(0.0)
                    val sym = item.tradingSymbol.toStringOrDefault("")
                    val ex = item.exchange.toStringOrDefault("NSE")
                    val lot = com.example.util.AppPreferences.getGlobalLotSize(sym)
                    val orderIdStr = item.orderId.toStringOrDefault("")
                    OrderEntity(
                        orderId = orderIdStr,
                        symbol = sym,
                        exchange = ex,
                        lotSize = lot,
                        qty = qty,
                        orderType = item.orderType.toStringOrDefault("LIMIT"),
                        side = item.transactionType.toStringOrDefault("BUY"),
                        price = price,
                        value = qty * price,
                        stopLoss = 0.0,
                        target = 0.0,
                        status = item.status.toStringOrDefault("").uppercase(),
                        time = item.orderUpdateTime.toStringOrDefault(""),
                        brokerOrderId = orderIdStr
                    )
                }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun placeOrder(order: OrderEntity): Result<String> {
        return runCatching {
            val token = if (order.symbolToken.isNotBlank()) order.symbolToken else instrumentMaster.resolveAngelToken(order.symbol, order.exchange) ?: ""
            val req = AngelPlaceOrderRequest(
                tradingSymbol = order.symbol,
                symbolToken = token,
                transactionType = order.side.uppercase(),
                exchange = order.exchange.uppercase(),
                orderType = order.orderType.uppercase(),
                productType = if (order.productType.isNotBlank()) order.productType.uppercase() else "INTRADAY",
                price = order.price.toString(),
                quantity = order.qty.toString()
            )
            val response = api.placeOrder(req)
            if (response.isSuccessful && response.body()?.status == true) {
                response.body()?.data?.orderId ?: throw Exception("Angel One returned success but no order ID.")
            } else {
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Unknown API Error"
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }

    override suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String): Result<Boolean> {
        return runCatching {
            val req = AngelModifyOrderRequest(
                orderId = orderId,
                orderType = orderType,
                price = newPrice.toString(),
                quantity = newQty.toString()
            )
            val response = api.modifyOrder(req)
            if (response.isSuccessful && response.body()?.status == true) true else throw Exception(response.body()?.message ?: "Modify Order Failed")
        }
    }

    override suspend fun cancelOrder(orderId: String): Result<Boolean> {
        return runCatching {
            val req = AngelCancelOrderRequest(orderId = orderId)
            val response = api.cancelOrder(req)
            if (response.isSuccessful && response.body()?.status == true) true else throw Exception(response.body()?.message ?: "Cancel Order Failed")
        }
    }

    override suspend fun getHoldings(): Result<List<PortfolioHoldingEntity>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) return Result.success(emptyList())
        return runCatching {
            val response = runCatching { api.getHoldings() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                val items = response.body()?.data ?: emptyList()
                items.map { item ->
                    val sym = item.tradingSymbol.toStringOrDefault("")
                    val ex = item.exchange.toStringOrDefault("NSE")
                    val qty = item.quantity.toIntOrDefault(0)
                    val avgPrice = if (item.averagePrice != null && item.averagePrice != 0.0) item.averagePrice.toDoubleOrDefault(0.0) else item.avgPrice.toDoubleOrDefault(0.0)
                    val ltp = item.ltp.toDoubleOrDefault(0.0)
                    val pnl = if (item.pnl != null && item.pnl != 0.0) item.pnl.toDoubleOrDefault(0.0) else item.realisedPnL.toDoubleOrDefault(item.unrealisedPnL.toDoubleOrDefault(0.0))
                    val pnlPct = item.pnlPercentage.toDoubleOrDefault(0.0)
                    PortfolioHoldingEntity(
                        symbol = sym,
                        exchange = ex,
                        type = "EQUITY",
                        qty = qty,
                        avgPrice = avgPrice,
                        ltp = ltp,
                        currentValue = qty * ltp,
                        pnl = pnl,
                        pnlPercent = pnlPct
                    )
                }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) return Result.success(emptyList())
        return runCatching {
            val response = runCatching { api.getPositions() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                val items = response.body()?.data ?: emptyList()
                items.map { item ->
                    val sym = item.tradingSymbol.toStringOrDefault("")
                    val ex = item.exchange.toStringOrDefault("NSE")
                    val pType = item.productType.toStringOrDefault("INTRADAY")
                    val qty = item.netQty.toIntOrDefault(0)
                    val ltp = item.ltp.toDoubleOrDefault(0.0)
                    val pnl = if (item.pnl != null && item.pnl != "0") item.pnl.toDoubleOrDefault(0.0) else item.realisedPnL.toDoubleOrDefault(item.unrealisedPnL.toDoubleOrDefault(0.0))
                    val buyAvg = item.buyAvgPrice.toDoubleOrDefault(item.sellAvgPrice.toDoubleOrDefault(0.0))
                    PortfolioHoldingEntity(
                        symbol = sym,
                        exchange = ex,
                        type = pType,
                        qty = qty,
                        avgPrice = buyAvg,
                        ltp = ltp,
                        currentValue = qty * ltp,
                        pnl = pnl,
                        pnlPercent = if (buyAvg > 0 && qty != 0) (pnl / (buyAvg * Math.abs(qty))) * 100 else 0.0
                    )
                }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return runCatching {
            val tokenMap = mutableMapOf<String, MutableList<String>>()
            symbols.forEach { sym ->
                val token = instrumentMaster.resolveAngelToken(sym, "NSE") ?: ""
                if (token.isNotBlank()) {
                    val ex = when {
                        sym.contains("CRUDE", ignoreCase = true) -> "MCX"
                        sym.contains("SENSEX", ignoreCase = true) || sym.contains("BANKEX", ignoreCase = true) -> "BSE"
                        else -> "NSE"
                    }
                    tokenMap.getOrPut(ex) { mutableListOf() }.add(token)
                }
            }

            val response = api.getQuotes(AngelQuoteRequest(exchangeTokens = tokenMap))
            if (response.isSuccessful && response.body()?.status == true) {
                val quotes = response.body()?.data?.fetched ?: emptyList()
                val unfetched = response.body()?.data?.unfetched ?: emptyList()
                unfetched.forEach { u -> android.util.Log.w("AngelOneBrokerService", "Unfetched quote: exchange=${u.exchange} token=${u.symbolToken} reason=${u.message}") }
                
                quotes.map { q ->
                    val sym = q.tradingSymbol.toStringOrDefault("")
                    val ex = q.exchange.toStringOrDefault("NSE")
                    val ltp = q.ltp.toDoubleOrDefault(0.0)
                    val change = q.netChange.toDoubleOrDefault(0.0)
                    val pct = q.percentChange.toDoubleOrDefault(0.0)
                    WatchlistItem(
                        symbol = sym,
                        exchange = ex,
                        ltp = ltp,
                        change = change,
                        changePercent = pct,
                        lotSize = com.example.util.AppPreferences.getGlobalLotSize(sym),
                        isPositive = change >= 0
                    )
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.body()?.message ?: "Unknown API Error"
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return runCatching {
            val apiResponse = api.getOptionExpiries(symbol)
            val liveExpiries = if (apiResponse.isSuccessful) {
                apiResponse.body()?.data ?: emptyList()
            } else {
                emptyList()
            }
            com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol, liveExpiries)
        }
    }

    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> {
        return runCatching {
            val instrument = instrumentMaster.resolveIndexToken(symbol)
            if (instrument == null) throw Exception("Option Chain unavailable for this instrument.")
            
            val exchangeForOptions = when (instrument.exch_seg) {
                "MCX" -> "MCX"
                "BSE" -> "BFO"
                else -> "NFO"
            }
            
            val request = AngelOptionChainRequest(
                exchange = exchangeForOptions,
                symboltoken = instrument.token,
                expirydate = expiry
            )
            val response = api.getOptionChain(request)
            if (response.isSuccessful && response.body()?.status == true) {
                response.body()?.data?.map { item ->
                    OptionStrikeItem(
                        strikePrice = item.strikePrice?.toDoubleOrNull() ?: 0.0,
                        callOi = "${item.callOi ?: 0.0}",
                        callChgOi = "${item.callChgOi ?: 0.0}",
                        callIv = 0.0,
                        callLtp = item.callLtp ?: 0.0,
                        callDelta = 0.0,
                        putDelta = 0.0,
                        putLtp = item.putLtp ?: 0.0,
                        putIv = 0.0,
                        putChgOi = "${item.putChgOi ?: 0.0}",
                        putOi = "${item.putOi ?: 0.0}",
                        callVolume = "${item.callVolume ?: 0.0}",
                        putVolume = "${item.putVolume ?: 0.0}"
                    )
                } ?: emptyList()
            } else {
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }
}
"""
open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt', 'w').write(content)
