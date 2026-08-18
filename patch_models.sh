cat << 'INNER_EOF' > app/src/main/java/com/example/data/network/BrokerApiModels.kt
package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==========================================
// ANGEL ONE API DTOs
// ==========================================
@JsonClass(generateAdapter = true)
data class AngelOneResponse<T>(
    @Json(name = "status") val status: Boolean? = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "errorcode") val errorCode: String? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class AngelLoginRequest(
    @Json(name = "clientcode") val clientCode: String,
    @Json(name = "password") val password: String,
    @Json(name = "totp") val totp: String? = null
)

@JsonClass(generateAdapter = true)
data class AngelLoginResponseData(
    @Json(name = "jwtToken") val jwtToken: String? = null,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "feedToken") val feedToken: String? = null
)

@JsonClass(generateAdapter = true)
data class AngelProfileData(
    @Json(name = "clientcode") val clientCode: Any? = "",
    @Json(name = "name") val name: Any? = "",
    @Json(name = "email") val email: Any? = "",
    @Json(name = "mobileno") val mobileNo: Any? = "",
    @Json(name = "exchanges") val exchanges: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class AngelRmsData(
    @Json(name = "net") val net: Any? = "0",
    @Json(name = "availablemargin") val availableMargin: Any? = "0",
    @Json(name = "collateral") val collateral: Any? = "0"
)

@JsonClass(generateAdapter = true)
data class AngelOrderBookItem(
    @Json(name = "orderid") val orderId: Any? = "",
    @Json(name = "tradingsymbol") val tradingSymbol: Any? = "",
    @Json(name = "exchange") val exchange: Any? = "",
    @Json(name = "transactiontype") val transactionType: Any? = "",
    @Json(name = "ordertype") val orderType: Any? = "",
    @Json(name = "quantity") val quantity: Any? = "0",
    @Json(name = "price") val price: Any? = "0",
    @Json(name = "status") val status: Any? = "",
    @Json(name = "orderupdatetime") val orderUpdateTime: Any? = ""
)

@JsonClass(generateAdapter = true)
data class AngelPlaceOrderRequest(
    @Json(name = "variety") val variety: String = "NORMAL",
    @Json(name = "tradingsymbol") val tradingSymbol: String,
    @Json(name = "symboltoken") val symbolToken: String = "3045",
    @Json(name = "transactiontype") val transactionType: String, // BUY / SELL
    @Json(name = "exchange") val exchange: String, // NSE / BSE / MCX
    @Json(name = "ordertype") val orderType: String, // LIMIT / MARKET
    @Json(name = "producttype") val productType: String = "INTRADAY",
    @Json(name = "duration") val duration: String = "DAY",
    @Json(name = "price") val price: String,
    @Json(name = "quantity") val quantity: String
)

@JsonClass(generateAdapter = true)
data class AngelPlaceOrderResponseData(
    @Json(name = "orderid") val orderId: String? = null
)

@JsonClass(generateAdapter = true)
data class AngelHoldingItem(
    @Json(name = "tradingsymbol") val tradingSymbol: Any? = "",
    @Json(name = "exchange") val exchange: Any? = "",
    @Json(name = "quantity") val quantity: Any? = 0,
    @Json(name = "averageprice") val averagePrice: Any? = 0.0,
    @Json(name = "avgprice") val avgPrice: Any? = 0.0,
    @Json(name = "ltp") val ltp: Any? = 0.0,
    @Json(name = "pnl") val pnl: Any? = 0.0,
    @Json(name = "realisedpnl") val realisedPnL: Any? = 0.0,
    @Json(name = "unrealisedpnl") val unrealisedPnL: Any? = 0.0,
    @Json(name = "pnlpercentage") val pnlPercentage: Any? = 0.0
)

@JsonClass(generateAdapter = true)
data class AngelPositionItem(
    @Json(name = "tradingsymbol") val tradingSymbol: Any? = "",
    @Json(name = "exchange") val exchange: Any? = "",
    @Json(name = "symboltoken") val symbolToken: Any? = "",
    @Json(name = "producttype") val productType: Any? = "",
    @Json(name = "netqty") val netQty: Any? = "0",
    @Json(name = "buyavgprice") val buyAvgPrice: Any? = "0",
    @Json(name = "sellavgprice") val sellAvgPrice: Any? = "0",
    @Json(name = "pnl") val pnl: Any? = "0",
    @Json(name = "realisedpnl") val realisedPnL: Any? = "0",
    @Json(name = "unrealisedpnl") val unrealisedPnL: Any? = "0",
    @Json(name = "ltp") val ltp: Any? = "0"
)

@JsonClass(generateAdapter = true)
data class AngelQuoteRequest(
    @Json(name = "mode") val mode: String = "FULL",
    @Json(name = "exchangeTokens") val exchangeTokens: Map<String, List<String>>
)

@JsonClass(generateAdapter = true)
data class AngelQuoteItem(
    @Json(name = "tradingSymbol") val tradingSymbol: Any? = "",
    @Json(name = "exchange") val exchange: Any? = "",
    @Json(name = "ltp") val ltp: Any? = 0.0,
    @Json(name = "netChange") val netChange: Any? = 0.0,
    @Json(name = "percentChange") val percentChange: Any? = 0.0
)


// ==========================================
// DHAN API v2 DTOs
// ==========================================
@JsonClass(generateAdapter = true)
data class DhanFundLimitResponse(
    @Json(name = "dhanClientId") val dhanClientId: String? = null,
    @Json(name = "availabelBalance") val availableBalance: Double? = null,
    @Json(name = "availableBalance") val altAvailableBalance: Double? = null,
    @Json(name = "sodLimit") val sodLimit: Double? = null,
    @Json(name = "collateralAmount") val collateralAmount: Double? = null,
    @Json(name = "utilizedAmount") val utilizedAmount: Double? = null
)

@JsonClass(generateAdapter = true)
data class DhanOrderBookItem(
    @Json(name = "orderId") val orderId: String = "",
    @Json(name = "tradingSymbol") val tradingSymbol: String = "",
    @Json(name = "exchangeSegment") val exchangeSegment: String = "",
    @Json(name = "transactionType") val transactionType: String = "",
    @Json(name = "orderType") val orderType: String = "",
    @Json(name = "quantity") val quantity: Int = 0,
    @Json(name = "tradedQty") val tradedQty: Int = 0,
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "triggerPrice") val triggerPrice: Double = 0.0,
    @Json(name = "averagePrice") val averagePrice: Double = 0.0,
    @Json(name = "orderStatus") val orderStatus: String = "",
    @Json(name = "createTime") val createTime: String? = null,
    @Json(name = "updateTime") val updateTime: String? = null
)

@JsonClass(generateAdapter = true)
data class DhanPlaceOrderRequest(
    @Json(name = "dhanClientId") val dhanClientId: String,
    @Json(name = "correlationId") val correlationId: String = "KINGKHAN_001",
    @Json(name = "transactionType") val transactionType: String, // BUY / SELL
    @Json(name = "exchangeSegment") val exchangeSegment: String, // NSE_EQ, NSE_FNO, MCX_COMM
    @Json(name = "productType") val productType: String = "INTRADAY",
    @Json(name = "orderType") val orderType: String, // LIMIT / MARKET
    @Json(name = "validity") val validity: String = "DAY",
    @Json(name = "tradingSymbol") val tradingSymbol: String,
    @Json(name = "securityId") val securityId: String = "1333",
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "price") val price: Double
)

@JsonClass(generateAdapter = true)
data class DhanOrderResponse(
    @Json(name = "orderId") val orderId: String? = null,
    @Json(name = "orderStatus") val orderStatus: String? = null,
    @Json(name = "remarks") val remarks: String? = null
)

@JsonClass(generateAdapter = true)
data class DhanHoldingItem(
    @Json(name = "tradingSymbol") val tradingSymbol: String = "",
    @Json(name = "exchange") val exchange: String = "",
    @Json(name = "isin") val isin: String = "",
    @Json(name = "totalQty") val totalQty: Int = 0,
    @Json(name = "dpQty") val dpQty: Int = 0,
    @Json(name = "t1Qty") val t1Qty: Int = 0,
    @Json(name = "availableQty") val availableQty: Int = 0,
    @Json(name = "collateralQty") val collateralQty: Int = 0,
    @Json(name = "avgCostPrice") val avgCostPrice: Double = 0.0,
    @Json(name = "lastTradedPrice") val lastTradedPrice: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DhanPositionItem(
    @Json(name = "dhanClientId") val dhanClientId: String = "",
    @Json(name = "tradingSymbol") val tradingSymbol: String = "",
    @Json(name = "securityId") val securityId: String = "",
    @Json(name = "exchangeSegment") val exchangeSegment: String = "",
    @Json(name = "productType") val productType: String = "",
    @Json(name = "orderType") val orderType: String = "",
    @Json(name = "positionType") val positionType: String = "",
    @Json(name = "netQty") val netQty: Int = 0,
    @Json(name = "buyAvg") val buyAvg: Double = 0.0,
    @Json(name = "buyQty") val buyQty: Int = 0,
    @Json(name = "sellAvg") val sellAvg: Double = 0.0,
    @Json(name = "sellQty") val sellQty: Int = 0,
    @Json(name = "realizedProfit") val realizedProfit: Double = 0.0,
    @Json(name = "unrealizedProfit") val unrealizedProfit: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DhanOptionChainRequest(
    @Json(name = "underlyingScrip") val underlyingScrip: Int = 13,
    @Json(name = "underlyingSeg") val underlyingSeg: String = "IDX_I",
    @Json(name = "expiry") val expiry: String = ""
)

@JsonClass(generateAdapter = true)
data class DhanOptionChainItem(
    @Json(name = "strikePrice") val strikePrice: Double,
    @Json(name = "callOi") val callOi: Double = 0.0,
    @Json(name = "callLtp") val callLtp: Double = 0.0,
    @Json(name = "putOi") val putOi: Double = 0.0,
    @Json(name = "putLtp") val putLtp: Double = 0.0
)
INNER_EOF
