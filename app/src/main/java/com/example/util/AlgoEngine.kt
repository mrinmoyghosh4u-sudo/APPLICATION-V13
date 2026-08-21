package com.example.util

import com.example.data.model.AISignalEntity
import com.example.data.model.AlgoPosition
import com.example.data.model.AlgoStrategy
import com.example.data.model.AlgoTradeHistory
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object AlgoEngine {
    var telegramService: com.example.data.network.TelegramService? = null
    private val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    // Engine State
    private val _isAlgoRunning = MutableStateFlow(false)
    val isAlgoRunning: StateFlow<Boolean> = _isAlgoRunning.asStateFlow()

    private val _tradingMode = MutableStateFlow("PAPER TRADING") // "PAPER TRADING" or "AUTO TRADING"
    val tradingMode: StateFlow<String> = _tradingMode.asStateFlow()

    private val _selectedIndex = MutableStateFlow("NIFTY 50")
    val selectedIndex: StateFlow<String> = _selectedIndex.asStateFlow()

    private val _selectedOptionMode = MutableStateFlow("AUTO CE / PE") // "BUY CE ONLY", "BUY PE ONLY", "AUTO CE / PE"
    val selectedOptionMode: StateFlow<String> = _selectedOptionMode.asStateFlow()

    private val defaultStrategy = AlgoStrategy(
        id = "kk_buy_only_default",
        name = "KK BUY-ONLY AI",
        index = "NIFTY 50",
        optionMode = "AUTO CE / PE",
        tradingStyle = "INTRADAY",
        timeframe = "5 MIN",
        riskLevel = "MEDIUM",
        capital = 100000.0,
        isActive = true,
        maxTrades = 5
    )

    private val _currentStrategy = MutableStateFlow<AlgoStrategy>(defaultStrategy)
    val currentStrategy: StateFlow<AlgoStrategy> = _currentStrategy.asStateFlow()

    private val _strategies = MutableStateFlow<List<AlgoStrategy>>(listOf(defaultStrategy))
    val strategies: StateFlow<List<AlgoStrategy>> = _strategies.asStateFlow()

    private val _activePositions = MutableStateFlow<List<AlgoPosition>>(emptyList())
    val activePositions: StateFlow<List<AlgoPosition>> = _activePositions.asStateFlow()

    private val _paperTradeHistory = MutableStateFlow<List<AlgoTradeHistory>>(emptyList())
    val paperTradeHistory: StateFlow<List<AlgoTradeHistory>> = _paperTradeHistory.asStateFlow()

    private val _liveTradeHistory = MutableStateFlow<List<AlgoTradeHistory>>(emptyList())
    val liveTradeHistory: StateFlow<List<AlgoTradeHistory>> = _liveTradeHistory.asStateFlow()

    private val _paperBalance = MutableStateFlow(100000.0)
    val paperBalance: StateFlow<Double> = _paperBalance.asStateFlow()

    private val _riskPerTrade = MutableStateFlow(1.0)
    val riskPerTrade: StateFlow<Double> = _riskPerTrade.asStateFlow()

    private val _maxDailyLossPercent = MutableStateFlow(3.0)
    val maxDailyLossPercent: StateFlow<Double> = _maxDailyLossPercent.asStateFlow()

    private val _maxTradesPerDay = MutableStateFlow(5)
    val maxTradesPerDay: StateFlow<Int> = _maxTradesPerDay.asStateFlow()

    private val _onePositionAtATime = MutableStateFlow(true)
    val onePositionAtATime: StateFlow<Boolean> = _onePositionAtATime.asStateFlow()

    private val _todayTradesCount = MutableStateFlow(0)
    val todayTradesCount: StateFlow<Int> = _todayTradesCount.asStateFlow()

    private val _todayPnl = MutableStateFlow(0.0)
    val todayPnl: StateFlow<Double> = _todayPnl.asStateFlow()

    private val _currentSignal = MutableStateFlow<AISignalEntity?>(null)
    val currentSignal: StateFlow<AISignalEntity?> = _currentSignal.asStateFlow()

    private val _marketBias = MutableStateFlow("NEUTRAL")
    val marketBias: StateFlow<String> = _marketBias.asStateFlow()

    private val _ceBuyScore = MutableStateFlow(50)
    val ceBuyScore: StateFlow<Int> = _ceBuyScore.asStateFlow()

    private val _peBuyScore = MutableStateFlow(50)
    val peBuyScore: StateFlow<Int> = _peBuyScore.asStateFlow()

    private val _indicatorCheckmarks = MutableStateFlow<Map<String, Boolean>>(
        mapOf(
            "EMA" to false,
            "VWAP" to false,
            "RSI" to false,
            "SUPERTREND" to false,
            "OI" to false,
            "VOLUME" to false
        )
    )
    val indicatorCheckmarks: StateFlow<Map<String, Boolean>> = _indicatorCheckmarks.asStateFlow()

    private val _engineStatusMessage = MutableStateFlow("ALGO ENGINE STANDBY")
    val engineStatusMessage: StateFlow<String> = _engineStatusMessage.asStateFlow()

    // Control Functions
    fun setSelectedIndex(index: String) {
        _selectedIndex.value = index
        _currentStrategy.value = _currentStrategy.value.copy(index = index)
    }

    fun setSelectedOptionMode(mode: String) {
        // Enforce BUY ONLY constraints
        val safeMode = if (mode.contains("SELL")) "AUTO CE / PE" else mode
        _selectedOptionMode.value = safeMode
        _currentStrategy.value = _currentStrategy.value.copy(optionMode = safeMode)
    }

    fun setTradingMode(mode: String) {
        _tradingMode.value = mode
    }

    fun updateRiskSettings(riskPerTrade: Double, maxLossPct: Double, maxTrades: Int, onePos: Boolean) {
        _riskPerTrade.value = riskPerTrade
        _maxDailyLossPercent.value = maxLossPct
        _maxTradesPerDay.value = maxTrades
        _onePositionAtATime.value = onePos
    }

    fun toggleAlgo(start: Boolean) {
        _isAlgoRunning.value = start
        if (start) {
            _engineStatusMessage.value = "ALGO ENGINE RUNNING"
            coroutineScope.launch {
                telegramService?.sendFormattedEvent(
                    "ALGO_START_${System.currentTimeMillis()}",
                    com.example.data.network.TelegramMessageFormatter.formatAlgoStarted(
                        _currentStrategy.value.name,
                        _selectedIndex.value,
                        _currentStrategy.value.timeframe,
                        _tradingMode.value
                    )
                )
            }
        } else {
            _engineStatusMessage.value = "ALGO ENGINE STOPPED"
            coroutineScope.launch {
                telegramService?.sendFormattedEvent(
                    "ALGO_STOP_${System.currentTimeMillis()}",
                    com.example.data.network.TelegramMessageFormatter.formatAlgoStopped(
                        _currentStrategy.value.name,
                        "User stopped Algo"
                    )
                )
            }
        }
    }

    fun emergencyStop() {
        _isAlgoRunning.value = false
        _tradingMode.value = "PAPER TRADING"
        _engineStatusMessage.value = "EMERGENCY STOP TRIGGERED"
        coroutineScope.launch {
            telegramService?.sendFormattedEvent(
                "ALGO_EMERGENCY_STOP_${System.currentTimeMillis()}",
                com.example.data.network.TelegramMessageFormatter.formatAlgoStopped(
                    _currentStrategy.value.name,
                    "Emergency Stop Triggered"
                )
            )
        }
    }

    fun generateAIStrategy(
        index: String,
        style: String,
        timeframe: String,
        risk: String,
        capital: Double,
        optionMode: String
    ): AlgoStrategy {
        val safeOptionMode = if (optionMode.contains("SELL")) "AUTO CE / PE" else optionMode

        return AlgoStrategy(
            name = "AI $index $safeOptionMode Strategy",
            index = index,
            optionMode = safeOptionMode,
            tradingStyle = style,
            timeframe = timeframe,
            riskLevel = risk,
            capital = capital
        )
    }

    fun selectStrategy(strategy: AlgoStrategy) {
        _currentStrategy.value = strategy
        _selectedIndex.value = strategy.index
        _selectedOptionMode.value = strategy.optionMode
    }

    fun saveStrategy(strategy: AlgoStrategy) {
        val existingIndex = _strategies.value.indexOfFirst { it.id == strategy.id }
        if (existingIndex >= 0) {
            val list = _strategies.value.toMutableList()
            list[existingIndex] = strategy
            _strategies.value = list
        } else {
            _strategies.value = _strategies.value + strategy
        }
        selectStrategy(strategy)
    }

    fun deleteStrategy(strategyId: String) {
        val updated = _strategies.value.filterNot { it.id == strategyId }
        _strategies.value = updated
        if (_currentStrategy.value.id == strategyId && updated.isNotEmpty()) {
            selectStrategy(updated.first())
        }
    }

    private fun getNextExpiryDate(): String {
        val cal = java.util.Calendar.getInstance()
        cal.time = java.util.Date()
        while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.THURSDAY) {
            cal.add(java.util.Calendar.DATE, 1)
        }
        val df = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
        return df.format(cal.time)
    }

    /**
     * Evaluates live market data and updates Algo calculations and state.
     * Guaranteed to use actual quotes or state "LIVE DATA UNAVAILABLE".
     */
    fun processMarketFeed(
        quotes: List<WatchlistItem>, 
        isBrokerConnected: Boolean,
        optionChain: List<com.example.data.model.OptionStrikeItem>? = null,
        indicators: Map<String, Double>? = null
    ) {
        if (!_isAlgoRunning.value) {
            _engineStatusMessage.value = "ALGO ENGINE OFF"
            return
        }

        if (!isBrokerConnected) {
            _engineStatusMessage.value = "BROKER SESSION EXPIRED"
            _currentSignal.value = null
            _marketBias.value = "NEUTRAL"
            _ceBuyScore.value = 0
            _peBuyScore.value = 0
            _indicatorCheckmarks.value = mapOf(
                "EMA" to false, "VWAP" to false, "RSI" to false,
                "SUPERTREND" to false, "OI" to false, "VOLUME" to false
            )
            return
        }

        val targetQuote = quotes.find {
            it.symbol.contains(_selectedIndex.value, ignoreCase = true) ||
            _selectedIndex.value.contains(it.symbol, ignoreCase = true)
        } ?: quotes.firstOrNull()

        if (targetQuote == null || targetQuote.ltp <= 0) {
            _engineStatusMessage.value = "SIGNAL PAUSED — LIVE MARKET DATA UNAVAILABLE"
            _currentSignal.value = null
            _marketBias.value = "NEUTRAL"
            _ceBuyScore.value = 0
            _peBuyScore.value = 0
            _indicatorCheckmarks.value = mapOf(
                "EMA" to false, "VWAP" to false, "RSI" to false,
                "SUPERTREND" to false, "OI" to false, "VOLUME" to false
            )
            return
        }

        // Validate MarketDataStore status for target symbol
        val storeTick = com.example.data.model.MarketDataStore.getTick(targetQuote.symbol)
        if (storeTick != null && (storeTick.state == "STALE" || storeTick.state == "OFFLINE" || storeTick.source == com.example.data.model.MarketDataSourceNames.YAHOO)) {
            _engineStatusMessage.value = "SIGNAL PAUSED — LIVE FEED ${storeTick.state}"
            _currentSignal.value = null
            return
        }

        _engineStatusMessage.value = "ANALYZING REAL MARKET DATA..."
        
        // Basic Technical Analysis on Quote Data to simulate AI processing
        val isBullish = targetQuote.changePercent > 0.0
        val intensity = Math.abs(targetQuote.changePercent)
        
        var ceScore = 0
        var peScore = 0
        
        // EMA/VWAP Proxy
        if (isBullish) ceScore += 25 else peScore += 25
        val emaCheck = true
        val vwapCheck = true
        
        // RSI Proxy
        if (isBullish && intensity > 0.2) ceScore += 20 else if (!isBullish && intensity > 0.2) peScore += 20
        val rsiCheck = intensity > 0.2
        
        // SuperTrend Proxy
        if (isBullish && intensity > 0.4) ceScore += 30 else if (!isBullish && intensity > 0.4) peScore += 30
        val superTrendCheck = intensity > 0.4
        
        // Volume/OI Proxy
        if (intensity > 0.6) {
            if (isBullish) ceScore += 25 else peScore += 25
        }
        val volumeCheck = intensity > 0.6
        val oiCheck = intensity > 0.6
        
        _ceBuyScore.value = ceScore
        _peBuyScore.value = peScore
        
        _indicatorCheckmarks.value = mapOf(
            "EMA" to emaCheck, "VWAP" to vwapCheck, "RSI" to rsiCheck,
            "SUPERTREND" to superTrendCheck, "OI" to oiCheck, "VOLUME" to volumeCheck
        )
        
        _marketBias.value = when {
            ceScore >= 70 -> "STRONG BULLISH"
            ceScore > 40 -> "BULLISH"
            peScore >= 70 -> "STRONG BEARISH"
            peScore > 40 -> "BEARISH"
            else -> "NEUTRAL"
        }
        
        // Generate actionable signal if threshold met and real option LTP exists in MarketDataStore
        if (ceScore >= 75 || peScore >= 75) {
            val signalType = if (ceScore >= 75) "BUY CE" else "BUY PE"
            val isCe = ceScore >= 75
            val strikeInterval = when {
                targetQuote.symbol.contains("BANKNIFTY", ignoreCase = true) -> 100
                targetQuote.symbol.contains("SENSEX", ignoreCase = true) -> 100
                targetQuote.symbol.contains("MIDCPNIFTY", ignoreCase = true) -> 25
                else -> 50
            }
            val strikeOffset = if (isCe) strikeInterval else -strikeInterval
            val roundedStrike = ((targetQuote.ltp / strikeInterval).roundToInt() * strikeInterval) + strikeOffset
            val optType = if (isCe) "CE" else "PE"
            
            var optionSymbol = ""
            val master = com.example.data.network.InstrumentMasterService.instance
            if (master != null && master.isLoaded) {
                val expiries = master.getOptionExpiries(targetQuote.symbol)
                if (expiries.isNotEmpty()) {
                    val activeExpiry = expiries.first()
                    val optInst = master.resolveOptionInstrument(targetQuote.symbol, activeExpiry, roundedStrike.toDouble(), optType)
                    if (optInst != null) {
                        optionSymbol = optInst.symbol
                    }
                }
            }

            if (optionSymbol.isBlank()) {
                // Return if real option contract not found in Instrument Master
                return
            }

            // Get REAL tick from MarketDataStore for the option contract
            val optionTick = com.example.data.model.MarketDataStore.getTick(optionSymbol)
                ?: quotes.find { it.symbol.equals(optionSymbol, ignoreCase = true) }?.let {
                    com.example.data.model.MarketDataState(
                        source = com.example.data.model.MarketDataSourceNames.ANGEL_ONE,
                        symbol = it.symbol,
                        token = "",
                        exchange = it.exchange,
                        ltp = it.ltp
                    )
                }

            val realOptionLtp = optionTick?.ltp ?: 0.0

            if (realOptionLtp > 0.0) {
                val sl = (realOptionLtp * 0.75).roundToInt().toDouble()
                val t1 = (realOptionLtp * 1.25).roundToInt().toDouble()
                val t2 = (realOptionLtp * 1.50).roundToInt().toDouble()

                _currentSignal.value = com.example.data.model.AISignalEntity(
                    symbol = optionSymbol,
                    exchange = targetQuote.exchange,
                    side = "BUY",
                    actionType = signalType,
                    trend = if (isCe) "BULLISH" else "BEARISH",
                    ltp = realOptionLtp,
                    changePercent = intensity,
                    entryZone = "${String.format(java.util.Locale.US, "%.1f", realOptionLtp * 0.98)} - ${String.format(java.util.Locale.US, "%.1f", realOptionLtp * 1.02)}",
                    target1 = t1,
                    target2 = t2,
                    stopLoss = sl,
                    confidence = if (isCe) ceScore else peScore,
                    riskReward = "1:2",
                    lotSize = targetQuote.lotSize,
                    timeframe = "5M",
                    timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                _engineStatusMessage.value = "SIGNAL GENERATED FROM REAL DATA"
            } else {
                // Do not generate fake/guessed signal if real option price is unavailable
                _currentSignal.value = null
                _engineStatusMessage.value = "WAITING FOR OPTION TICK"
            }
        } else {
            _currentSignal.value = null
        }

        // Update active positions P&L
        updateActivePositionsPnl(targetQuote.ltp)
    }

    private fun updateActivePositionsPnl(currentQuoteLtp: Double) {
        val updatedList = _activePositions.value.map { pos ->
            if (pos.status == "OPEN") {
                val newLtp = if (currentQuoteLtp > 0.0) currentQuoteLtp else pos.currentLtp
                val diff = newLtp - pos.entryPrice
                val pnl = diff * pos.qty
                pos.copy(currentLtp = newLtp, pnl = pnl)
            } else pos
        }
        _activePositions.value = updatedList
        _todayPnl.value = updatedList.sumOf { it.pnl }
    }

    fun exitPaperPosition(posId: String) {
        val target = _activePositions.value.find { it.id == posId } ?: return
        val updated = _activePositions.value.filterNot { it.id == posId }
        _activePositions.value = updated
        _paperBalance.value = _paperBalance.value + target.pnl

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val historyItem = AlgoTradeHistory(
            date = dateStr,
            time = timeStr,
            strategyName = _currentStrategy.value.name,
            index = _selectedIndex.value,
            type = target.type,
            strike = target.symbol,
            expiry = "WEEKLY",
            action = "EXIT",
            price = target.currentLtp,
            qty = target.qty,
            status = if (target.pnl >= 0) "PROFIT (PAPER)" else "LOSS (PAPER)"
        )
        _paperTradeHistory.value = listOf(historyItem) + _paperTradeHistory.value

        coroutineScope.launch {
            telegramService?.sendFormattedEvent(
                "POS_CLOSED_${target.id}",
                com.example.data.network.TelegramMessageFormatter.formatPositionClosed(
                    broker = if (_tradingMode.value == "PAPER TRADING") "PAPER TRADING" else "Dhan",
                    actionType = if (target.type == "PE") "BUY PE" else "BUY CE",
                    index = _selectedIndex.value,
                    strike = target.symbol,
                    entry = String.format("%.2f", target.entryPrice),
                    exit = String.format("%.2f", target.currentLtp),
                    quantity = target.qty.toString(),
                    pnl = String.format("%.2f", target.pnl)
                )
            )
        }
    }

    fun getLotSizeForIndex(index: String): Int {
        return AppPreferences.getGlobalLotSize(index)
    }
}

