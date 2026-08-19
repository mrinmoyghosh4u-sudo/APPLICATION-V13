package com.example.data.network

import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem

interface IBrokerService {
    val brokerName: String

    suspend fun getProfile(): Result<UserProfileEntity>
    suspend fun getFunds(): Result<Double>
    suspend fun getOrders(): Result<List<OrderEntity>>
    suspend fun placeOrder(order: OrderEntity): Result<String>
    suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String): Result<Boolean>
    suspend fun cancelOrder(orderId: String): Result<Boolean>
    suspend fun getHoldings(): Result<List<PortfolioHoldingEntity>>
    suspend fun getPositions(): Result<List<PortfolioHoldingEntity>>
    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>>
    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>>
    suspend fun getOptionExpiries(symbol: String): Result<List<String>>
    suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<com.example.ui.components.CandleData>>
}
