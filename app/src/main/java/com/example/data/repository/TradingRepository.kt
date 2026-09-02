package com.example.data.repository

import com.example.data.local.TradingDao
import com.example.data.model.AISignalEntity
import com.example.data.model.OptionStrikeItem
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import com.example.data.model.PnlState
import com.example.data.model.MarginState
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

    suspend fun markNotificationsAsRead() {
        dao.markAllNotificationsAsRead()
    }

    suspend fun clearBrokerSnapshot(newBrokerName: String) {
        dao.clearAllHoldings()
        dao.clearAllOrders()
        val current = dao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        dao.insertOrUpdateProfile(
            current.copy(
                name = "",
                connectedBroker = newBrokerName,
                availableMargin = 0.0,
                accountBalance = 0.0,
                totalBalance = 0.0,
                realizedPnl = 0.0,
                unrealizedPnl = 0.0,
                todaysPnl = 0.0,
                todaysPnlPercent = 0.0,
                pnlStatus = PnlState.UNAVAILABLE.name,
                marginStatus = MarginState.UNAVAILABLE.name,
                lastPnlSyncTime = 0L,
                lastMarginSyncTime = 0L,
                pnlErrorMessage = ""
            )
        )
    }

    suspend fun syncWithBroker() {
        brokerManager?.let { manager ->
            val profileRes = manager.getProfile()
            val current = dao.getUserProfile().firstOrNull() ?: UserProfileEntity()

            if (profileRes.isSuccess) {
                val prof = profileRes.getOrThrow()
                val isAngelConn = if (prof.connectedBroker == "Angel One") true else prof.isAngelConnected
                val isDhanConn = if (prof.connectedBroker == "Dhan") true else prof.isDhanConnected

                // Determine P&L handling:
                val finalPnlStatus: String
                val finalRealized: Double
                val finalUnrealized: Double
                val finalTodaysPnl: Double
                val finalLastPnlSyncTime: Long

                if (prof.pnlStatus == PnlState.AVAILABLE.name) {
                    finalPnlStatus = PnlState.AVAILABLE.name
                    finalRealized = prof.realizedPnl
                    finalUnrealized = prof.unrealizedPnl
                    finalTodaysPnl = prof.todaysPnl
                    finalLastPnlSyncTime = prof.lastPnlSyncTime
                } else {
                    if ((current.pnlStatus == PnlState.AVAILABLE.name || current.pnlStatus == PnlState.STALE.name) && current.lastPnlSyncTime > 0L) {
                        finalPnlStatus = PnlState.STALE.name
                        finalRealized = current.realizedPnl
                        finalUnrealized = current.unrealizedPnl
                        finalTodaysPnl = current.todaysPnl
                        finalLastPnlSyncTime = current.lastPnlSyncTime
                    } else {
                        finalPnlStatus = prof.pnlStatus.ifBlank { PnlState.UNAVAILABLE.name }
                        finalRealized = 0.0
                        finalUnrealized = 0.0
                        finalTodaysPnl = 0.0
                        finalLastPnlSyncTime = 0L
                    }
                }

                // Determine Margin handling:
                val finalMarginStatus: String
                val finalAvailMargin: Double
                val finalAccountBal: Double
                val finalTotalBal: Double
                val finalLastMarginTime: Long

                if (prof.marginStatus == MarginState.AVAILABLE.name) {
                    finalMarginStatus = MarginState.AVAILABLE.name
                    finalAvailMargin = prof.availableMargin
                    finalAccountBal = prof.accountBalance
                    finalTotalBal = prof.totalBalance
                    finalLastMarginTime = prof.lastMarginSyncTime
                } else {
                    if ((current.marginStatus == MarginState.AVAILABLE.name || current.marginStatus == MarginState.STALE.name) && current.lastMarginSyncTime > 0L) {
                        finalMarginStatus = MarginState.STALE.name
                        finalAvailMargin = current.availableMargin
                        finalAccountBal = current.accountBalance
                        finalTotalBal = current.totalBalance
                        finalLastMarginTime = current.lastMarginSyncTime
                    } else {
                        finalMarginStatus = MarginState.UNAVAILABLE.name
                        finalAvailMargin = 0.0
                        finalAccountBal = 0.0
                        finalTotalBal = 0.0
                        finalLastMarginTime = 0L
                    }
                }

                dao.insertOrUpdateProfile(
                    current.copy(
                        name = if (prof.name.isNotBlank()) prof.name else current.name,
                        email = if (prof.email.isNotBlank()) prof.email else current.email,
                        phone = if (prof.phone.isNotBlank()) prof.phone else current.phone,
                        connectedBroker = prof.connectedBroker,
                        isAngelConnected = isAngelConn,
                        angelClientId = if (prof.connectedBroker == "Angel One" && prof.angelClientId.isNotBlank()) prof.angelClientId else current.angelClientId,
                        isDhanConnected = isDhanConn,
                        dhanClientId = if (prof.connectedBroker == "Dhan" && prof.dhanClientId.isNotBlank()) prof.dhanClientId else current.dhanClientId,
                        availableMargin = finalAvailMargin,
                        accountBalance = finalAccountBal,
                        totalBalance = finalTotalBal,
                        realizedPnl = finalRealized,
                        unrealizedPnl = finalUnrealized,
                        todaysPnl = finalTodaysPnl,
                        pnlStatus = finalPnlStatus,
                        marginStatus = finalMarginStatus,
                        lastPnlSyncTime = finalLastPnlSyncTime,
                        lastMarginSyncTime = finalLastMarginTime,
                        pnlErrorMessage = prof.pnlErrorMessage
                    )
                )
            } else {
                if (current.isBrokerConnected) {
                    val stalePnlStatus = if (current.lastPnlSyncTime > 0L) PnlState.STALE.name else PnlState.UNAVAILABLE.name
                    val staleMarginStatus = if (current.lastMarginSyncTime > 0L) MarginState.STALE.name else MarginState.UNAVAILABLE.name
                    dao.insertOrUpdateProfile(
                        current.copy(
                            pnlStatus = stalePnlStatus,
                            marginStatus = staleMarginStatus,
                            pnlErrorMessage = profileRes.exceptionOrNull()?.message ?: "Profile sync failed"
                        )
                    )
                }
            }
                
            val holdingsRes = manager.getHoldings()
            val positionsRes = manager.getPositions()
            if (holdingsRes.isSuccess || positionsRes.isSuccess) {
                val holdingsList = holdingsRes.getOrDefault(emptyList())
                val positionsList = positionsRes.getOrDefault(emptyList())
                val combined = holdingsList + positionsList
                dao.clearAllHoldings()
                if (combined.isNotEmpty()) {
                    dao.insertHoldings(combined)
                }
            }

            val ordersRes = manager.getOrders()
            if (ordersRes.isSuccess) {
                val ordersList = ordersRes.getOrDefault(emptyList())
                val localOrders = dao.getAllOrders().firstOrNull() ?: emptyList()
                val preservedLocalOrders = localOrders.filter { local ->
                    local.status == "PENDING" && ordersList.none { api -> api.brokerOrderId == local.brokerOrderId || api.orderId == local.orderId }
                }
                
                dao.clearAllOrders()
                val finalOrders = ordersList + preservedLocalOrders
                if (finalOrders.isNotEmpty()) {
                    dao.insertOrders(finalOrders)
                }
            }

            // Sync Market Quotes
            runCatching {
                val currentWatchlist = dao.getWatchlist("ALL").firstOrNull() ?: emptyList()
                if (currentWatchlist.isEmpty()) {
                    // Seed default symbols (Strict 8 approved underlyings)
                    val defaultSymbols = listOf(
                        WatchlistItem(symbol = "NIFTY 50", exchange = "NSE", ltp = 0.0, change = 0.0, changePercent = 0.0, lotSize = com.example.util.AppPreferences.getGlobalLotSize("NIFTY 50"), isPositive = true),
                        WatchlistItem(symbol = "BANKNIFTY", exchange = "NSE", ltp = 0.0, change = 0.0, changePercent = 0.0, lotSize = com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY"), isPositive = true),
                        WatchlistItem(symbol = "FINNIFTY", exchange = "NSE", ltp = 0.0, change = 0.0, changePercent = 0.0, lotSize = com.example.util.AppPreferences.getGlobalLotSize("FINNIFTY"), isPositive = true),
                        WatchlistItem(symbol = "MIDCPNIFTY", exchange = "NSE", ltp = 0.0, change = 0.0, changePercent = 0.0, lotSize = com.example.util.AppPreferences.getGlobalLotSize("MIDCPNIFTY"), isPositive = true),
                        WatchlistItem(symbol = "SENSEX", exchange = "BSE", ltp = 0.0, change = 0.0, changePercent = 0.0, lotSize = com.example.util.AppPreferences.getGlobalLotSize("SENSEX"), isPositive = true),
                        WatchlistItem(symbol = "BANKEX", exchange = "BSE", ltp = 0.0, change = 0.0, changePercent = 0.0, lotSize = com.example.util.AppPreferences.getGlobalLotSize("BANKEX"), isPositive = true),
                        WatchlistItem(symbol = "CRUDEOIL", exchange = "MCX", ltp = 0.0, change = 0.0, changePercent = 0.0, lotSize = com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL"), isPositive = true),
                        WatchlistItem(symbol = "CRUDEOIL M", exchange = "MCX", ltp = 0.0, change = 0.0, changePercent = 0.0, lotSize = com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL M"), isPositive = true)
                    )
                    dao.insertWatchlist(defaultSymbols)
                }
                
                val symbolsToFetch = (dao.getWatchlist("ALL").firstOrNull() ?: emptyList()).map { it.symbol }
                if (symbolsToFetch.isNotEmpty()) {
                    val quotesRes = manager.getMarketQuotes(symbolsToFetch)
                    quotesRes.getOrNull()?.let { quotes ->
                        if (quotes.isNotEmpty()) {
                            dao.insertWatchlist(quotes)
                        }
                    }
                }
            }
        }
    }

    suspend fun updateWatchlistQuotes(quotes: List<WatchlistItem>) {
        if (quotes.isNotEmpty()) {
            dao.insertWatchlist(quotes)
        }
    }

    suspend fun checkAndSeedInitialData() {
        val currentList = dao.getWatchlist("ALL").firstOrNull() ?: emptyList()
        val indices = listOf(
            com.example.data.model.WatchlistItem(symbol = "NIFTY 50", exchange = "NSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("NIFTY 50"), isPositive=true),
            com.example.data.model.WatchlistItem(symbol = "BANKNIFTY", exchange = "NSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY"), isPositive=true),
            com.example.data.model.WatchlistItem(symbol = "FINNIFTY", exchange = "NSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("FINNIFTY"), isPositive=true),
            com.example.data.model.WatchlistItem(symbol = "MIDCPNIFTY", exchange = "NSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("MIDCPNIFTY"), isPositive=true),
            com.example.data.model.WatchlistItem(symbol = "SENSEX", exchange = "BSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("SENSEX"), isPositive=true),
            com.example.data.model.WatchlistItem(symbol = "BANKEX", exchange = "BSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("BANKEX"), isPositive=true),
            com.example.data.model.WatchlistItem(symbol = "CRUDEOIL", exchange = "MCX", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL"), isPositive=true),
            com.example.data.model.WatchlistItem(symbol = "CRUDEOIL M", exchange = "MCX", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL M"), isPositive=true)
        )
        indices.forEach { idx ->
            if (currentList.none { it.symbol == idx.symbol }) {
                dao.addWatchlistItem(idx)
            }
        }
        if (dao.getUserProfile().firstOrNull() == null) {
            dao.insertOrUpdateProfile(UserProfileEntity())
            val seedIndices = listOf(
                com.example.data.model.WatchlistItem(symbol = "NIFTY 50", exchange = "NSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("NIFTY 50"), isPositive=true),
                com.example.data.model.WatchlistItem(symbol = "BANKNIFTY", exchange = "NSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("BANKNIFTY"), isPositive=true),
                com.example.data.model.WatchlistItem(symbol = "FINNIFTY", exchange = "NSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("FINNIFTY"), isPositive=true),
                com.example.data.model.WatchlistItem(symbol = "MIDCPNIFTY", exchange = "NSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("MIDCPNIFTY"), isPositive=true),
                com.example.data.model.WatchlistItem(symbol = "SENSEX", exchange = "BSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("SENSEX"), isPositive=true),
                com.example.data.model.WatchlistItem(symbol = "BANKEX", exchange = "BSE", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("BANKEX"), isPositive=true),
                com.example.data.model.WatchlistItem(symbol = "CRUDEOIL", exchange = "MCX", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL"), isPositive=true),
                com.example.data.model.WatchlistItem(symbol = "CRUDEOIL M", exchange = "MCX", ltp=0.0, change=0.0, changePercent=0.0, lotSize=com.example.util.AppPreferences.getGlobalLotSize("CRUDEOIL M"), isPositive=true)
            )
            seedIndices.forEach { dao.addWatchlistItem(it) }
        }
    }

    suspend fun placeOrder(order: OrderEntity): String {
        val manager = brokerManager
        val realOrderId = if (manager != null) {
            val res = manager.placeOrder(order)
            res.getOrThrow() // Throws broker API error if failed
        } else {
            order.orderId
        }
        val finalOrder = order.copy(
            orderId = realOrderId,
            brokerOrderId = realOrderId,
            status = "PENDING"
        )
        dao.insertOrder(finalOrder)
        return realOrderId
    }

    suspend fun cancelOrder(orderId: String) {
        val manager = brokerManager
        if (manager != null) {
            val res = manager.cancelOrder(orderId)
            res.getOrThrow()
        }
        dao.cancelOrder(orderId)
    }

    suspend fun modifyOrder(orderId: String, newPrice: Double, newQty: Int, orderType: String = "LIMIT", stopLoss: Double = 0.0, target: Double = 0.0) {
        val manager = brokerManager
        if (manager != null) {
            val res = manager.modifyOrder(orderId, newPrice, newQty, orderType)
            res.getOrThrow()
        }
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

    suspend fun getOptionChainStrikes(symbol: String = "NIFTY", expiry: String = "", forceRefresh: Boolean = false): List<OptionStrikeItem> {
        val live = runCatching {
            brokerManager?.getOptionChain(symbol, expiry, forceRefresh)?.getOrNull()
        }.getOrNull()
        if (!live.isNullOrEmpty()) return live
        return emptyList()
    }
}
