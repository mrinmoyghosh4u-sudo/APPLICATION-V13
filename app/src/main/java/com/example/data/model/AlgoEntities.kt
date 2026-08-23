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
    val target3: Double = target2 * 1.15,
    val target4: Double = target2 * 1.30,
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

data class AlgoSystemLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
    val level: String = "INFO", // "INFO", "SIGNAL", "EXECUTION", "RISK", "TELEGRAM", "WARN"
    val tag: String,
    val message: String
)

data class BacktestResult(
    val strategyName: String,
    val index: String,
    val timeframe: String,
    val days: Int,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val totalProfit: Double,
    val totalLoss: Double,
    val netPnl: Double,
    val profitFactor: Double,
    val maxDrawdownPct: Double,
    val avgTradePnl: Double,
    val sharpeRatio: Double,
    val equityCurve: List<Double>
)
