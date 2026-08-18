package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AISignalEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingDao {

    // Watchlist
    @Query("SELECT * FROM watchlist WHERE exchange = :exchange OR :exchange = 'ALL'")
    fun getWatchlist(exchange: String): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(items: List<WatchlistItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWatchlistItem(item: WatchlistItem)

    // Orders
    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Query("DELETE FROM orders")
    suspend fun clearAllOrders()

    @Query("UPDATE orders SET status = 'CANCELLED' WHERE orderId = :orderId")
    suspend fun cancelOrder(orderId: String)

    @Query("UPDATE orders SET price = :price, qty = :qty, value = :price * :qty, orderType = :orderType, stopLoss = :stopLoss, target = :target WHERE orderId = :orderId")
    suspend fun modifyOrderFull(orderId: String, price: Double, qty: Int, orderType: String, stopLoss: Double, target: Double)

    @Query("UPDATE orders SET stopLoss = :stopLoss, target = :target WHERE orderId = :orderId")
    suspend fun updateOrderStopLossTarget(orderId: String, stopLoss: Double, target: Double)

    @Query("UPDATE orders SET status = 'CLOSED', exitPrice = :exitPrice, realizedPnl = :realizedPnl WHERE orderId = :orderId")
    suspend fun exitOrderPosition(orderId: String, exitPrice: Double, realizedPnl: Double)

    @Query("UPDATE orders SET qty = qty - :exitLots, value = price * (qty - :exitLots), realizedPnl = realizedPnl + :partialPnl WHERE orderId = :orderId")
    suspend fun partialExitOrderPosition(orderId: String, exitLots: Int, partialPnl: Double)

    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrder(orderId: String)

    @Update
    suspend fun updateWatchlistItem(item: WatchlistItem)

    @Query("UPDATE watchlist SET isFavorite = :isFavorite WHERE symbol = :symbol")
    suspend fun updateFavoriteStatus(symbol: String, isFavorite: Boolean)

    @Update
    suspend fun updateHolding(holding: PortfolioHoldingEntity)

    // Holdings
    @Query("SELECT * FROM holdings")
    fun getAllHoldings(): Flow<List<PortfolioHoldingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldings(holdings: List<PortfolioHoldingEntity>)

    @Query("DELETE FROM holdings")
    suspend fun clearAllHoldings()

    // AI Signals
    @Query("SELECT * FROM ai_signals ORDER BY id DESC")
    fun getAISignals(): Flow<List<AISignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAISignals(signals: List<AISignalEntity>)

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestampMillis DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllNotificationsAsRead()
}
