package com.example.data.network

import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem

class TradeSmartMarketDataService(
    private val sessionManager: SessionManager
) {
    fun isConfigured(): Boolean {
        return sessionManager.isTradeSmartConfigured()
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (!isConfigured()) {
            return Result.failure(Exception("TradeSmart Backup Provider is not configured or authenticated"))
        }
        return Result.failure(Exception("TradeSmart Market Data unavailable"))
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        if (!isConfigured()) {
            return Result.failure(Exception("TradeSmart Backup Provider is not configured or authenticated"))
        }
        return Result.failure(Exception("TradeSmart Option Chain unavailable"))
    }
}
