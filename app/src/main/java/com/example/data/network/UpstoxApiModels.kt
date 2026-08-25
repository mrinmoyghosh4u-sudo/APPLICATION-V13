package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==========================================
// UPSTOX API DTOs (Official V2 & V3 APIs)
// ==========================================

@JsonClass(generateAdapter = true)
data class UpstoxTokenResponse(
    @Json(name = "email") val email: String? = null,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "extended_token") val extendedToken: String? = null,
    @Json(name = "is_active") val isActive: Boolean? = null,
    @Json(name = "exchanges") val exchanges: List<String>? = null,
    @Json(name = "products") val products: List<String>? = null,
    @Json(name = "order_types") val orderTypes: List<String>? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "errors") val errors: List<UpstoxApiError>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxTokenData(
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "extended_token") val extendedToken: String? = null,
    @Json(name = "is_active") val isActive: Boolean? = null,
    @Json(name = "exchanges") val exchanges: List<String>? = null,
    @Json(name = "products") val products: List<String>? = null,
    @Json(name = "order_types") val orderTypes: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxProfileResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: UpstoxProfileData? = null,
    @Json(name = "errors") val errors: List<UpstoxApiError>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxProfileData(
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "broker") val broker: String? = null,
    @Json(name = "exchanges") val exchanges: List<String>? = null,
    @Json(name = "products") val products: List<String>? = null,
    @Json(name = "order_types") val orderTypes: List<String>? = null,
    @Json(name = "user_type") val userType: String? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxMarketQuotesResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: Map<String, UpstoxMarketQuoteItem>? = null,
    @Json(name = "errors") val errors: List<UpstoxApiError>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxMarketQuoteItem(
    @Json(name = "ohlc") val ohlc: UpstoxOHLC? = null,
    @Json(name = "depth") val depth: UpstoxDepth? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "instrument_token") val instrumentToken: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "last_price") val lastPrice: Double? = null,
    @Json(name = "volume") val volume: Long? = null,
    @Json(name = "average_price") val averagePrice: Double? = null,
    @Json(name = "oi") val oi: Double? = null,
    @Json(name = "net_change") val netChange: Double? = null,
    @Json(name = "total_buy_quantity") val totalBuyQuantity: Long? = null,
    @Json(name = "total_sell_quantity") val totalSellQuantity: Long? = null,
    @Json(name = "lower_circuit_limit") val lowerCircuitLimit: Double? = null,
    @Json(name = "upper_circuit_limit") val upperCircuitLimit: Double? = null,
    @Json(name = "last_trade_time") val lastTradeTime: String? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxOHLC(
    @Json(name = "open") val open: Double? = null,
    @Json(name = "high") val high: Double? = null,
    @Json(name = "low") val low: Double? = null,
    @Json(name = "close") val close: Double? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxDepth(
    @Json(name = "buy") val buy: List<UpstoxDepthItem>? = null,
    @Json(name = "sell") val sell: List<UpstoxDepthItem>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxDepthItem(
    @Json(name = "quantity") val quantity: Long? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "orders") val orders: Int? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxOptionChainResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: List<UpstoxOptionChainData>? = null,
    @Json(name = "errors") val errors: List<UpstoxApiError>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxOptionChainData(
    @Json(name = "expiry") val expiry: String? = null,
    @Json(name = "pcr") val pcr: Double? = null,
    @Json(name = "strike_price") val strikePrice: Double? = null,
    @Json(name = "underlying_key") val underlyingKey: String? = null,
    @Json(name = "underlying_spot_price") val underlyingSpotPrice: Double? = null,
    @Json(name = "call_options") val callOptions: UpstoxOptionDetail? = null,
    @Json(name = "put_options") val putOptions: UpstoxOptionDetail? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxOptionDetail(
    @Json(name = "instrument_key") val instrumentKey: String? = null,
    @Json(name = "market_data") val marketData: UpstoxOptionMarketData? = null,
    @Json(name = "option_greeks") val optionGreeks: UpstoxOptionGreeks? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxOptionMarketData(
    @Json(name = "ltp") val ltp: Double? = null,
    @Json(name = "volume") val volume: Long? = null,
    @Json(name = "oi") val oi: Double? = null,
    @Json(name = "close_price") val closePrice: Double? = null,
    @Json(name = "bid_price") val bidPrice: Double? = null,
    @Json(name = "bid_qty") val bidQty: Long? = null,
    @Json(name = "ask_price") val askPrice: Double? = null,
    @Json(name = "ask_qty") val askQty: Long? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxOptionGreeks(
    @Json(name = "iv") val iv: Double? = null,
    @Json(name = "delta") val delta: Double? = null,
    @Json(name = "theta") val theta: Double? = null,
    @Json(name = "gamma") val gamma: Double? = null,
    @Json(name = "vega") val vega: Double? = null,
    @Json(name = "pop") val pop: Double? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxOptionContractsResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: List<UpstoxOptionContractItem>? = null,
    @Json(name = "errors") val errors: List<UpstoxApiError>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxOptionContractItem(
    @Json(name = "name") val name: String? = null,
    @Json(name = "segment") val segment: String? = null,
    @Json(name = "exchange") val exchange: String? = null,
    @Json(name = "expiry") val expiry: String? = null,
    @Json(name = "strike_price") val strikePrice: Double? = null,
    @Json(name = "instrument_type") val instrumentType: String? = null,
    @Json(name = "instrument_key") val instrumentKey: String? = null,
    @Json(name = "lot_size") val lotSize: Int? = null,
    @Json(name = "underlying_key") val underlyingKey: String? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxHistoricalCandlesResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: UpstoxCandlesData? = null,
    @Json(name = "errors") val errors: List<UpstoxApiError>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxCandlesData(
    @Json(name = "candles") val candles: List<List<Any>>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxFeedAuthResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "data") val data: UpstoxFeedAuthData? = null,
    @Json(name = "errors") val errors: List<UpstoxApiError>? = null
)

@JsonClass(generateAdapter = true)
data class UpstoxFeedAuthData(
    @Json(name = "authorizedRedirectUri") val authorizedRedirectUri: String? = null,
    @Json(name = "authorized_redirect_uri") val authorized_redirect_uri: String? = null
) {
    val redirectUri: String?
        get() = authorizedRedirectUri ?: authorized_redirect_uri
}

@JsonClass(generateAdapter = true)
data class UpstoxApiError(
    @Json(name = "errorCode") val errorCode: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "propertyPath") val propertyPath: String? = null,
    @Json(name = "invalidValue") val invalidValue: Any? = null
)
