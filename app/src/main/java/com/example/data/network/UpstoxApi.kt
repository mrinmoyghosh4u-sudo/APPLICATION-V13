package com.example.data.network

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UpstoxApi {

    @FormUrlEncoded
    @POST("v2/login/authorization/token")
    suspend fun getAccessToken(
        @Header("Api-Version") apiVersion: String = "2.0",
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String = "authorization_code"
    ): Response<UpstoxTokenResponse>

    @GET
    suspend fun exchangeTokenSecurely(
        @retrofit2.http.Url url: String,
        @Query("code") code: String,
        @Query("redirect_uri") redirectUri: String,
        @Query("client_id") clientId: String? = null,
        @Query("client_secret") clientSecret: String? = null
    ): Response<UpstoxTokenResponse>

    @GET("v2/user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String,
        @Header("Api-Version") apiVersion: String = "2.0"
    ): Response<UpstoxProfileResponse>

    @GET("v3/market-quote/quotes")
    suspend fun getMarketQuotes(
        @Header("Authorization") token: String,
        @Query("instrument_key") instrumentKeys: String,
        @Header("Api-Version") apiVersion: String = "3.0"
    ): Response<UpstoxMarketQuotesResponse>

    @GET("v3/market-quote/ohlc")
    suspend fun getOHLCQuotes(
        @Header("Authorization") token: String,
        @Query("instrument_key") instrumentKeys: String,
        @Query("interval") interval: String = "1d",
        @Header("Api-Version") apiVersion: String = "3.0"
    ): Response<UpstoxMarketQuotesResponse>

    @GET("v3/market-quote/ltp")
    suspend fun getLTPQuotes(
        @Header("Authorization") token: String,
        @Query("instrument_key") instrumentKeys: String,
        @Header("Api-Version") apiVersion: String = "3.0"
    ): Response<UpstoxMarketQuotesResponse>

    @GET("v3/option/chain")
    suspend fun getOptionChain(
        @Header("Authorization") token: String,
        @Query("instrument_key") instrumentKey: String,
        @Query("expiry_date") expiryDate: String,
        @Header("Api-Version") apiVersion: String = "3.0"
    ): Response<UpstoxOptionChainResponse>

    @GET("v3/option/contract")
    suspend fun getOptionContracts(
        @Header("Authorization") token: String,
        @Query("instrument_key") instrumentKey: String,
        @Header("Api-Version") apiVersion: String = "3.0"
    ): Response<UpstoxOptionContractsResponse>

    @GET("v3/historical-candle/{instrument_key}/{unit}/{interval}/{to_date}/{from_date}")
    suspend fun getHistoricalCandles(
        @Header("Authorization") token: String,
        @Path("instrument_key") instrumentKey: String,
        @Path("unit") unit: String,
        @Path("interval") interval: String,
        @Path("to_date") toDate: String,
        @Path("from_date") fromDate: String,
        @Header("Api-Version") apiVersion: String = "3.0"
    ): Response<UpstoxHistoricalCandlesResponse>

    @GET("v3/historical-candle/intraday/{instrument_key}/{interval}")
    suspend fun getIntradayCandles(
        @Header("Authorization") token: String,
        @Path("instrument_key") instrumentKey: String,
        @Path("interval") interval: String,
        @Header("Api-Version") apiVersion: String = "3.0"
    ): Response<UpstoxHistoricalCandlesResponse>

    @GET("v3/feed/market-data-feed/authorize")
    suspend fun getWebSocketFeedAuth(
        @Header("Authorization") token: String
    ): Response<UpstoxFeedAuthResponse>
}
