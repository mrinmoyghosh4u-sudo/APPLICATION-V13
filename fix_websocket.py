import re

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'r') as f:
    content = f.read()

replacement = """package com.example.data.network

import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import android.util.Log

class AngelOneMarketDataService(
    private val angelOneService: AngelOneBrokerService,
    private val sessionManager: SessionManager
) {
    private var lastUpdateTime: Long = 0L
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val client = OkHttpClient()

    init {
        connectWebSocket()
    }

    private fun connectWebSocket() {
        val token = sessionManager.angelJwtToken
        val clientCode = sessionManager.angelClientCode
        val feedToken = sessionManager.angelFeedToken
        
        if (token.isNullOrEmpty() || clientCode.isNullOrEmpty()) {
            isConnected = false
            return
        }

        val request = Request.Builder()
            .url("wss://smartapisocket.angelone.in/smart-stream")
            .header("Authorization", "Bearer $token")
            .header("x-client-code", clientCode)
            .header("x-feed-token", feedToken ?: "")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d("AngelOneMarketDataService", "WebSocket Opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                lastUpdateTime = System.currentTimeMillis()
                // Process JSON market data here
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                lastUpdateTime = System.currentTimeMillis()
                // Process binary market data here
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d("AngelOneMarketDataService", "WebSocket Closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("AngelOneMarketDataService", "WebSocket Failure", t)
            }
        })
    }

    fun isConnectionLive(): Boolean {
        if (!isConnected && !sessionManager.angelJwtToken.isNullOrEmpty()) {
            // Reconnect if we have a token but aren't connected
            connectWebSocket()
        }
        return isConnected
    }

    fun getLastUpdatedTime(): String {
        if (lastUpdateTime == 0L) return "Not Updated"
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(lastUpdateTime))
    }

    suspend fun getMarketQuotes(symbols: List<String>): Result<List<WatchlistItem>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) {
            return Result.failure(Exception("Angel One Market Data is not connected"))
        }
        val result = angelOneService.getMarketQuotes(symbols)
        if (result.isSuccess) {
            lastUpdateTime = System.currentTimeMillis()
        }
        return result
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) {
            return Result.failure(Exception("Angel One Market Data is not connected"))
        }
        val result = angelOneService.getOptionChain(symbol, expiry)
        if (result.isSuccess) {
            lastUpdateTime = System.currentTimeMillis()
        }
        return result
    }

    suspend fun getOptionExpiries(symbol: String): Result<List<String>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) {
            return Result.failure(Exception("Angel One Market Data is not connected"))
        }
        return angelOneService.getOptionExpiries(symbol)
    }
}
"""

with open('app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt', 'w') as f:
    f.write(replacement)

