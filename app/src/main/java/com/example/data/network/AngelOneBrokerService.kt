package com.example.data.network

import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AngelOneBrokerService(
    private val api: AngelOneApi,
    private val sessionManager: SessionManager,
    private val instrumentMaster: InstrumentMasterService
) : IBrokerService {
    override val brokerName: String = "Angel One"

    override suspend fun getProfile(): Result<UserProfileEntity> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val res = api.getProfile()
            if (res.isSuccessful && res.body()?.status == true) {
                val data = res.body()?.data ?: throw Exception("Empty profile data from Angel One")
                UserProfileEntity(
                    id = 1,
                    name = data.name?.toString() ?: "",
                    email = data.email?.toString() ?: "",
                    phone = data.mobileNo?.toString() ?: "",
                    connectedBroker = "Angel One",
                    isAngelConnected = true,
                    angelClientId = data.clientCode?.toString() ?: sessionManager.angelClientId ?: ""
                )
            } else {
                val errorMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Failed to get Angel One profile"
                Log.e("AngelOneBrokerService", "Profile API Error (Status ${res.code()}): $errorMsg")
                throw Exception("API Error ${res.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getFunds(): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val res = api.getRMS()
            if (res.isSuccessful && res.body()?.status == true) {
                res.body()?.data?.net?.toString()?.toDoubleOrNull() ?: 0.0
            } else {
                val errorMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Failed to get RMS funds"
                Log.e("AngelOneBrokerService", "RMS API Error (Status ${res.code()}): $errorMsg")
                throw Exception("API Error ${res.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getOrders(): Result<List<OrderEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val res = api.getOrderBook()
            if (res.isSuccessful && res.body()?.status == true) {
                val data = res.body()?.data ?: emptyList()
                data.map { item ->
                    val lot = com.example.util.AppPreferences.getGlobalLotSize(item.tradingSymbol?.toString() ?: "")
                    val qty = item.quantity?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                    val filled = item.filledShares?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                    val price = item.price?.toString()?.toDoubleOrNull() ?: 0.0
                    val avgPrice = item.averagePrice?.toString()?.toDoubleOrNull() ?: price
                    
                    OrderEntity(
                        orderId = item.orderId?.toString() ?: "",
                        symbol = item.tradingSymbol?.toString() ?: "",
                        exchange = item.exchange?.toString() ?: "NSE",
                        lotSize = lot,
                        qty = qty,
                        filledQty = filled,
                        remainingQty = if (qty >= filled) qty - filled else 0,
                        orderType = item.orderType?.toString() ?: "LIMIT",
                        productType = item.productType?.toString() ?: "INTRADAY",
                        side = item.transactionType?.toString() ?: "BUY",
                        price = price,
                        avgPrice = avgPrice,
                        value = price * qty,
                        stopLoss = item.triggerPrice?.toString()?.toDoubleOrNull() ?: 0.0,
                        target = 0.0,
                        status = item.status?.toString()?.uppercase() ?: "PENDING",
                        time = item.orderUpdateTime?.toString() ?: item.updateTime?.toString() ?: "",
                        brokerOrderId = item.orderId?.toString() ?: ""
                    )
                }
            } else {
                val errorMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Failed to get order book"
                Log.e("AngelOneBrokerService", "OrderBook API Error (Status ${res.code()}): $errorMsg")
                throw Exception("API Error ${res.code()}: $errorMsg")
            }
        }
    }

    override suspend fun placeOrder(order: OrderEntity): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val token = if (order.symbolToken.isNotBlank()) order.symbolToken else {
                instrumentMaster.resolveAngelToken(order.symbol, order.exchange) ?: ""
            }
            if (token.isBlank()) {
                throw Exception("Cannot resolve Angel One symbol token for ${order.symbol} on ${order.exchange}")
            }

            val req = AngelPlaceOrderRequest(
                variety = "NORMAL",
                tradingSymbol = order.symbol,
                symbolToken = token,
                transactionType = order.side.uppercase(),
                exchange = InstrumentMasterService.normalizeExchange(order.exchange),
                orderType = order.orderType.uppercase(),
                productType = order.productType.uppercase(),
                duration = "DAY",
                price = order.price.toString(),
                quantity = order.qty.toString()
            )
            val res = api.placeOrder(req)
            if (res.isSuccessful && res.body()?.status == true) {
                res.body()?.data?.orderId ?: throw Exception("No order ID returned by Angel One API")
            } else {
                val errorMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Order placement failed"
                Log.e("AngelOneBrokerService", "PlaceOrder API Error (Status ${res.code()}): $errorMsg")
                throw Exception("API Error ${res.code()}: $errorMsg")
            }
        }
    }

    override suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val req = AngelModifyOrderRequest(
                orderId = orderId,
                variety = "NORMAL",
                orderType = orderType.uppercase(),
                price = newPrice.toString(),
                quantity = newQty.toString()
            )
            val res = api.modifyOrder(req)
            if (res.isSuccessful && res.body()?.status == true) {
                true
            } else {
                val errorMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Modify order failed"
                Log.e("AngelOneBrokerService", "ModifyOrder API Error (Status ${res.code()}): $errorMsg")
                throw Exception("API Error ${res.code()}: $errorMsg")
            }
        }
    }

    override suspend fun cancelOrder(orderId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val req = mapOf("variety" to "NORMAL", "orderId" to orderId)
            val res = api.cancelOrder(req)
            if (res.isSuccessful && res.body()?.status == true) {
                true
            } else {
                val errorMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Cancel order failed"
                Log.e("AngelOneBrokerService", "CancelOrder API Error (Status ${res.code()}): $errorMsg")
                throw Exception("API Error ${res.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getHoldings(): Result<List<PortfolioHoldingEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val res = api.getHoldings()
            if (res.isSuccessful && res.body()?.status == true) {
                val data = res.body()?.data ?: emptyList()
                data.map { item ->
                    val qty = item.quantity?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                    val avgPrice = item.averagePrice?.toString()?.toDoubleOrNull() ?: item.avgPrice?.toString()?.toDoubleOrNull() ?: 0.0
                    val ltp = item.ltp?.toString()?.toDoubleOrNull() ?: 0.0
                    val invested = qty * avgPrice
                    val currentValue = qty * ltp
                    val pnl = currentValue - invested
                    val pnlPct = if (invested > 0) (pnl / invested) * 100 else 0.0

                    PortfolioHoldingEntity(
                        symbol = item.tradingSymbol?.toString() ?: "",
                        exchange = item.exchange?.toString() ?: "NSE",
                        qty = qty,
                        avgPrice = avgPrice,
                        ltp = ltp,
                        currentValue = currentValue,
                        pnl = pnl,
                        pnlPercent = pnlPct,
                        type = "EQUITY"
                    )
                }
            } else {
                val errorMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Failed to get holdings"
                Log.e("AngelOneBrokerService", "Holdings API Error (Status ${res.code()}): $errorMsg")
                throw Exception("API Error ${res.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val res = api.getPositions()
            if (res.isSuccessful && res.body()?.status == true) {
                val data = res.body()?.data ?: emptyList()
                data.map { item ->
                    val netQty = item.netQty?.toString()?.toIntOrNull() ?: 0
                    val buyAvg = item.buyAvgPrice?.toString()?.toDoubleOrNull() ?: 0.0
                    val sellAvg = item.sellAvgPrice?.toString()?.toDoubleOrNull() ?: 0.0
                    val avgPrice = if (netQty >= 0) buyAvg else sellAvg
                    val ltp = item.ltp?.toString()?.toDoubleOrNull() ?: 0.0
                    val pnl = item.pnl?.toString()?.toDoubleOrNull() ?: 0.0
                    val invested = kotlin.math.abs(netQty * avgPrice)
                    val pnlPct = if (invested > 0) (pnl / invested) * 100 else 0.0
                    val isClosed = netQty == 0

                    PortfolioHoldingEntity(
                        symbol = item.tradingSymbol?.toString() ?: "",
                        exchange = item.exchange?.toString() ?: "NSE",
                        qty = netQty,
                        avgPrice = avgPrice,
                        ltp = ltp,
                        currentValue = if (isClosed) 0.0 else (invested + pnl),
                        pnl = pnl,
                        pnlPercent = pnlPct,
                        positionStatus = if (isClosed) "CLOSED" else "OPEN",
                        productType = item.productType?.toString() ?: "INTRADAY"
                    )
                }
            } else {
                val errorMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Failed to get positions"
                Log.e("AngelOneBrokerService", "Positions API Error (Status ${res.code()}): $errorMsg")
                throw Exception("API Error ${res.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val tokenMap = mutableMapOf<String, MutableList<String>>()
            symbols.forEach { sym ->
                val normExchange = if (sym.contains("SENSEX") || sym.contains("BANKEX")) "BSE" else if (sym.contains("CRUDE")) "MCX" else "NSE"
                val token = instrumentMaster.resolveAngelToken(sym, normExchange)
                if (!token.isNullOrBlank()) {
                    tokenMap.getOrPut(normExchange) { mutableListOf() }.add(token)
                }
            }
            if (tokenMap.isEmpty()) return@runCatching emptyList()
            
            val response = api.getQuotes(AngelQuoteRequest(exchangeTokens = tokenMap))
            if (response.isSuccessful && response.body()?.status == true) {
                val quotes = response.body()?.data?.fetched ?: emptyList()
                val unfetched = response.body()?.data?.unfetched ?: emptyList()
                if (unfetched.isNotEmpty()) {
                    Log.w("AngelOneBrokerService", "Unfetched quote items: size=${unfetched.size}")
                }
                quotes.map { q ->
                    val sym = q.tradingSymbol ?: ""
                    val ex = q.exchange ?: "NSE"
                    val ltpVal = q.ltp ?: 0.0
                    val changeVal = q.netChange ?: 0.0
                    val changePctVal = q.percentChange ?: 0.0

                    if (ltpVal > 0.0 && sym.isNotBlank()) {
                        MarketDataStore.updateTick(
                            source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                            symbol = sym,
                            token = q.symbolToken ?: "",
                            exchange = ex,
                            ltp = ltpVal,
                            open = q.open ?: 0.0,
                            high = q.high ?: 0.0,
                            low = q.low ?: 0.0,
                            close = q.close ?: 0.0,
                            volume = q.tradeVolume ?: 0L,
                            receivedTimestamp = System.currentTimeMillis()
                        )
                    }

                    WatchlistItem(
                        symbol = sym,
                        exchange = ex,
                        ltp = ltpVal,
                        change = changeVal,
                        changePercent = changePctVal,
                        lotSize = com.example.util.AppPreferences.getGlobalLotSize(sym),
                        isPositive = changeVal >= 0
                    )
                }
            } else {
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Quote API error"
                Log.e("AngelOneBrokerService", "Quote API Error (Status ${response.code()}): $errorMsg")
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getOptionChain(symbol: String, expiry: String): Result<List<OptionStrikeItem>> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val instrument = instrumentMaster.resolveIndexToken(symbol)
                ?: throw Exception("Index token not found for option chain: $symbol")
            
            val upperSym = symbol.uppercase().trim()
            val exch = when {
                upperSym.contains("CRUDE", true) -> "MCX"
                upperSym.contains("SENSEX", true) || upperSym.contains("BANKEX", true) -> "BFO"
                else -> "NFO"
            }
            val angelExpiry = com.example.util.OptionExpiryUtil.formatForAngel(expiry)

            val request = AngelOptionChainRequest(
                exchange = exch,
                symboltoken = instrument.token,
                expirydate = angelExpiry
            )
            val response = api.getOptionChain(request)
            if (response.isSuccessful && response.body()?.status == true) {
                response.body()?.data?.map { item ->
                    val sp = item.strikePrice?.toDoubleOrNull() ?: 0.0
                    val ceInst = instrumentMaster.resolveOptionInstrument(symbol, expiry, sp, "CE")
                    val peInst = instrumentMaster.resolveOptionInstrument(symbol, expiry, sp, "PE")
                    OptionStrikeItem(
                        strikePrice = sp,
                        callOi = "${item.callOi ?: 0.0}",
                        callChgOi = "${item.callChgOi ?: 0.0}",
                        callLtp = item.callLtp ?: 0.0,
                        putLtp = item.putLtp ?: 0.0,
                        putChgOi = "${item.putChgOi ?: 0.0}",
                        putOi = "${item.putOi ?: 0.0}",
                        callVolume = "${item.callVolume ?: 0.0}",
                        putVolume = "${item.putVolume ?: 0.0}",
                        callSymbol = ceInst?.symbol ?: "",
                        callToken = ceInst?.token ?: "",
                        putSymbol = peInst?.symbol ?: "",
                        putToken = peInst?.token ?: ""
                    )
                } ?: emptyList()
            } else {
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Option chain request failed"
                Log.e("AngelOneBrokerService", "OptionChain API Error (Status ${response.code()}): $errorMsg")
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getOptionExpiries(symbol: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val apiResponse = api.getOptionExpiries(symbol)
            if (apiResponse.isSuccessful && apiResponse.body()?.status == true) {
                apiResponse.body()?.data ?: emptyList()
            } else {
                val errorMsg = apiResponse.body()?.message ?: apiResponse.errorBody()?.string() ?: "Option expiries request failed"
                Log.e("AngelOneBrokerService", "OptionExpiries API Error (Status ${apiResponse.code()}): $errorMsg")
                throw Exception("API Error ${apiResponse.code()}: $errorMsg")
            }
        }
    }

    override suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<com.example.ui.components.CandleData>> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionManager.angelJwtToken.isNullOrEmpty()) throw Exception("Not authenticated with Angel One")
            val ex = when {
                symbol.contains("CRUDE", ignoreCase = true) -> "MCX"
                symbol.contains("SENSEX", ignoreCase = true) || symbol.contains("BANKEX", ignoreCase = true) -> "BSE"
                else -> "NSE"
            }
            val token = instrumentMaster.resolveAngelToken(symbol, ex)
                ?: throw Exception("Cannot resolve Angel One token for historical data of $symbol on $ex")

            val angelInterval = when (interval.lowercase()) {
                "1m" -> "ONE_MINUTE"
                "3m" -> "THREE_MINUTE"
                "5m" -> "FIVE_MINUTE"
                "15m" -> "FIFTEEN_MINUTE"
                "30m" -> "THIRTY_MINUTE"
                "1h" -> "ONE_HOUR"
                "1d" -> "ONE_DAY"
                else -> "FIFTEEN_MINUTE"
            }

            val request = AngelHistoricalRequest(
                exchange = ex,
                symboltoken = token,
                interval = angelInterval,
                fromdate = fromDate,
                todate = toDate
            )
            val response = api.getHistoricalData(request)
            if (response.isSuccessful && response.body()?.status == true) {
                val data = response.body()?.data ?: throw Exception("Empty candle data from Angel One Historical API")
                val candles = data.mapNotNull { row ->
                    try {
                        if (row.size >= 6) {
                            com.example.ui.components.CandleData(
                                open = row[1].toString().toFloat(),
                                high = row[2].toString().toFloat(),
                                low = row[3].toString().toFloat(),
                                close = row[4].toString().toFloat(),
                                volume = row[5].toString().toFloat()
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                if (candles.isEmpty()) {
                    throw Exception("No valid candle records returned for $symbol ($interval)")
                }
                candles
            } else {
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Historical data request failed"
                Log.e("AngelOneBrokerService", "Historical API Error (Status ${response.code()}): $errorMsg for $symbol, token=$token")
                throw Exception("API Error ${response.code()}: $errorMsg")
            }
        }
    }
}

