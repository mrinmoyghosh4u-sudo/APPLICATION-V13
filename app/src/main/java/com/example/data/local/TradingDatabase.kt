package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AISignalEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.OrderEntity
import com.example.data.model.PortfolioHoldingEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchlistItem

@Database(
    entities = [
        WatchlistItem::class,
        OrderEntity::class,
        PortfolioHoldingEntity::class,
        AISignalEntity::class,
        UserProfileEntity::class,
        NotificationEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class TradingDatabase : RoomDatabase() {

    abstract fun tradingDao(): TradingDao

    companion object {
        @Volatile
        private var INSTANCE: TradingDatabase? = null

        fun getDatabase(context: Context): TradingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TradingDatabase::class.java,
                    "king_khan_trade_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
