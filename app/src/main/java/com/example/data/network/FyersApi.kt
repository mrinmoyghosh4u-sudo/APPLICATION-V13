package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class FyersRefreshTokenRequest(
    @Json(name = "grant_type") val grant_type: String = "refresh_token",
    @Json(name = "appIdHash") val appIdHash: String,
    @Json(name = "refresh_token") val refresh_token: String,
    @Json(name = "pin") val pin: String
)

@JsonClass(generateAdapter = true)
data class FyersTokenRequest(
    @Json(name = "grant_type") val grant_type: String = "authorization_code",
    @Json(name = "appIdHash") val appIdHash: String,
    @Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class FyersTokenResponse(
    @Json(name = "s") val s: String? = null,
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "access_token") val access_token: String? = null,
    @Json(name = "refresh_token") val refresh_token: String? = null
)

@JsonClass(generateAdapter = true)
data class FyersHistoryResponse(
    @Json(name = "s") val s: String? = null,
    @Json(name = "candles") val candles: List<List<Double>>? = null
)

@JsonClass(generateAdapter = true)
data class FyersQuotesResponse(
    @Json(name = "s") val s: String? = null,
    @Json(name = "d") val d: List<FyersQuoteData>? = null
)

@JsonClass(generateAdapter = true)
data class FyersQuoteData(
    @Json(name = "n") val n: String? = null,
    @Json(name = "v") val v: FyersQuoteValues? = null
)

@JsonClass(generateAdapter = true)
data class FyersQuoteValues(
    @Json(name = "ch") val ch: Double? = null,
    @Json(name = "chp") val chp: Double? = null,
    @Json(name = "lp") val lp: Double? = null,
    @Json(name = "spread") val spread: Double? = null,
    @Json(name = "ask") val ask: Double? = null,
    @Json(name = "bid") val bid: Double? = null,
    @Json(name = "open_price") val open_price: Double? = null,
    @Json(name = "high_price") val high_price: Double? = null,
    @Json(name = "low_price") val low_price: Double? = null,
    @Json(name = "prev_close_price") val prev_close_price: Double? = null,
    @Json(name = "volume") val volume: Long? = null,
    @Json(name = "short_name") val short_name: String? = null,
    @Json(name = "exchange") val exchange: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "original_name") val original_name: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "fyToken") val fyToken: String? = null,
    @Json(name = "tt") val tt: Long? = null
)

@JsonClass(generateAdapter = true)
data class FyersOptionChainResponse(
    @Json(name = "s") val s: String? = null,
    @Json(name = "data") val data: FyersOptionChainData? = null
)

@JsonClass(generateAdapter = true)
data class FyersOptionChainData(
    @Json(name = "expiryData") val expiryData: List<FyersExpiryData>? = null
)

@JsonClass(generateAdapter = true)
data class FyersExpiryData(
    @Json(name = "expiry") val expiry: String? = null,
    @Json(name = "optionChain") val optionChain: List<FyersOptionContract>? = null
)

@JsonClass(generateAdapter = true)
data class FyersOptionContract(
    @Json(name = "strike_price") val strike_price: Double? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "ltp") val ltp: Double? = null,
    @Json(name = "oi") val oi: Double? = null,
    @Json(name = "volume") val volume: Double? = null,
    @Json(name = "option_type") val option_type: String? = null,
    @Json(name = "ch") val ch: Double? = null,
    @Json(name = "chp") val chp: Double? = null,
    @Json(name = "bid") val bid: Double? = null,
    @Json(name = "ask") val ask: Double? = null
)

@JsonClass(generateAdapter = true)
data class FyersProfileResponse(
    @Json(name = "s") val s: String? = null,
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: FyersProfileData? = null
)

@JsonClass(generateAdapter = true)
data class FyersProfileData(
    @Json(name = "name") val name: String? = null,
    @Json(name = "fy_id") val fy_id: String? = null,
    @Json(name = "email_id") val email_id: String? = null
)

interface FyersApi {
    @GET("api/v3/profile")
    suspend fun getProfile(
        @Header("Authorization") auth: String
    ): Response<FyersProfileResponse>

    @GET("data/options-chain-v3")
    suspend fun getOptionChain(
        @Header("Authorization") auth: String,
        @Query("symbol") symbol: String,
        @Query("strikecount") strikecount: Int = 10,
        @Query("timestamp") timestamp: String = ""
    ): Response<FyersOptionChainResponse>

    
    @POST("api/v3/validate-refresh-token")
    suspend fun validateRefreshToken(
        @Body request: FyersRefreshTokenRequest
    ): Response<FyersTokenResponse>

    @POST("api/v3/validate-authcode")
    suspend fun validateAuthCode(
        @Body request: FyersTokenRequest
    ): Response<FyersTokenResponse>

    @GET
    suspend fun exchangeTokenSecurely(
        @retrofit2.http.Url url: String,
        @Query("code") code: String,
        @Query("redirect_uri") redirectUri: String
    ): Response<FyersTokenResponse>

    @GET("data/history")
    suspend fun getHistory(
        @Header("Authorization") auth: String,
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("date_format") dateFormat: Int = 1,
        @Query("range_from") from: String,
        @Query("range_to") to: String
    ): Response<FyersHistoryResponse>

    @GET("data/quotes")
    suspend fun getQuotes(
        @Header("Authorization") auth: String,
        @Query("symbols") symbols: String
    ): Response<FyersQuotesResponse>
}
