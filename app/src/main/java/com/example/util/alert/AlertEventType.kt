package com.example.util.alert

enum class AlertEventType(val key: String, val displayName: String) {
    BROKER_CONNECTED("BROKER_CONNECTED", "Broker Connected"),
    BROKER_DISCONNECTED("BROKER_DISCONNECTED", "Broker Disconnected"),
    BUY_CE_SIGNAL("BUY_CE_SIGNAL", "AI BUY CE Signal"),
    BUY_PE_SIGNAL("BUY_PE_SIGNAL", "AI BUY PE Signal"),
    ENTRY_POSITION("ENTRY_POSITION", "Entry / Position Opened"),
    STOP_LOSS_HIT("STOP_LOSS_HIT", "Stop Loss Hit"),
    TARGET_1_HIT("TARGET_1_HIT", "Target 1 Hit"),
    TARGET_2_HIT("TARGET_2_HIT", "Target 2 Hit"),
    TARGET_3_HIT("TARGET_3_HIT", "Target 3 Hit"),
    TARGET_4_HIT("TARGET_4_HIT", "Target 4 Hit"),
    TRAILING_SL_HIT("TRAILING_SL_HIT", "Trailing Stop Loss Hit"),
    ORDER_EXECUTED("ORDER_EXECUTED", "Order Executed"),
    ORDER_REJECTED("ORDER_REJECTED", "Order Rejected"),
    ALGO_STARTED("ALGO_STARTED", "Algo Started"),
    ALGO_STOPPED("ALGO_STOPPED", "Algo Stopped"),
    RISK_LIMIT_REACHED("RISK_LIMIT_REACHED", "Risk Limit Reached"),
    MARKET_NEWS("MARKET_NEWS", "Market News & Intelligence")
}
