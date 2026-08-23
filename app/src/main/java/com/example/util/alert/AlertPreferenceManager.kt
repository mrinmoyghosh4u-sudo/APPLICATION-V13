package com.example.util.alert

import com.example.util.AlertPreferences
import com.example.util.AppPreferences

class AlertPreferenceManager(private val appPreferences: AppPreferences) {

    fun isEventEnabled(eventType: AlertEventType): Boolean {
        val prefs = appPreferences.getAlertPreferences()
        return when (eventType) {
            AlertEventType.BROKER_CONNECTED -> prefs.brokerConnected
            AlertEventType.BROKER_DISCONNECTED -> prefs.brokerDisconnected
            AlertEventType.BUY_CE_SIGNAL -> prefs.buyCeSignal
            AlertEventType.BUY_PE_SIGNAL -> prefs.buyPeSignal
            AlertEventType.ENTRY_POSITION -> prefs.entryPosition
            AlertEventType.STOP_LOSS_HIT -> prefs.stopLossHit
            AlertEventType.TARGET_1_HIT -> prefs.target1Hit
            AlertEventType.TARGET_2_HIT -> prefs.target2Hit
            AlertEventType.TARGET_3_HIT -> prefs.target3Hit
            AlertEventType.TARGET_4_HIT -> prefs.target4Hit
            AlertEventType.TRAILING_SL_HIT -> prefs.trailingSlHit
            AlertEventType.ORDER_EXECUTED -> prefs.orderExecuted
            AlertEventType.ORDER_REJECTED -> prefs.orderRejected
            AlertEventType.ALGO_STARTED -> prefs.algoStarted
            AlertEventType.ALGO_STOPPED -> prefs.algoStopped
            AlertEventType.RISK_LIMIT_REACHED -> prefs.riskLimitReached
        }
    }

    fun getAllPreferences(): AlertPreferences {
        return appPreferences.getAlertPreferences()
    }

    fun saveAllPreferences(preferences: AlertPreferences) {
        appPreferences.saveAlertPreferences(preferences)
    }
}
