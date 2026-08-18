package com.example.data.network

import retrofit2.Response
import retrofit2.http.*

interface DhanApi {

    @GET("v2/fundlimit")
    suspend fun getFundLimit(): Response<DhanFundLimitResponse>

    @GET("v2/orders")
    suspend fun getOrders(): Response<List<DhanOrderBookItem>>

    @POST("v2/orders")
    suspend fun placeOrder(
        @Body request: DhanPlaceOrderRequest
    ): Response<DhanOrderResponse>

    @PUT("v2/orders/{order-id}")
    suspend fun modifyOrder(
        @Path("order-id") orderId: String,
        @Body request: DhanModifyOrderRequest
    ): Response<DhanOrderResponse>

    @DELETE("v2/orders/{order-id}")
    suspend fun cancelOrder(
        @Path("order-id") orderId: String
    ): Response<DhanOrderResponse>

    @GET("v2/holdings")
    suspend fun getHoldings(): Response<List<DhanHoldingItem>>

    @GET("v2/positions")
    suspend fun getPositions(): Response<List<DhanPositionItem>>

    @GET("v2/optionchain/expirylist")
    suspend fun getOptionExpiries(
        @Query("underlying") underlying: String
    ): Response<List<String>>

    @POST("v2/optionchain")
    suspend fun getOptionChain(
        @Body request: DhanOptionChainRequest
    ): Response<List<DhanOptionChainItem>>

    @GET("v2/trades")
    suspend fun getTrades(): Response<List<DhanTradeItem>>
}
