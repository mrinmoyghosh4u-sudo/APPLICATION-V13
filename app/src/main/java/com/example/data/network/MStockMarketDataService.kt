package com.example.data.network

import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem

class MStockMarketDataService(
    private val sessionManager: SessionManager
) {
    fun isConfigured(): Boolean {
        return sessionManager.isMStockConfigured()
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (!isConfigured()) {
            return Result.failure(Exception("m.Stock Backup Provider is not configured or authenticated"))
        }
        return Result.failure(Exception("m.Stock Market Data unavailable"))
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        if (!isConfigured()) {
            return Result.failure(Exception("m.Stock Backup Provider is not configured or authenticated"))
        }
        return Result.failure(Exception("m.Stock Option Chain unavailable"))
    }
}
