package com.example.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class FyersRefreshTokenRequest(
    val grant_type: String = "refresh_token",
    val appIdHash: String,
    val refresh_token: String,
    val pin: String
)

data class FyersTokenRequest(
    val grant_type: String = "authorization_code",
    val appIdHash: String,
    val code: String
)

data class FyersTokenResponse(
    val s: String?,
    val code: Int?,
    val message: String?,
    val access_token: String?,
    val refresh_token: String?
)

data class FyersHistoryResponse(
    val s: String?,
    val candles: List<List<Double>>?
)

data class FyersQuotesResponse(
    val s: String?,
    val d: List<FyersQuoteData>?
)

data class FyersQuoteData(
    val n: String?,
    val v: FyersQuoteValues?
)

data class FyersQuoteValues(
    val ch: Double?,
    val chp: Double?,
    val lp: Double?,
    val spread: Double?,
    val ask: Double?,
    val bid: Double?,
    val open_price: Double?,
    val high_price: Double?,
    val low_price: Double?,
    val prev_close_price: Double?,
    val volume: Long?,
    val short_name: String?,
    val exchange: String?,
    val description: String?,
    val original_name: String?,
    val symbol: String?,
    val fyToken: String?,
    val tt: Long?
)

interface FyersApi {
    
    @POST("api/v3/validate-refresh-token")
    suspend fun validateRefreshToken(
        @Body request: FyersRefreshTokenRequest
    ): Response<FyersTokenResponse>

    @POST("api/v3/validate-authcode")
    suspend fun validateAuthCode(
        @Body request: FyersTokenRequest
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
