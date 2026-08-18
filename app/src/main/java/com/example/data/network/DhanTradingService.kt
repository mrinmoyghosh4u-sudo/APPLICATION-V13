package com.example.data.network

import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity

class DhanTradingService(
    private val dhanService: DhanBrokerService,
    private val sessionManager: SessionManager
) {
    fun isConnected(): Boolean {
        return !sessionManager.dhanAccessToken.isNullOrEmpty()
    }

    suspend fun getProfile(): Result<UserProfileEntity> {
        if (!isConnected()) {
            return Result.failure(Exception("Dhan account is not connected."))
        }
        return dhanService.getProfile()
    }

    suspend fun getFunds(): Result<Double> {
        if (!isConnected()) {
            return Result.failure(Exception("Dhan account is not connected."))
        }
        return dhanService.getFunds()
    }

    suspend fun getOrders(): Result<List<OrderEntity>> {
        if (!isConnected()) {
            return Result.failure(Exception("Dhan account is not connected."))
        }
        return dhanService.getOrders()
    }

    suspend fun placeOrder(order: OrderEntity): Result<String> {
        if (!isConnected()) {
            return Result.failure(Exception("Dhan account is not connected."))
        }

        // Validate lot size
        val lotSize = com.example.util.AppPreferences.getGlobalLotSize(order.symbol)
        if (order.qty <= 0) {
            return Result.failure(Exception("Invalid quantity. Quantity must be greater than 0."))
        }
        if (lotSize > 1 && order.qty % lotSize != 0) {
            return Result.failure(Exception("Invalid quantity. Quantity must be a valid multiple of current lot size ($lotSize)."))
        }

        return dhanService.placeOrder(order)
    }

    suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String): Result<Boolean> {
        if (!isConnected()) {
            return Result.failure(Exception("Dhan account is not connected."))
        }
        return dhanService.modifyOrder(orderId, newPrice, newQty, orderType)
    }

    suspend fun cancelOrder(orderId: String): Result<Boolean> {
        if (!isConnected()) {
            return Result.failure(Exception("Dhan account is not connected."))
        }
        return dhanService.cancelOrder(orderId)
    }

    suspend fun getHoldings(): Result<List<PortfolioHoldingEntity>> {
        if (!isConnected()) {
            return Result.failure(Exception("Dhan account is not connected."))
        }
        return dhanService.getHoldings()
    }

    suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> {
        if (!isConnected()) {
            return Result.failure(Exception("Dhan account is not connected."))
        }
        return dhanService.getPositions()
    }
}
