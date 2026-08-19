package com.example.data.network

import retrofit2.Response
import retrofit2.http.*

interface AngelOneApi {

    @POST("rest/auth/angelbroking/user/v1/loginByPassword")
    suspend fun login(
        @Body request: AngelLoginRequest
    ): Response<AngelOneResponse<AngelLoginResponseData>>

    @GET("rest/secure/angelbroking/user/v1/getProfile")
    suspend fun getProfile(): Response<AngelOneResponse<AngelProfileData>>

    @GET("rest/secure/angelbroking/user/v1/getRMS")
    suspend fun getRMS(): Response<AngelOneResponse<AngelRmsData>>

    @GET("rest/secure/angelbroking/order/v1/getOrderBook")
    suspend fun getOrderBook(): Response<AngelOneResponse<List<AngelOrderBookItem>>>

    @POST("rest/secure/angelbroking/order/v1/placeOrder")
    suspend fun placeOrder(
        @Body request: AngelPlaceOrderRequest
    ): Response<AngelOneResponse<AngelPlaceOrderResponseData>>

    @POST("rest/secure/angelbroking/order/v1/modifyOrder")
    suspend fun modifyOrder(
        @Body request: AngelModifyOrderRequest
    ): Response<AngelOneResponse<AngelPlaceOrderResponseData>>

    @POST("rest/secure/angelbroking/order/v1/cancelOrder")
    suspend fun cancelOrder(
        @Body request: Map<String, String>
    ): Response<AngelOneResponse<AngelPlaceOrderResponseData>>

    @GET("rest/secure/angelbroking/portfolio/v1/getHolding")
    suspend fun getHoldings(): Response<AngelOneResponse<List<AngelHoldingItem>>>

    @GET("rest/secure/angelbroking/order/v1/getPosition")
    suspend fun getPositions(): Response<AngelOneResponse<List<AngelPositionItem>>>

    @POST("rest/secure/angelbroking/market/v1/quote/")
    suspend fun getQuotes(
        @Body request: AngelQuoteRequest
    ): Response<AngelOneResponse<AngelQuoteResponseData>>

    @GET("rest/secure/angelbroking/market/v1/optionchain/expirylist")
    suspend fun getOptionExpiries(
        @Query("symbol") symbol: String
    ): Response<AngelOneResponse<List<String>>>
    @POST("rest/secure/angelbroking/market/v1/optionchain")
    suspend fun getOptionChain(
        @Body request: AngelOptionChainRequest
    ): Response<AngelOptionChainResponse>
}
