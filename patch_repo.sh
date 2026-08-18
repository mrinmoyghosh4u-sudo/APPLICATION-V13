cat << 'INNER_EOF' > app/src/main/java/com/example/data/repository/TradingRepository.kt
package com.example.data.repository

import com.example.data.local.TradingDao
import com.example.data.model.AISignalEntity
import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.data.network.BrokerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TradingRepository(
    private val dao: TradingDao,
    val brokerManager: BrokerManager? = null
) {
    val watchlistNSE: Flow<List<WatchlistItem>> = dao.getWatchlist("NSE")
    val watchlistBSE: Flow<List<WatchlistItem>> = dao.getWatchlist("BSE")
    val watchlistMCX: Flow<List<WatchlistItem>> = dao.getWatchlist("MCX")
    val watchlistAll: Flow<List<WatchlistItem>> = dao.getWatchlist("ALL")
    val allOrders: Flow<List<OrderEntity>> = dao.getAllOrders()
    val allHoldings: Flow<List<PortfolioHoldingEntity>> = dao.getAllHoldings()
    val aiSignals: Flow<List<AISignalEntity>> = dao.getAISignals()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val notifications: Flow<List<com.example.data.model.NotificationEntity>> = dao.getAllNotifications()

    suspend fun addNotification(title: String, message: String, type: String = "INFO") {
        dao.insertNotification(com.example.data.model.NotificationEntity(title = title, message = message, type = type))
    }

    suspend fun clearNotifications() {
        dao.clearAllNotifications()
    }

    suspend fun syncWithBroker() {
        brokerManager?.let { manager ->
            val profileRes = manager.getProfile()
            profileRes.getOrNull()?.let { prof ->
                val current = dao.getUserProfile().firstOrNull() ?: UserProfileEntity()
                dao.insertOrUpdateProfile(
                    current.copy(
                        name = prof.name.ifEmpty { current.name },
                        email = prof.email.ifEmpty { current.email },
                        phone = prof.phone.ifEmpty { current.phone },
                        connectedBroker = prof.connectedBroker,
                        isAngelConnected = prof.isAngelConnected,
                        angelClientId = prof.angelClientId.ifEmpty { current.angelClientId },
                        isDhanConnected = prof.isDhanConnected,
                        dhanClientId = prof.dhanClientId.ifEmpty { current.dhanClientId },
                        availableMargin = prof.availableMargin,
                        accountBalance = prof.accountBalance
                    )
                )
            }
            
            val holdingsList = manager.getHoldings().getOrDefault(emptyList())
            val positionsList = manager.getPositions().getOrDefault(emptyList())
            val combined = holdingsList + positionsList
            
            dao.clearAllHoldings()
            if (combined.isNotEmpty()) {
                dao.insertHoldings(combined)
            }

            val ordersList = manager.getOrders().getOrDefault(emptyList())
            dao.clearAllOrders()
            if (ordersList.isNotEmpty()) {
                dao.insertOrders(ordersList)
            }
        }
    }

    suspend fun checkAndSeedInitialData() {
        if (dao.getUserProfile().firstOrNull() == null) {
            dao.insertOrUpdateProfile(UserProfileEntity())
        }
    }

    suspend fun placeOrder(order: OrderEntity) {
        val brokerOrderId = runCatching {
            brokerManager?.placeOrder(order)?.getOrThrow()
        }.getOrNull()
        val finalOrder = if (!brokerOrderId.isNullOrEmpty()) {
            order.copy(orderId = brokerOrderId)
        } else {
            order
        }
        dao.insertOrder(finalOrder)
    }

    suspend fun cancelOrder(orderId: String) {
        runCatching {
            brokerManager?.cancelOrder(orderId)
        }
        dao.cancelOrder(orderId)
    }

    suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String = "LIMIT", stopLoss: Double = 0.0, target: Double = 0.0) {
        dao.modifyOrderFull(orderId, newPrice, newQty, orderType, stopLoss, target)
    }

    suspend fun updateOrderStopLossTarget(orderId: String, stopLoss: Double, target: Double) {
        dao.updateOrderStopLossTarget(orderId, stopLoss, target)
    }

    suspend fun exitPosition(orderId: String, exitPrice: Double, realizedPnl: Double) {
        dao.exitOrderPosition(orderId, exitPrice, realizedPnl)
    }

    suspend fun partialExitPosition(orderId: String, exitLots: Int, partialPnl: Double) {
        dao.partialExitOrderPosition(orderId, exitLots, partialPnl)
    }

    suspend fun deleteOrder(orderId: String) {
        dao.deleteOrder(orderId)
    }

    suspend fun addWatchlistItem(item: WatchlistItem) {
        dao.addWatchlistItem(item)
    }

    suspend fun updateWatchlistItem(item: WatchlistItem) {
        dao.updateWatchlistItem(item)
    }

    suspend fun updateFavoriteStatus(symbol: String, isFavorite: Boolean) {
        dao.updateFavoriteStatus(symbol, isFavorite)
    }

    suspend fun updateHolding(item: PortfolioHoldingEntity) {
        dao.updateHolding(item)
    }

    suspend fun updateAISignals(signals: List<AISignalEntity>) {
        dao.insertAISignals(signals)
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        dao.insertOrUpdateProfile(profile)
    }

    suspend fun getOptionExpiries(symbol: String): List<String> {
        val liveExpiries = runCatching {
            brokerManager?.getOptionExpiries(symbol)?.getOrNull()
        }.getOrNull() ?: emptyList()
        return com.example.util.OptionExpiryUtil.getUpcomingExpiriesForSymbol(symbol, liveExpiries)
    }

    suspend fun getOptionChainStrikes(symbol: String = "NIFTY", expiry: String = ""): List<OptionStrikeItem> {
        val live = runCatching {
            brokerManager?.getOptionChain(symbol, expiry)?.getOrNull()
        }.getOrNull()
        if (!live.isNullOrEmpty()) return live
        return emptyList()
    }
}
INNER_EOF
