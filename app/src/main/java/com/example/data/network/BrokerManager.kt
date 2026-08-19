package com.example.data.network

import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem

class BrokerManager(
    private val sessionManager: SessionManager,
    val angelOneService: AngelOneBrokerService,
    val dhanService: DhanBrokerService,
    val instrumentMasterService: InstrumentMasterService
) {
    val angelMarketDataService = AngelOneMarketDataService(angelOneService, sessionManager, instrumentMasterService)
    val dhanTradingService = DhanTradingService(dhanService, sessionManager)
    val mStockMarketDataService = MStockMarketDataService(sessionManager)
    val tradeSmartMarketDataService = TradeSmartMarketDataService(sessionManager)

    val marketDataManager = MarketDataManager(
        angelMarketDataService = angelMarketDataService,
        mStockMarketDataService = mStockMarketDataService,
        sessionManager = sessionManager
    )

    val currentMarketDataSource: String
        get() = marketDataManager.activeProvider.value

    val activeService: IBrokerService
        get() = if (sessionManager.activeBroker == "Dhan") dhanService else angelOneService

    fun setActiveBroker(broker: String) {
        sessionManager.activeBroker = broker
    }

    suspend fun getProfile(): Result<UserProfileEntity> {
        return if (sessionManager.activeBroker == "Dhan") {
            dhanTradingService.getProfile()
        } else if (sessionManager.activeBroker == "Angel One") {
            angelOneService.getProfile()
        } else if (sessionManager.activeBroker == "m.Stock") {
            Result.success(com.example.data.model.UserProfileEntity(
                name = "", // Blank so repository keeps existing name
                connectedBroker = "m.Stock",
                isAngelConnected = false,
                isDhanConnected = false
            ))
        } else {
            Result.failure(Exception("No active broker selected"))
        }
    }

    suspend fun getFunds(): Result<Double> {
        return if (sessionManager.activeBroker == "Dhan") {
            dhanTradingService.getFunds()
        } else {
            angelOneService.getFunds()
        }
    }

    suspend fun getOrders(): Result<List<OrderEntity>> {
        return if (sessionManager.activeBroker == "Dhan") {
            dhanTradingService.getOrders()
        } else {
            angelOneService.getOrders()
        }
    }

    suspend fun placeOrder(order: OrderEntity): Result<String> {
        return if (sessionManager.activeBroker == "Dhan") {
            dhanTradingService.placeOrder(order)
        } else {
            angelOneService.placeOrder(order)
        }
    }

    suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String): Result<Boolean> {
        return if (sessionManager.activeBroker == "Dhan") {
            dhanTradingService.modifyOrder(orderId, newPrice, newQty, orderType)
        } else {
            angelOneService.modifyOrder(orderId, newPrice, newQty, orderType)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Boolean> {
        return if (sessionManager.activeBroker == "Dhan") {
            dhanTradingService.cancelOrder(orderId)
        } else {
            angelOneService.cancelOrder(orderId)
        }
    }

    suspend fun getHoldings(): Result<List<PortfolioHoldingEntity>> {
        return if (sessionManager.activeBroker == "Dhan") {
            dhanTradingService.getHoldings()
        } else {
            angelOneService.getHoldings()
        }
    }

    suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> {
        return if (sessionManager.activeBroker == "Dhan") {
            dhanTradingService.getPositions()
        } else {
            angelOneService.getPositions()
        }
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return marketDataManager.getMarketQuotes(symbols)
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        return marketDataManager.getOptionChain(symbol, expiry)
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return marketDataManager.getOptionExpiries(symbol)
    }
}
