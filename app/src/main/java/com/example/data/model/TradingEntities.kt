package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val exchange: String, // NSE, BSE, MCX
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val lotSize: Int,
    val isPositive: Boolean,
    val isFavorite: Boolean = false,
    val volume: Long = 0L,
    val oiChange: Double = 0.0
)

@Immutable
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: String,
    val symbol: String,
    val exchange: String,
    val lotSize: Int,
    val qty: Int,
    val orderType: String, // LIMIT, MARKET, SL
    val side: String, // BUY, SELL
    val price: Double,
    val value: Double,
    val stopLoss: Double = 0.0,
    val target: Double = 0.0,
    val status: String, // PENDING, OPEN, EXECUTED, CANCELLED, REJECTED, CLOSED
    val time: String,
    val expiry: String = "",
    val productType: String = "INTRADAY", // INTRADAY, MARGIN, CNC, MIS, CARRYFORWARD
    val ltp: Double = 0.0,
    val exitPrice: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val remarks: String = "",
    val optionType: String = "CE", // CE, PE, EQ
    val strike: Double = 0.0,
    val filledQty: Int = 0,
    val remainingQty: Int = 0,
    val brokerOrderId: String = "",
    val avgPrice: Double = 0.0,
    val dayQty: Int = 0,
    val securityId: String = "",
    val symbolToken: String = ""
)

@Immutable
@Entity(tableName = "holdings")
data class PortfolioHoldingEntity(
    @PrimaryKey val symbol: String,
    val exchange: String = "NSE",
    val type: String = "EQUITY", // INDEX, CE, PE, FUT, EQUITY
    val expiry: String = "",
    val qty: Int = 0,
    val avgPrice: Double = 0.0,
    val ltp: Double = 0.0,
    val currentValue: Double = 0.0,
    val pnl: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val remarks: String = "",
    val unrealizedPnl: Double = 0.0,
    val securityId: String = "",
    val buyAvg: Double = 0.0,
    val buyQty: Int = 0,
    val sellAvg: Double = 0.0,
    val sellQty: Int = 0,
    val optionType: String = "", // CE, PE, EQ
    val strikePrice: Double = 0.0,
    val positionStatus: String = "OPEN", // OPEN, CLOSED
    val productType: String = "INTRADAY"
)

@Immutable
@Entity(tableName = "ai_signals")
data class AISignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val exchange: String,
    val side: String, // BUY, SELL
    val actionType: String = "BUY CE", // BUY CE, BUY PE
    val trend: String = "BULLISH",
    val ltp: Double,
    val changePercent: Double,
    val entryZone: String,
    val target1: Double,
    val target2: Double,
    val target3: Double = 0.0,
    val target4: Double = 0.0,
    val stopLoss: Double,
    val trailingSl: Double = 0.0,
    val confidence: Int, // e.g. 87
    val riskReward: String,
    val lotSize: Int,
    val timeframe: String,
    val timestamp: String,
    val isLive: Boolean = true,
    val status: String = "LIVE",
    val strikePrice: String = "",
    val expiry: String = "",
    val reasons: String = "",
    val underlyingLtp: Double = 0.0,
    val underlyingChange: Double = 0.0
)

@Immutable
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "INFO" // INFO, SUCCESS, ALERT
)

fun formatRelativeTimestamp(createdMillis: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = (now - createdMillis).coerceAtLeast(0)
    val diffSeconds = diffMillis / 1000
    val diffMinutes = diffSeconds / 60
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24

    return when {
        diffSeconds < 60 -> "Just now"
        diffMinutes < 60 -> if (diffMinutes == 1L) "1 min ago" else "$diffMinutes min ago"
        diffHours < 24 -> if (diffHours == 1L) "1 hour ago" else "$diffHours hours ago"
        diffDays == 1L -> "Yesterday"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date(createdMillis))
        }
    }
}

enum class PnlState {
    AVAILABLE,
    UNAVAILABLE,
    LOADING,
    STALE,
    AUTH_ERROR,
    NETWORK_ERROR
}

enum class MarginState {
    AVAILABLE,
    UNAVAILABLE,
    LOADING,
    STALE,
    AUTH_ERROR,
    NETWORK_ERROR
}

@Immutable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val role: String = "Pro Trader",
    val plan: String = "Free",
    val email: String = "",
    val phone: String = "",
    val pan: String = "",
    val kycStatus: String = "Not Connected",
    val memberSince: String = "",
    val totalOrders: Int = 0,
    val winRate: Double = 0.0,
    val avgReturn: Double = 0.0,
    val rank: String = "Standard",
    val connectedBroker: String = "", // Angel One or Dhan
    val isAngelConnected: Boolean = false,
    val angelClientId: String = "",
    val isDhanConnected: Boolean = false,
    val dhanClientId: String = "",
    val isBiometricEnabled: Boolean = false,
    val riskPreference: String = "Moderate",
    val defaultOrderType: String = "LIMIT",
    val totalBalance: Double = 0.0,
    val availableMargin: Double = 0.0,
    val accountBalance: Double = 0.0,
    val todaysPnl: Double = 0.0,
    val todaysPnlPercent: Double = 0.0,
    val overallPnl: Double = 0.0,
    val overallPnlPercent: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val remarks: String = "",
    val unrealizedPnl: Double = 0.0,
    val buyingPower: Double = 0.0,
    val pnlStatus: String = PnlState.UNAVAILABLE.name,
    val marginStatus: String = MarginState.UNAVAILABLE.name,
    val lastPnlSyncTime: Long = 0L,
    val lastMarginSyncTime: Long = 0L,
    val pnlErrorMessage: String = ""
) {
    val isBrokerConnected: Boolean
        get() = (isAngelConnected || isDhanConnected) && connectedBroker.isNotBlank()

    val isPnlAvailable: Boolean
        get() = pnlStatus == PnlState.AVAILABLE.name

    val isPnlStale: Boolean
        get() = pnlStatus == PnlState.STALE.name

    val isPnlUnavailable: Boolean
        get() = pnlStatus == PnlState.UNAVAILABLE.name || pnlStatus == PnlState.AUTH_ERROR.name || pnlStatus == PnlState.NETWORK_ERROR.name || pnlStatus.isBlank()

    val isMarginAvailable: Boolean
        get() = marginStatus == MarginState.AVAILABLE.name

    val isMarginStale: Boolean
        get() = marginStatus == MarginState.STALE.name

    val isMarginUnavailable: Boolean
        get() = marginStatus == MarginState.UNAVAILABLE.name || marginStatus == MarginState.AUTH_ERROR.name || marginStatus == MarginState.NETWORK_ERROR.name || marginStatus.isBlank()
}

@Immutable
data class OptionStrikeItem(
    val strikePrice: Double,
    val callOi: String = "0",
    val callChgOi: String = "0",
    val callIv: Double? = null,
    val callLtp: Double = 0.0,
    val callDelta: Double? = null,
    val putDelta: Double? = null,
    val putLtp: Double = 0.0,
    val putIv: Double? = null,
    val putChgOi: String = "0",
    val putOi: String = "0",
    val isAtm: Boolean = false,
    val callGamma: Double? = null,
    val callTheta: Double? = null,
    val callVega: Double? = null,
    val putGamma: Double? = null,
    val putTheta: Double? = null,
    val putVega: Double? = null,
    val callVolume: String = "0",
    val putVolume: String = "0",
    val callBid: Double = 0.0,
    val callAsk: Double = 0.0,
    val putBid: Double = 0.0,
    val putAsk: Double = 0.0,
    val callToken: String = "",
    val putToken: String = "",
    val callSymbol: String = "",
    val putSymbol: String = ""
)
