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


data class FyersOptionChainResponse(
    val s: String?,
    val data: FyersOptionChainData?
)

data class FyersOptionChainData(
    val expiryData: List<FyersExpiryData>?
)

data class FyersExpiryData(
    val expiry: String?,
    val optionChain: List<FyersOptionContract>?
)

data class FyersOptionContract(
    val strike_price: Double?,
    val symbol: String?,
    val ltp: Double?,
    val oi: Double?,
    val volume: Double?,
    val option_type: String?,
    val ch: Double?,
    val chp: Double?,
    val bid: Double?,
    val ask: Double?
)

data class FyersProfileResponse(
    val s: String?,
    val code: Int?,
    val message: String?,
    val data: FyersProfileData?
)

data class FyersProfileData(
    val name: String?,
    val fy_id: String?,
    val email_id: String?
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
