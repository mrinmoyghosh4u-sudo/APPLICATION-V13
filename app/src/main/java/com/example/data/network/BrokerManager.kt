package com.example.data.network

import com.example.data.model.HistoricalCandle
import com.example.data.model.MarketBreadth
import com.example.data.model.MarketTick
import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CandleData

/**
 * Broker Manager for KING KHAN AI TRADER
 * 
 * Rules:
 * - Market Data: Unified MarketDataEngine manages hidden automatic failover across Angel One, m.Stock, NSE, and Yahoo.
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
    val tradeSmartMarketDataService = TradeSmartMarketDataService(sessionManager, instrumentMasterService)
    val nseFeedService = NseAuthorizedFeedService(sessionManager)
    val healthManager = ProviderHealthManager()

    // Unified Market Data Engine with Hidden Failover Router
    val marketDataEngine = MarketDataEngine(
        angelMarketDataService = angelMarketDataService,
        mStockMarketDataService = mStockMarketDataService,
        nseFeedService = nseFeedService,
        tradeSmartMarketDataService = tradeSmartMarketDataService,
        sessionManager = sessionManager,
        healthManager = healthManager
    )

    // Central Order Execution Manager (Dhan-only)
    val orderManager = OrderManager(
        dhanTradingService = dhanTradingService,
        instrumentMasterService = instrumentMasterService
    )

    val marketDataManager = MarketDataManager(
        angelMarketDataService = angelMarketDataService,
        mStockMarketDataService = mStockMarketDataService,
        tradeSmartMarketDataService = tradeSmartMarketDataService,
        sessionManager = sessionManager
    )

    val brokerAuthManager = BrokerAuthManager(
        sessionManager = sessionManager,
        dhanService = dhanTradingService,
        angelOneService = angelOneService,
        angelMarketDataService = angelMarketDataService,
        mStockMarketDataService = mStockMarketDataService,
        tradeSmartMarketDataService = tradeSmartMarketDataService,
        brokerManager = this
    )

    val currentMarketDataSource: String
        get() = marketDataEngine.unifiedFeedStatus.value

    val activeService: IBrokerService
        get() = dhanService

    fun setActiveBroker(broker: String) {
        sessionManager.activeBroker = "Dhan"
    }

    suspend fun getProfile(): Result<UserProfileEntity> {
        return dhanTradingService.getProfile()
    }

    suspend fun getFunds(): Result<Double> {
        return dhanTradingService.getFunds()
    }

    suspend fun getOrders(): Result<List<OrderEntity>> {
        return dhanTradingService.getOrders()
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
        return dhanTradingService.getHoldings()
    }

    suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> {
        return dhanTradingService.getPositions()
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        return marketDataEngine.getMarketQuotes(symbols)
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        return marketDataEngine.getOptionChain(symbol, expiry)
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        return marketDataEngine.getOptionExpiries(symbol)
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<CandleData>> {
        return marketDataEngine.getHistoricalCandleData(symbol, interval)
    }

    suspend fun getMarketBreadth(): Result<MarketBreadth> {
        return marketDataEngine.getAdvanceDecline()
    }

    suspend fun getLiveTick(symbol: String, exchange: String = "NSE"): MarketTick? {
        return marketDataEngine.getLiveTick(symbol, exchange)
    }
}
