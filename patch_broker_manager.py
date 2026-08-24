import os

filepath = "app/src/main/java/com/example/data/network/BrokerManager.kt"

with open(filepath, "r") as f:
    content = f.read()

# Replace getOptionChain return type 
# Replace getHistoricalCandleData 
# Replace getAdvanceDecline 
# We need to make sure we don't break the return types of BrokerManager

new_file = """package com.example.data.network

import com.example.data.model.HistoricalCandle
import com.example.data.model.MarketBreadth
import com.example.data.model.MarketTick
import com.example.data.model.OptionChain
import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.ui.components.CandleData
import kotlinx.coroutines.flow.StateFlow

/**
 * Broker Manager for KING KHAN AI TRADER
 * 
 * Rules:
 * - Market Data: Unified MarketDataEngine manages hidden automatic failover across Fyers, Angel One, m.Stock.
 * - Dhan is STRICTLY FOR ORDER EXECUTION ONLY. Dhan NEVER provides market data.
 * - ALL live orders route strictly through OrderManager -> DhanTradingService -> Dhan API.
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
    val healthManager = ProviderHealthManager()

    // Unified Market Data Engine with Hidden Failover Router
    val marketDataEngine = MarketDataEngine(
        angelMarketDataService = angelMarketDataService,
        mStockMarketDataService = mStockMarketDataService,
        nseFeedService = null, // removed
        tradeSmartMarketDataService = tradeSmartMarketDataService,
        sessionManager = sessionManager,
        healthManager = healthManager
    )

    val networkClient = BrokerNetworkClient(sessionManager)
    val fyersAuthManager = FyersAuthManager(sessionManager, networkClient.fyersApi)
    val fyersMarketDataService = FyersMarketDataService(sessionManager, marketDataEngine, networkClient.fyersApi).apply { marketDataEngine.fyersMarketDataService = this }

    // Central Order Execution Manager (Dhan-only)
    val orderManager = OrderManager(
        dhanTradingService = dhanTradingService,
        instrumentMasterService = instrumentMasterService
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

    fun setPrimaryMarketDataProvider(providerName: String) {
        marketDataEngine.setPrimaryMarketDataProvider(providerName)
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
        val res = marketDataEngine.getOptionChain(symbol, expiry)
        if (res.isSuccess) {
            return Result.success(res.getOrThrow().strikes)
        }
        return Result.failure(res.exceptionOrNull() ?: Exception("Option chain fetch failed"))
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        val res = marketDataEngine.getOptionChain(symbol, "")
        if (res.isSuccess) {
            return Result.success(res.getOrThrow().expiries)
        }
        return Result.failure(res.exceptionOrNull() ?: Exception("Option expiries fetch failed"))
    }

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<CandleData>> {
        val res = marketDataEngine.getHistoricalCandles(symbol, interval)
        if (res.isSuccess) {
            val candles = res.getOrThrow().map { 
                CandleData(timestamp = it.timestamp, open = it.open.toFloat(), high = it.high.toFloat(), low = it.low.toFloat(), close = it.close.toFloat())
            }
            return Result.success(candles)
        }
        return Result.failure(res.exceptionOrNull() ?: Exception("Historical data fetch failed"))
    }

    suspend fun getMarketBreadth(): Result<MarketBreadth> {
        return marketDataEngine.getMarketBreadth()
    }

    suspend fun getLiveTick(symbol: String, exchange: String = "NSE"): MarketTick? {
        val quotes = marketDataEngine.getMarketQuotes(listOf(symbol))
        val item = quotes.getOrNull()?.firstOrNull()
        if (item != null) {
            return MarketTick(symbol = item.symbol, ltp = item.ltp, timestamp = System.currentTimeMillis())
        }
        return null
    }
}
"""
with open(filepath, "w") as f:
    f.write(new_file)
