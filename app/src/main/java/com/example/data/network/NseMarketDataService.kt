package com.example.data.network

import android.util.Log
import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider interface for Authorized NSE Real-Time Data Source / Vendor.
 * 
 * Rules:
 * - Never scrape public NSE webpages for guaranteed real-time trading feeds.
 * - Authorized vendors connect via dedicated socket or multicast feeds.
 * - When unconfigured or disconnected, reports "NSE REAL-TIME DATA UNAVAILABLE".
 */
interface INseRealTimeProvider {
    val isConnected: Boolean
    val connectionState: StateFlow<String> // "LIVE", "STALE", "OFFLINE", "UNAVAILABLE"
    fun connect()
    fun disconnect()
    fun subscribe(tokens: List<String>)
    fun unsubscribe(tokens: List<String>)
}

class NseAuthorizedFeedService(
    private val sessionManager: SessionManager
) : INseRealTimeProvider {
    companion object {
        private const val TAG = "NseMarketData"
    }

    private val _connectionState = MutableStateFlow("OFFLINE")
    override val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    override val isConnected: Boolean
        get() = _connectionState.value == "LIVE"

    fun isConfigured(): Boolean {
        // Authorized NSE feed credentials check
        return false // Defaults to unconfigured unless official vendor credentials provided
    }

    override fun connect() {
        if (!isConfigured()) {
            Log.d(TAG, "Authorized NSE feed not configured. Status: OFFLINE (NSE REAL-TIME DATA UNAVAILABLE)")
            _connectionState.value = "OFFLINE"
            MarketDataStore.setSourceHealth(MarketDataSourceNames.NSE, "OFFLINE")
            return
        }
    }

    override fun disconnect() {
        _connectionState.value = "OFFLINE"
        MarketDataStore.setSourceHealth(MarketDataSourceNames.NSE, "OFFLINE")
    }

    override fun subscribe(tokens: List<String>) {
        if (!isConnected) return
    }

    override fun unsubscribe(tokens: List<String>) {
        if (!isConnected) return
    }
}
