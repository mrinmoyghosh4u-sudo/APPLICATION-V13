package com.example.data.network

import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem

/**
 * Broker Manager for KING KHAN AI TRADER
 * 
 * Rules:
 * - Market Data Providers: ANGEL ONE (Primary Live), m.STOCK (Live), NSE (Authorized Feed), YAHOO (Reference Only).
 * - Dhan is STRICTLY FOR ORDER EXECUTION ONLY. Dhan NEVER provides market data.
 * - ALL live orders route strictly through OrderManager -> DhanTradingService -> Dhan API.
 * - Angel One NEVER receives live trading orders.
 */
class BrokerManager(
    private val sessionManager: SessionManager,
    val angelOneService: AngelOneBrokerService,
    val dhanService: DhanBrokerService,
    val instrumentMasterService: InstrumentMasterService
) {
    val angelMarketDataService = AngelOneMarketDataService(angelOneService, sessionManager, instrumentMasterService)
    val dhanTradingService = DhanTradingService(dhanService, sessionManager)
    val mStockMarketDataService = MStockMarketDataService(sessionManager, instrumentMasterService)
    val tradeSmartMarketDataService = TradeSmartMarketDataService(sessionManager)

    // Central Order Execution Manager (Dhan-only)
    val orderManager = OrderManager(
        dhanTradingService = dhanTradingService,
        instrumentMasterService = instrumentMasterService
    )

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
            Result.success(UserProfileEntity(
                name = "",
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

    /**
     * Live Order Placement: FORCED to OrderManager -> DhanTradingService -> Dhan API
     */
    suspend fun placeOrder(order: OrderEntity): Result<String> {
        val result = orderManager.executeOrder(order, isUserConfirmed = true)
        return if (result.isSuccess) {
            Result.success(result.getOrThrow().orderId)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Order Execution Failed"))
        }
    }

    /**
     * Live Order Modification: FORCED to OrderManager -> DhanTradingService -> Dhan API
     */
    suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String): Result<Boolean> {
        return orderManager.modifyOrder(orderId, newPrice, newQty, orderType)
    }

    /**
     * Live Order Cancellation: FORCED to OrderManager -> DhanTradingService -> Dhan API
     */
    suspend fun cancelOrder(orderId: String): Result<Boolean> {
        return orderManager.cancelOrder(orderId)
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
