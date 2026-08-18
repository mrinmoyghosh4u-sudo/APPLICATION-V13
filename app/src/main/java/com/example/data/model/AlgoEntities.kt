package com.example.data.model

data class AlgoStrategy(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val index: String,
    val optionMode: String, // "BUY CE ONLY", "BUY PE ONLY", "AUTO CE / PE"
    val tradingStyle: String,
    val timeframe: String,
    val riskLevel: String,
    val capital: Double,
    val isActive: Boolean = false,
    val maxTrades: Int = 5
)

data class AlgoPosition(
    val id: String,
    val symbol: String,
    val type: String, // "CE" or "PE"
    val entryPrice: Double,
    val qty: Int,
    val sl: Double,
    val target1: Double,
    val target2: Double,
    val trailingSl: Double? = null,
    val currentLtp: Double,
    val pnl: Double,
    val status: String // "OPEN", "CLOSED"
)

data class AlgoTradeHistory(
    val date: String,
    val time: String,
    val strategyName: String,
    val index: String,
    val type: String,
    val strike: String,
    val expiry: String,
    val action: String, // BUY or EXIT
    val price: Double,
    val qty: Int,
    val status: String
)
